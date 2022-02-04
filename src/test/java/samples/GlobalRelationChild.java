package samples;

import awsutil.dynamodb.tabledefinition.*;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "global-relation-child",
        readCapacityUnit = 2L,
        writeCapacityUnit = 1L
)
public class GlobalRelationChild implements IGenericDynamoDbTable {
    @PartitionKey
    public String id = "";

    @SortKey
    public String sortKey = "";

    @GlobalSI(
            indexName = "global-relation",
            keyType = KeyType.HASH
    )
    public String gsiHash = "";

    @GlobalSI(
            indexName = "global-relation",
            keyType = KeyType.RANGE
    )
    public Integer gsiRange = 0;
}
