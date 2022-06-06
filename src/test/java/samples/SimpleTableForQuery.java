package samples;

import awsutil.dynamodb.tabledefinition.DynamoDBTable;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.PartitionKey;
import awsutil.dynamodb.tabledefinition.SortKey;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "scan-test"
)
public class SimpleTableForQuery implements IGenericDynamoDbTable {
    @PartitionKey
    public String partKey;
    @SortKey
    public String sortKey;

    public String value;
}
