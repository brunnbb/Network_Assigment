package com.example.fileserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonNodeExample {
    public static void main(String[] args) {
        try {
            // Criar um ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Criar um JSON de exemplo como String
            String jsonString = "{ \"name\" : \"John\", \"age\" : 30, \"city\" : \"New York\" }";

            // Ler a string JSON e convertê-la para um JsonNode
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // Acessar valores do JsonNode
            String name = jsonNode.get("name").asText();
            int age = jsonNode.get("age").asInt();
            String city = jsonNode.get("city").asText();
            String weight = jsonNode.has("weight") ? jsonNode.get("weight").asText() : " ";

            System.out.println("Name: " + name);
            System.out.println("Age: " + age);
            System.out.println("City: " + city);
            System.out.println("Weight: " + weight);

            // Criar um novo objeto JSON usando ObjectNode
            JsonNode newNode = objectMapper.createObjectNode()
                    .put("name", "Jane")
                    .put("age", 25)
                    .put("city", "San Francisco");

            // Converter o novo JsonNode para string
            String newJsonString = objectMapper.writeValueAsString(newNode);

            System.out.println("New JSON: " + newJsonString);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
