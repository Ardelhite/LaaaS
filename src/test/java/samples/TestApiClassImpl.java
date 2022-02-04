package samples;


import enums.ApiRequestType;
import facade.ApiGwEvents;
import facade.ApiGwFunction;
import facade.IGenericApi;
import utils.PathParameter;

/**
 * Expected request body for test is
 * {"test":"body"}
 */
@ApiGwFunction(basePath = "/testPath01")
public class TestApiClassImpl implements IGenericApi {
    @ApiGwEvents(
            basePath = "/{PathParam01}/testPath02/",
            requestMethodType = ApiRequestType.GET,
            requestPayloadDataModel = TestApiReqModel.class)
    public TestApiResModel testApi(Object request, PathParameter param) {
        TestApiReqModel test = ((TestApiReqModel) request);
        TestApiResModel response = new TestApiResModel();
        response.returnedCode = 200;
        response.apiResponseModel = "TEST RESPONSE: " + test.test + "\nPathParam: " + param.getPathParameters();
        return response;
    }

    public void TestIgnoredMethod() { }
}
