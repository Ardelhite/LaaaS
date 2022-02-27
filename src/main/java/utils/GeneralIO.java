package utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class GeneralIO {
    static final private String DEFAULT_TEMP_DIR = "/tmp/LaaaSTempDir/";
    static final private String ENV_NAME_TEMP_DIR = "LAAAS-TEMP-DIR";
    /**
     * Encode File to encoded strings
     * @param path Path to encoding file
     * @return Base64 encoded string from file
     * @throws IOException Throws when could not read the target file
     */
    public static String FileToBase64(Path path) throws IOException {
        System.out.println("[LAAAS/Base64Util]::FileToBase64:" + path.toFile().getName() + "@" + path.toFile().toPath());
        return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    /**
     * Decode base64 strings to File
     * @param base64EncodedString base64
     * @param fileName to saving
     * @return Path of saved file
     * @throws IOException Throws when failed to save the decoded file
     */
    public static Path base64ToFile(String base64EncodedString, String fileName) throws IOException {
        System.out.println("[LAAAS/Base64Util]::Base64ToFile: "
                + fileName
                + "@" + getDefaultTempDirPath() + fileName);
        return Files.write(
                Paths.get(getDefaultTempDirPath() + fileName),
                Base64.getDecoder().decode(base64EncodedString));
    }

    /**
     * Get text data from file
     * @param path to reading
     * @return Read path
     * @throws FileNotFoundException Throws when could not find the specified file
     * @throws IOException Throws when error has be occurred at read the file
     */
    public static String readTextFromFile(Path path) throws FileNotFoundException, IOException {
        StringBuilder outputted = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toAbsolutePath().toString()))) {
            String text;
            while ((text = br.readLine()) != null) {
                outputted.append(text);
            }
        }
        return outputted.toString();
    }

    /**
     * Get default directory to using as temporally
     * @return path of dir as String
     */
    public static Path getDefaultTempDirPath() {
        String tempDir = System.getenv(ENV_NAME_TEMP_DIR);
        return Paths.get(tempDir == null || tempDir.isEmpty() ? DEFAULT_TEMP_DIR :
                // Set separater if that is not set in tail
                tempDir.charAt(tempDir.length() - 1) != '/' ? tempDir + "/" : tempDir);
    }

    /**
     * Get random file name to saving temporally file
     * @return generated file name
     */
    public static String getRandomFileName() {
        while (true) {
            // Generate file name
            String rowFileName = String.valueOf(System.currentTimeMillis());
            String rowDummyExt = String.valueOf(new SecureRandom().nextInt(2048));
            // Filename = Filename + dummy extension
            String randomFileName = rowFileName + "." + rowDummyExt;
            // Retry if exists same file name temporary directory
            if(!Files.exists(Paths.get(getDefaultTempDirPath() + randomFileName))) {
                System.out.println("[S3 CrudFacade] Temporary filename:" + randomFileName);
                return randomFileName;
            }
        }
    }
}
