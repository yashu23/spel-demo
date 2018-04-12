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
    private final static JexlEngine jexl = new JexlBuilder().create();
    private final static ObjectMapper mapper = new ObjectMapper();

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
            //System.out.println("final expression :: " + expressionString);
            JexlExpression e = jexl.createExpression(expressionString);
            //System.out.println("eval expression => " + e.getSourceText());

            // Now evaluate the expression, getting the result
            Object o = e.evaluate(jc);
            //System.out.println("value => " + o);
            return o;
        } 
        else
            return value;
    }

    private final static void printMessage(int counter, long startTime) {
        System.out.println("Time taken for translation " + (counter++) + " " + (System.currentTimeMillis() - startTime));
    }

    private String runBusinessRules(final String request,
                                    final String role,
                                    final String channel,
                                    final String key) {

        try {

            String data = request;
            String templateJson = readFile("mapper_" + key + ".json");
            String scriptFile = readFile("mapper_" + key + "_rr.js");
            JexlScript script = jexl.createScript(scriptFile);
            int counter = 1;

            long startTime = System.currentTimeMillis();
            data = data.replace("\\r\\n", "")
                    .replace("\\", "")
                    .replace("\"{", "{")
                    .replace("}\"", "}");
            printMessage(counter++, startTime);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            JsonNode jsonTree = mapper.readTree(data);
            printMessage(counter++, startTime);
            JexlContext jc = new MapContext();
            jc.set("input", jsonTree);

            jc.set("rr", new ArrayList<String>());
            jc.set("rrText", new ArrayList<String>());
            jc.set("result", "1");

            // Execute script and add output variable to context
            script.execute(jc);
            printMessage(counter++, startTime);
            //System.out.println("variables => " + script.getPragmas() + "," + script.getVariables());

            //System.out.println("Raw api response => " + mapper.writeValueAsString(jsonTree));

            // Populate template json
            Pattern pattern = Pattern.compile("(\\{)(\\S+)(\\})");
            Matcher matcher = pattern.matcher(templateJson);

            while (matcher.find()) {
                String jsonPointer = matcher.group(2);
                JsonNode node = jsonTree.at(jsonPointer.trim());
                if (node != null) {
                    //System.out.println(jsonPointer + " => " + node.asText());
                    templateJson = templateJson.replace("{" + jsonPointer + "}", node.asText());
                } else {
                    //System.out.println(jsonPointer + " => null");
                }
            }
            printMessage(counter++, startTime);
            //System.out.println("translated json => " + templateJson);

            //JsonNode rootNode =
            Map<String, Object> valueMap = mapper.readValue(templateJson, Map.class);

            evaluateDataMap(valueMap, jc);

            System.out.println("Time taken for translation " + (counter++) + " " + (System.currentTimeMillis() - startTime));
            return mapper.writeValueAsString(valueMap);

        } catch(Exception ex){
            throw new RuntimeException(ex);
        }

    }

    public static void main(String[] args) throws Exception {
        MapperService mapperService = new MapperService();
        String data = readFile("nal_response.json");
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("Json Response =>\n" + mapperService.runBusinessRules(data, "", "" , "ncdstatus"));
    }
}
