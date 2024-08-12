package com.example.udpchat;

import java.util.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import com.fasterxml.jackson.databind.*;

public class Chat {
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        String group = "230.0.0.1";
        int port = 5656;

        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress ia = InetAddress.getByName(group);
            socket.joinGroup(ia);

            System.out.println("------------------------------------------------------------------");
            Listener listener = new Listener(socket, ia);
            Sender sender = new Sender(socket, ia, listener, port);
            sender.start();
            listener.run();
        } catch (IOException e) {
        }

    }
}

class Listener {
    private MulticastSocket socket;
    private InetAddress ia;
    private boolean isRunning;

    public Listener(MulticastSocket socket, InetAddress ia) {
        this.socket = socket;
        this.ia = ia;
        this.isRunning = true;
    }

    public void changeRunStatus() {
        isRunning = false;
    }

    @SuppressWarnings("deprecation")
    public void run() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String data, time, date, username, message;

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.setSoTimeout(2000);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    if (isRunning) {
                        continue;
                    } else {
                        socket.leaveGroup(ia);
                        socket.close();
                        break;
                    }
                }

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
    private MulticastSocket socket;
    private InetAddress ia;
    private Listener listener;
    private int port;

    public Sender(MulticastSocket socket, InetAddress ia, Listener listener, int port) {
        this.socket = socket;
        this.ia = ia;
        this.listener = listener;
        this.port = port;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            Scanner input = new Scanner(System.in);
            System.out.print("Hello, please choose your username: ");
            String username = input.nextLine();
            System.out.println("You can start typing");
            System.out.println("------------------------------------------------------------------");

            while (true) {
                String message = input.nextLine();

                if ("<exit>".equals(message)) {
                    break;
                }

                String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                JsonNode jsonNode = objectMapper.createObjectNode()
                        .put("date", date)
                        .put("time", time)
                        .put("username", username)
                        .put("message", message);

                String payload = objectMapper.writeValueAsString(jsonNode);
                byte[] msg = payload.getBytes();
                DatagramPacket packet = new DatagramPacket(msg, msg.length, ia, port);
                socket.send(packet);
            }

            System.out.println("Exiting....");
            input.close();
            listener.changeRunStatus();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
