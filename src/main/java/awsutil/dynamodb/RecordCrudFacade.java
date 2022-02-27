package awsutil.dynamodb;

import awsutil.dynamodb.exceptions.DoesNotExistsFunctionException;
import awsutil.dynamodb.exceptions.ExistsCircularReferenceException;
import awsutil.dynamodb.exceptions.InvalidParametersInDynamoDbException;
import awsutil.dynamodb.tabledefinition.GlobalSecondlyIndexStructure;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.TableDefinition;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CRUD Facade for dynamoDB
 */
public class RecordCrudFacade {

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
    private static final DynamoDB dynamoDB = new DynamoDB(client);

    /**
     * Insert single record into single table
     * @param record to inserting
     * @return result of inserting
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws DoesNotExistsFunctionException Throws when failed create new instance to inserting result
     * @throws AmazonServiceException Throws errors had be occurred in AWS
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     * @throws InstantiationException Throws when failed create new instance to inserting result
     */
    public static IGenericDynamoDbTable insertSingleRecord(IGenericDynamoDbTable record)
            throws IllegalAccessException, DoesNotExistsFunctionException, AmazonServiceException,
            InvalidParametersInDynamoDbException, InstantiationException {
        TableDefinition def = record.toTableDefinition();
        Table table = dynamoDB.getTable(def.tableName);

        if(table != null) {
            table.putItem(record.toItem().getValue());
            // Confirming whether is success inserting
            return RecordCrudFacade.queryByTableKeys(record);
        }
        return null;
    }

    /**
     * Get items from DB
     * @param dataCondition query conditions to searching parent table
     * @return all result that contained relation tables
     * @throws ExistsCircularReferenceException Throws when tables have circular reference between each tables
     * @throws InstantiationException Throws when failed create new instance to inserting result
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     * @throws DoesNotExistsFunctionException Throws when failed create new instance to inserting result
     */
    public static List<IGenericDynamoDbTable> query(IGenericDynamoDbTable dataCondition)
            throws ExistsCircularReferenceException, InstantiationException, IllegalAccessException,
            InvalidParametersInDynamoDbException, DoesNotExistsFunctionException {
        System.out.println("[LAAAS/DDB(QUERY)] \\/====================================================================\\/");
        // Checking whether table has not circular reference
        dataCondition.toRelationTree(null, 0, null);

        // Result of query
        List<IGenericDynamoDbTable> allResults = new ArrayList<>();

        // Get item for current table
        IGenericDynamoDbTable resultOfParent = RecordCrudFacade.queryByTableKeys(dataCondition);
        if(resultOfParent != null) {
            allResults.add(resultOfParent);

            for(IGenericDynamoDbTable query: resultOfParent.issueNewQueryToRelation()) {
                // Search by table keys
                try {
                    IGenericDynamoDbTable resultByTableKey = RecordCrudFacade.queryByTableKeys(query);
                    if(resultByTableKey != null) allResults.add(resultByTableKey);
                } catch (AmazonDynamoDBException e) {
                    // Ignore exception regarding query
                    e.printStackTrace();
                }

                // Search by GSI
                try {
                    List<IGenericDynamoDbTable> resultByGlobalSi = new ArrayList<>(RecordCrudFacade.queryByGlobalSecondlyIndex(query));
                    if(!resultByGlobalSi.isEmpty()) allResults.addAll(resultByGlobalSi);
                } catch (AmazonDynamoDBException e) {
                    // Ignore exception regarding query
                    e.printStackTrace();
                }
            }
        }
        System.out.println("[LAAAS/DDB(QUERY)] /\\=====================================================================/\\");
        return allResults;
    }

    /**
     * Query by table keys ( Partition key and sort key )
     * @param dataCondition query parameter as table model
     * @return result
     * @throws InstantiationException Throws when failed create new instance to inserting result
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws DoesNotExistsFunctionException Throws when failed create new instance to inserting result
     * @throws AmazonServiceException Throws errors had be occurred in AWS
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     */
    public static IGenericDynamoDbTable queryByTableKeys(IGenericDynamoDbTable dataCondition) throws
            InstantiationException, IllegalAccessException, DoesNotExistsFunctionException,
            AmazonServiceException, InvalidParametersInDynamoDbException {
        System.out.println("[LAAAS/DDB(Query by TableKey)] <-------------------------------------------------->");
        System.out.println("[LAAAS/DDB(Query by TableKey)] START QUERY BY TABLE KEYS: " + dataCondition.getTableName());

        TableDefinition def = dataCondition.toTableDefinition();
        if(def.isSetTableKeys()) {
            try {
                GetItemRequest request = new GetItemRequest()
                        .withKey(getTableKeysForCondition(dataCondition))
                        .withTableName(dataCondition.toTableDefinition().tableName);
                System.out.println("[LAAAS/DDB(Query by TableKey)] CONDITIONS " + getTableKeysForCondition(dataCondition).toString());
                GetItemResult result = client.getItem(request);

                System.out.println("[LAAAS/DDB(Query by TableKey)] <-------------------------------------------------->");
                // Return converted result
                return result.getItem() != null ?
                        dataCondition.getClass().newInstance().insertResultIntoModel(result.getItem()) : null;

            } catch (AmazonDynamoDBException e) {
                e.printStackTrace();
            }
        }
        System.out.println("[LAAAS/DDB(Query by TableKey)] SKIP QUERY BY TABLE KEYS");
        System.out.println("[LAAAS/DDB(Query by TableKey)] " + def.tableName + " : CONDITION HAS NO VALUE FOR HASH KEY");
        System.out.println("[LAAAS/DDB(Query by TableKey)] <-------------------------------------------------->");
        return null;
    }

    /**
     *
     * @param condition
     * @return
     * @throws InvalidParametersInDynamoDbException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private static HashMap<String, AttributeValue> getTableKeysForCondition(IGenericDynamoDbTable condition)
            throws InvalidParametersInDynamoDbException, IllegalAccessException, InstantiationException {
        TableDefinition def = condition.toTableDefinition();

        HashMap<String, AttributeValue> keyAndAttributes = new HashMap<>();
        // TODO: to like as Generics
        AttributeValue valueOfPartitionKey = def.partitionKey.getValue().getClass().equals(Integer.class) ?
                new AttributeValue().withN((String.valueOf(def.partitionKey.getValue()))) :
                new AttributeValue((String.valueOf(def.partitionKey.getValue())));
        keyAndAttributes.put(def.partitionKey.getKey(), valueOfPartitionKey);
        if(def.sortKey != null && def.sortKey.getValue() != null) {
            keyAndAttributes.put(def.sortKey.getKey(), new AttributeValue(String.valueOf(def.sortKey.getValue())));
        }
        System.out.println("[LAAAS/DDB(getAttrAndValueByTableKeys)] Table name: " + def.tableName);
        System.out.println("[LAAAS/DDB(getAttrAndValueByTableKeys)] Model class name: " + condition.getClass().getName());
        System.out.println("[LAAAS/DDB(getAttrAndValueByTableKeys)] Query conditions: " + keyAndAttributes);
        return keyAndAttributes;
    }

    /**
     *
     * @param dataCondition
     * @param keysAsCondition
     * @return
     * @throws InvalidParametersInDynamoDbException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private static Get issueGetCursor(IGenericDynamoDbTable dataCondition, HashMap<String, AttributeValue> keysAsCondition)
            throws InvalidParametersInDynamoDbException, IllegalAccessException, InstantiationException {
            return new Get()
                    .withKey(keysAsCondition)
                    .withTableName(dataCondition.toTableDefinition().tableName);
    }

    /**
     * Query by Global secondly index
     * @param dataCondition search condition as same data model
     * @return All result entities as data model
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws InstantiationException Throws when failed create new instance to inserting result
     */

    /**
     *
     * @param dataCondition search condition as same data model
     * @return All result entities as data model
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws InstantiationException Throws when failed create new instance to inserting result
     * @throws DoesNotExistsFunctionException Throws when failed create new instance to inserting result
     */
    public static List<IGenericDynamoDbTable> queryByGlobalSecondlyIndex(IGenericDynamoDbTable dataCondition)
            throws InvalidParametersInDynamoDbException, IllegalAccessException, InstantiationException, DoesNotExistsFunctionException {
        System.out.print("[LAAAS/DDB(Query by GSI)] <-------------------------------------------------->\n");
        System.out.print("[LAAAS/DDB(Query by GSI)]: START QUERY BY GSI: " + dataCondition.getTableName() + "\n");

        // Table definition
        TableDefinition def = dataCondition.toTableDefinition();
        Table table = dynamoDB.getTable(def.tableName);
        // GSI info
        HashMap<String, GlobalSecondlyIndexStructure> gsiStructures = def.gsiStructures;
        List<TableDefinition.GsiValueStructure> gsiValues = def.gsiValue;

        System.out.print("[LAAAS/DDB(Query by GSI)]: QUERY BY GSI - GSI VALUE: " + gsiValues + "\n");

        // Converted entities to returning
        List<IGenericDynamoDbTable> resultEntities = new ArrayList<>();

        if(table != null && gsiStructures != null && !gsiStructures.isEmpty()) {
            // Get value by GSI
            for(String indexName: gsiStructures.keySet()) {
                // Search
                Index index = table.getIndex(indexName);
                if(index != null) {
                    // Be corresponded current GSI indexes GSI Structure
                    GlobalSecondlyIndexStructure gsiKey = gsiStructures.get(indexName);
                    // Be set value into GSI field
                    List<TableDefinition.GsiValueStructure> values = gsiValues.stream().filter(
                            gsiValueStructure -> gsiValueStructure.indexName.equals(indexName)).collect(Collectors.toList());
                    System.out.print("[LAAAS/DDB(Query by GSI)]: All query condition: " + values + "\n");
                    // Value of GSI Hash key
                    List<TableDefinition.GsiValueStructure> hashValue = values.stream().filter(
                            gsiValueStructure -> gsiValueStructure.keyType == KeyType.HASH).collect(Collectors.toList());
                    System.out.print("[LAAAS/DDB(Query by GSI)]: For HASH KEY: " + hashValue + "\n");
                    // Value of GSI Sort key
                    List<TableDefinition.GsiValueStructure> sortValue = values.stream().filter(
                            gsiValueStructure -> gsiValueStructure.keyType == KeyType.RANGE).collect(Collectors.toList());
                    System.out.print("[LAAAS/DDB(Query by GSI)]: For SORT KEY: " + sortValue + "\n");

                    QuerySpec querySpec = new QuerySpec();
                    if(!hashValue.isEmpty() && hashValue.get(0).value != null) {
                        // Request query if be set condition in current GSI
                        if(gsiKey.isOnlyHashKey() || gsiKey.hasSortKey()) {
                            // Set condition for HASH key
                            querySpec.withHashKey(new KeyAttribute(
                                    hashValue.get(0).fieldName, hashValue.get(0).value)
                            );
                            System.out.print("[LAAAS/DDB(Query by GSI)]: Set HASH KEY: " +
                                    hashValue.get(0).value + " INTO " + hashValue.get(0).fieldName + "\n");
                        }
                        if(gsiKey.hasSortKey() && !sortValue.isEmpty() && sortValue.get(0).value != null) {
                            // Set condition for Sort key
                            querySpec.withRangeKeyCondition(new RangeKeyCondition(sortValue.get(0).fieldName)
                                            .eq(sortValue.get(0).value)
                            );
                            System.out.print("[LAAAS/DDB(Query by GSI)]: Set SORT: " +
                                    sortValue.get(0).value + " INTO " + sortValue.get(0).fieldName + "\n");
                        }

                        // Convert result to data model
                        try {
                            for(Page<Item, QueryOutcome> page: index.query(querySpec).pages()) {
                                for(Map<String, AttributeValue> item: page.getLowLevelResult().getQueryResult().getItems()) {
                                    resultEntities.add(dataCondition.getClass().newInstance().insertResultIntoModel(item));
                                    System.out.print("[LAAAS/DDB(Query by GSI)]: ROW RESULT: " + item + "\n");
                                }
                            }
                        } catch (AmazonDynamoDBException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        System.out.print("[LAAAS/DDB(Query by GSI)]: All result by query: " + resultEntities.stream().map(entity -> {
            try {
                return entity.toMap();
            } catch (IllegalAccessException | DoesNotExistsFunctionException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList()) + "\n");
        System.out.print("[LAAAS/DDB(Query by GSI)] <-------------------------------------------------->\n");
        return resultEntities;
    }

    /**
     * Update single table by single record
     * @param record to updating
     * @return Changed record
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws InstantiationException Throws when failed create new instance to inserting result
     * @throws DoesNotExistsFunctionException Throws when failed create new instance to inserting result
     */
    public static IGenericDynamoDbTable updateSingleRecord(IGenericDynamoDbTable record)
            throws InvalidParametersInDynamoDbException, IllegalAccessException,
            InstantiationException, DoesNotExistsFunctionException {
        if(TableCrudFacade.isExistsTable(record.getClass()) && isExistRecordHasSameKey(record)) {
            // Get current record ( Keys as same )
            IGenericDynamoDbTable before = queryByTableKeys(record);
            if(before != null) {
                // Set new record after delete
                RecordCrudFacade.deleteSingleRecord(before);
                return RecordCrudFacade.insertSingleRecord(record);
            }
        }
        return null;
    }

    /**
     * Delete single record
     * @param record to deleting
     * @throws AmazonServiceException Throws errors had be occurred in AWS
     * @throws InvalidParametersInDynamoDbException Throws when data model is not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws: TableDefinition, Creating new instance to inserting result
     * @throws InstantiationException Throws when failed create new instance to inserting result
     * @throws DoesNotExistsFunctionException Throws when failed create new instance to inserting result
     */
    public static DeleteItemResult deleteSingleRecord(IGenericDynamoDbTable record)
            throws AmazonServiceException, InvalidParametersInDynamoDbException,
            IllegalAccessException, InstantiationException, DoesNotExistsFunctionException {
        if(TableCrudFacade.isExistsTable(record.getClass()) && isExistRecordHasSameKey(record)) {
            return client.deleteItem(record.toTableDefinition().tableName, getTableKeysForCondition(record));
        }
        return null;
    }

    public static Boolean isExistRecordHasSameKey(IGenericDynamoDbTable record)
            throws InvalidParametersInDynamoDbException, DoesNotExistsFunctionException,
            InstantiationException, IllegalAccessException {
        return RecordCrudFacade.queryByTableKeys(record) != null;
    }
}