package com.lucky5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.jexl3.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * author : Yashpal_Rawat
 * lastModifiedDate : 12/04/2018 6:23 PM
 */
public class MapperService {
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
        Path path = Paths.get(MapperService.class.getClassLoader()
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
        if (value.contains("expr#")) {
            String expressionString = value.substring(value.indexOf("expr#") + 5);
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

    public static void print(JsonNode node) {
        System.out.println(node);
    }

    public static void main(String[] args) throws Exception {

        String templateJson = readFile("template.json");

        String data = readFile("nal_response.json");
        data = data.replace("\\r\\n","").replace("\\","").replace("\"{","{").replace("}\"","}");

        //System.out.println(data);

        //final String apiRequest = readFile("nal_api_response.json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        JsonNode jsonTree = mapper.readTree(data);
        JexlContext jc = new MapContext();
        jc.set("input", jsonTree);

        System.out.println("Raw api response => " + mapper.writeValueAsString(jsonTree));

        // Populate template json
        Pattern pattern = Pattern.compile("(\\{)(\\S+)(\\})");
        Matcher matcher = pattern.matcher(templateJson);

        while(matcher.find()) {
            String jsonPointer = matcher.group(2);
            JsonNode node = jsonTree.at(jsonPointer.trim());
            if (node != null) {
                System.out.println(jsonPointer + " => " + node.asText());
                templateJson = templateJson.replaceAll("{" + jsonPointer + "}", node.asText());
            } else {
                System.out.println(jsonPointer + " => null");
            }
        }

        System.out.println("translated json => " + templateJson);

        //JsonNode rootNode =
        Map<String, Object> valueMap = mapper.readValue(templateJson, Map.class);

        evaluateDataMap(valueMap, jc);

       // Map map = updateMasterMap(valueMap, mapper.readValue(apiRequest, Map.class));

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("new json document "  + mapper.writeValueAsString(valueMap));
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
