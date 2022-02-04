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
        tableName = "circular_reference_child",
        writeCapacityUnit = 5L,
        readCapacityUnit = 10L
)
public class CircularReferenceChild implements IGenericDynamoDbTable {
    @PartitionKey
    public String crChildPartKey = "";

    @ExternalRelation(
            indexName = "circular-reference",
            relationTo = CircularReferenceParent.class,
            relationKeyType = ERelationKeyType.PARTITION_KEY)
    public String crCrReference = "";

}
