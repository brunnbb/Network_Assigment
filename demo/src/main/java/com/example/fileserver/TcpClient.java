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
    private DataOutputStream out;
    private DataInputStream in;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        ObjectMapper objectMapper = new ObjectMapper();
        String filePath = null;

        try {
            Socket socket = new Socket("127.0.0.1", 6777);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showCommandMenu() {
        System.out.println("---------------------");
        System.out.println("LIST");
        System.out.println("PUT file_name");
        System.out.println("GET file_name");
        System.out.println("EXIT");
        System.out.println("---------------------");
    }

    public static String hashFile(String filePath) throws NoSuchAlgorithmException, IOException {
        byte[] fileToBytes = Files.readAllBytes(Paths.get(filePath));
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(fileToBytes);
        BigInteger bigInteger = new BigInteger(1, messageDigest);
        return bigInteger.toString(16);
    }

    public void receiveFile() {

    }

    public void sendFile() {

    }

}
