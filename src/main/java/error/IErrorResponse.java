package error;

public interface IErrorResponse<C> {

    public IErrorResponse<?> setErrorMsg(String msg);

    public C getErrorResponse();
}
