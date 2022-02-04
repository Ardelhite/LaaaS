package samples;

import awsutil.dynamodb.tabledefinition.DynamoDBTable;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(
        tableName = "test-integer-table"
)
public class IntegerKeySample implements IGenericDynamoDbTable {
    @PartitionKey
    public Integer ikey;

    public String var;
}
