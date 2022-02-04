package facade;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import enums.LogLevel;
import error.DefaultErrorImpl;
import error.IErrorResponse;
import utils.ILogger;
import utils.PathParameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class ApiFacade {

    private final List<Class<? extends IGenericApi>> apis;
    private final IErrorResponse<?> noMethodResBody;

    // Logging class
    private final ILogger logger;

    /**
     * Constructor
     * Set to execute api class, logger object, and data instance for default error
     * @param ApiExecutionClasses Api unit classes (Not instances)
     * @param logger Logging obj
     * @param noMethodFoundResponseDataModel Error response
     */
    public ApiFacade(List<Class<? extends IGenericApi>> ApiExecutionClasses,
                     ILogger logger,
                     IErrorResponse<?> noMethodFoundResponseDataModel
    ) {
        this.apis = ApiExecutionClasses;
        this.logger = logger;
        this.noMethodResBody =
                (noMethodFoundResponseDataModel == null) ? new DefaultErrorImpl() : noMethodFoundResponseDataModel;
        this.apis.forEach(
                unitClass -> {
                    logging("[ApiFacade#doOnExecuteApi - " + LogLevel.INFO.logLebel() +
                            "] LOAD CLASS: " + unitClass.getName(), LogLevel.INFO);
                }
        );
    }

    /**
     * Execute api by given request from AWS API GW
     * @param input request from AWS API GW
     * @return APIGatewayProxyResponseEvent
     */
    public APIGatewayProxyResponseEvent doExecuteApi(APIGatewayProxyRequestEvent input) {
        // Scan all registered classes
        for(Class<? extends IGenericApi> apiUnitClass: apis) {
            if(apiUnitClass.isAnnotationPresent(ApiGwFunction.class)) {

                // Scan all method in this class
                for(Method method: apiUnitClass.getDeclaredMethods()) {
                    if(method.isAnnotationPresent(ApiGwEvents.class)) {
                        if(isSameCalledOnSamePath(apiUnitClass, method, input.getHttpMethod(), input.getPath())) {
                            // Convert request body by class as defined in annotation
                            Class<?> requestDataModel = method.getAnnotation(ApiGwEvents.class).requestPayloadDataModel();
                            Object apiRequestBody = input.getBody() != null && !input.getBody().isEmpty() ?
                                    new Gson().fromJson(input.getBody(), requestDataModel) : "";
                            // Execute API
                            return executeApi(method, apiUnitClass, apiRequestBody, new PathParameter(input));
                        } else {
                            logging("[ApiFacade#doOnExecuteApi - " + LogLevel.INFO.logLebel() +
                                    "] (SKIP) Not match request as method > (ClassBase)" +
                                            apiUnitClass.getAnnotation(ApiGwFunction.class).basePath() +
                                    "\n(MethodBase)"+ method.getAnnotation(ApiGwEvents.class).basePath() +
                                    " and \n(API GW)" + input.getPath(), LogLevel.INFO);
                        }
                    } else {
                        logging("[ApiFacade#doOnExecuteApi - " + LogLevel.INFO.logLebel()+
                                "] (SKIP) No annotated method > " + method.getName(), LogLevel.INFO);
                    }
                }
            } else {
                logging("[ApiFacade#doOnExecuteApi - " + LogLevel.WARN.logLebel() +
                        "] (SKIP) No annotated class > " + apiUnitClass.getName(), LogLevel.WARN);
            }
        }

        // Nothing to do, Skipped all class and method
        return new APIGatewayProxyResponseEvent().withBody(new Gson().toJson(
                noMethodResBody
                        .setErrorMsg("[ApiFacade#doOnExecuteApi - " + LogLevel.ERROR.logLebel() + "] No methods found")
                        .getErrorResponse()
        )).withStatusCode(500);
    }

    /**
     * Execute apiUnitClass method
     * Will return 200 response in all cases other than be occurred internal error
     * @param method method to execute as API, This needed to be contained following class
     * @param apiUnitClass Class obj that contain method to execute as API
     * @param requestBody requestBody from AWS API GW
     * @param pathParam path parameter as PathParameter obj
     * @return APIGatewayProxyResponseEvent
     */
    private APIGatewayProxyResponseEvent executeApi(
            Method method, Class<? extends IGenericApi> apiUnitClass, Object requestBody, PathParameter pathParam
    ) {
        AbsGenericResponse resultOfApi = null;
        try {
            /*
             * Define of method for API
             * Return: AbsGenericResponse
             * Arg: (Object requestBody, PathParameter pathParam)
             */
            Object[] args = { requestBody, pathParam };
            resultOfApi = (AbsGenericResponse) method.invoke(apiUnitClass.newInstance(), args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
            return new APIGatewayProxyResponseEvent().withBody(new Gson().toJson(
                    this.noMethodResBody
                            .setErrorMsg("[ApiFacade#executeApi - " + LogLevel.ERROR.logLebel() +
                                    "] Exceptions are thrown\n" + exception.getMessage())
                            .getErrorResponse()
            )).withStatusCode(500);
        } catch (ClassCastException e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent().withBody(new Gson().toJson(
                    this.noMethodResBody
                            .setErrorMsg("[ApiFacade#executeApi - " + LogLevel.ERROR.logLebel() +
                                    "] Returned invalid data class\n" + e.getMessage())
                            .getErrorResponse()
            )).withStatusCode(500);
        }

        // Convert response of API
        if(resultOfApi == null || resultOfApi.returnedCode == null) {
            return new APIGatewayProxyResponseEvent().withBody(new Gson().toJson(
                    this.noMethodResBody
                            .setErrorMsg("[ApiFacade#executeApi - " + LogLevel.ERROR.logLebel() +
                                    "] Invalid body or response code")
                            .getErrorResponse()
            )).withStatusCode(500);
        } else {
            // Set default value
            if(resultOfApi.apiResponseModel == null) {
                resultOfApi.apiResponseModel = "{}";
            }

            // return result of executed APi (on success executing API)
            return new APIGatewayProxyResponseEvent()
                    .withBody(new Gson().toJson(resultOfApi.apiResponseModel))
                    .withStatusCode(resultOfApi.returnedCode);
        }
    }

    /**
     * Checking whether same request expected API and selected API in this class
     * @param selectedApiUnitClass Class obj that contain method to execute as API
     * @param selectedMethod method to execute as API, This needed to be contained above class
     * @param requestType HTTP request type contained in request from AWS API GW
     * @param requestedPath Request path that given from AWS API GW
     * @return If collect, will return true
     */
    private Boolean isSameCalledOnSamePath(
            Class<? extends IGenericApi> selectedApiUnitClass, Method selectedMethod, String requestType, String requestedPath
    ) {
        if(selectedApiUnitClass.isAnnotationPresent(ApiGwFunction.class) && selectedMethod.isAnnotationPresent(ApiGwEvents.class)) {
            // Ignore method when be set wrong request type
            if(!requestType.equals(selectedMethod.getAnnotation(ApiGwEvents.class).requestMethodType().reqType())) {
                return false;
            }

            // Eval by path
            // 0: Convert path to blocks
            String definedPath = selectedApiUnitClass.getAnnotation(ApiGwFunction.class).basePath() +
                    selectedMethod.getAnnotation(ApiGwEvents.class).basePath();
            // replace duplicated slash
            definedPath = definedPath.replaceAll("//", "/");
            // chomp last slash ( Ignore this when path is only "/" )
            definedPath = !(definedPath.equals("/")) && definedPath.charAt(definedPath.length() - 1) == '/' ?
                    definedPath.substring(0, definedPath.length() - 1) : definedPath;
            // For evaluating path blocks ( /path/to/{param01}/api/{param02} -> {path, to, param01, api, param02}
            List<String> definedPathBlock = Arrays.asList(definedPath.split("/"));
            List<String> pathBlockOnAws = Arrays.asList(requestedPath.split("/"));

            // 1: Eval path length
            if(definedPathBlock.size() == pathBlockOnAws.size()) {
                Pattern param = Pattern.compile("\\{.+}");
                // Compare path in order
                for(int pathCounter = 0; pathCounter < pathBlockOnAws.size(); pathCounter++) {
                    if(param.matcher(definedPathBlock.get(pathCounter)).find()) {
                        // Skip when block is path parameter
                        continue;
                    }
                    // 2: return false when found wrong directory name between defined and given
                    if(!(pathBlockOnAws.get(pathCounter).equals(definedPathBlock.get(pathCounter)))) {
                        return false;
                    }
                }
                // Defined path and given path have same length and same directory path
                return true;
            } else {
                // Length have not matched between given and defined
                return false;
            }
        } else {
            // Different request given http request type
            return false;
        }
    }

    /**
     * Logging
     * @param msg For To embedded message into log
     */
    private void logging(String msg, LogLevel logLevel) {
        if(this.logger != null) logger.logging(msg, logLevel);
    }
}
