import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.jexl3.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
        if (value.indexOf("expr#") != -1) {
            JexlExpression e = jexl.createExpression(value.substring(value.indexOf("expr#") + 5));
            System.out.println("eval expression => " + e.getSourceText());
            // Now evaluate the expression, getting the result
            Object o = e.evaluate(jc);
            System.out.println("value => " + o);
            return "" + o;
        }
        else
            return value;
    }

    public static void main(String[] args) throws Exception {

        final String templateJson = readFile("template.json");
        final String data = readFile("data.json");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dataMap = mapper.readValue(data.toString(), Map.class);
        // Create a context and add data
        JexlContext jc = new MapContext();
        jc.set("input", dataMap );


        //JsonNode rootNode = 
        Map<String, Object> valueMap = mapper.readValue(templateJson, Map.class);

        //evaluateJsonNode(rootNode, jc);
        evaluateDataMap(valueMap, jc);

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        System.out.println("new json document "  + mapper.writeValueAsString(valueMap));

        // Create an expression
//        String jexlExp = "data.get('event').get('properties').get('weatherF') > 100 " +
//                "? data.get('event').get('properties').get('value') : 0";

    }
}
