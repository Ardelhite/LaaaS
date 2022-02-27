package awsutil.ses;

import awsutil.s3.S3CrudFacade;
import awsutil.s3.S3ObjectModel;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * SesSendingMailFacade.java
 * Sending email by AWS SES with Java mail
 */
public class SesSendingMailFacade {
    private static final String SUBJECT_CHAR_CODE = "UTF-8";
    private static final String PLAIN_TEXT_FORMAT = "text/plain; charset=UTF-8";
    private static final String HTML_TEXT_FORMAT = "text/html; charset=UTF-8";

    /**
     * Get clients for each region
     * @param regions AWS region as Region
     * @return AWS SES client
     */
    private static AmazonSimpleEmailService getClient(Regions regions) {
        return AmazonSimpleEmailServiceClientBuilder.standard().withRegion(regions).build();
    }

    /**
     * Sending email via AWS SES
     * @param requestModel request data model
     * @throws MessagingException Throws when failed to sending the email
     * @throws IOException Throws can not be created the directory temporally
     */
    public static void sendingMail(SendingMailRequest requestModel) throws MessagingException, IOException {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        mimeMessage.setSubject(requestModel.getSubject(), SUBJECT_CHAR_CODE);

        // Set addresses
        mimeMessage.setFrom(new InternetAddress(requestModel.getSender()));
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(requestModel.getRecipient()));
        // CC
        for (String cc: requestModel.getAddressOfCc()) {
            mimeMessage.addRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        // BCC
        for (String bcc: requestModel.getAddressOfBcc()) {
            mimeMessage.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
        }

        // Set body
        MimeMultipart msgBody = new MimeMultipart("alternative");
        // Set plain text body
        MimeBodyPart textPart = new MimeBodyPart();
        if (requestModel.getTextBody() != null) {
            textPart.setContent(requestModel.getTextBody(), PLAIN_TEXT_FORMAT);
        } else {
            textPart.setContent("", PLAIN_TEXT_FORMAT);
        }
        msgBody.addBodyPart(textPart);

        // Set html body
        MimeBodyPart htmlPart = new MimeBodyPart();
        if (requestModel.getHtmlBody() != null) {
            htmlPart.setContent(requestModel.getHtmlBody(), HTML_TEXT_FORMAT);
            msgBody.addBodyPart(htmlPart);
        }

        // Converting each parts
        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(msgBody);
        MimeMultipart msg = new MimeMultipart("mixed");
        mimeMessage.setContent(msg);
        msg.addBodyPart(wrap);

        // All to attaching files
        // Pair(File name, File)
        List<AbstractMap.SimpleEntry<String, Path>> toAttachFiles = new ArrayList<>();
        try {
            // Add attachment files from S3
            for (S3ObjectModel s3Object: requestModel.getAttachmentObjectList()) {
                // Download object into temp directory
                Path attachmentObject = S3CrudFacade.getObject(s3Object);
                toAttachFiles.add(new AbstractMap.SimpleEntry<>(s3Object.getObjectName(), attachmentObject));
            }
            // Add attachment files from base64
            toAttachFiles.addAll(requestModel.getAttachmentFilesByBase64());

            // Attach all files into MIME body
            for (AbstractMap.SimpleEntry<String, Path> entry: toAttachFiles) {
                MimeBodyPart att = new MimeBodyPart();
                DataSource dataSource = new FileDataSource(entry.getValue().toFile());
                att.setDataHandler(new DataHandler(dataSource));
                att.setFileName(entry.getKey());
                // Set files into mail
                msg.addBodyPart(att);
            }
            System.out.println("[LAAAS/SES] Created mail and will sending:" +
                    "-------------------------------------------------------");
            mimeMessage.writeTo(System.out);
            System.out.println("-------------------------------------------------------");

            // Output mail as byte
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            // Send email
            SendRawEmailRequest sendingRequest = new SendRawEmailRequest(rawMessage);
            getClient(requestModel.getRegions()).sendRawEmail(sendingRequest);
            System.out.println("[LAAAS/SES] Sent email to: " + requestModel.getRecipient());

        } finally {
            // Deleting temporally files
            for (AbstractMap.SimpleEntry<String, Path> objectAsFile: toAttachFiles) {
                if (objectAsFile.getValue().toFile().exists() && objectAsFile.getValue().toFile().delete()) {
                    System.out.println("[LAAAS/SES] Deleted temporally file: " + objectAsFile.getValue().toFile().getName());
                }
            }
        }
    }
}
