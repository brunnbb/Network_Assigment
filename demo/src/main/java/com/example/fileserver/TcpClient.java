package com.example.fileserver;

import java.util.*;
import java.io.*;
import java.net.*;
import java.math.*;
import java.nio.file.*;
import java.security.*;
import com.fasterxml.jackson.databind.*;

public class TcpClient {
    private static String clientFilePath = "demo\\src\\main\\java\\com\\example\\fileserver\\clientfiles";

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        ObjectMapper mapper = new ObjectMapper();

        // 192.168.0.75
        try (Socket socket = new Socket("localhost", 6777)) {
            System.out.println("------------------------------------------------------");
            System.out.println("Connected to the file server");
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (true) {
                int command = 0;
                try {
                    showCommandMenu();
                    command = Integer.parseInt(input.nextLine());

                } catch (NumberFormatException e) {
                    System.out.println("------------------------------------------------------");
                    System.out.println("Unknown command");

                }

                switch (command) {
                    case (1):
                        getFileList(mapper, in, out);
                        break;
                    case (2):
                        putFile(mapper, in, out);
                        break;
                    case (3):
                        getFile(mapper, in, out);
                        break;
                    case (4):
                        out.writeUTF("{ \"command\" : \"exit\"}");
                        out.flush();
                        break;
                    default:
                        System.out.println("------------------------------------------------------");
                        System.err.println("Please, type in one of the numbers of the menu");
                        break;
                }

                if (command == 4) {
                    System.out.println("------------------------------------------------------");
                    System.out.println("Exiting server...");
                    System.out.println("------------------------------------------------------");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            input.close();
        }
    }

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

    public static String hashFile(String fileName) throws NoSuchAlgorithmException, IOException {
        byte[] fileToBytes;
        try {
            fileToBytes = Files.readAllBytes(Paths.get(clientFilePath + "\\" + fileName));
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(fileToBytes);
            BigInteger bigInteger = new BigInteger(1, messageDigest);
            return bigInteger.toString(16);
        } catch (IOException e) {
            System.out.println("[Error] Could not read the file for hashing: " + e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("[Error] Could not hash the file: " + e.getMessage());
            return null;
        }
    }

    public static void getFileList(ObjectMapper mapper, DataInputStream in, DataOutputStream out) {
        try {
            String payload = "{ \"command\" : \"list\"}";
            out.writeUTF(payload);
            out.flush();
            String[] fileList = mapper.readValue(in.readUTF(), String[].class);

            System.out.println("------------------------------------------------------");
            System.out.println("Files available at the server: ");
            System.out.println("------------------------------------------------------");
            for (String item : fileList) {
                System.out.println(item);
            }

        } catch (IOException e) {
            System.out.println("[Error] Could not send/read file");
        }
    }

    public static void putFile(ObjectMapper mapper, DataInputStream in, DataOutputStream out) {
        Scanner input = new Scanner(System.in);
        System.out.println("------------------------------------------------------");
        System.out.print("Name the file you want to put in the server, with the proper ext: ");
        String fileName = input.nextLine();
        System.out.println("------------------------------------------------------");

        File fileToSend = new File(clientFilePath + "\\" + fileName);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSend))) {
            long fileSize = fileToSend.length();

            JsonNode payload = mapper.createObjectNode()
                    .put("command", "put")
                    .put("file", fileName)
                    .put("hash", hashFile(fileName))
                    .put("size", fileSize);

            out.writeUTF(mapper.writeValueAsString(payload));
            out.flush();

            int retries = 0;
            String status = "fail";

            while (retries < 5) {

                byte[] buffer = new byte[4096];
                int byteRead;

                while ((byteRead = bis.read(buffer)) > 0) {
                    out.write(buffer, 0, byteRead);
                }
                out.flush();

                System.out.println("File " + fileName + " sent, awaiting confirmation...");

                JsonNode response = mapper.readTree(in.readUTF());
                status = response.get("status").asText();

                if (status.equals("success")) {
                    break;
                } else {
                    retries++;
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

    public static void getFile(ObjectMapper mapper, DataInputStream in, DataOutputStream out) {
        Scanner input = new Scanner(System.in);
        System.out.println("------------------------------------------------------");
        System.out.print("Name the file you want to get from the server, with the proper ext: ");
        String fileName = input.nextLine();
        System.out.println("------------------------------------------------------");

        File fileToReceive = new File(clientFilePath + "\\" + fileName);

        try {
            String checkExistence = "{ \"command\" : \"list\"}";
            out.writeUTF(checkExistence);
            out.flush();
            String[] fileList = mapper.readValue(in.readUTF(), String[].class);

            boolean exists = false;
            for (String item : fileList) {
                if (fileName.equals(item)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                fileToReceive.delete();
                System.out.println("The file requested does not exist in the server folder");
                return;
            }

            JsonNode payload = mapper.createObjectNode()
                    .put("command", "get")
                    .put("file", fileName);
            out.writeUTF(mapper.writeValueAsString(payload));
            out.flush();

            JsonNode response = mapper.readTree(in.readUTF());
            String receivedHash = response.get("hash").asText();
            long fileSize = response.get("size").asLong();

            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToReceive))) {
                int retries = 0;
                String status = "fail";

                while (retries < 5) {
                    byte[] buffer = new byte[4096];
                    int byteRead;
                    long totalBytesRead = 0;

                    while (totalBytesRead < fileSize && ((byteRead = in.read(buffer)) > 0)) {
                        bos.write(buffer, 0, byteRead);
                        totalBytesRead += byteRead;
                    }
                    bos.flush();

                    System.out.println("File " + fileName + " received, verifying hash...");

                    String calculatedHash = hashFile(fileName);
                    if (receivedHash.equals(calculatedHash)) {
                        JsonNode confirmation = mapper.createObjectNode()
                                .put("file", fileName)
                                .put("operation", "get")
                                .put("status", "success");
                        out.writeUTF(mapper.writeValueAsString(confirmation));
                        out.flush();
                        status = "success";
                        break;
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
                    fileToReceive.delete();
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
