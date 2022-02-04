package awsutil.s3;

import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * For DynamoDB Embedded Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectInfoTable implements IGenericDynamoDbTable {
    public String bucketName = "";
    public String directoryPath = "";
    public String objectName = "";
}
