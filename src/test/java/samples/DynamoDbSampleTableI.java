package samples;

import awsutil.dynamodb.tabledefinition.*;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "apigw-saminterpriter-module-test",
        readCapacityUnit = 10L,
        writeCapacityUnit = 5L
)
public class DynamoDbSampleTableI implements IGenericDynamoDbTable {

    @PartitionKey
    public String partKey;

    @SortKey
    public String sortKey;

    @LocalSI(indexName = "date")
    public String date;

    @GlobalSI(
            indexName = "gsi-integer-index",
            keyType = KeyType.HASH)
    public Integer otherAttr01;

    public ArrayList<String> listAttr01;

    @Embedded
    public SampleNestedTableI nestedTable;

    @GlobalSI(
            indexName = "gsi-integer-index",
            keyType = KeyType.RANGE)
    public String additionalField;
}
