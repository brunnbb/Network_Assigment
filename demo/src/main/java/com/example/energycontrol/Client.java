package com.example.energycontrol;

import java.util.*;
import java.io.*;
import java.net.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Client {
    public static void main(String[] args) {
        try {
            DatagramSocket client = new DatagramSocket();
            InetAddress add = InetAddress.getByName("localhost");
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode jsonNode = objectMapper.createObjectNode()
                    .put("command", "get")
                    .put("locate", "luz_sala_reunioes");
            String payload = objectMapper.writeValueAsString(jsonNode);
            byte[] msg = payload.getBytes();
            // DatagramPacket packet = new DatagramPacket(msg, msg.length, ia, port); //
            // Create a packet with the data
            // socket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
