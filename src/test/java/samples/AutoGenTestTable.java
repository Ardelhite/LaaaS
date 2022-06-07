package samples;

import awsutil.dynamodb.tabledefinition.DynamoDBTable;
import awsutil.dynamodb.tabledefinition.GlobalSI;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.PartitionKey;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(
        tableName = "autogen-test"
)
public class AutoGenTestTable implements IGenericDynamoDbTable {
    @PartitionKey(
            isAutoGen = true,
            range = 32
    )
    public String pri;

    @GlobalSI(
            indexName = "param",
            keyType = KeyType.HASH
    )
    public String val;
}
