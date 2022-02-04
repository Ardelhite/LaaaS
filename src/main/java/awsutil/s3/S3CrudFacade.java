package awsutil.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class S3CrudFacade {
    final static AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();

    /**
     * Upload file into s3 bucket
     * @param s3ObjectModel to mapping by Java object
     * @param toUploading File to uploading
     * @return Whether be deleted uploaded data
     */
    static public void uploadFile(S3ObjectModel s3ObjectModel, File toUploading) {
        System.out.println("[S3 CrudFacade] Uploading:" + toUploading.getName());
        // Create bucket
        List<Bucket> bucketList = s3.listBuckets();
        if(!bucketList.stream().map(Bucket::getName)
                .collect(Collectors.toSet()).contains(s3ObjectModel.getBucketName()))
        {
            s3.createBucket(s3ObjectModel.getBucketName());
        }

        // Upload to S3
        s3.putObject(s3ObjectModel.getBucketName(), s3ObjectModel.getDirectoryPath() + s3ObjectModel.getObjectName(), toUploading);
    }

    /**
     * Upload file as base64Encoded into S3
     * @param s3ObjectModel to mapping by Java object
     * @param base64EncodedString base64 encoded file
     * @throws IOException throws regarding I/O to internal temporary file
     */
    static public void uploadFile(S3ObjectModel s3ObjectModel, String base64EncodedString) throws IOException {
        // Create temporary file
        Path filePath = Paths.get(s3ObjectModel.getS3temporaryDirectoryPath() + "/" +
                S3CrudFacade.getRandomFileName(s3ObjectModel));
        File tempFile = Files.createFile(filePath).toFile();

        // Decode and write to temp file
        byte[] rowData = Base64.getDecoder().decode(base64EncodedString);
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        outputStream.write(rowData);

        // Put file into S3
        S3CrudFacade.uploadFile(s3ObjectModel, tempFile);
    }

    /**
     * Get object from S3
     * @param s3ObjectModel to mapping by Java object
     * @return Files from S3
     * @throws IOException throws regarding I/O to internal temporary file
     */
    static public File getObject(S3ObjectModel s3ObjectModel) throws IOException {
        // S3 Object
        S3Object s3Object = s3.getObject(s3ObjectModel.getBucketName(),
                s3ObjectModel.getDirectoryPath() + s3ObjectModel.getObjectName());
        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
        // Temporary file
        File downloadedObject = new File(s3ObjectModel.getS3temporaryDirectoryPath() + "/"
                + S3CrudFacade.getRandomFileName( s3ObjectModel));
        FileOutputStream outputStream = new FileOutputStream(downloadedObject);

        // Write to temporary
        byte[] readBuffer = new byte[1024];
        int readLength = 0;
        while ((readLength = s3ObjectInputStream.read(readBuffer)) > 0) {
            outputStream.write(readBuffer, 0, readLength);
        }
        s3ObjectInputStream.close();
        outputStream.close();

        return downloadedObject;
    }

    /**
     * Get object from S3 as Base64 String
     * @param s3ObjectModel to mapping by Java object
     * @return Files from S3
     * @throws IOException throws regarding I/O to internal temporary file
     */
    static public String getBase64EncodedObject(S3ObjectModel s3ObjectModel) throws IOException {
        // Download object from S3
        File temp = S3CrudFacade.getObject(s3ObjectModel);
        // Converting Base64
        byte[] row = Files.readAllBytes(temp.toPath());
        return Base64.getEncoder().encodeToString(row);
    }

    /**
     * Create random file name for temporary
     * @param s3ObjectModel to mapping by Java object
     * @return Generated file name
     */
    static private String getRandomFileName(S3ObjectModel s3ObjectModel) {
        while (true) {
            // Generate file name
            String rowFileName = String.valueOf(System.currentTimeMillis());
            String rowDummyExt = String.valueOf(new SecureRandom().nextInt(2048));
            // Filename = Filename + dummy extension
            String randomFileName = rowFileName + "." + rowDummyExt;
            // Retry if exists same file name temporary directory
            if(!Files.exists(Paths.get(s3ObjectModel.getS3temporaryDirectoryPath() + "/" + randomFileName))) {
                System.out.println("[S3 CrudFacade] Temporary filename:" + randomFileName);
                return randomFileName;
            }
        }
    }
}
