package utils;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

public class GeneralIOTest {

    private static final String TEST_FILE = "testresource/test_image.png";

    @Test
    public void TestingEncodeAndDecode() throws IOException {
        System.out.println("[(TEST)::TestingEncodeAndDecode] Encoding");
        String encoded = GeneralIO.FileToBase64(Paths.get(TEST_FILE));
        System.out.println(encoded);
        System.out.println("[(TEST)::TestingEncodeAndDecode] Done encoding");

        System.out.println("[(TEST)::TestingEncodeAndDecode] Decode file from encoded string");
        GeneralIO.base64ToFile(encoded, "test_decoded_re-recoded");
        System.out.println();
        System.out.println("[(TEST)::TestingEncodeAndDecode] Decode file from encoded string from test resource");
        GeneralIO.base64ToFile(
                GeneralIO.readTextFromFile(Paths.get("testresource/test_base64encodedText.txt")),
                "test_decoded_from_resource");
        System.out.println("[(TEST)::TestingEncodeAndDecode] Done testing");
    }
}
