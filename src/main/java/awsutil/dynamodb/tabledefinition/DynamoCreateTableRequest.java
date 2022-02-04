package awsutil.dynamodb.tabledefinition;

import awsutil.dynamodb.exceptions.DuplicatedSortKeyException;
import awsutil.dynamodb.exceptions.InvalidParametersInDynamoDbException;
import com.amazonaws.services.dynamodbv2.model.*;
import enums.LogLevel;
import utils.LogHeader;

import java.util.ArrayList;

public class DynamoCreateTableRequest {

    private final KeySchemaElement partitionKey;
    private KeySchemaElement sortKey = null;

    // Create table request
    private final CreateTableRequest request = new CreateTableRequest();

    // All attributes
    private final ArrayList<AttributeDefinition> attrs = new ArrayList<>();

    // All local secondary index
    private final ArrayList<LocalSecondaryIndex> lsis = new ArrayList<>();

    private final String tableName;

    /**
     * PartitionKey is set in constructor
     * (KeyType is HASH only)
     * @param tableName table name
     * @param partitionKeyName Partition key name (Primary key)
     * @param partitionKeyFieldType Type of Partition key
     * @throws InvalidParametersInDynamoDbException throws when invalid parameters are contained to creating table
     */
    public DynamoCreateTableRequest(String tableName,
                                    String partitionKeyName, ScalarAttributeType partitionKeyFieldType
    ) throws InvalidParametersInDynamoDbException {
        if(tableName == null || tableName.isEmpty()) {
           throw new InvalidParametersInDynamoDbException(LogHeader.logHeader(this.getClass(), LogLevel.ERROR) +
                   "Invalid table name is set as null or empty on create table");
        }
        this.tableName = tableName;
        // Set field as attribute
        this.attrs.add(createAttribute(partitionKeyName, partitionKeyFieldType));
        // Set field as partition key
        this.partitionKey = new KeySchemaElement()
                .withAttributeName(partitionKeyName)
                .withKeyType(KeyType.HASH);
    }

    /**
     * Set SortKey
     * @param sortKeyName Name of sort key
     * @return This oneself
     * @throws DuplicatedSortKeyException Throws if already sort key is registered in this instance
     */
    public DynamoCreateTableRequest setSortKey(String sortKeyName, ScalarAttributeType sortKeyFieldType)
            throws DuplicatedSortKeyException {
        if(this.sortKey == null) {
            // Set field as attribute
            AttributeDefinition attr = createAttribute(sortKeyName, sortKeyFieldType);
            if(!this.attrs.contains(attr)) this.attrs.add(attr);
            // Set field as sort key
            this.sortKey = new KeySchemaElement()
                    .withAttributeName(sortKeyName)
                    .withKeyType(KeyType.RANGE);
        } else {
            throw new DuplicatedSortKeyException(this.sortKey.getAttributeName(), sortKeyName);
        }
        return this;
    }

    /**
     * Set Local secondary index
     * @param lsiAttributeName Attribute name
     * @param lsiIndexName Index name
     * @param attributeType Attribute type
     * @return This oneself
     */
    public DynamoCreateTableRequest setLocalSecondaryIndex(
            String lsiAttributeName, String lsiIndexName, ScalarAttributeType attributeType) {
        // set field as attribute
        AttributeDefinition attr = createAttribute(lsiAttributeName, attributeType);
        // set field as Key schema
        if(!this.attrs.contains(attr)) this.attrs.add(attr);
        this.lsis.add(new LocalSecondaryIndex().withIndexName(lsiIndexName).withKeySchema(
                partitionKey, new KeySchemaElement().withAttributeName(lsiAttributeName).withKeyType(KeyType.RANGE)
        ).withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY)));
        return this;
    }

    /**
     * Set general attribute field
     * @param attributeName attribute name
     * @param attributeType attribute type
     * @return this oneself
     */
    public DynamoCreateTableRequest setAttribute(String attributeName, ScalarAttributeType attributeType) {
        this.attrs.add(createAttribute(attributeName, attributeType));
        return this;
    }

    /**
     * Build request
     * @param readCapacityUnit read capacity unit
     * @param writeCapacityUnit write capacity unit
     * @return request to creating dynamoDB table
     */
    public CreateTableRequest build(long readCapacityUnit, long writeCapacityUnit) {
        // Reduce all key scheme elements
        ArrayList<KeySchemaElement> keySchemas = new ArrayList<>();
        keySchemas.add(partitionKey);
        if(sortKey != null) keySchemas.add(sortKey);

        // Init create request
        this.request.withTableName(this.tableName).withKeySchema(keySchemas)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(readCapacityUnit).withWriteCapacityUnits(writeCapacityUnit));
        // Set LSI
        if(!this.lsis.isEmpty()) request.setLocalSecondaryIndexes(this.lsis);
        // Set attributes
        request.setAttributeDefinitions(this.attrs);
        return this.request;
    }

    /**
     * Set new attribute
     * @param fieldName filed name for new attribute
     * @param type attribute type
     * @return AttributeDefinition
     */
    private AttributeDefinition createAttribute(String fieldName, ScalarAttributeType type) {
        return new AttributeDefinition(fieldName, type);
    }
}
