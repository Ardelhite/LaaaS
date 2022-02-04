package awsutil.dynamodb.exceptions;

public class InvalidParametersInDynamoDbException extends Exception {

    private final String mes;

    public InvalidParametersInDynamoDbException(String mes) {
        this.mes = mes;
    }

    @Override
    public String getMessage() {
        return this.mes;
    }
}
