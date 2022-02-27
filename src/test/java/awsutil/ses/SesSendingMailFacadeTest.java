package awsutil.ses;

import awsutil.ses.testmodel.AttachmentFilesAsBase64;
import awsutil.ses.testmodel.AttachmentFilesFromS3;
import awsutil.ses.testmodel.SesTestCaseModel;
import com.amazonaws.regions.Regions;
import com.google.gson.Gson;
import org.junit.Test;
import utils.GeneralIO;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Paths;

public class SesSendingMailFacadeTest {

//    @Test
//    public void simpleMailSendingTest() throws MessagingException, IOException {
//        System.out.println("[(TEST)::simpleMailSendingTest] Sending e-mail");
//
//        // Convert to entity
//        SesTestCaseModel testCase = new Gson().fromJson(
//                GeneralIO.readTextFromFile(Paths.get("testresource/test_address.json")),
//                SesTestCaseModel.class);
//        System.out.println("[(TEST)::simpleMailSendingTest] Test case is:\n" + testCase.toString());
//        String test = GeneralIO.FileToBase64(Paths.get("testresource/test_image.png"));
//
//        // Sending e-mail
//        System.out.println("[(TEST)::simpleMailSendingTest] Sending email to "
//                + testCase.getRecipient() +" from " + testCase.getSender());
//        SendingMailRequest request = new SendingMailRequest(
//                testCase.getSender(),
//                testCase.getRecipient(),
//                testCase.getSubject(),
//                Regions.AP_NORTHEAST_1
//        );
//        for (String cc: testCase.getCarbonCopy()) {
//            request.addAddressAsCarbonCopy(cc);
//        }
//        for (String bcc: testCase.getBlindCarbonCopy()) {
//            request.addAddressAsBlindCarbonCopy(bcc);
//        }
//        for (AttachmentFilesFromS3 s3Files: testCase.getAttachmentFilesFromS3()) {
//            request.addAttachmentFileFromS3(
//                    s3Files.getBukkitName(),
//                    s3Files.getDirectoryPath(),s3Files.getObjectName()
//            );
//        }
//        for (AttachmentFilesAsBase64 base64File: testCase.getAttachmentFilesAsBase64()) {
//            request.addAttachmentFileAsBase64Str(
//                    GeneralIO.readTextFromFile(Paths.get("testresource/" + base64File.getPhysicalFileName())),
//                    base64File.getSendingFileName()
//            );
//        }
//        request.send();
//
//        System.out.println("[(TEST)::simpleMailSendingTest] Done testing");
//    }
}
