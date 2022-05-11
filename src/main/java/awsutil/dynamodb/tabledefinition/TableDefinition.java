package awsutil.dynamodb.tabledefinition;

import awsutil.dynamodb.exceptions.InvalidParametersInDynamoDbException;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Data class for information of annotation that set in data model class
 */
public class TableDefinition {
    /**
     * For using same table in different environment of applications or systems
     * If exists this env values in current system, All tables name change to name that has this value in head
     */
    static final String bundlerIdentifier = "LAAAS_DDB_BUNDLER_IDENTIFIER";

    public String tableName;
    // AbstractMap.SimpleEntry<Name of partition key, Be set value>
    public AbstractMap.SimpleEntry<String, Object> partitionKey;
    // AbstractMap.SimpleEntry<Name of sort key, Be set value>
    public AbstractMap.SimpleEntry<String, Object> sortKey;
    // List<AbstractMap.SimpleEntry<Name of Local secondly index, Be set value>>
    public List<AbstractMap.SimpleEntry<String, Object>> localSecondlyIndexes = new ArrayList<>();
    // Values of GSI <field name, <KeyType, Value>>
    public List<GsiValueStructure> gsiValue = new ArrayList<>();
    // HashMap<Index name HashMap<KeyType, be set value>>
    public HashMap<String, GlobalSecondlyIndexStructure> gsiStructures = new HashMap<>();
    public Long readCapacityUnit;
    public Long writeCapacityUnit;
    // Class info
    Class<?> modelClass;

    @AllArgsConstructor
    public static class GsiValueStructure {
        public String fieldName;
        public String indexName;
        public Object value;
        public KeyType keyType;
    }

    /**
     * Auto init by data model for dynamoDB table
     * @param dataModel data model for DynamoDB table
     */
    private void initTableDefinition(Class<?> dataModel, IGenericDynamoDbTable instance)
            throws InvalidParametersInDynamoDbException, IllegalAccessException {
        // Init param by arguments
        if(dataModel == null && instance != null) {
            this.modelClass = instance.getClass();
        } else {
            this.modelClass = dataModel;
        }

        if(modelClass != null && modelClass.isAnnotationPresent(DynamoDBTable.class)) {
            // Basic parameters
            String tableNameAsEntity = this.modelClass.getAnnotation(DynamoDBTable.class).tableName();
            this.tableName = getBundlerIdentifier() != null ? getBundlerIdentifier() + tableNameAsEntity : tableNameAsEntity;
            this.readCapacityUnit = this.modelClass.getAnnotation(DynamoDBTable.class).readCapacityUnit();
            this.writeCapacityUnit = this.modelClass.getAnnotation(DynamoDBTable.class).writeCapacityUnit();

            // Field information
            for(Field modelsField: this.modelClass.getDeclaredFields()) {
                // Get field value if exist instance
                Object value = instance != null ? instance.getValueFromFieldName(modelsField.getName()) : null;

                // Checking table keys
                if(modelsField.isAnnotationPresent(PartitionKey.class)) {
                    // Partition Key
                    this.partitionKey = new AbstractMap.SimpleEntry<>(modelsField.getName(), value);

                } else if(modelsField.isAnnotationPresent(SortKey.class)){
                    // Sort key
                    this.sortKey = new AbstractMap.SimpleEntry<>(modelsField.getName(), value);

                }
                // Checking LSI
                if(modelsField.isAnnotationPresent(LocalSI.class)) {
                    // Get Local secondly indexes
                    this.localSecondlyIndexes.add(new AbstractMap.SimpleEntry<>(modelsField.getName(), value));

                }
                // Checking GSI
                if(modelsField.isAnnotationPresent(GlobalSI.class)) {
                    // Get Global secondly indexes
                    String indexName = modelsField.getAnnotation(GlobalSI.class).indexName();
                    KeyType keyType = modelsField.getAnnotation(GlobalSI.class).keyType();
                    long readCapacity = modelsField.getAnnotation(GlobalSI.class).readCapacity();
                    long writeCapacity = modelsField.getAnnotation(GlobalSI.class).writeCapacity();
                    // Set be set value
                    this.gsiValue.add(new GsiValueStructure(
                            modelsField.getName(), indexName, value, keyType
                    ));

                    if(gsiStructures.get(indexName) != null) {
                        // Update structure: Same index name already exists in structure
                        gsiStructures.get(indexName).addNewKey(modelsField.getName(), keyType);
                    } else {
                        // Insert new structure
                        gsiStructures.put(indexName, new GlobalSecondlyIndexStructure(
                                indexName, new ArrayList<AbstractMap.SimpleEntry<String, KeyType>>() {{
                                    add(new AbstractMap.SimpleEntry<>(modelsField.getName(), keyType));
                                }},
                        readCapacity, writeCapacity));
                    }
                }
            }
        } else {
            throw new InvalidParametersInDynamoDbException("DataModels are not annotated");
        }
    }

    /**
     * Checkin whether table kes are set into instance correctly
     * @return is set keys correctly
     */
    public boolean isSetTableKeys() {
        // Checking values
        boolean isExistsPartKey = partitionKey != null && partitionKey.getKey() != null && !partitionKey.getKey().isEmpty();
        boolean isExistsSortKey = sortKey != null && sortKey.getKey() != null && !sortKey.getKey().isEmpty();
        boolean isExistsPartValue = partitionKey != null && partitionKey.getValue() != null && !partitionKey.getValue().toString().isEmpty();
        boolean isExistsSortValue = sortKey != null && sortKey.getValue() != null && !sortKey.getValue().toString().isEmpty();

        // Checking whether is parameter set into pair
        boolean isCorrectPartKey = isExistsPartKey && isExistsPartValue;
        boolean isCorrectSortKey = !isExistsSortKey || isExistsSortValue;

        return isCorrectPartKey && isCorrectSortKey;
    }

    /**
     * Construct with only class info
     * @param dataModel data model class
     * @throws InvalidParametersInDynamoDbException Throws when data model is null or not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws when can not be get field value: This exception is not occurred in this method
     */
    public TableDefinition(Class<? extends IGenericDynamoDbTable> dataModel) throws
            InvalidParametersInDynamoDbException, IllegalAccessException {
        initTableDefinition(dataModel, null);
    }

    /**
     * Construct with instance
     * @param record Generate table info from this
     * @throws InvalidParametersInDynamoDbException Throws when data model is null or not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws when can not be get field value
     */
    public TableDefinition(IGenericDynamoDbTable record) throws
            InvalidParametersInDynamoDbException, IllegalAccessException {
        initTableDefinition(null, record);
    }

    static private String getBundlerIdentifier() {
        return System.getenv(bundlerIdentifier);
    }

    static private String getEnvNameOfBundlerIdentifier() {
        return bundlerIdentifier;
    }
}
