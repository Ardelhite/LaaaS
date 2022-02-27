package awsutil.ses.testmodel;

import lombok.Data;

import java.util.AbstractMap;
import java.util.List;

@Data
public class SesTestCaseModel {
    public String sender;
    public String recipient;
    public String subject;
    public String textBody;
    public String htmlBody;
    public List<String> CarbonCopy;
    public List<String> BlindCarbonCopy;

    public List<AttachmentFilesFromS3> attachmentFilesFromS3;
    public List<AttachmentFilesAsBase64> attachmentFilesAsBase64;
}
