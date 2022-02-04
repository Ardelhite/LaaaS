package samples;

import awsutil.dynamodb.tabledefinition.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "circular_reference_parent",
        writeCapacityUnit = 5L,
        readCapacityUnit = 10L
)
public class CircularReferenceParent implements IGenericDynamoDbTable {
    @PartitionKey
    public String crParentPartKey = "";

    @ExternalRelation(
            indexName = "circular-reference",
            relationTo = CircularReferenceChild.class,
            relationKeyType = ERelationKeyType.PARTITION_KEY)
    public String crExternalRelation = "";
}
