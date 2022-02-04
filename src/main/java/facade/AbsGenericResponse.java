package facade;

public class AbsGenericResponse {
    // return code be setting in return response to API GW
    public Integer returnedCode;

    // insert each response body into this value as specific type
    public Object apiResponseModel;
}
