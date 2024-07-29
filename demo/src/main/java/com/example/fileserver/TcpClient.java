package com.example.fileserver;

import java.util.*;
import java.io.*;
import java.net.*;
import java.math.*;
import java.nio.file.*;
import java.security.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TcpClient {
    private static String clientFilePath = "demo\\src\\main\\java\\com\\example\\fileserver\\clientfiles";

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in); // Scanner for user input
        ObjectMapper mapper = new ObjectMapper(); // ObjectMapper for JSON processing

        try (Socket socket = new Socket("127.0.0.1", 6777)) { // Connect to the server on localhost, port 6777
            System.out.println("------------------------------------------------------");
            System.out.println("Connected to the file server");
            // Output stream to send data to the server
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            // Input stream to receive data from the server
            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (true) {
                int command = 0;
                try {
                    showCommandMenu(); // Display command menu to the user
                    command = Integer.parseInt(input.nextLine()); // Read user command

                } catch (NumberFormatException e) {
                    System.err.println("Please, type in one of the numbers of the menu");
                }

                switch (command) {
                    case (1):
                        getFileList(mapper, in, out); // List files on the server
                        break;
                    case (2):
                        putFile(mapper, in, out); // Send a file to the server
                        break;
                    case (3):
                        getFile(mapper, in, out); // Receive a file from the server
                        break;
                    case (4):
                        out.writeUTF("{ \"command\" : \"exit\"}"); // Send exit command to the server
                        out.flush();
                        break;
                    default:
                        System.out.println("Chose: Unknown command");
                        break;
                }

                if (command == 4) {
                    System.out.println("Exiting server...");
                    break; // Exit the loop if the user chose to exit
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            input.close();
        }
    }

    // Display command menu to the user
    public static void showCommandMenu() {
        System.out.println("------------------------------------------------------");
        System.out.println("Choose one of the following numbers for commands: ");
        System.out.println("------------------------------------------------------");
        System.out.println("1 - LIST");
        System.out.println("2 - PUT file_name.ext");
        System.out.println("3 - GET file_name.ext");
        System.out.println("4 - EXIT");
        System.out.println("------------------------------------------------------");
    }

    // Method to generate the MD5 hash of a file
    public static String hashFile(String fileName) throws NoSuchAlgorithmException, IOException {
        byte[] fileToBytes;
        try {
            fileToBytes = Files.readAllBytes(Paths.get(clientFilePath + "\\" + fileName)); // Read file as bytes
            MessageDigest md = MessageDigest.getInstance("MD5"); // Get MD5 digest instance
            byte[] messageDigest = md.digest(fileToBytes); // Generate MD5 hash
            BigInteger bigInteger = new BigInteger(1, messageDigest); // Convert hash to BigInteger
            return bigInteger.toString(16); // Return hash as hexadecimal string
        } catch (IOException e) {
            System.out.println("[Error] Could not read the file for hashing: " + e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("[Error] Could not hash the file: " + e.getMessage());
            return null;
        }
    }

    // Method to get the list of files from the server
    public static void getFileList(ObjectMapper mapper, DataInputStream in, DataOutputStream out) {
        try {
            String payload = "{ \"command\" : \"list\"}"; // Create JSON payload for listing files
            out.writeUTF(payload); // Send the payload to the server
            out.flush();
            String[] fileList = mapper.readValue(in.readUTF(), String[].class); // Read and parse the server response

            System.out.println("------------------------------------------------------");
            System.out.println("Files available at the server: ");
            System.out.println("------------------------------------------------------");
            for (String item : fileList) {
                System.out.println(item); // Print each file name
            }

        } catch (IOException e) {
            System.out.println("[Error] Could not send/read file");
        }
    }

    // Method to send a file to the server
    public static void putFile(ObjectMapper mapper, DataInputStream in, DataOutputStream out) {
        Scanner input = new Scanner(System.in);
        System.out.println("------------------------------------------------------");
        System.out.print("Name the file you want to put in the server, with the proper ext: ");
        String fileName = input.nextLine(); // Read file name from user
        System.out.println("------------------------------------------------------");

        File fileToSend = new File(clientFilePath + "\\" + fileName); // Create File object for the file to send
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSend))) {
            long fileSize = fileToSend.length(); // Get the file size

            JsonNode payload = mapper.createObjectNode()
                    .put("command", "put") // Command to put a file on the server
                    .put("file", fileName)
                    .put("hash", hashFile(fileName)) // Include file hash
                    .put("size", fileSize); // Include file size

            out.writeUTF(mapper.writeValueAsString(payload)); // Send file metadata to the server
            out.flush();

            int retries = 0;
            String status = "fail";

            while (retries < 5) { // Retry up to 4 times if necessary

                byte[] buffer = new byte[4096];
                int byteRead;

                while ((byteRead = bis.read(buffer)) > 0) { // Read file data and send to the server
                    out.write(buffer, 0, byteRead);
                }
                out.flush();

                System.out.println("File " + fileName + " sent, awaiting confirmation...");

                JsonNode response = mapper.readTree(in.readUTF()); // Read server response
                status = response.get("status").asText();

                if (status.equals("success")) { // If server confirms successful reception, break the loop
                    break;
                } else {
                    retries++; // Increment retry counter
                    System.out.println("Retrying to send file " + fileName + " to server");
                }
            }

            if (status.equals("success")) {
                System.out.println("File " + fileName + " was sent to server");
            } else {
                System.out.println("Failed to sent file : " + fileName + " to server");
            }

        } catch (FileNotFoundException e) {
            System.err.println("[Error] The file " + fileName + " does not exist");
        } catch (IOException e) {
            System.err.println("[Error] Problem reading/sending the file");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("[Error] Problem hashing the file");
        }
    }

    // Method to receive a file from the server
    public static void getFile(ObjectMapper mapper, DataInputStream in, DataOutputStream out) {
        Scanner input = new Scanner(System.in);
        System.out.println("------------------------------------------------------");
        System.out.print("Name the file you want to get from the server, with the proper ext: ");
        String fileName = input.nextLine(); // Read file name from user
        System.out.println("------------------------------------------------------");

        File fileToReceive = new File(clientFilePath + "\\" + fileName); // Create File object for the file to receive

        try {
            // First, check if the file exists on the server
            String checkExistence = "{ \"command\" : \"list\"}";
            out.writeUTF(checkExistence); // Send list command to the server
            out.flush();
            String[] fileList = mapper.readValue(in.readUTF(), String[].class); // Read the list of files from server

            boolean exists = false;
            for (String item : fileList) {
                if (fileName.equals(item)) {
                    exists = true; // If file is found in the list, set exists to true
                    break;
                }
            }

            if (!exists) {
                fileToReceive.delete(); // Delete the file if it was accidentally created
                System.out.println("The file requested does not exist in the server folder");
                return;
            }

            JsonNode payload = mapper.createObjectNode()
                    .put("command", "get") // Command to get a file from the server
                    .put("file", fileName);
            out.writeUTF(mapper.writeValueAsString(payload)); // Send request to the server
            out.flush();

            JsonNode response = mapper.readTree(in.readUTF()); // Read server response
            String receivedHash = response.get("hash").asText(); // Get file hash from response
            long fileSize = response.get("size").asLong(); // Get file size from response

            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToReceive))) {
                int retries = 0;
                String status = "fail";

                while (retries < 5) { // Retry up to 4 times if necessary
                    byte[] buffer = new byte[4096];
                    int byteRead;
                    long totalBytesRead = 0;

                    while (totalBytesRead < fileSize && ((byteRead = in.read(buffer)) > 0)) { // Read file data from
                                                                                              // server
                        bos.write(buffer, 0, byteRead); // Write data to the output stream
                        totalBytesRead += byteRead;
                    }
                    bos.flush();

                    System.out.println("File " + fileName + " received, verifying hash...");

                    String calculatedHash = hashFile(fileName); // Calculate the hash of the received file
                    if (receivedHash.equals(calculatedHash)) { // If hashes match, send success confirmation to server
                        JsonNode confirmation = mapper.createObjectNode()
                                .put("file", fileName)
                                .put("operation", "get")
                                .put("status", "success");
                        out.writeUTF(mapper.writeValueAsString(confirmation));
                        out.flush();
                        status = "success";
                        break; // Exit retry loop
                    } else {
                        retries++;
                        if (retries < 5) {
                            System.out.println("Retrying to get file " + fileName + " from server");
                            JsonNode confirmation = mapper.createObjectNode()
                                    .put("file", fileName)
                                    .put("operation", "get")
                                    .put("status", "fail");
                            out.writeUTF(mapper.writeValueAsString(confirmation));
                            out.flush();
                        }
                    }
                }

                if (status.equals("success")) {
                    System.out.println("File " + fileName + " was received from server");
                } else {
                    fileToReceive.delete(); // Delete the file if it was accidentally created
                    System.out.println("Failed to receive file: " + fileName + " from server");
                }

            } catch (IOException e) {
                System.err.println("[ERROR] Failed to receive the file: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("[ERROR] Failed to calculate the file hash: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("[ERROR] Failed to communicate with the server: " + e.getMessage());
        }
    }

}
