package com.example.udpchat;

import java.util.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import com.fasterxml.jackson.databind.*;

public class Chat {
    public static void main(String[] args) {
        String group = "230.0.0.1"; // Multicast group address
        int port = 5656; // Port number for the chat

        System.out.println("------------------------------------------------------------------");
        Listener listener = new Listener(group, port);
        Sender sender = new Sender(group, port, listener);
        sender.start();
        listener.run();

    }
}

class Listener {
    private String group;
    private int port;
    private boolean isRunning;

    public Listener(String group, int port) {
        this.group = group;
        this.port = port;
        this.isRunning = true;
    }

    public void changeRunStatus() {
        isRunning = false;
    }

    @SuppressWarnings("deprecation")
    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(port); // Create a multicast socket
            InetAddress ia = InetAddress.getByName(group); // InetAddress object for the group
            ObjectMapper objectMapper = new ObjectMapper(); // Create an ObjectMapper for JSON processing
            String data, time, date, username, message;

            socket.joinGroup(ia); // Join the multicast group

            while (true) {
                byte[] buffer = new byte[1024]; // Buffer for incoming data
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // Packet for incoming data

                socket.setSoTimeout(2000); // Set socket timeout to 2 seconds
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    if (isRunning) {
                        continue; // Continue listening if running
                    } else {
                        socket.leaveGroup(ia); // Leave the multicast group
                        socket.close(); // Close the socket
                        break;
                    }
                }

                // To extract data and display it
                data = new String(packet.getData(), 0, packet.getLength());
                JsonNode jsonNode = objectMapper.readTree(data);
                time = jsonNode.get("time").asText();
                date = jsonNode.get("date").asText();
                username = jsonNode.get("username").asText();
                message = jsonNode.get("message").asText();

                System.out.println(date + " " + time + " <" + username + "> said: " + message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class Sender extends Thread {
    private String group;
    private int port;
    private Listener listener;

    public Sender(String group, int port, Listener listener) {
        this.group = group;
        this.port = port;
        this.listener = listener;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(port); // Create a multicast socket
            InetAddress ia = InetAddress.getByName(group); // Get the InetAddress object for the group
            ObjectMapper objectMapper = new ObjectMapper(); // Create an ObjectMapper for JSON processing

            socket.joinGroup(ia); // Join the multicast group

            Scanner input = new Scanner(System.in); // Create a Scanner for user input
            System.out.print("Hello, please choose your username: "); // Prompt for username
            String username = input.nextLine(); // Read username
            System.out.println("You can start typing");
            System.out.println("------------------------------------------------------------------");

            while (true) {
                String message = input.nextLine(); // Read message from user input

                if ("<exit>".equals(message)) {
                    break; // Exit if message is "<exit>"
                }

                // Get the current date and time
                String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                // Create a JSON object with the message data
                JsonNode jsonNode = objectMapper.createObjectNode()
                        .put("date", date)
                        .put("time", time)
                        .put("username", username)
                        .put("message", message);

                String payload = objectMapper.writeValueAsString(jsonNode); // Convert JSON object to string
                byte[] msg = payload.getBytes(); // Convert string to byte array
                DatagramPacket packet = new DatagramPacket(msg, msg.length, ia, port); // Create a packet with the data
                socket.send(packet); // Send the packet through the socket
            }

            System.out.println("Exiting....");
            input.close(); // Close the input scanner
            socket.leaveGroup(InetAddress.getByName(group)); // Leave the multicast group
            socket.close(); // Close the socket
            listener.changeRunStatus(); // Change the listener's running status

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
