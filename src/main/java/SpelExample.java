import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.jexl3.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Project      : speldemo
 * Name         : PACKAGE_NAME.SpelExample.java
 * Author       : yashpalrawat
 * Created      : 8/04/2018 10:28
 * Description  : <Insert class description>
 */
public class SpelExample {

    // Create or retrieve an engine
    final static JexlEngine jexl = new JexlBuilder().create();

    /**
     * Read File from resource file
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    private static final String readFile(final String fileName) throws Exception {
        Path path = Paths.get(SpelExample.class.getClassLoader()
                .getResource(fileName).toURI());

        StringBuilder data = new StringBuilder();
        Stream<String> lines = Files.lines(path);
        lines.forEach(line -> data.append(line).append("\n"));
        lines.close();
        return data.toString();
    }

    private static final void evaluateDataMap(final Map<String, Object> map, JexlContext jc) {
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String,Object> entry : entries) {
            String fieldName = entry.getKey();

            // if element is object
            if (entry.getValue() instanceof Map ) {
                evaluateDataMap((Map<String, Object>) entry.getValue(), jc);
            } else if (entry.getValue() instanceof List) {
                List<Object> modifiedList = new ArrayList<>();

                // if element is array
                List<Object> lst = (List<Object>) entry.getValue();
                lst.forEach((v) -> {
                    if (v instanceof Map ) {
                        evaluateDataMap((Map<String, Object>) v, jc);
                        modifiedList.add(v);
                    } else {
                        modifiedList.add(getValue((String) v, jc));
                    }
                });
                map.put(fieldName, modifiedList);
            }
            else {
                map.put(fieldName, getValue((String) entry.getValue(), jc));
            }
        }
    }

    private static Object getValue(String value, JexlContext jc) {
        System.out.println("expression " + value);
        if (value.contains("expr#")) {
            String expressionString = value.substring(value.indexOf("expr#") + 5);
            /*String[] tokens = value.substring(value.indexOf("expr#") + 5).split("\\.");

            if (tokens.length > 0) {
                StringBuilder strb = new StringBuilder(tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    strb.append(".get('" + tokens[i] + "')");
                }
                expressionString = strb.toString();
            }*/
            System.out.println("final expression :: " + expressionString);



            JexlExpression e = jexl.createExpression(expressionString);
            System.out.println("eval expression => " + e.getSourceText());


            // Now evaluate the expression, getting the result
            Object o = e.evaluate(jc);
            System.out.println("value => " + o);
            return "" + o;
        } else if (value.startsWith("lookup#(")) {
            int start = value.indexOf("lookup#(") + "lookup#(".length();
            String substr = value.substring(start);
            int end = substr.indexOf(")");
            return value.substring(start+2,end);
        }
        else
            return value;
    }

    public static void main(String[] args) throws Exception {

        String templateJson = readFile("mapper_ncdstatus.json");

        String data = readFile("nal_response.json");
        data = data.replace("\r\n","").replace("\\\"","");


        //final String apiRequest = readFile("nal_api_response.json");

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonTree = mapper.readTree(data.toString());
        JexlContext jc = new MapContext();
        jc.set("input", jsonTree);

        // Populate template json
        Pattern pattern = Pattern.compile("(\\{)(\\S+)(\\})");
        Matcher matcher = pattern.matcher(templateJson);
        String output;

        while(matcher.find()) {
            String jsonPointer = matcher.group(2);
            //System.out.println("jsonPointer => " + jsonPointer);
            JsonNode node = jsonTree.at(jsonPointer);
            System.out.println(jsonPointer + " => " + node.textValue());
            templateJson = templateJson.replace("{" + jsonPointer + "}", node.textValue());
        }

        // Add template json to apiRequest

        //var mapper = new('com.fasterxml.jackson.databind.ObjectMapper');
        //var input = mapper.readValue(data.toString(), java.util.Map.class);

        //JexlScript script = jexl.createScript(scriptJs);
        //script.execute(jc);
        //Map<String, Object> valueMap = (Map<String, Object>) script.execute(jc);


        //JsonNode rootNode = 
        Map<String, Object> valueMap = mapper.readValue(templateJson, Map.class);

        //evaluateJsonNode(rootNode, jc);
        evaluateDataMap(valueMap, jc);

        //Map map = updateMasterMap(valueMap, mapper.readValue(apiRequest, Map.class));

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        //System.out.println("value map json " + mapper.writeValueAsString(valueMap));

        //System.out.println("new json document "  + mapper.writeValueAsString(map));

        // Create an expression
//        String jexlExp = "data.get('event').get('properties').get('weatherF') > 100 " +
//                "? data.get('event').get('properties').get('value') : 0";

    }

    private static Map updateMasterMap(Map<String, Object> valueMap, Map map) {
        Set<Map.Entry<String, Object>> enteries = valueMap.entrySet();
        for (Map.Entry<String, Object> entry : enteries) {
            if(valueMap.get(entry.getKey()) == null) {
                map.put(entry.getKey(), entry.getValue());
            } else {
                if(entry.getValue() instanceof Map && map.get(entry.getKey()) instanceof Map) {
                    updateMasterMap((Map<String, Object>)entry.getValue(), (Map) map.get(entry.getKey()));
                } else {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return map;
    }
}
