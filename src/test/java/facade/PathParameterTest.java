package facade;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import enums.ApiRequestType;
import org.junit.Test;
import samples.TestApiClassImpl;
import samples.TestApiReqModel;
import utils.StudioLogger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class PathParameterTest {
    @Test
    public void whetherCanBeInvokeMethod() {

        // Create simple test case
        APIGatewayProxyRequestEvent testCase = new APIGatewayProxyRequestEvent();
        TestApiReqModel testBody = new TestApiReqModel();
        testCase.setPathParameters(new HashMap<String, String>(){
            {
                put("PathParam01", "testParameter");
            }
        });
        testBody.test = "body";
        testCase.setBody(new Gson().toJson(testBody));
        testCase.setHttpMethod(ApiRequestType.GET.reqType());
        testCase.setPath("/testPath01/PathParam01/testPath02");

        // Out put result to stdout
        StudioLogger logger = new StudioLogger();

        // Execute annotated method
        ApiFacade facade = new ApiFacade(Arrays.asList(TestApiClassImpl.class), logger, null);
        APIGatewayProxyResponseEvent result = facade.doExecuteApi(testCase);
        System.out.println("StatusCode: " + result.getStatusCode() + "\nResult: " + result.getBody());
    }
}
