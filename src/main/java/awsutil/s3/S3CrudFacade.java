package awsutil.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import utils.GeneralIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class S3CrudFacade {

    /**
     * Get clients for each region
     * @param regions AWS region as Region
     * @return AWS S3 client
     */
    private static AmazonS3 getS3Client(Regions regions) {
        return AmazonS3ClientBuilder.standard().withRegion(regions).build();
    }

    /**
     * Upload file into s3 bucket
     * @param s3ObjectModel to mapping by Java object
     * @param toUploading File to uploading
     */
    static public void uploadFile(S3ObjectModel s3ObjectModel, File toUploading) {
        System.out.println("[LAAAS/S3::uploadFile] Uploading:" + toUploading.getName());
        // Create bucket
        List<Bucket> bucketList = getS3Client(s3ObjectModel.getRegions()).listBuckets();
        if(!bucketList.stream().map(Bucket::getName)
                .collect(Collectors.toSet()).contains(s3ObjectModel.getBucketName()))
        {
            getS3Client(s3ObjectModel.getRegions()).createBucket(s3ObjectModel.getBucketName());
        }

        // Upload to S3
        getS3Client(s3ObjectModel.getRegions()).putObject(
                s3ObjectModel.getBucketName(),
                s3ObjectModel.getDirectoryPath() + s3ObjectModel.getObjectName(),
                toUploading);
        System.out.println("[LAAAS/S3::uploadFile] Done uploading: " + toUploading.getName());
    }

    /**
     * Upload file as base64Encoded into S3
     * @param s3ObjectModel to mapping by Java object
     * @param base64EncodedString base64 encoded file
     * @throws IOException throws regarding I/O to internal temporary file
     */
    static public void uploadFile(S3ObjectModel s3ObjectModel, String base64EncodedString) throws IOException {
        // Create temporary file
        Path filePath = Paths.get(GeneralIO.getDefaultTempDirPath() + GeneralIO.getRandomFileName());
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
    static public Path getObject(S3ObjectModel s3ObjectModel) throws IOException {
        // S3 Object
        S3Object s3Object = getS3Client(s3ObjectModel.getRegions()).getObject(s3ObjectModel.getBucketName(),
                s3ObjectModel.getDirectoryPath() + s3ObjectModel.getObjectName());
        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
        // Temporary file
        File downloadedObject = new File(GeneralIO.getDefaultTempDirPath() + GeneralIO.getRandomFileName());
        FileOutputStream outputStream = new FileOutputStream(downloadedObject);

        // Write to temporary
        byte[] readBuffer = new byte[1024];
        int readLength = 0;
        while ((readLength = s3ObjectInputStream.read(readBuffer)) > 0) {
            outputStream.write(readBuffer, 0, readLength);
        }
        s3ObjectInputStream.close();
        outputStream.close();

        return downloadedObject.toPath();
    }

    /**
     * Get object from S3 as Base64 String
     * @param s3ObjectModel to mapping by Java object
     * @return Files from S3
     * @throws IOException throws regarding I/O to internal temporary file
     */
    static public String getBase64EncodedObject(S3ObjectModel s3ObjectModel) throws IOException {
        // Download object from S3
        File temp = S3CrudFacade.getObject(s3ObjectModel).toFile();
        // Converting Base64
        byte[] row = Files.readAllBytes(temp.toPath());
        return Base64.getEncoder().encodeToString(row);
    }
}
