package awsutil.dynamodb;

import awsutil.dynamodb.exceptions.*;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.TableDefinition;
import awsutil.dynamodb.tabledefinition.TableRelation;
import com.amazonaws.services.dynamodbv2.document.Table;

import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import org.junit.Test;
import samples.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing dynamoDB libs
 */
public class DynamoDbTableTest {
    @Test
    public void ddbGeneralTest() {
        System.out.println("[(TEST)::ddbGeneralTest] Start test");
        try {
            // New record
            DynamoDbSampleTableI sample = new DynamoDbSampleTableI(
                    "part01",
                    "sort01",
                    "1978-12-01",
                    1024,
                    new ArrayList<String>(Arrays.asList("t1", "t2")),
                    new SampleNestedTableI().init("nested str", new ArrayList<>(Arrays.asList("n1", "n2"))),
                    "X1"
            );

            System.out.println("[(TEST)::ddbGeneralTest] Creating table for: " + DynamoDbSampleTableI.class.getName());
            // Create table
            Table table = TableCrudFacade.create(DynamoDbSampleTableI.class);
            System.out.println("[(TEST)::ddbGeneralTest] Created: " + table.getTableName());

            // Check result of creating table
            TableDefinition sampleDef = sample.toTableDefinition();
            assertEquals(sampleDef.tableName, table.getTableName());

            try {
                // insert & get inserted data
                IGenericDynamoDbTable result = RecordCrudFacade.insertSingleRecord(sample);
                assertNotNull(result);
                IGenericDynamoDbTable res = RecordCrudFacade.queryByTableKeys(new DynamoDbSampleTableI(
                        "part01",
                        "sort01",
                        null, null, null, null, null
                ));
                assertNotNull(res);
                IGenericDynamoDbTable updated = RecordCrudFacade.updateSingleRecord(new DynamoDbSampleTableI(
                        "part01",
                        "sort01",
                        "2100-12-07",
                        256,
                        new ArrayList<String>(Arrays.asList("Updated1", "updated2", "updated3")),
                        new SampleNestedTableI(
                                "UpdatedNestedField",
                                new ArrayList<String>(Arrays.asList("UpdatedN1", "UpdatedN2"))
                        ),"UPDATED_X1"
                ));
                assertNotNull(updated);
            } catch (InvalidParametersInDynamoDbException | IllegalAccessException |
                    DoesNotExistsFunctionException | InstantiationException e) {
                e.printStackTrace();
            }

            // delete table
            DeleteTableResult result = TableCrudFacade.drop(table);
            assertEquals(sample.getTableName(), result.getTableDescription().getTableName());

            System.out.println("[(TEST)::ddbGeneralTest] Done checking create table and delete table");

        } catch (InvalidParametersInDynamoDbException | InvalidDynamoFieldTypeException | DuplicatedSortKeyException
                | InterruptedException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        System.out.println("[(TEST)::ddbGeneralTest] Done \n\n");
    }

    @Test
    public void mapConvertingTest() throws InvalidDynamoFieldTypeException, DuplicatedSortKeyException, InvalidParametersInDynamoDbException, InterruptedException, InstantiationException {
        System.out.println("[(TEST)::mapConvertingTest] Start test");
        try {
            Table table = TableCrudFacade.create(DynamoDbSampleTableI.class);
            DynamoDbSampleTableI sample = new DynamoDbSampleTableI(
                    "part01",
                    "sort01",
                    "1980-01-01",
                    1, new ArrayList<String>(Arrays.asList("t1", "t2")),
                    new SampleNestedTableI().init("nested str", new ArrayList<>(Arrays.asList("n1", "n2"))),
                    "X1_MAP");
            sample.toMap();
            DynamoDbSampleTableI sample2 = new DynamoDbSampleTableI(
                    "part01",
                    "sort01",
                    "1990-12-01",
                    1, new ArrayList<String>(Arrays.asList("t1", "t2")),
                    null,
                    "X1_MAP");
            sample.toMap();
            RecordCrudFacade.insertSingleRecord(sample2);
            List<IGenericDynamoDbTable> results = RecordCrudFacade.query(new DynamoDbSampleTableI(
                    "part01",
                    "sort01",
                    null, null, null, null,null
            ));
            TableCrudFacade.drop(table);
        } catch (IllegalAccessException | DoesNotExistsFunctionException | ExistsCircularReferenceException e) {
            e.printStackTrace();
        }
        System.out.println("[(TEST)::mapConvertingTest] Done \n\n");
    }

    @Test(expected = ExistsCircularReferenceException.class)
    public void externalRelationTableTest() throws ExistsCircularReferenceException {
        System.out.println("[(TEST)::externalRelationTableTest] Start test");
        SampleRelationalTable rTableParent = new SampleRelationalTable(
                "PART_KEY", "RELATION_KEY"
        );
        SampleRelationalChild rTableChild = new SampleRelationalChild(
                "RELATION_KEY", "ChileValue", "TEST_CHILD"
        );

        try {
            // Create testing tables
            Table child = TableCrudFacade.create(SampleRelationalChild.class);
            RecordCrudFacade.insertSingleRecord(rTableChild);
            Table parent = TableCrudFacade.create(SampleRelationalTable.class);
            RecordCrudFacade.insertSingleRecord(rTableParent);

            List<IGenericDynamoDbTable> multipleResult = RecordCrudFacade.query(rTableParent);
            assertEquals(multipleResult.size(), 2);
            System.out.println("");

            // Delete each tables
            TableCrudFacade.drop(child);
            TableCrudFacade.drop(parent);
        } catch (IllegalAccessException | DoesNotExistsFunctionException | InvalidParametersInDynamoDbException
                | InstantiationException | InvalidDynamoFieldTypeException | DuplicatedSortKeyException
                | InterruptedException e) {
            e.printStackTrace();
        }

        CircularReferenceParent crParent = new CircularReferenceParent(
                "crParentKey", "crRelationKey"
        );
        CircularReferenceChild circularReferenceChild = new CircularReferenceChild(
                "crRelationKey", "crParentKey"
        );

        try {
            List<TableRelation> relations = rTableParent.toRelationTree(null, 0, null);
            List<TableRelation> relationsCr = crParent.toRelationTree(null, 0, null);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        System.out.println("[(TEST)::externalRelationTableTest] Done \n\n");
    }

    @Test
    public void testOfLocalSI() throws InvalidDynamoFieldTypeException, DuplicatedSortKeyException,
            InvalidParametersInDynamoDbException, InterruptedException, InstantiationException,
            IllegalAccessException, DoesNotExistsFunctionException {
        Table table = TableCrudFacade.create(DynamoDbSampleTableI.class);
        System.out.println("[(TEST)::testOfLocalSI] Created: " + table.getTableName());

        DynamoDbSampleTableI sample1 = new DynamoDbSampleTableI(
                "part01",
                "sort02",
                "2000-01-01",
                1024,
                new ArrayList<String>(Arrays.asList("t1", "t2")),
                new SampleNestedTableI().init("nested str", new ArrayList<>(Arrays.asList("n1", "n2"))),
                "X1");
        RecordCrudFacade.insertSingleRecord(sample1);
        DynamoDbSampleTableI sample2 = new DynamoDbSampleTableI(
                "part01",
                "sort02",
                "2000-01-01\"",
                1024,
                new ArrayList<String>(Arrays.asList("t1", "t2")),
                new SampleNestedTableI().init("nested str", new ArrayList<>(Arrays.asList("n1", "n2"))),
                "X1");
        RecordCrudFacade.insertSingleRecord(sample2);
        DynamoDbSampleTableI sample3 = new DynamoDbSampleTableI(
                "part01",
                "sort03",
                "2100-12-07",
                1024,
                new ArrayList<String>(Arrays.asList("t1", "t2")),
                new SampleNestedTableI().init("nested str", new ArrayList<>(Arrays.asList("n1", "n2"))),
                "ALIAS");
        RecordCrudFacade.insertSingleRecord(sample3);
        IGenericDynamoDbTable res = RecordCrudFacade.queryByTableKeys(new DynamoDbSampleTableI(
                "part01",
                "sort01",
                "2000-01-01", null, null, null, ""
        ));
        TableCrudFacade.drop(table);

        System.out.println("[(TEST)::testOfLocalSI] Done \n\n");
    }

    @Test
    public void testOfGlobalSI() throws InvalidDynamoFieldTypeException, DuplicatedSortKeyException, InvalidParametersInDynamoDbException, InterruptedException, InstantiationException, IllegalAccessException, DoesNotExistsFunctionException {
        Table table = TableCrudFacade.create(SimpleGsiTable.class);
        System.out.println("[(TEST)::testOfGlobalSI] Starting test");
        System.out.println("[(TEST)::testOfGlobalSI] Created: " + table.getTableName());

        SimpleGsiTable sample01 = new SimpleGsiTable(
                "part01", "gsi-part01", "gsi-sort01"
        );
        SimpleGsiTable sample02 = new SimpleGsiTable(
                "part02", "gsi-part01", "gsi-sort01"
        );
        RecordCrudFacade.insertSingleRecord(sample01);
        RecordCrudFacade.insertSingleRecord(sample02);
        SimpleGsiTable queryCondition01 = new SimpleGsiTable(
                "part01", "gsi-part01", "gsi-sort01"
        );
        List<IGenericDynamoDbTable> results = RecordCrudFacade.queryByGlobalSecondlyIndex(queryCondition01);
        TableCrudFacade.drop(table);
        System.out.println("[(TEST)::testOfGlobalSI] Done \n\n\n");
    }

    @Test
    public void testOfBooleanTable() throws InvalidDynamoFieldTypeException, DuplicatedSortKeyException,
            InvalidParametersInDynamoDbException, InterruptedException, InstantiationException, IllegalAccessException,
            DoesNotExistsFunctionException, ExistsCircularReferenceException {
        System.out.println("\n\n\n[(TEST)::testOfBooleanTable] Starting test");
        BooleanTable booleanTable = new BooleanTable(
                "testingHash01", false, new ArrayList<Boolean>() {{
                    add(false); add(true); add(false); add(true);
                }}
        );

        Table table = TableCrudFacade.create(BooleanTable.class);
        RecordCrudFacade.insertSingleRecord(booleanTable);
        List<IGenericDynamoDbTable> result = RecordCrudFacade.query(new BooleanTable(
                "testingHash01", null, null));
        System.out.println(result);
        TableCrudFacade.drop(table);

        System.out.println("[(TEST)::testOfGlobalSI] Done \n\n");
    }

    @Test
    public void getExternalTable() throws InvalidDynamoFieldTypeException, DuplicatedSortKeyException,
            InvalidParametersInDynamoDbException, InterruptedException, InstantiationException,
            IllegalAccessException, DoesNotExistsFunctionException, ExistsCircularReferenceException {
        System.out.println("[(TEST)::getExternalTable] Starting test");

        System.out.println("[(TEST)::getExternalTable] Creating tables");
        Table parent = TableCrudFacade.create(GlobalRelationParent.class);
        Table gsiChild = TableCrudFacade.create(GlobalRelationChild.class);
        Table hashChild = TableCrudFacade.create(GlobalRelationHashChild.class);
        System.out.println("\t[(TEST)::getExternalTable] Created: " + parent.getTableName());
        System.out.println("\t[(TEST)::getExternalTable] Created: " + gsiChild.getTableName());
        System.out.println("\t[(TEST)::getExternalTable] Created: " + hashChild.getTableName());

        List<IGenericDynamoDbTable> resultOfInsert = new ArrayList<>();
        // Child as TableKey
        resultOfInsert.add(RecordCrudFacade.insertSingleRecord(new GlobalRelationHashChild(
           "hashPart01", "hashSort01", "value01"
        )));
        resultOfInsert.add(RecordCrudFacade.insertSingleRecord(new GlobalRelationHashChild(
                "hashPart02", "hashSort01", "value02"
        )));

        // Child as GSI
        resultOfInsert.add(RecordCrudFacade.insertSingleRecord(new GlobalRelationChild(
                "gsiHash01", "gsiHash02", "gsiIndexHash01", 0
        )));
        resultOfInsert.add(RecordCrudFacade.insertSingleRecord(new GlobalRelationChild(
                "gsiHash02", "gsiHash02", "gsiIndexHash01", 0
        )));
        resultOfInsert.add(RecordCrudFacade.insertSingleRecord(new GlobalRelationChild(
                "gsiHash02", "gsiHash01", "gsiIndexHash01", 1
        )));

        // Parent
        resultOfInsert.add(RecordCrudFacade.insertSingleRecord(new GlobalRelationParent(
                "parentHash01", "gsiIndexHash01", 0,
                "hashPart01", "hashSort01"
        )));

        System.out.println("[(TEST)::getExternalTable] Done inserting records");
        resultOfInsert.forEach(res -> {
            System.out.println("[(TEST)::getExternalTable] Result : " + res.toString());
        });

        // Execute query from parent
        List<IGenericDynamoDbTable> queryResult = RecordCrudFacade.query(new GlobalRelationParent(
                "parentHash01", null, null, null, null
        ));

        System.out.println("[(TEST)::getExternalTable] Done executing query");
        queryResult.forEach(res -> {
            System.out.println("\t[(TEST)::getExternalTable] Result: " + res.toString());
        });

        TableCrudFacade.drop(hashChild);
        TableCrudFacade.drop(gsiChild);
        TableCrudFacade.drop(parent);
        System.out.println("[(TEST)::getExternalTable] Done \n\n");
    }

    @Test
    public void tableCheck() throws InvalidParametersInDynamoDbException, IllegalAccessException, InvalidDynamoFieldTypeException, DuplicatedSortKeyException, InterruptedException, InstantiationException {
        Table table = TableCrudFacade.create(BooleanTable.class);
        assertEquals(true, TableCrudFacade.isExistsTable(BooleanTable.class));
        TableCrudFacade.drop(table);
        assertEquals(false, TableCrudFacade.isExistsTable(BooleanTable.class));
    }

    @Test
    public void testingIntegerPartitionKey() throws InvalidDynamoFieldTypeException, DuplicatedSortKeyException, InvalidParametersInDynamoDbException, InterruptedException, InstantiationException, IllegalAccessException, DoesNotExistsFunctionException {
        System.out.println("\n\n\n[(TEST)::testingIntegerPartitionKey] Starting test");
        System.out.println("[(TEST)::testingIntegerPartitionKey] Creating table");
        Table table = TableCrudFacade.create(IntegerKeySample.class);
        IGenericDynamoDbTable resultOfInsert = RecordCrudFacade.insertSingleRecord(new IntegerKeySample(
                0, "test"
        ));
        System.out.println("[(TEST)::testingIntegerPartitionKey] Result of inserting : \n" +
                "\t[Partition key] : " + ((IntegerKeySample)resultOfInsert).ikey + "\n" +
                "\t[Value] : " + ((IntegerKeySample)resultOfInsert).var);
        TableCrudFacade.drop(table);
        System.out.println("[(TEST)::testingIntegerPartitionKey] Done \n\n");
    }
}
