package awsutil.dynamodb.exceptions;

import java.lang.reflect.Field;

public class InvalidDynamoFieldTypeException extends Exception {

    Field occurredField;
    Class<?> modelClass;

    public InvalidDynamoFieldTypeException(Class<?> modelCall, Field field) {
        this.occurredField = field;
        this.modelClass = modelCall;
    }

    @Override
    public String getMessage() {
        return modelClass.getName() + "#" + this.occurredField.getName() + " is invalid type " + this.occurredField.getType();
    }
}
