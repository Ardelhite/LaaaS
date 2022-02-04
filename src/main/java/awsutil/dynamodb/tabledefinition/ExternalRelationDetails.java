package awsutil.dynamodb.tabledefinition;

import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;

/**
 * External relation info of table
 */
@AllArgsConstructor
public class ExternalRelationDetails {
    // Relation key type
    public ERelationKeyType keyType;
    // For LSI or GSI
    public String indexName;
    // For GSI
    public KeyType relationKeyType;
    // Destination table as class
    public Class<? extends IGenericDynamoDbTable> destinationClass;
    // Destination table as name
    public String destinationTableName;
    // Expected value between each tables
    public Object expectedObjectBetweenEachTables;
}
