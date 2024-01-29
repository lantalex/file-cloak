package io.github.lantalex;

import io.github.lantalex.cloaked.FileCloak;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class Sample {

    public static final String FILE_NAME = "C:\\Users\\lantalex\\Desktop\\starfive-jh7110-202311-SD-minimal-desktop.img";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        long now = System.nanoTime();

        FileCloak fileCloak = FileCloak.create(Path.of(FILE_NAME));

        System.out.println("Creating cloaked file: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - now) + " ms");
        System.out.println();

        now = System.nanoTime();
        try (InputStream is = fileCloak.getInputStream()) {
            System.out.println("MD5 cloaked: " + calculateMd5(is));
        }
        System.out.println("time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - now) + "ms ");
        System.out.println();

        fileCloak.close();

        now = System.nanoTime();
        try (InputStream is = new FileInputStream(FILE_NAME)) {
            System.out.println("MD5 original: " + calculateMd5(is));
        }
        System.out.println("time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - now) + "ms ");
        System.out.println();
    }


    private static String calculateMd5(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int read;

        while ((read = is.read(buffer, 0, bufferSize)) >= 0) {
            if (read > 0) {
                md.update(buffer, 0, read);
            }
        }

        return byteToHexString(md.digest());
    }

    private static String byteToHexString(byte[] input) {
        StringBuilder output = new StringBuilder();
        for (byte b : input) {
            output.append(String.format("%02X", b));
        }
        return output.toString();
    }
}