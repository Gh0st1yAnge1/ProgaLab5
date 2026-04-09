package org.example;

import org.example.manager.*;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientApp {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args){

        TerminalManager.enableRawMode();
        Runtime.getRuntime().addShutdownHook(new Thread(TerminalManager::disableRawMode));

        InputManager inputManager = new InputManager();
        ClientCommandManager commandManager = new ClientCommandManager(inputManager);
        SocketChannel socketChannel = null;

        try{
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);  //unblocking mode

            System.out.println("Connecting to server at" + HOST + ":" + PORT + "...");

            //Connecting loop
            while (!socketChannel.connect(new InetSocketAddress(HOST, PORT))){
                System.out.println("Server is not available. Retrying in 2 sec...");
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
                        commandManager.executeScript(request.argument(), socketChannel);
                        isRunning = false;
                    }

                    if (request.commandType() == CommandType.EXECUTE_SCRIPT){
                        commandManager.executeScript(request.argument(), socketChannel);
                    }


                    //to server
                    sendRequest(socketChannel, request);

                    //waiting for response
                    Response response = readResponse(socketChannel);

                    if (response.message() != null){
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(request);
        oos.flush();

        byte[] data = baos.toByteArray();

        ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();

        while (buffer.hasRemaining()){
            socketChannel.write(buffer);
        }
    }

    public static Response readResponse(SocketChannel socketChannel) throws IOException, ClassNotFoundException{

        //Read length
        ByteBuffer len = ByteBuffer.allocate(4);
        while (len.hasRemaining()){
            if (socketChannel.read(len) == -1) return null;
        }
        len.flip();
        int length = len.getInt();

        //Read data
        ByteBuffer dataBuf = ByteBuffer.allocate(length);
        while (dataBuf.hasRemaining()){
            if (socketChannel.read(dataBuf) == -1) return null;
        }

        dataBuf.flip();
        byte[] data = new byte[length];
        dataBuf.get(data);

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Response) ois.readObject();
    }
}