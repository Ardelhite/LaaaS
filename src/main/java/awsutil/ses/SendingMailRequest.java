package awsutil.ses;

import awsutil.s3.S3ObjectModel;
import com.amazonaws.regions.Regions;
import lombok.Data;
import utils.GeneralIO;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@Data
public class SendingMailRequest {

    /**
     * Mandatory field to sending e-mail by AWS SES
     */
    private final String sender;
    private final String recipient;
    private final String subject;
    private final Regions regions;

    /**
     * Set mandatory fields at constructor
     * @param sender "From" address
     * @param recipient "To" address
     * @param subject Subject to sending mail
     * @param regions Using regions that applied both of SES and S3 to be attached files (e.g. Regions.AP_NORTHEAST_1)
     */
    public SendingMailRequest(String sender, String recipient, String subject, Regions regions) {
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.regions = regions;
    }

    private List<String> addressOfCc = new ArrayList<>();
    /**
     * Add address by carbon copy
     * @param ccAddress to settings address by carbon copy
     * @return this
     */
    public SendingMailRequest addAddressAsCarbonCopy(String ccAddress){
        if (ccAddress != null && !ccAddress.isEmpty()) {
            this.addressOfCc.add(ccAddress);
        }
        return this;
    }

    private List<String> addressOfBcc = new ArrayList<>();
    /**
     * add address by blind carbon copy
     * @param bccAddress to setting address by blind carbon copy
     * @return this
     */
    public SendingMailRequest addAddressAsBlindCarbonCopy(String bccAddress) {
        if (bccAddress != null && !bccAddress.isEmpty()) {
            addressOfBcc.add(bccAddress);
        }
        return this;
    }

    private String textBody;
    /**
     * Set body as text
     * @param body of e-mail as text
     * @return this
     */
    public SendingMailRequest setTextBody(String body) {
        this.textBody = body;
        return this;
    }

    private String htmlBody;
    /**
     * Set body as html
     * @param body of e-mail as html
     * @return this
     */
    public SendingMailRequest setHtmlBody(String body) {
        this.htmlBody = body;
        return this;
    }

    private List<S3ObjectModel> attachmentObjectList = new ArrayList<>();
    /**
     * Set to attach files has been exists on S3
     * @param bukkitName S3 bukkit name
     * @param directoryPath S3 directory name
     * @param objectName Attachment file's object name
     * @return this
     * @throws IOException Throws when can not creat the directory as temporally
     */
    public SendingMailRequest addAttachmentFileFromS3(
            String bukkitName, String directoryPath, String objectName) throws IOException {
        attachmentObjectList.add(new S3ObjectModel(bukkitName, directoryPath, objectName, this.regions));
        return this;
    }

    private List<AbstractMap.SimpleEntry<String, Path>> attachmentFilesByBase64 = new ArrayList<>();
    /**
     * Add to attach converted files from base64
     * @param encodedFileAsBase64Str Base64 encoded file as String
     * @param actualFileName Actual file name
     * @return this
     */
    public SendingMailRequest addAttachmentFileAsBase64Str(
            String encodedFileAsBase64Str, String actualFileName) throws IOException {
        attachmentFilesByBase64.add(new AbstractMap.SimpleEntry<>(
                actualFileName,
                GeneralIO.base64ToFile(encodedFileAsBase64Str, GeneralIO.getRandomFileName())));
        return this;
    }

    /**
     * Send email via facade
     * @throws MessagingException Throws when failed to sending the email
     * @throws IOException Throws can not be created the directory temporally
     */
    public void send() throws MessagingException, IOException {
        SesSendingMailFacade.sendingMail(this);
    }
}
