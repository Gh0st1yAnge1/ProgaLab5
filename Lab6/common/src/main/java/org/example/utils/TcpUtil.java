package org.example.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class TcpUtil {
    public static final int CHUNK_SIZE = 8 * 1024;

    private TcpUtil(){};

    public static Path serializeToTempFile(Object object) throws IOException {
        Path tempFile = Files.createTempFile("tcp-payload", ".bin");
        try (OutputStream outputStream = Files.newOutputStream(tempFile);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
        }
        return tempFile;
    }

    public static Object desirealizeFromFile(Path filePath) throws IOException, ClassNotFoundException{
        try (InputStream inputStream = Files.newInputStream(filePath);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)){
            return objectInputStream.readObject();
        }
    }

    public static void writeChunckedFromFile(SocketChannel socketChannel, Path filePath) throws IOException{
        ByteBuffer chunckLengthBuffer = ByteBuffer.allocate(4);
        byte[] chunk  = new byte[CHUNK_SIZE];

        try (InputStream inputStream = Files.newInputStream(filePath)){
            int read;
            while ((read = inputStream.read(chunk)) != -1){
                chunckLengthBuffer.clear();
                chunckLengthBuffer.putInt(read);
                chunckLengthBuffer.flip();
                while (chunckLengthBuffer.hasRemaining()){
                    socketChannel.write(chunckLengthBuffer);
                }

                ByteBuffer payloadBuffer = ByteBuffer.wrap(chunk,0, read);
                while (payloadBuffer.hasRemaining()){
                    socketChannel.write(payloadBuffer);
                }
            }
        }

        chunckLengthBuffer.clear();
        chunckLengthBuffer.putInt(0);
        chunckLengthBuffer.flip();
        while (chunckLengthBuffer.hasRemaining()){
            socketChannel.write(chunckLengthBuffer);
        }
    }
}
