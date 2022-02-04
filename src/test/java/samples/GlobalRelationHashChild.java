package samples;

import awsutil.dynamodb.tabledefinition.DynamoDBTable;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.PartitionKey;
import awsutil.dynamodb.tabledefinition.SortKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "global-relation-child-hash",
        readCapacityUnit = 2L,
        writeCapacityUnit = 1L
)
public class GlobalRelationHashChild implements IGenericDynamoDbTable {
    @PartitionKey
    public String hashKey;

    @SortKey
    public String sortKey;

    public String hashChildValue01;
}
