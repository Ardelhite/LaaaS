package utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathParameter {

    private HashMap<String, String> pathParameter = new HashMap<>();

    /**
     * Constructor
     * /path/to/api/{apiPathParameterName[0]}/{apiPathParameterName[1]...}
     * @param input ApiRequest which given from API GW
     */
    public PathParameter(APIGatewayProxyRequestEvent input) {
        if(input != null && input.getPathParameters() != null) {
            // Get path parameter from input
            Matcher m = Pattern.compile("\\{.+}").matcher(input.getPathParameters().toString());
            if(m.find()) {
                pathParameter = convertPathParameterToHash(m.group(0));
            }
        }
    }

    /**
     * Convert {ParameterName1=Value1, ParameterName2=Valuer2} to HashMap
     * @param param ApiRequest which given from API GW (Converted to only request)
     * @return {ParameterName1: Value1, ParameterName2: Value2}
     */
    private HashMap<String, String> convertPathParameterToHash(String param) {
        HashMap<String, String> req = new HashMap<>();
        Arrays.asList(param
                .substring(1, param.length()-1) // Delete brace in edge
                .split(",")) // split parameter when that has multiple value
                .forEach(item -> {
                    String[] splitField = item.split("="); // [ParameterName1, Value1]
                    if(Pattern.compile("^\\s+").matcher(splitField[0]).find()) {
                        // Delete white space in head
                        splitField[0] = splitField[0].substring(1);
                    }
                    req.put(splitField[0], splitField[1]);
                }
        );
        return req;
    }

    public HashMap<String, String> getPathParameters() {
        return this.pathParameter;
    }

    @Override
    public String toString() {
        return this.pathParameter.keySet().stream().reduce(
                "", (sum, elm) -> {
                    return sum + "PathPramName: " + elm + " Value: " + pathParameter.get(elm) + " - ";
                }
        );
    }
}
