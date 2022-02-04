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
        tableName = "global-relation-parent",
        readCapacityUnit = 2L,
        writeCapacityUnit = 1L
)
public class GlobalRelationParent implements IGenericDynamoDbTable {
    @PartitionKey
    public String id = "";

    @ExternalRelation(
            relationTo = GlobalRelationChild.class,
            relationKeyType = ERelationKeyType.GLOBAL_SECONDLY_INDEX,
            gsiKeyType = KeyType.HASH,
            indexName = "global-relation"
    )
    public String gsiChildHashKey;

    @ExternalRelation(
            relationTo = GlobalRelationChild.class,
            relationKeyType = ERelationKeyType.GLOBAL_SECONDLY_INDEX,
            gsiKeyType = KeyType.RANGE,
            indexName = "global-relation"
    )
    public Integer gsiChildSortKey;

    @ExternalRelation(
            relationTo = GlobalRelationHashChild.class,
            relationKeyType = ERelationKeyType.PARTITION_KEY
    )
    public String childHashKey;

    @ExternalRelation(
            relationTo = GlobalRelationHashChild.class,
            relationKeyType = ERelationKeyType.SORT_KEY
    )
    public String childSortKey;
}
