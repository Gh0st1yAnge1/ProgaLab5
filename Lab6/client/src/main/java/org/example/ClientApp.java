package org.example;

import org.example.manager.*;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;
import org.example.utils.RouteBuilder;
import org.example.utils.TcpUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientApp {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args){

        TerminalManager.enableRawMode();
        Runtime.getRuntime().addShutdownHook(new Thread(TerminalManager::disableRawMode));

        InputManager inputManager = new InputManager();
        RouteBuilder routeBuilder = new RouteBuilder(inputManager);
        ClientCommandManager commandManager = new ClientCommandManager(inputManager, routeBuilder);
        SocketChannel socketChannel = null;

        try{
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);  //unblocking mode

            System.out.println("Connecting to server at" + HOST + ":" + PORT + "...");

            socketChannel.connect(new InetSocketAddress(HOST, PORT));
            //Connecting loop
            while (!socketChannel.finishConnect()){
                System.out.println("Server is not available. Retrying...");
                Thread.sleep(2000);
            }

            System.out.println("Successfully connected!");

            //Main loop
            boolean isRunning = true;
            while (isRunning){
                System.out.print("> ");
                String input = inputManager.readline();

                try {
                    Request request = commandManager.execute(input, socketChannel);
                    if (request == null){
                        continue;
                    }
                    if (request.commandType() == CommandType.EXIT){
                        isRunning = false;
                        continue;
                    }

                    //to server
                    sendRequest(socketChannel, request);

                    //waiting for response
                    Response response = readResponse(socketChannel);

                    if (!response.message().isEmpty()){
                        System.out.println(response.message());
                        if (response.collection() != null){
                            System.out.println(response.collection());
                        }
                    } else {
                        System.out.println("Server disconnected.");
                        isRunning = false;
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println(e.getMessage());;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());;
        } finally {
            try {
                if (socketChannel != null) socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            TerminalManager.disableRawMode();
        }
    }


    public static void sendRequest(SocketChannel socketChannel, Request request) throws IOException{
        Path payLoadFile = TcpUtil.serializeToTempFile(request);
        try {
            TcpUtil.writeChunckedFromFile(socketChannel, payLoadFile);
        } finally {
            Files.deleteIfExists(payLoadFile);
        }
    }

    public static Response readResponse(SocketChannel socketChannel) throws IOException, ClassNotFoundException, InterruptedException {
        Path payLoadFile = Files.createTempFile("tcp-response", ".bin");
        try (OutputStream outputStream = Files.newOutputStream(payLoadFile)) {
            byte[] transferBuffer = new byte[TcpUtil.CHUNK_SIZE];
            while (true){
                ByteBuffer chunckLengthBuffer = ByteBuffer.allocate(4);
                while (chunckLengthBuffer.hasRemaining()){
                    int bytesRead = socketChannel.read(chunckLengthBuffer);
                    if (bytesRead == -1){
                        return null;
                    }
                    if (bytesRead == 0){
                        Thread.sleep(5);
                    }
                }
                chunckLengthBuffer.flip();
                int chunckLength = chunckLengthBuffer.getInt();
                if (chunckLength < 0){
                    throw new IOException("Invalid chunk size:" + chunckLength);
                }
                if (chunckLength == 0) {
                    break;
                }

                int remaining  = chunckLength;
                while (remaining > 0) {
                    int portion = Math.min(remaining, transferBuffer.length);
                    ByteBuffer payloadBuffer = ByteBuffer.wrap(transferBuffer, 0, portion);
                    while (payloadBuffer.hasRemaining()){
                        int bytesRead = socketChannel.read(payloadBuffer);
                        if (bytesRead == -1){
                            return null;
                        }
                        if  (bytesRead == 0){
                            Thread.sleep(5);
                        }
                    }
                    outputStream.write(transferBuffer, 0, portion);
                    remaining -= portion;
                }
            }
        }
        try {
            return (Response) TcpUtil.desirealizeFromFile(payLoadFile);
        } finally {
            Files.deleteIfExists(payLoadFile);
        }
    }
}