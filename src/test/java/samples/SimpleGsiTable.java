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
        tableName = "sample-gsi-table",
        readCapacityUnit = 2L,
        writeCapacityUnit = 1L
)
public class SimpleGsiTable implements IGenericDynamoDbTable {
    @PartitionKey
    public String id;

    @GlobalSI(
            indexName = "gsi-index-example",
            keyType = KeyType.HASH
    )
    public String gsiHash;

    @GlobalSI(
            indexName = "gsi-index-example",
            keyType = KeyType.RANGE
    )
    public String gsiSort;
}
