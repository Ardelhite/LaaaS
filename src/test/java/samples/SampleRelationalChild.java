package samples;

import awsutil.dynamodb.tabledefinition.DynamoDBTable;
import awsutil.dynamodb.tabledefinition.GlobalSI;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.PartitionKey;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "laaas_relation_test_child",
        readCapacityUnit = 10L,
        writeCapacityUnit = 5L
)
public class SampleRelationalChild implements IGenericDynamoDbTable {
    @PartitionKey
    public String childParkKey = "";

    @GlobalSI(
            indexName = "relation-index-name",
            keyType = KeyType.HASH)
    public String chValue = "";

    public String chValue2 = "";
}
