package error;

public class DefaultErrorImpl implements IErrorResponse<DefaultErrorResponseModel> {

    private DefaultErrorResponseModel res;

    @Override
    public IErrorResponse<DefaultErrorResponseModel> setErrorMsg(String msg) {
        this.res = new DefaultErrorResponseModel(msg);
        return this;
    }

    @Override
    public DefaultErrorResponseModel getErrorResponse() {
        return this.res;
    }
}
