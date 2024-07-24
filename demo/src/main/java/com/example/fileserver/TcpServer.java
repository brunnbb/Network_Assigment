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
        try {
            ServerSocket serverSocket = new ServerSocket(6777);
            int counter = 0;
            System.out.println("[SERVER] Wainting for client connection...");
            while (true) {
                Socket socket = serverSocket.accept();
                counter++;
                System.out.println("[SERVER] Connected to client " + counter);
                ClientHandler clientHandler = new ClientHandler(socket, counter);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Fatal error, shutingdown");
            System.exit(0);
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private int clienteNumber;
    private DataOutputStream out;
    private DataInputStream in;
    private static String serverFilePath = "demo\\src\\main\\java\\com\\example\\fileserver\\serverfiles";
    private static ObjectMapper objectMapper;

    // Inicializa o socket e o objectMapper(Para formatar e ler as mensagems como
    // objetos json)
    public ClientHandler(Socket socket, int clienteNumber) {
        this.socket = socket;
        this.clienteNumber = clienteNumber;
        ClientHandler.objectMapper = new ObjectMapper();
    }

    // Retorna um hash MD5
    public String hashFile(String fileName) throws IOException, NoSuchAlgorithmException {
        byte[] fileToBytes;
        fileToBytes = Files.readAllBytes(Paths.get(serverFilePath + "\\" + fileName));
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(fileToBytes);
        BigInteger bigInteger = new BigInteger(1, messageDigest);
        return bigInteger.toString(16);

    }

    // Vai receber um arquivo, com o in
    public void receiveFile(String fileName, String hash) {
        File fileToReceive = new File(serverFilePath + "\\" + fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileToReceive))) {

            byte[] buffer = new byte[1024];
            int byteRead;

            while ((byteRead = in.read(buffer)) != -1) {
                bos.write(buffer, 0, byteRead);
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

            int retries = -1;
            String status;

            do {
                if (retries > -1) {
                    System.out.println("[SERVER] Retrying to send file " + fileName + " to Client" + clienteNumber);
                }

                byte[] buffer = new byte[1024];
                int byteRead;

                // Envia o arquivo em pedaços de 1024
                while ((byteRead = bis.read(buffer)) > 0) {
                    out.write(buffer, 0, byteRead);
                }
                out.flush();

                // Aguarda confirmação de recebimento do cliente
                JsonNode response = objectMapper.readTree(in.readUTF());
                status = response.get("status").asText();
                retries++;

            } while (status.equals("fail") && retries < 3);

            if (status.equals("success")) {
                System.out.println("[SERVER] File " + fileName + "sent to Client " + clienteNumber);
            } else {
                System.out.println("[SERVER] Failed to sent file : " + fileName + " to Client " + clienteNumber);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Error while sending file: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("[SERVER] Error calculating hash: " + e.getMessage());
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
            System.out.println("Error while processing list request");
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
                command = jsonNode.get("command").asText().toUpperCase();
                fileName = jsonNode.has("file") ? jsonNode.get("file").asText() : " ";
                hash = jsonNode.has("hash") ? jsonNode.get("hash").asText() : " ";

                switch (command) {
                    case "LIST":
                        out.writeUTF(listFiles());
                        break;
                    case "PUT":
                        receiveFile(fileName, hash);
                        break;
                    case "GET":
                        sendFile(fileName);
                        break;
                    case "EXIT":
                        System.out.println("[SERVER] Client " + clienteNumber + " disconnected");
                        break;
                }

                if (command.equals("EXIT")) {
                    break;
                }
            }

            in.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
