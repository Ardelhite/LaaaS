package awsutil.s3;

import com.amazonaws.regions.Regions;
import lombok.Data;
import utils.GeneralIO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Mapping S3 object between Java object
 */
@Data
public class S3ObjectModel {
    private String bucketName = "";
    private String directoryPath = "";
    private String objectName = "";
    private Regions regions = Regions.DEFAULT_REGION;

    /**
     * [Constructor] Object definitions
     * @param bucketName bucket name
     * @param directoryPath directory path in S3 bucket
     * @param objectName S3 object name
     * @param regions Using region (e.g. Regions.AP_NORTHEAST_1)
     * @throws IOException throws regarding I/O to internal temporary file
     */
    public S3ObjectModel(String bucketName, String directoryPath, String objectName, Regions regions) throws IOException {
        checkingWhetherTemporaryDirectoryExists();

        this.bucketName = bucketName;
        this.directoryPath = directoryPath.equals("/") ? "" : directoryPath;
        this.objectName = objectName;
        this.regions = regions;
    }

    /**
     * [Constructor] Object definitions
     * @param structureInfo Object definition as object to embedded into Dynamo DB
     * @throws IOException throws regarding I/O to internal temporary file
     */
    public S3ObjectModel(S3ObjectInfoTable structureInfo) throws IOException {
        checkingWhetherTemporaryDirectoryExists();

        this.bucketName = structureInfo.bucketName;
        this.directoryPath = structureInfo.directoryPath;
        this.objectName = structureInfo.objectName;
    }

    /**
     * Upload file into s3 bucket
     * @param toUploading File to upload
     */
    public void uploadFile(File toUploading) {
        S3CrudFacade.uploadFile(this, toUploading);
    }

    /**
     * Upload file as encode base64 into bucket
     * @param base64EncodedFile File as string encoded by Base64
     * @throws IOException throws regarding I/O to internal temporary file
     */
    public void uploadFile(String base64EncodedFile) throws IOException {
        S3CrudFacade.uploadFile(this, base64EncodedFile);
    }

    /**
     * Get object as File
     * @return File
     * @throws IOException throws regarding I/O to internal temporary file
     */
    public File getObject() throws IOException {
        return S3CrudFacade.getObject(this).toFile();
    }

    /**
     * Get Object as String which is encoded by base64
     * @return Base64 String
     * @throws IOException throws regarding I/O to internal temporary file
     */
    public String getBase64EncodedObjects() throws IOException {
        return S3CrudFacade.getBase64EncodedObject(this);
    }

    private void checkingWhetherTemporaryDirectoryExists() throws IOException {
        // Create temporary object
        if(!Files.exists(GeneralIO.getDefaultTempDirPath())) {
            Files.createDirectory(GeneralIO.getDefaultTempDirPath());
        }
    }
}
