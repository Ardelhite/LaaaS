package samples;

import awsutil.dynamodb.tabledefinition.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(
        tableName = "laaas_relation_test_parent",
        readCapacityUnit = 10L,
        writeCapacityUnit = 5L
)
public class SampleRelationalTable implements IGenericDynamoDbTable {
    @PartitionKey
    public String parentPartitionKey = "";

    @ExternalRelation(
            indexName = "relation-index-name",
            relationTo = SampleRelationalChild.class,
            relationKeyType = ERelationKeyType.GLOBAL_SECONDLY_INDEX)
    public String externalRelationTable = "";
}
