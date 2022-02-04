package awsutil.dynamodb.exceptions;

public class DoesNotExistsFunctionException extends Exception{

    String message;

    public DoesNotExistsFunctionException(String mes) {
        this.message = mes;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
