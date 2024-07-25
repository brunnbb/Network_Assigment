package com.example.fileserver;

import java.io.*;
import java.net.*;
import java.math.*;
import java.nio.file.*;
import java.security.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TcpServer {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(6777)) {
            int counter = 0;
            System.out.println("------------------------------------------------------");
            System.out.println("[SERVER] Waiting for a client connection...");
            System.out.println("------------------------------------------------------");
            while (true) {
                Socket socket = serverSocket.accept();
                counter++;
                System.out.println("[SERVER] Connected to client " + counter);
                ClientHandler clientHandler = new ClientHandler(socket, counter);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Fatal error, shuting down");
            System.exit(0);
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private int clientNumber;
    private DataOutputStream out;
    private DataInputStream in;
    private static String serverFilePath = "demo\\src\\main\\java\\com\\example\\fileserver\\serverfiles";
    private static ObjectMapper objectMapper;

    // Inicializa o socket e o objectMapper(Para formatar e ler as mensagems como
    // objetos json)
    public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        ClientHandler.objectMapper = new ObjectMapper();
    }

    // Retorna um hash MD5
    public String hashFile(String fileName) {
        byte[] fileToBytes;
        try {
            fileToBytes = Files.readAllBytes(Paths.get(serverFilePath + "\\" + fileName));
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(fileToBytes);
            BigInteger bigInteger = new BigInteger(1, messageDigest);
            return bigInteger.toString(16);
        } catch (IOException e) {
            System.out.println("[SERVER] Error in reading the file for hashing: " + e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("[SERVER] Error in hashing the file: " + e.getMessage());
            return null;
        }
    }

    // Vai receber um arquivo, com o in
    public void receiveFile(String fileName, String hash) {
        File fileToReceive = new File(serverFilePath + "\\" + fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToReceive))) {

            int retries = 0;
            String status = "fail";

            while (retries < 3) {
                byte[] buffer = new byte[8192];
                int byteRead;

                while (in.available() > 0 && (byteRead = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, byteRead);
                }
                bos.flush();

                String calculatedHash = hashFile(fileName);
                if (hash.equals(calculatedHash)) {
                    JsonNode payload = objectMapper.createObjectNode()
                            .put("file", fileName)
                            .put("operation", "put")
                            .put("status", "success");
                    out.writeUTF(objectMapper.writeValueAsString(payload));
                    out.flush();
                    status = "success";
                    break;
                } else {
                    retries++;
                    if (retries < 3) {
                        System.out.println("[SERVER] Retrying to receive file "
                                + fileName + " from Client " + clientNumber);
                        JsonNode payload = objectMapper.createObjectNode()
                                .put("file", fileName)
                                .put("operation", "put")
                                .put("status", "fail");
                        out.writeUTF(objectMapper.writeValueAsString(payload));
                        out.flush();
                    }
                }
            }

            if (status.equals("success")) {
                System.out.println("[SERVER] File " + fileName + " received from Client " + clientNumber);
            } else {
                System.out.println("[SERVER] Failed to receive : " + fileName + " from Client " + clientNumber);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Error receiving file: " + e.getMessage());
        }
    }

    // Vai enviar um arquivo, com o out
    public void sendFile(String fileName) {
        File fileToSend = new File(serverFilePath + "\\" + fileName);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSend))) {

            JsonNode payload = objectMapper.createObjectNode()
                    .put("file", fileName)
                    .put("operation", "get")
                    .put("hash", hashFile(fileName));

            out.writeUTF(objectMapper.writeValueAsString(payload));
            out.flush();

            int retries = 0;
            String status = "fail";

            while (retries < 3) {

                byte[] buffer = new byte[8192];
                int byteRead;

                while ((byteRead = bis.read(buffer)) > 0) {
                    out.write(buffer, 0, byteRead);
                }
                out.flush();

                JsonNode response = objectMapper.readTree(in.readUTF());
                status = response.get("status").asText();

                if (response.get("status").asText().equals("success")) {
                    status = "sucess";
                    break;
                } else {
                    retries++;
                    System.out.println("[SERVER] Retrying to send file " + fileName + " to Client " + clientNumber);
                }
            }

            if (status.equals("success")) {
                System.out.println("[SERVER] File " + fileName + " sent to Client " + clientNumber);
            } else {
                System.out.println("[SERVER] Failed to sent file : " + fileName + " to Client " + clientNumber);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Error while sending file: " + e.getMessage());
        }
    }

    // Retorna um vetor String[] com a lista dos arquivos do caminho especificado
    public static String listFiles() {
        File fatherDirectory = new File(serverFilePath);
        String[] list = fatherDirectory.list();
        if (list == null) {
            list = new String[1];
            list[0] = "-";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            System.out.println("[SERVER] Error while processing list request: " + e.getMessage());
            return "-";
        }
    }

    @Override
    public void run() {
        String command, fileName, hash;
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            while (true) {

                JsonNode jsonNode = objectMapper.readTree(in.readUTF());
                command = jsonNode.get("command").asText();
                fileName = jsonNode.has("file") ? jsonNode.get("file").asText() : " ";
                hash = jsonNode.has("hash") ? jsonNode.get("hash").asText() : " ";

                switch (command) {
                    case "list":
                        out.writeUTF(listFiles());
                        out.flush();
                        break;
                    case "put":
                        receiveFile(fileName, hash);
                        break;
                    case "get":
                        sendFile(fileName);
                        break;
                    case "exit":
                        System.out.println("[SERVER] Client " + clientNumber + " disconnected");
                        break;
                }

                if (command.equals("exit")) {
                    break;
                }
            }

        } catch (JsonProcessingException e) {
            System.out.println("[SERVER] Error trying to process a Json: " + e.getMessage());
        } catch (IOException e) {
            System.out.println(
                    "[SERVER] Error in establishing a conection with client " + clientNumber + ": " + e.getMessage());
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("[SERVER] Error in closing resources: " + e.getMessage());
            }
        }
    }
}
