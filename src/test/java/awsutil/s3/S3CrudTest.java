package awsutil.s3;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class S3CrudTest {
    @Test
    public void uploadTest() throws IOException {
//        System.out.println("[TEST S3CrudTest] uploadTest: Start");
//        // Get encoded test file
//        Path testFile = Paths.get("./testFiles/base64EncodedTest.txt");
//        List<String> encodedFiles = Files.lines(testFile).collect(Collectors.toList());
//
//        S3ObjectInfoTable objectInfoTable = new S3ObjectInfoTable(
//                "laaas-testing-bucket", "dir/dir2/", "testUploadingObject"
//        );
//        S3ObjectModel objectModel = new S3ObjectModel(objectInfoTable);
//        objectModel.uploadFile(encodedFiles.get(0));
//        String base64Encoded = objectModel.getBase64EncodedObjects();
//        assertEquals(encodedFiles.get(0), base64Encoded);
//        System.out.println("[TEST S3CrudTest] uploadTest: Done");
    }
}
