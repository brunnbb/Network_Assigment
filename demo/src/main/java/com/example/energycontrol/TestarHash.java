package com.example.energycontrol;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TestarHash {
    static String path = "demo\\src\\main\\java\\com\\example\\fileserver\\clientfiles";

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        String hash = hashFile();
        System.out.println(hash);
    }

    public static String hashFile() throws NoSuchAlgorithmException, IOException {
        byte[] fileToBytes;
        try {
            fileToBytes = Files
                    .readAllBytes(Paths.get(path + "\\" + "meme.txt"));
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
}
