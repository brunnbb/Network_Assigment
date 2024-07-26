package com.example.energycontrol;

import java.util.Hashtable;

public class TestarHashTable {
    public static void main(String[] args) {
        // Create a Hashtable
        Hashtable<String, Integer> hashtable = new Hashtable<>();

        // Add key-value pairs
        hashtable.put("Alice", 30);
        hashtable.put("Bob", 25);
        hashtable.put("Charlie", 35);

        // Retrieve and print values
        System.out.println("Alice's age: " + hashtable.get("Alice"));

        // Check if a key and value exist
        System.out.println("Contains key Bob: " + hashtable.containsKey("Bob"));
        System.out.println("Contains value 30: " + hashtable.containsValue(30));

        // Remove a key-value pair
        hashtable.remove("Charlie");

        // Iterate through the Hashtable
        for (String key : hashtable.keySet()) {
            Integer value = hashtable.get(key);
            System.out.println(key + ": " + value);
        }

        // Test of change
        for (String key : hashtable.keySet()) {
            if (key.equals("Alice")) {
                hashtable.put(key, 60);
                break;
            }
        }

        // Iterate through the Hashtable
        for (String key : hashtable.keySet()) {
            Integer value = hashtable.get(key);
            System.out.println(key + ": " + value);
        }

        // Clear the Hashtable
        hashtable.clear();
    }
}
