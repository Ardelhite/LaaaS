package awsutil.dynamodb.tabledefinition;

import awsutil.dynamodb.exceptions.DoesNotExistsFunctionException;
import awsutil.dynamodb.exceptions.ExistsCircularReferenceException;
import awsutil.dynamodb.exceptions.InvalidParametersInDynamoDbException;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public interface IGenericDynamoDbTable {

    /**
     * Create list of relation between each table as tree
     * When this method find circular reference, throw exception
     * @param primaryList be passed info of all parent if exist parent
     * @param tableRank be passed rank# if exist parent. root table is 0
     * @param parentTable parent obj that is passed above info
     * @return updated relation info
     * @throws InstantiationException Throws when can not create new child instance by newInstance to execute this recursively
     * @throws IllegalAccessException Same as above
     * @throws ExistsCircularReferenceException Throws when this method found circular reference
     */
    default List<TableRelation> toRelationTree(List<TableRelation> primaryList,
                                               Integer tableRank, Class<? extends IGenericDynamoDbTable> parentTable)
            throws InstantiationException, IllegalAccessException, ExistsCircularReferenceException {
        // Init relational info for this table
        TableRelation currentTableInfo = new TableRelation(tableRank != null ? tableRank: 0, this.getClass());
        currentTableInfo.rankOfTable = tableRank;

        // At root table, Init primary list
        List<TableRelation> listOfRelation = primaryList == null || primaryList.isEmpty() ? new ArrayList<>(): primaryList;
        // Add parent
        if(parentTable != null) {
            currentTableInfo.parentTables.add(parentTable);
        }
        listOfRelation.add(currentTableInfo);

        // inquiry current table for finding child table
        // If exists children, Pass family tree to child to updating
        for(Field field: this.getClass().getDeclaredFields()) {

            if(field.isAnnotationPresent(ExternalRelation.class)) {
                // If child table does not exist on list of THIS parent, register child table
                // Checking parent whether this contains child of this table
                Class<?> child  = field.getAnnotation(ExternalRelation.class).relationTo();

                // Inquiry parent whether be not contain same class as child
                List<TableRelation>listOfParent = new ArrayList<>();
                for(int rank = currentTableInfo.rankOfTable - 1; rank >= 0; rank--) {
                    listOfParent.addAll(listOfRelation.stream().filter(
                            p -> p.typeOfTable == child
                    ).collect(Collectors.toList()));
                }

                // If this find circular reference, throw exception
                if(!listOfParent.isEmpty()) {
                    // throw Exception
                    throw new ExistsCircularReferenceException(listOfParent, this.getClass(), tableRank);
                } else {
                    listOfRelation = (((IGenericDynamoDbTable) child.newInstance()).toRelationTree(
                            listOfRelation, currentTableInfo.rankOfTable + 1, this.getClass()
                    ));
                }
            }
        }
        return listOfRelation;
    }

    /**
     * Generate external relation info
     * @return external relation info of this table
     * @throws IllegalAccessException Throws when can not create new child instance by newInstance to execute this recursively
     * @throws InvalidParametersInDynamoDbException Invalid type are set in Table model
     * @throws InstantiationException Throws when can not create new child instance by newInstance to execute this recursively
     */
    default List<ExternalRelationDetails> getExternalRelationDetails()
            throws IllegalAccessException, InvalidParametersInDynamoDbException, InstantiationException {
        // All relationship info
        List<ExternalRelationDetails> relationDetails = new ArrayList<>();
        // For ignoring checking same child class
        List<Class<? extends IGenericDynamoDbTable>> checkedClass = new ArrayList<>();
        for(Field field: this.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(ExternalRelation.class))
            {
                // Add relation info
                relationDetails.add(new ExternalRelationDetails(
                        field.getAnnotation(ExternalRelation.class).relationKeyType(),
                        field.getAnnotation(ExternalRelation.class).indexName(),
                        field.getAnnotation(ExternalRelation.class).gsiKeyType(),
                        field.getAnnotation(ExternalRelation.class).relationTo(),
                        field.getAnnotation(ExternalRelation.class).relationTo().newInstance().getTableName(),
                        field.get(this)
                ));
            }
        }
        return relationDetails;
    }

    /**
     * Create new instance to query relation table from this instance ( This should be result of query )
     * @return Queries for relation tables
     * @throws InvalidParametersInDynamoDbException Throws when table model has not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws when can not be get field value
     * @throws InstantiationException Throws when can not create new table instance to inserting result
     */
    default List<IGenericDynamoDbTable> issueNewQueryToRelation()
            throws InvalidParametersInDynamoDbException, IllegalAccessException, InstantiationException {
        System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] Start creating new query - Current table is:"
                + this.getTableName());
        // Extracted relation info from this instance
        List<ExternalRelationDetails> externalRelationDetails = this.getExternalRelationDetails();
        // Relational child tables
        Set<Class<? extends IGenericDynamoDbTable>> childrenTables = externalRelationDetails.stream().map(
                detail -> detail.destinationClass
        ).collect(Collectors.toSet());
        System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] Relation ship:" + childrenTables);

        // All queries for each children tables
        List<IGenericDynamoDbTable> queries = new ArrayList<>();

        // Set conditions into child table
        for(Class<? extends IGenericDynamoDbTable> childTable: childrenTables) {
            // Get conditions for child table
            List<ExternalRelationDetails> conditions = externalRelationDetails.stream().filter(
                    details -> details.destinationClass == childTable
            ).collect(Collectors.toList());

            // Set conditions into fields from result of parent
            IGenericDynamoDbTable queryCondition = childTable.newInstance();
            for(ExternalRelationDetails details: conditions) {
                System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] Class of child:" + details.destinationClass);
                System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] Child table:" + details.destinationTableName);
                System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] KeyType:" + details.keyType);

                // Set condition into field
                switch (details.keyType) {
                    case PARTITION_KEY:
                        System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] ======== RELATION TYPE: PARTITION KEY ========");
                        List<Field> partitionKeyField = Arrays.stream(queryCondition.getClass().getDeclaredFields()).filter(
                                field -> field.isAnnotationPresent(PartitionKey.class)
                        ).collect(Collectors.toList());
                        if(partitionKeyField.size() == 1) {
                            System.out.println(">>> CONDITION OF PARTITION KEY <<< \n" +
                                    "Value: " + details.expectedObjectBetweenEachTables);
                            partitionKeyField.get(0).set(queryCondition, details.expectedObjectBetweenEachTables);
                        }
                        break;

                    case SORT_KEY:
                        System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] ======== RELATION TYPE: SORT KEY ========");
                        List<Field> sortKeyField = Arrays.stream(queryCondition.getClass().getDeclaredFields()).filter(
                                field -> field.isAnnotationPresent(SortKey.class)
                        ).collect(Collectors.toList());
                        if(sortKeyField.size() == 1) {
                            System.out.println(">>> CONDITION OF SORT KEY <<< \n" +
                                    "Value: " + details.expectedObjectBetweenEachTables);
                            sortKeyField.get(0).set(queryCondition, details.expectedObjectBetweenEachTables);
                        }
                        break;

                    case GLOBAL_SECONDLY_INDEX:
                        System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] ======== RELATION TYPE: GSI ========");
                        List<Field> gsiField = Arrays.stream(queryCondition.getClass().getDeclaredFields()).filter(
                                field -> field.isAnnotationPresent(GlobalSI.class)
                                        && field.getAnnotation(GlobalSI.class).indexName().equals(details.indexName)
                                        && field.getAnnotation(GlobalSI.class).keyType() == details.relationKeyType
                        ).collect(Collectors.toList());
                        if(gsiField.size() == 1) {
                            Field field = gsiField.get(0);
                            System.out.println(">>> CONDITION OF GSI RELATIONSHIP <<< \n" +
                                    "Index name: " + field.getAnnotation(GlobalSI.class).indexName() +
                                    "GSI key type: " + field.getAnnotation(GlobalSI.class).keyType() +
                                    "Value: " + details.expectedObjectBetweenEachTables);
                            field.set(queryCondition, details.expectedObjectBetweenEachTables);
                        }
                        break;

                    case LOCAL_SECONDLY_INDEX:
                        System.out.println("[LAAAS/DDB(IGenDDBTable - issue new query)] ======== RELATION TYPE: LSI ========");
                        List<Field> lsiField = Arrays.stream(queryCondition.getClass().getDeclaredFields()).filter(
                                field -> field.isAnnotationPresent(LocalSI.class)
                        ).collect(Collectors.toList());
                        if(lsiField.size() == 1) {
                            Field field = lsiField.get(0);
                            System.out.println(">>> CONDITION OF LSI RELATIONSHIP <<<\n" +
                                    "Index name: " + field.getAnnotation(LocalSI.class).indexName() +
                                    "Value: " + details.expectedObjectBetweenEachTables);
                            field.set(queryCondition, details.expectedObjectBetweenEachTables);
                        }
                        break;
                }
            }
            queries.add(queryCondition);
        }
        return queries;
    }

    /**
     * Convert table to Item to writing into DynamoDB table
     * @return Items for this model and external child tables
     * Map ( TableName, record as Item to insert )
     *
     * @throws InvalidParametersInDynamoDbException Throws when table model has not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws when can not be get field value
     * @throws InstantiationException Throws when can not create new table instance to inserting result
     * @throws DoesNotExistsFunctionException Could not find the corresponding function to specific column type
     */
    default AbstractMap.SimpleEntry<String, Item> toItem()
            throws InvalidParametersInDynamoDbException, IllegalAccessException,
            InstantiationException, DoesNotExistsFunctionException {

        // Converted items by this table to writing into table
        Item record = new Item();

        // Convert field to Item to inserting into DynamoDB table
        // T: AbstractMap.SimpleEntry(Field name, Value)
        // R: HashMap(Field name, Item)
        List<FlattenFunctionPoint<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Item>>> functions = new ArrayList<>();
        // String
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(0,0)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 0,0 - String : " + arg.toString());
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withString(
                                arg.getKey(), arg.getValue() != null? arg.getValue().toString(): ""
                        ));
                    }};
                }
        ));
        // Integer
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(1,0)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 1,0 - Integer : " + arg.toString());
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withNumber(
                                arg.getKey(), arg.getValue() != null? ((Integer) arg.getValue()): 0
                        ));
                    }};
                }
        ));
        // Boolean
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(3,0)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 3,0 - Boolean : " + arg.toString());
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withBoolean(
                                arg.getKey(), arg.getValue() != null? ((Boolean) arg.getValue()): false
                        ));
                    }};
                }
        ));
        // Embedded
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(2,0)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 2,0 - Embedded : " + arg.toString());
                    if(arg.getValue() != null) {
                        return new HashMap<String, Item>() {{
                            try {
                                put(arg.getKey(), record.withMap(arg.getKey(), ((IGenericDynamoDbTable) arg.getValue()).toMap()));
                            } catch (IllegalAccessException | DoesNotExistsFunctionException e) {
                                e.printStackTrace();
                            }
                        }};
                    } else {
                        return null;
                    }
                }
        ));
        // ArrayList (String)
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(0,1)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 0,1- ArrayList(String) : " + arg.toString());
                    // * Type of array list is identified by FieldIdentifier
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withList(arg.getKey(), ((List<String>) arg.getValue())));
                    }};
                }
        ));
        // ArrayList (Integer)
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(1,1)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 1,1 - ArrayList(Integer) : " + arg.toString());
                    // * Type of array list is identified by FieldIdentifier
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withList(arg.getKey(), ((List<Integer>) arg.getValue())));
                    }};
                }
        ));
        // ArrayList (Boolean)
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(3,1)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 3,1 - ArrayList(Boolean) : " + arg.toString());
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withList(arg.getKey(), ((List<Boolean>) arg.getValue())));
                    }};
                }
        ));
        // ArrayList (Embedded)
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(2,1)); }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] 2,1 - ArrayList(Embedded) : " + arg.toString());
                    if(arg.getValue() != null && !Arrays.asList((Object[]) arg.getValue()).isEmpty()) {
                        // Convert to list of embedded object
                        List<Object> objList = Arrays.asList((Object[]) arg.getValue()).stream().map(
                                obj -> {
                                    try {
                                        return ((IGenericDynamoDbTable) obj).toMap();
                                    } catch (IllegalAccessException | DoesNotExistsFunctionException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                        ).collect(Collectors.toList());
                        return new HashMap<String, Item>() {{
                            put(arg.getKey(), record.withList(arg.getKey(), objList));
                        }};
                    }
                    return null;
                }
        ));
        // PartitionKey
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{
                    add(new AbstractMap.SimpleEntry<>(0,3)); add(new AbstractMap.SimpleEntry<>(1,3));
                }},
                // Function
                arg -> {
                    System.out.println("[ TABLE MODEL -> ITEM ] (0,3) & (1,3) - PartitionKey(Integer&String) : " + arg.toString());
                    return new HashMap<String, Item>() {{
                        put(arg.getKey(), record.withPrimaryKey(arg.getKey(), arg.getValue()));
                    }};
                }
        ));

        // Generate Item from this table
        for(Field field: this.getClass().getDeclaredFields()) {
            FieldIdentifier<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Item>> identifier =
                    new FieldIdentifier<>(field, this, functions);
            // Get function by field type
            FlattenFunctionPoint<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Item>> functionPoint = identifier.getFunction();
            // Add mew item
            functionPoint.biMappedFunction.apply(new AbstractMap.SimpleEntry<>(field.getName(), field.get(this)));
        }

        // Return generated item
        String tableName = this.getTableName();
        return new AbstractMap.SimpleEntry<>(this.getTableName(), record);
    }

    /**
     * For writing record into table
     * Convert and Export model for embedded data structure as Map
     * @return Map for embedded record
     * @throws IllegalAccessException Throws when can not be get field value
     * @throws DoesNotExistsFunctionException Throws at could not find function that is related by field type
     * @throws IllegalArgumentException Throws at wrong instance is set when get field value
     */
    default Map<String, Object> toMap() throws
            IllegalAccessException, DoesNotExistsFunctionException, IllegalArgumentException {

        // Functions for converting
        // AbstractMap.SimpleEntry(Field name, Field value)
        // HashMap (Field name, Converted Field value)
        List<FlattenFunctionPoint<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Object>>> functions = new ArrayList<>();
        // String
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(0,0)); add(new AbstractMap.SimpleEntry<>(0,3)); }},
                // Function
                arg -> {
                    System.out.println("[ MODEL -> MAP ] (0,0) & (0,3) - String　: " + arg.toString());
                    return new HashMap<String, Object>() {{
                        put(arg.getKey(), arg.getValue() != null? arg.getValue().toString(): "");
                    }};
                }
        ));
        // Integer
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(1,0)); add(new AbstractMap.SimpleEntry<>(1,3)); }},
                // Function
                arg -> {
                    System.out.println("[ MODEL -> MAP ] (1,0) & (1,3) - Integer : " + arg.toString());
                    return new HashMap<String, Object>() {{
                        put(arg.getKey(), arg.getValue() != null? ((Integer) arg.getValue()): 0);
                    }};
                }
        ));
        // Boolean
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(3,0)); }},
                // Function
                arg -> {
                    System.out.println("[ MODEL -> MAP ] (3,0) - Boolean : " + arg.toString());
                    return new HashMap<String, Object>() {{
                        put(arg.getKey(), arg.getValue() != null? ((Boolean) arg.getValue()): false);
                    }};
                }
        ));
        // Embedded
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(2,0)); }},
                // Function
                arg -> {
                    System.out.println("[ MODEL -> MAP ] 2,0 - Embedded : " + arg.toString());
                    if(arg.getValue() != null && arg.getValue() instanceof IGenericDynamoDbTable) {
                        return new HashMap<String, Object>() {{
                            try {
                                // Create map recursively
                                put(arg.getKey(), ((IGenericDynamoDbTable) arg.getValue()).toMap());
                            } catch (IllegalAccessException | DoesNotExistsFunctionException e) {
                                e.printStackTrace();
                            }
                        }};
                    }
                    return null;
                }
        ));
        // List (String & Integer & Boolean)
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{
                    add(new AbstractMap.SimpleEntry<>(0,1));
                    add(new AbstractMap.SimpleEntry<>(1,1));
                    add(new AbstractMap.SimpleEntry<>(3,1));
                }},
                // Function
                arg -> {
                    System.out.println("[ MODEL -> MAP ] (0,1) & (1,1) - ArrayList(String,Integer,Boolean) : " + arg.toString());
                    return new HashMap<String, Object>() {{
                        put(arg.getKey(), arg.getValue());
                    }};
                }
        ));
        // List (Embedded)
        functions.add(new FlattenFunctionPoint<>(
                // Coordinate
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(2,1)); }},
                // Function
                arg -> {
                    System.out.println("[ MODEL -> MAP ] 2,1 - ArrayList(Embedded) : " + arg.toString());
                    ArrayList<Object> objList = ((ArrayList<Object>) arg.getValue());

                    if(!objList.isEmpty() && objList.get(0) instanceof IGenericDynamoDbTable) {
                        // Convert elements to map recursively
                        return new HashMap<String, Object>() {{
                            put(arg.getKey(), objList.stream().map(
                                    attr -> {
                                        try {
                                            // Convert child field to map
                                            return ((IGenericDynamoDbTable) attr).toMap();
                                        } catch (IllegalAccessException | DoesNotExistsFunctionException e) {
                                            e.printStackTrace();
                                        }
                                        return null;
                                    }
                            ));
                        }};
                    }
                    return null;
                }
        ));

        // Converted instance field
        Map<String, Object> mappedInstanceField = new HashMap<>();

        // Convert all field to Map
        for(Field field: this.getClass().getDeclaredFields()) {
            // Relation field and function to converting current field
            FieldIdentifier<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Object>> identifier =
                    new FieldIdentifier<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Object>>(field, this, functions);
            // Get function to converting
            FlattenFunctionPoint<AbstractMap.SimpleEntry<String, Object>, HashMap<String, Object>> execFunc = identifier.getFunction();
            // Exec function and get map
            Map<String, Object> filedNameAndObject = execFunc.biMappedFunction.apply(
                    new AbstractMap.SimpleEntry<String, Object>(field.getName(), field.get(this)));
            if (filedNameAndObject != null) {
                mappedInstanceField.putAll(filedNameAndObject);
            }
        }
        return mappedInstanceField;
    }

    /**
     * Insert result into data model ( Table )
     * @param result Got result from query for this table
     * @return this oneself
     * @throws IllegalAccessException Throws when can not create new table instance to inserting result
     * @throws DoesNotExistsFunctionException Does not exist function to converting result
     * @throws InstantiationException Throws when can not create new table instance to inserting result
     */
    default IGenericDynamoDbTable insertResultIntoModel(Map<String, AttributeValue> result) throws IllegalAccessException,
            DoesNotExistsFunctionException, InstantiationException {

        // new instance of response
        IGenericDynamoDbTable resultOfTable = this.getClass().newInstance();

        // Functions for converting
        // AbstractMap.SimpleEntry(Field name, HashMap(Result of query))
        // Object: Converted value
        List<FlattenFunctionPoint<AbstractMap.SimpleEntry<String, Map<String, AttributeValue>>, Object>> functions = new ArrayList<>();

        // String
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(0,0)); add(new AbstractMap.SimpleEntry<>(0,3)); }},
                // Function
                arg -> {
                    System.out.println("[ ATTRIBUTE VALUE -> MODEL ] 0,0 - String : " + arg.toString());
                    String fieldName = arg.getKey();
                    Map<String, AttributeValue> resultOfQuery = arg.getValue();
                    if(resultOfQuery.containsKey(fieldName) && resultOfQuery.get(fieldName).getS() != null) {
                        // If filed name contains in response as key, get value
                        return resultOfQuery.get(fieldName).getS();
                    }
                    return null;
                }
        ));
        // Integer
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{
                    add(new AbstractMap.SimpleEntry<>(1,0)); add(new AbstractMap.SimpleEntry<>(1,3));
                }},
                // Function
                arg -> {
                    System.out.println("[ ATTRIBUTE VALUE -> MODEL ] 1,0 - Integer : " + arg.toString());
                    String fieldName = arg.getKey();
                    Map<String, AttributeValue> resultOfQuery = arg.getValue();
                    if(resultOfQuery.containsKey(fieldName) && resultOfQuery.get(fieldName).getN() != null) {
                        // If filed name contains in response as key, get value
                        return Integer.parseInt(resultOfQuery.get(fieldName).getN());
                    }
                    return null;
                }
        ));
        // Boolean
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(3,0)); }},
                // Function
                arg -> {
                    System.out.println("[ ATTRIBUTE VALUE -> MODEL ] 3,0 - Boolean : " + arg.toString());
                    String fieldName = arg.getKey();
                    Map<String, AttributeValue> resultOfQuery = arg.getValue();
                    if(resultOfQuery.containsKey(fieldName) && resultOfQuery.get(fieldName).getBOOL() != null) {
                        return resultOfQuery.get(fieldName).getBOOL();
                    }
                    return null;
                }
        ));
        // Embedded
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(2,0)); }},
                // Function
                arg -> {
                    System.out.println("[ ATTRIBUTE VALUE -> MODEL ] 2,0 - Embedded : " + arg.toString());
                    String fieldName = arg.getKey();
                    if(arg.getValue() != null) {
                        Map<String, AttributeValue> resultOfQuery = arg.getValue();
                        if(resultOfQuery.containsKey(fieldName) && resultOfQuery.get(fieldName).getM() != null) {
                            // Checking type of this field to creating new instance to inserting result
                            List<Field> currentField = Arrays.stream(resultOfTable.getClass().getDeclaredFields()).filter(
                                    (field -> field.getName().equals(fieldName))
                            ).collect(Collectors.toList());

                            // Insert results into child table recursively
                            if(currentField.size() == 1 && currentField.get(0).isAnnotationPresent(Embedded.class)) {
                                try {
                                    // Check type of field
                                    Object newInstanceFromTypeOfField = currentField.get(0).getType().newInstance();
                                    if(newInstanceFromTypeOfField instanceof IGenericDynamoDbTable) {
                                        // Insert result into created instance as same as child
                                        Map<String, AttributeValue> forChild = resultOfQuery.get(fieldName).getM();
                                        return ((IGenericDynamoDbTable) newInstanceFromTypeOfField).insertResultIntoModel(forChild);
                                    }
                                } catch (InstantiationException | IllegalAccessException | DoesNotExistsFunctionException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        return null;
                    }
                    return null;
                }
        ));
        // ArrayList
        functions.add(new FlattenFunctionPoint<>(
                // Coordinates
                new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>() {{ add(new AbstractMap.SimpleEntry<>(99, 0)); }},
                // Function
                arg -> {
                    System.out.println("[ ATTRIBUTE VALUE -> MODEL ] 99,0 - AllyList<AllType> : " + arg.toString());
                    String fieldName = arg.getKey();
                    Map<String, AttributeValue> resultOfQuery = arg.getValue();

                    if(resultOfQuery.containsKey(fieldName) && resultOfQuery.get(fieldName).getL() != null) {
                        List<AttributeValue> attrList = resultOfQuery.get(fieldName).getL();
                        if(!attrList.isEmpty() && attrList.get(0).getS() != null) {
                            // List of String
                            return attrList.stream().map(AttributeValue::getS).collect(Collectors.toList());

                        } else if(!attrList.isEmpty() && attrList.get(0).getN() != null) {
                            // List of Integer
                            return attrList.stream().map((attr) -> Integer.parseInt(attr.getN())).collect(Collectors.toList());

                        } else if(!attrList.isEmpty() && attrList.get(0).getBOOL() != null) {
                            // List of Boolean
                            return attrList.stream().map(AttributeValue::getBOOL).collect(Collectors.toList());

                        } else if(!attrList.isEmpty() && attrList.get(0).getM() != null) {
                            // List of embedded tables
                            // Checking type of this field to creating new instance to inserting result
                            List<Field> currentField = Arrays.stream(resultOfTable.getClass().getDeclaredFields()).filter(
                                    (field -> field.getName().equals(fieldName))
                            ).collect(Collectors.toList());

                            if(currentField.size() == 1 && currentField.get(0).isAnnotationPresent(ListedEmbedded.class)) {
                                // Class to converting
                                Class<? extends IGenericDynamoDbTable> targetClass =
                                        currentField.get(0).getAnnotation(ListedEmbedded.class).embeddedClass();
                                // Convert map to table model by each object recursively
                                return attrList.stream().map((embObj) -> {
                                    try {
                                        return targetClass.newInstance().insertResultIntoModel(embObj.getM());
                                    } catch (IllegalAccessException | DoesNotExistsFunctionException | InstantiationException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                });
                            }
                        }
                    }
                    return null;
                }
        ));

        // Set results into new instance
        for(Field field: resultOfTable.getClass().getDeclaredFields()) {
            // Result has the field of data model
            FieldIdentifier<AbstractMap.SimpleEntry<String, Map<String, AttributeValue>>, Object> identifier
                    = new FieldIdentifier<>(field, resultOfTable, functions);
            // Get functions to convert model
            FlattenFunctionPoint<AbstractMap.SimpleEntry<String, Map<String, AttributeValue>>, Object> execFunc = identifier.getFunction();
            // Set Converted value
            field.set(resultOfTable, execFunc.biMappedFunction.apply(new AbstractMap.SimpleEntry<>(field.getName(), result)));
        }
        return resultOfTable;
    }

    /**
     * Create table definition data class
     * @return definition of table
     * @throws InvalidParametersInDynamoDbException Throws when table model has not annotated by DynamoDBTable
     * @throws IllegalAccessException Throws when can not create new table instance to inserting result
     * @throws InstantiationException Throws when can not create new table instance to inserting result
     */
    default TableDefinition toTableDefinition()
            throws InvalidParametersInDynamoDbException, IllegalAccessException, InstantiationException {
        return new TableDefinition(this);
    }

    default Object getValueFromFieldName(String fieldName) throws IllegalAccessException {
        for(Field field: this.getClass().getDeclaredFields()) {
            if(field.getName().equals(fieldName)) return field.get(this);
        }
        return null;
    }

    default String getTableName()
            throws InvalidParametersInDynamoDbException, IllegalAccessException, InstantiationException {
        return this.toTableDefinition().tableName;
    }

    /**
     * Eval the same instances are same
     * @param record (or search conditions as instance)
     * @return whether to having same values between this and given instance
     */
    default Boolean isEqualsRecord(IGenericDynamoDbTable record) {
        if (record != null && this.getClass().equals(record.getClass())) {
            // Pair of filed name and value
            HashMap<String, Object> fNameAndValues = new HashMap<>();
            try {
                // Set filed name and value from this
                for (Field field: this.getClass().getDeclaredFields()) {
                    fNameAndValues.put( field.getName(), field.get(this));
                }
                // Eval values by given instance
                for (Field field: record.getClass().getDeclaredFields()) {
                    if (! fNameAndValues.get(field.getName()).equals(field.get(record))) return false;
                }
                // This and given instances are same class and having same values
                return true;
            } catch (IllegalAccessException e) {
                // Throws when model has private field
                throw new RuntimeException(e);
            }
        } else {
            // this instance and arg are different table model
            return false;
        }
    }

    /**
     * Checking whether to existing this instances in given list
     * @param records list of records
     * @return is exists
     */
    default boolean isExistsInList(List<IGenericDynamoDbTable> records) {
        for (IGenericDynamoDbTable record: records) {
            if (record.isEqualsRecord(this)) return true;
        }
        return false;
    }
}
