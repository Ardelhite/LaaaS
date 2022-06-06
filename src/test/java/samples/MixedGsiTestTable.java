package samples;

import awsutil.dynamodb.tabledefinition.*;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "mixed-gsi-table"
)
public class MixedGsiTestTable implements IGenericDynamoDbTable{
    @PartitionKey
    @GlobalSI(
            indexName = "primary-gsi",
            keyType = KeyType.HASH
    )
    public String partKey;
    @SortKey
    @GlobalSI(
            indexName = "primary-gsi",
            keyType = KeyType.RANGE
    )
    public String sortKey;
}
