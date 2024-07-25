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
        Scanner input = new Scanner(System.in);
        ObjectMapper mapper = new ObjectMapper();

        try (Socket socket = new Socket("127.0.0.1", 6777)) {
            System.out.println("------------------------------------------------------");
            System.out.println("Connected to the file server");
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            int command;
            while (true) {

                showCommandMenu();
                command = Integer.parseInt(input.nextLine());

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
                        System.out.println("Unknown command");
                        break;
                }

                if (command == 4) {
                    System.out.println("Exiting server...");
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
        System.out.println("2 - PUT file_name");
        System.out.println("3 - GET file_name");
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
        System.out.print("Name the file you want to put in the server: ");
        String fileName = input.nextLine();
        System.out.println("------------------------------------------------------");

        File fileToSend = new File(clientFilePath + "\\" + fileName);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSend))) {

            JsonNode payload = mapper.createObjectNode()
                    .put("command", "put")
                    .put("file", fileName)
                    .put("hash", hashFile(fileName));
            out.writeUTF(mapper.writeValueAsString(payload));

            int retries = 0;
            String status = "fail";

            while (retries < 3) {
                byte[] buffer = new byte[8192];
                int byteRead;

                while ((byteRead = bis.read(buffer)) > 0) {
                    out.write(buffer, 0, byteRead);
                }
                out.flush();

                JsonNode response = mapper.readTree(in.readUTF());
                if (response.get("status").asText().equals("success")) {
                    status = "success";
                    break;
                } else {
                    retries++;
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
        System.out.print("Name the file you want to get from the server: ");
        String fileName = input.nextLine();
        System.out.println("------------------------------------------------------");

        File fileToReceive = new File(clientFilePath + "\\" + fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToReceive))) {

            JsonNode payload = mapper.createObjectNode()
                    .put("command", "get")
                    .put("file", fileName);
            out.writeUTF(mapper.writeValueAsString(payload));

            JsonNode response = mapper.readTree(in.readUTF());
            String receivedHash = response.get("hash").asText();

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
                    if (retries < 3) {
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
                System.out.println("Failed to receive file : " + fileName + " from server");
            }

        } catch (IOException e) {
            System.err.println("[ERROR] Failed to receive the file: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("[ERROR] Failed to calculate the file hash: " + e.getMessage());
        }
    }

}
