package awsutil.dynamodb.tabledefinition;

import awsutil.dynamodb.exceptions.DoesNotExistsFunctionException;
import enums.LogLevel;
import utils.LogHeader;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

/**
 * Create Map to mapping functions that converting value
 * | String | Integer | Other(Embedded) | Boolean        |
 * +------- + ------- + --------------- + -------------- +
 * | F 0,0 | F 1,0   | F 2,0            | F 3,0          | Single
 * +------- + ------- + --------------- + -------------- +
 * | F 0,1 | F 1,1   | F 2,1            | F 3,1          | ArrayList
 * +------ + -------- + ----------------+ ---------------+
 * | F 0,2 | F 1,2   | F 2,2            | F 3,2          | Set
 * +------ + -------- + ----------------+ ---------------+
 * | F 0,3 | F 1,3   | (DO NOT DEFINE ) | (DO NOT DEFINE)| PrimaryKey
 * +------ + -------- + ----------------+----------------+
 *
 *  Can not identify class type of arrayList (When inserting result into model)
 *  F 99,0
 *
 * @param <T>
 * @param <R>
 */
public class FieldIdentifier<T, R> {

    // Flags at fist time evaluation
    private final HashSet<EFieldType> types = new HashSet<>();
    // HashMap < Coordinates of map, Flag>
    private final HashMap<AbstractMap.SimpleEntry<Integer, Integer>, Boolean> fieldTypeMap = new HashMap<>();
    // HashMap < Coordinates of map, Functions>
    private final List<FlattenFunctionPoint<T, R>> functionMap;

    // For error message
    Object instance;
    Field field;

    /**
     * Set field type as Map and init functions that are executed by created map
     * @param field field in data model instances
     * @param instanceThatContainField instances that contain above field
     * @param functions Be execute functions as map
     * @throws IllegalAccessException throws when can be get field value
     */
    public FieldIdentifier(Field field, Object instanceThatContainField,
                    List<FlattenFunctionPoint<T, R>> functions) throws IllegalAccessException {

        this.instance = instanceThatContainField;
        this.field = field;

        this.functionMap = functions;
        Class<?> type = field.getType();
        if(!isString(type) && !isInteger(type) && !isBoolean(type)) {

            if(isArrayList(type)) {
                // Array List
                if(field.get(instanceThatContainField) != null) {
                    ArrayList<Object> rowList = ((ArrayList<Object>) field.get(instanceThatContainField));
                    if (rowList != null && !rowList.isEmpty()
                            && !isString(rowList.get(0).getClass())
                            && !isInteger(rowList.get(0).getClass())
                            && !isBoolean(rowList.get(0).getClass())
                    ) {
                        // Other type of class
                        isEmbedded(field);
                    } else {
                        // Can not identify type of ArrayList
                        this.fieldTypeMap.put(
                                new AbstractMap.SimpleEntry<>(99, 0), true
                        );
                    }
                } else {
                    // Can not identify type of ArrayList
                    this.fieldTypeMap.put(
                            new AbstractMap.SimpleEntry<>(99, 0), true
                    );
                }

            } else if(isSet(type)) {
                // Set
                if(field.get(instanceThatContainField) != null) {
                    HashSet<Object> hashObj = ((HashSet<Object>) field.get(instanceThatContainField));
                    List<Object> objList = Arrays.asList(hashObj.toArray());

                    if(!objList.isEmpty()
                            && !isString(objList.get(0).getClass())
                            && !isInteger(objList.get(0).getClass())
                            && !isBoolean(objList.get(0).getClass())
                    ) {
                        isEmbedded(field);
                    }
                }

            } else {
                // Other type of class
                // Not annotated class will be ignored
                isEmbedded(field);
            }
        } else {
            isPartitionKey(field);
        }
        convertToFlatMap();
    }

    private boolean isString(Class<?> type) {
        if(type == String.class) {
            this.types.add(EFieldType.STRING);
            return true;
        } else {
            return false;
        }
    }

    private boolean isInteger(Class<?> type) {
        if(type == Integer.class) {
            this.types.add(EFieldType.INTEGER);
            return true;
        } else {
            return false;
        }
    }

    private boolean isBoolean(Class<?> type) {
        if(type == Boolean.class) {
            this.types.add(EFieldType.BOOLEAN);
            return true;
        } else {
            return false;
        }
    }

    private boolean isArrayList(Class<?> type) {
        if(type == ArrayList.class) {
            this.types.add(EFieldType.ARRAY_LIST);
            return true;
        } else {
            return false;
        }
    }

    private boolean isEmbedded(Field field) {
        if(field.isAnnotationPresent(Embedded.class)) {
            this.types.add(EFieldType.EMBEDDED);
            return true;
        } else {
            return false;
        }
    }

    private boolean isSet(Class<?> type) {
        if(type == HashSet.class) {
            this.types.add(EFieldType.SET);
            return true;
        } else {
            return false;
        }
    }

    private boolean isPartitionKey(Field field) {
        if(field.isAnnotationPresent(PartitionKey.class)) {
            this.types.add(EFieldType.PARTITION_KEY);
            return true;
        } else {
            return false;
        }
    }

    private void convertToFlatMap() {
        Integer row = 0;
        if(this.types.contains(EFieldType.ARRAY_LIST)) row = 1;
        else if(this.types.contains(EFieldType.SET)) row = 2;
        else if(this.types.contains(EFieldType.PARTITION_KEY)) row = 3;

        // Only basically type
        List<EFieldType> allEfTypeList = Arrays.asList(
                EFieldType.STRING,
                EFieldType.INTEGER,
                EFieldType.EMBEDDED,
                EFieldType.BOOLEAN);

        // Convert liner list to flatten map
        for(int count = 0; count < allEfTypeList.size(); count++) {
            int column = count;
            this.fieldTypeMap.put(
                    new AbstractMap.SimpleEntry<>(column, row), this.types.contains(allEfTypeList.get(column))
            );
        }
    }

    /**
     * Return function that put in same coordinates
     * @return Function<T,R>
     * @throws DoesNotExistsFunctionException No functions are existed on defined coordinates
     */
    public FlattenFunctionPoint<T, R> getFunction() throws DoesNotExistsFunctionException {
        for(AbstractMap.SimpleEntry<Integer, Integer> flagCoordinates: this.fieldTypeMap.keySet()) {
            for(FlattenFunctionPoint<T, R> functionPoint: this.functionMap) {
                if(this.fieldTypeMap.get(flagCoordinates) &&
                        functionPoint.correspondenceCoordinates.contains(flagCoordinates)) {
                    return functionPoint;
                }
            }
        }
        throw new DoesNotExistsFunctionException(LogHeader.logHeader(this.getClass(), LogLevel.ERROR) +
                "Function has not mapping at <Object>: " +  this.instance.getClass() +
                " <Field>: " + this.field.getName());
    }
}
