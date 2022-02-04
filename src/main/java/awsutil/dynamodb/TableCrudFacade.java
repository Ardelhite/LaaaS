package awsutil.dynamodb;

import awsutil.dynamodb.exceptions.DoesNotExistsFunctionException;
import awsutil.dynamodb.exceptions.DuplicatedSortKeyException;
import awsutil.dynamodb.exceptions.InvalidDynamoFieldTypeException;
import awsutil.dynamodb.exceptions.InvalidParametersInDynamoDbException;
import awsutil.dynamodb.tabledefinition.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import enums.LogLevel;

import utils.LogHeader;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Create and Delete functions for DynamoDB
 * On create table: Create only field that be regarding PartitionKey, SortKey and LSI.
 * Other field are added at inserting data
 */
public class TableCrudFacade {
    static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();

    static DynamoDB dynamoDB = new DynamoDB(client);

    /**
     * Create DynamoDB table into AWS
     * @param tableModel table model to creating table
     * @return Created table info
     * @throws InvalidParametersInDynamoDbException throws when invalid parameters are contained to creating table
     * @throws InvalidDynamoFieldTypeException Other String or Integer has be contained in data model
     * @throws DuplicatedSortKeyException throws when invalid sort key is set
     * @throws InterruptedException throws by waiting for active DynamoDB table
     * @throws InstantiationException Throws when could not creating new instance from table model
     * @throws IllegalAccessException Throws when could not access to data field in table model class
     */
    public static Table create(Class<? extends IGenericDynamoDbTable> tableModel)
            throws InvalidParametersInDynamoDbException, InvalidDynamoFieldTypeException,
            DuplicatedSortKeyException, InterruptedException, InstantiationException, IllegalAccessException {
        // Wrapper to creating request table
        TableDefinition def = new TableDefinition(tableModel);

        if(tableModel.isAnnotationPresent(DynamoDBTable.class)) {
            String partitionKeyName = def.partitionKey.getKey();
            String sortKeyName = def.sortKey != null ? def.sortKey.getKey(): "";

            // Check partition key
            if(def.partitionKey.getKey().isEmpty()) {
                throw new InvalidParametersInDynamoDbException(LogHeader.logHeader("TableCrudFacade", LogLevel.ERROR)
                        + "PartitionKey is empty " + tableModel.getName());
            }

            // Instance for type identifier
            IGenericDynamoDbTable tableInstance = tableModel.newInstance();
            // TypeIdentifier
            List<FlattenFunctionPoint<String, String>> functionPoints = new ArrayList<>();
            functionPoints.add(new FlattenFunctionPoint<>( // String
                    new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{
                        add(new AbstractMap.SimpleEntry<>(0,0)); add(new AbstractMap.SimpleEntry<>(0,3));
                    }},
                    arg -> "S"
            ));
            functionPoints.add(new FlattenFunctionPoint<>( // Integer
                    new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{
                        add(new AbstractMap.SimpleEntry<>(1,0)); add(new AbstractMap.SimpleEntry<>(1,3));
                    }},
                    arg -> "N"
            ));

            // All attribute definition
            List<AttributeDefinition> definitions = new ArrayList<>();
            // Table key schema ( PartitionKey and sortKey)
            List<KeySchemaElement> tableKeySchema = new ArrayList<>();
            // List of LocalSI
            HashMap<String, List<KeySchemaElement>> localSi = new HashMap<>();
            // List of GlobalSI
            HashMap<String, GlobalSecondlyIndexStructure> gsiStructure = new HashMap<>();

            // Set other fields as request
            for(Field field: tableModel.getDeclaredFields()) {
                // Set attribute type by field identifier
                try {
                    FieldIdentifier<String, String> identifier = new FieldIdentifier<>(field, tableInstance, functionPoints);
                    Function<String, String> fieldType = identifier.getFunction().biMappedFunction;

                    if(field.getName().equals(partitionKeyName)) {
                        // Add field attribute
                        if(!definitions.stream().map(AttributeDefinition::getAttributeName)
                                .collect(Collectors.toList()).contains(field.getName())) {
                            definitions.add(new AttributeDefinition()
                                    .withAttributeName(field.getName()).withAttributeType(fieldType.apply("")));
                        }
                        // Set partition key
                        tableKeySchema.add(new KeySchemaElement().withAttributeName(field.getName()).withKeyType(KeyType.HASH));

                    } else if(field.getName().equals(sortKeyName)) {
                        // Add field attribute
                        if(!definitions.stream().map(AttributeDefinition::getAttributeName)
                                .collect(Collectors.toList()).contains(field.getName())) {
                            definitions.add(new AttributeDefinition()
                                    .withAttributeName(field.getName()).withAttributeType(fieldType.apply("")));
                        }
                        // Set sort key
                        tableKeySchema.add(new KeySchemaElement().withAttributeName(field.getName()).withKeyType(KeyType.RANGE));
                    }

                    // Set GSI
                    if(field.isAnnotationPresent(GlobalSI.class)) {
                        // Add field attribute
                        if(!definitions.stream().map(AttributeDefinition::getAttributeName)
                                .collect(Collectors.toList()).contains(field.getName())) {
                            // No exists field as key
                            definitions.add(new AttributeDefinition()
                                    .withAttributeName(field.getName()).withAttributeType(fieldType.apply("")));
                        }
                        // Add field attribute
                        if(gsiStructure.get(field.getAnnotation(GlobalSI.class).indexName()) != null) {
                            // Update structure: Same index name already exists in structure
                            GlobalSI gsiAnnotation = field.getAnnotation(GlobalSI.class);
                            gsiStructure.get(gsiAnnotation.indexName())
                                    .addNewKey(field.getName(), gsiAnnotation.keyType());
                        } else {
                            // Insert new structure
                            GlobalSI gsiAnnotation = field.getAnnotation(GlobalSI.class);
                            gsiStructure.put(gsiAnnotation.indexName(), new GlobalSecondlyIndexStructure(
                                    gsiAnnotation.indexName(),
                                    new ArrayList<AbstractMap.SimpleEntry<String, KeyType>>() {{
                                        add(new AbstractMap.SimpleEntry<>(field.getName(), gsiAnnotation.keyType()));
                                    }},
                                    gsiAnnotation.readCapacity(), gsiAnnotation.writeCapacity()
                            ));
                        }
                    }
                    // Set LSI
                    if(field.isAnnotationPresent(LocalSI.class)) {
                        // Add field attribute
                        if(!definitions.stream().map(AttributeDefinition::getAttributeName)
                                .collect(Collectors.toList()).contains(field.getName())) {
                            // No exists field as key
                            definitions.add(new AttributeDefinition()
                                    .withAttributeName(field.getName()).withAttributeType(fieldType.apply("")));
                        }
                        String indexName = field.getAnnotation(LocalSI.class).indexName();
                        String attributeNAme = field.getName();
                        if(localSi.containsKey(indexName)) {
                            localSi.get(indexName).add(
                                    new KeySchemaElement().withAttributeName(def.partitionKey.getKey()).withKeyType(KeyType.HASH));
                            localSi.get(indexName).add(
                                    new KeySchemaElement().withAttributeName(attributeNAme).withKeyType(KeyType.RANGE));
                        } else {
                            List<KeySchemaElement> keySchemas = new ArrayList<KeySchemaElement>() {{
                                add(new KeySchemaElement().withAttributeName(def.partitionKey.getKey()).withKeyType(KeyType.HASH));
                                add(new KeySchemaElement().withAttributeName(attributeNAme).withKeyType(KeyType.RANGE));
                            }};
                            localSi.put(indexName, keySchemas);
                        }
                    }
                } catch (DoesNotExistsFunctionException exception) {
                    System.out.println("IGNORED FIELD: " + field.getName() + " - " + field.getType());
                }
            }

            // Convert attribute and schema to LSI
            List<LocalSecondaryIndex> localSecondaryIndices = new ArrayList<>();
            localSi.keySet().forEach( indexName -> {
                LocalSecondaryIndex index = new LocalSecondaryIndex().withIndexName(indexName)
                        .withProjection(new Projection().withProjectionType(ProjectionType.ALL));
                index.setKeySchema(localSi.get(indexName));
                localSecondaryIndices.add(index);
            });

            CreateTableRequest request = new CreateTableRequest().withTableName(def.tableName)
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(def.readCapacityUnit)
                            .withWriteCapacityUnits(def.writeCapacityUnit))
                    .withAttributeDefinitions(definitions)
                    .withKeySchema(tableKeySchema.stream()
                            .sorted(Comparator.comparing(KeySchemaElement::getKeyType)).collect(Collectors.toList()));

            // Set GSI
            if(!gsiStructure.isEmpty()) {
                request.setGlobalSecondaryIndexes(gsiStructure.keySet().stream().map(
                        key -> gsiStructure.get(key).toKeySchemeElement()).collect(Collectors.toList()));
            }
            // Set LSI
            if(!localSecondaryIndices.isEmpty()) {
                request.setLocalSecondaryIndexes(localSecondaryIndices);
            }

            // Create dynamoDB table into AWS
            Table table = dynamoDB.createTable(request);
            table.waitForActive();

            return table;

        } else {
            throw new InvalidParametersInDynamoDbException(LogHeader.logHeader("TableCrudFacade", LogLevel.ERROR)
                    + "Invalid data class has be set as DynamoDB Table: " + tableModel.getName());
        }
    }

    /**
     * Set attribute type form field type
     * @param field field in model
     * @return attribute type
     * @throws InvalidDynamoFieldTypeException throws when field type is not String or Integer
     */
    private static ScalarAttributeType setAttrType(Field field, Class<? extends IGenericDynamoDbTable> dataModel)
            throws InvalidDynamoFieldTypeException{
        if(field.getType() == String.class) {
            if(field.isAnnotationPresent(BinaryField.class)) {
                return ScalarAttributeType.B;
            } else {
                return ScalarAttributeType.S;
            }
        } else if(field.getType() == Integer.class) {
            return ScalarAttributeType.N;
        } else {
            throw new InvalidDynamoFieldTypeException(dataModel, field);
        }
    }

    /**
     * Drop table in AWS DynamoDB
     * @param table Table object to deleting table
     * @return result of deleting table
     * @throws InstantiationException Throws when could not creating new instance from table model
     * @throws IllegalAccessException Throws when could not access to data field in table model class
     * @throws InvalidParametersInDynamoDbException throws when invalid parameters are contained to creating table
     * @throws InterruptedException throws by waiting for active DynamoDB table
     */
    public static DeleteTableResult drop(Table table)
            throws InstantiationException, IllegalAccessException, InvalidParametersInDynamoDbException,
            InterruptedException {
        DeleteTableResult result = client.deleteTable(table.getTableName());
        table.waitForDelete();
        return result;
    }

    public static Boolean isExistsTable(Class<? extends IGenericDynamoDbTable> tableModel)
            throws InvalidParametersInDynamoDbException, IllegalAccessException {
        TableDefinition def = new TableDefinition(tableModel);
        try {
            dynamoDB.getTable(def.tableName).describe();
        } catch (ResourceNotFoundException e) {
            return false;
        }
        return true;
    }
}
