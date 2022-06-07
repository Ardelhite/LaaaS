package awsutil.dynamodb.exceptions;

import java.lang.reflect.Field;

public class InvalidDynamoFieldTypeException extends Exception {

    private final Field occurredField;
    private final Class<?> modelClass;

    private String message = "";

    public InvalidDynamoFieldTypeException(Class<?> modelCall, Field field) {
        this.occurredField = field;
        this.modelClass = modelCall;
    }

    public InvalidDynamoFieldTypeException(Class<?> modelCall, Field field, String message) {
        this.occurredField = field;
        this.modelClass = modelCall;
        this.message = message;
    }

    @Override
    public String getMessage() {
        if (message.isEmpty()) {
            return modelClass.getName() + "#" + this.occurredField.getName() + " is invalid type " + this.occurredField.getType();
        } else {
            return "[" + modelClass.getName() + "#" + this.occurredField.getName()+ "]" + message;
        }
    }
}
