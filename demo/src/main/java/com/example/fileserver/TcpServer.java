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
                Socket socket = serverSocket.accept(); // Accepts incoming client connections
                counter++;
                System.out.println("[SERVER] Connected to client " + counter);
                ClientHandler clientHandler = new ClientHandler(socket, counter);
                clientHandler.start(); // Starts the client handler thread
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Fatal error, shuting down");
            System.exit(0); // Shut down the server in case of a fatal error
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket; // The socket for communication with the client
    private int clientNumber; // The client's identifier
    private DataOutputStream out; // Output stream to send data to the client
    private DataInputStream in; // Input stream to receive data from the client
    private static String serverFilePath = "demo\\src\\main\\java\\com\\example\\fileserver\\serverfiles";
    private static ObjectMapper objectMapper; // ObjectMapper for JSON processing

    // Constructor to initialize the socket and client number
    public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        ClientHandler.objectMapper = new ObjectMapper();
    }

    // Method to return the MD5 hash of a file
    public String hashFile(String fileName) {
        byte[] fileToBytes;
        try {
            fileToBytes = Files.readAllBytes(Paths.get(serverFilePath + "\\" + fileName)); // Read file as bytes
            MessageDigest md = MessageDigest.getInstance("MD5"); // Get MD5 digest instance
            byte[] messageDigest = md.digest(fileToBytes); // Generate MD5 hash
            BigInteger bigInteger = new BigInteger(1, messageDigest); // Convert hash to BigInteger
            return bigInteger.toString(16); // Return hash as hexadecimal string
        } catch (IOException e) {
            System.out.println("[SERVER] Error in reading the file for hashing: " + e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("[SERVER] Error in hashing the file: " + e.getMessage());
            return null;
        }
    }

    // Method to return a JSON string of the list of files in the specified
    // directory
    public static String listFiles() {
        File fatherDirectory = new File(serverFilePath);
        String[] list = fatherDirectory.list(); // List files in the directory
        if (list == null) { // If directory is empty
            list = new String[1];
            list[0] = "-";
        }
        try {
            return objectMapper.writeValueAsString(list); // Convert file list to JSON string
        } catch (JsonProcessingException e) {
            System.out.println("[SERVER] Error while processing list request: " + e.getMessage());
            return "-";
        }
    }

    // Method to receive a file from the client
    public void receiveFile(String fileName, String hash, Long fileSize) {
        File fileToReceive = new File(serverFilePath + "\\" + fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToReceive))) {

            int retries = 0; // Counter for retries
            String status = "fail";

            while (retries < 5) { // Retry up to 4 times to receive file
                byte[] buffer = new byte[4096];
                int byteRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize && ((byteRead = in.read(buffer))) > 0) {
                    bos.write(buffer, 0, byteRead); // Write data to the output stream
                    totalBytesRead += byteRead;
                }
                bos.flush(); // Flush the output stream

                System.out.println(
                        "[SERVER] File " + fileName + " received from Client " + clientNumber + ", verifying hash...");

                String calculatedHash = hashFile(fileName); // Calculate the hash of the received file
                if (hash.equals(calculatedHash)) { // Check if hashes match
                    JsonNode payload = objectMapper.createObjectNode()
                            .put("file", fileName)
                            .put("operation", "put")
                            .put("status", "success");
                    out.writeUTF(objectMapper.writeValueAsString(payload)); // Send success response to client
                    out.flush();
                    status = "success";
                    break; // Exit retry loop
                } else {
                    retries++; // Increment retry counter
                    if (retries < 5) {
                        System.out.println("[SERVER] Fail with hash, retrying to receive file "
                                + fileName + " from Client " + clientNumber);
                        JsonNode payload = objectMapper.createObjectNode()
                                .put("file", fileName)
                                .put("operation", "put")
                                .put("status", "fail");
                        out.writeUTF(objectMapper.writeValueAsString(payload)); // Send fail response to client
                        out.flush();
                    }
                }
            }

            if (status.equals("success")) {
                System.out.println("[SERVER] File " + fileName + " successfully received from Client " + clientNumber);
            } else {
                fileToReceive.delete();
                System.out
                        .println("[SERVER] Failed to successfully receive file: " + fileName + " from Client "
                                + clientNumber);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Error receiving file: " + e.getMessage());
        }
    }

    // Method to send a file to the client
    public void sendFile(String fileName) {
        File fileToSend = new File(serverFilePath + "\\" + fileName);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSend))) {
            long fileSize = fileToSend.length(); // Get file size

            JsonNode payload = objectMapper.createObjectNode()
                    .put("file", fileName)
                    .put("operation", "get")
                    .put("hash", hashFile(fileName)) // Include file hash
                    .put("size", fileSize); // Include file size

            out.writeUTF(objectMapper.writeValueAsString(payload)); // Send initial file metadata to client
            out.flush();

            int retries = 0;
            String status = "fail";

            while (retries < 5) {

                byte[] buffer = new byte[4096];
                int byteRead;

                while (((byteRead = bis.read(buffer)) > 0)) { // Read file data from input stream
                    out.write(buffer, 0, byteRead); // Write data to output stream
                }
                out.flush(); // Flush the output stream

                System.out.println(
                        "[SERVER] File " + fileName + " sent to Client " + clientNumber + ", awaiting confirmation...");

                JsonNode response = objectMapper.readTree(in.readUTF()); // Read response from client
                status = response.get("status").asText();

                if (status.equals("success")) { // Check if the client successfully received the file
                    break; // Exit retry loop
                } else {
                    retries++; // Increment retry counter
                    System.out.println("[SERVER] Retrying to send file " + fileName + " to Client " + clientNumber);
                }
            }

            if (status.equals("success")) {
                System.out.println("[SERVER] File " + fileName + " successfully sent to Client " + clientNumber);
            } else {
                System.out.println(
                        "[SERVER] Failed to successfully send file : " + fileName + " to Client " + clientNumber);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Error while sending file: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String command, fileName, hash;
        Long fileSize;
        try {
            out = new DataOutputStream(socket.getOutputStream()); // Initialize output stream
            in = new DataInputStream(socket.getInputStream()); // Initialize input stream

            while (true) {

                JsonNode jsonNode = objectMapper.readTree(in.readUTF()); // Read JSON command from client
                command = jsonNode.get("command").asText(); // Extract command
                fileName = jsonNode.has("file") ? jsonNode.get("file").asText() : " "; // Extract file name if present
                hash = jsonNode.has("hash") ? jsonNode.get("hash").asText() : " "; // Extract hash if present
                fileSize = jsonNode.has("size") ? jsonNode.get("size").asLong() : 0; // Extract file size if present

                switch (command) {
                    case "list":
                        out.writeUTF(listFiles()); // Send list of files to client
                        out.flush();
                        break;
                    case "put":
                        receiveFile(fileName, hash, fileSize); // Receive file from client
                        break;
                    case "get":
                        sendFile(fileName); // Send file to client
                        break;
                    case "exit":
                        System.out.println("[SERVER] Client " + clientNumber + " disconnected");
                        break;
                }

                if (command.equals("exit")) {
                    break; // Exit the loop if client wants to disconnect
                }
            }

        } catch (JsonProcessingException e) {
            System.out.println("[SERVER] Error trying to process a Json: " + e.getMessage());
        } catch (IOException e) {
            System.out.println(
                    "[SERVER] Error in establishing a conection with client " + clientNumber + ": " + e.getMessage());
        } finally {
            try {
                in.close(); // Close input stream
                out.close(); // Close output stream
                socket.close(); // Close socket connection
            } catch (IOException e) {
                System.out.println("[SERVER] Error in closing resources: " + e.getMessage());
            }
        }
    }
}
