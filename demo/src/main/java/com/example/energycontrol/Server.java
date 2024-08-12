package com.example.energycontrol;

import java.util.*;
import java.io.*;
import java.net.*;
import com.fasterxml.jackson.databind.*;

public class Server {
    static final int PORT = 49300;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Server is running and waiting for messages...");

            ObjectMapper mapper = new ObjectMapper();
            byte[] receiveData = new byte[1024];

            HashMap<String, String> hashmap = new HashMap<>();
            hashmap.put("luz_guarita", "off");
            hashmap.put("ar_guarita", "off");
            hashmap.put("luz_estacionamento", "off");
            hashmap.put("luz_galpao_externo", "off");
            hashmap.put("luz_galpao_interno", "off");
            hashmap.put("luz_escritorios", "off");
            hashmap.put("ar_escritorios", "off");
            hashmap.put("luz_sala_reunioes", "off");
            hashmap.put("ar_sala_reunioes", "off");

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                System.out.println("Server received: " + message);

                JsonNode messageNode = mapper.readTree(message);
                String command = messageNode.get("command").asText();
                String locate = messageNode.get("locate").asText();
                String value = messageNode.has("value") ? messageNode.get("value").asText() : " ";
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                JsonNode responseNode;
                String response;
                byte[] msg;
                DatagramPacket packet;

                switch (command) {
                    case "set":
                        for (String key : hashmap.keySet()) {
                            if (key.equals(locate)) {
                                hashmap.put(key, value);
                                break;
                            }
                        }

                        responseNode = mapper.createObjectNode()
                                .put("locate", locate)
                                .put("status", value);
                        response = mapper.writeValueAsString(responseNode);
                        msg = response.getBytes();

                        System.out.println("Server responded with: " + response);

                        packet = new DatagramPacket(msg, msg.length, clientAddress, clientPort);
                        socket.send(packet);

                        break;
                    case "get":
                        String status = hashmap.get(locate);

                        responseNode = mapper.createObjectNode()
                                .put("locate", locate)
                                .put("status", status);
                        response = mapper.writeValueAsString(responseNode);
                        msg = response.getBytes();

                        System.out.println("Server responded with: " + response);

                        packet = new DatagramPacket(msg, msg.length, clientAddress, clientPort);
                        socket.send(packet);

                        break;
                    default:
                        System.out.println("Unknown command/location");

                        responseNode = mapper.createObjectNode().put("error", "Unknown  command/location");
                        response = mapper.writeValueAsString(responseNode);
                        msg = response.getBytes();

                        System.out.println("Server responded with: " + response);

                        packet = new DatagramPacket(msg, msg.length, clientAddress, clientPort);
                        socket.send(packet);

                        break;
                }
            }

        } catch (SocketException e) {
            System.err.println("Problem with socket: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Problem with host: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Problem with datagram packet: " + e.getMessage());
        }
    }
}
