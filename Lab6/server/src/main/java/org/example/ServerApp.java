package org.example;

import org.example.manager.CollectionManager;
import org.example.manager.FileManager;
import org.example.manager.ServerCommandExecutor;
import org.example.model.Route;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;
import org.example.utils.LoggerUtil;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApp
{
    private static final int PORT = 12345;
    private static final Logger logger = LoggerUtil.getLogger();
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ServerApp.class);

    private static CollectionManager collectionManager;
    private static FileManager fileManager;
    private static ServerCommandExecutor serverCommandExecutor;

    public static void main( String[] args ){
        logger.setLevel(Level.INFO);
        logger.info("=== LOADING SERVER ===");

        String filePath = System.getenv("FILE_PATH");
        if (filePath == null || filePath.trim().isEmpty()){
            logger.severe("ERROR: the env does not set.");
            return;
        }
        logger.info("File path: " + filePath);

        fileManager = new FileManager(filePath);
        collectionManager = new CollectionManager();

        logger.info("Try to load collection file.");
        try{
            LinkedHashMap<Integer, Route> loadedDatd = fileManager.loadCollection();
            if (loadedDatd == null)
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null){
            clientChannel.configureBlocking(false);

            clientChannel.register(selector, SelectionKey.OP_READ);
            logger.info("New connection from: " + clientChannel.getRemoteAddress());
        }
    }


    private static void handleRead(SelectionKey key) throws  IOException, ClassNotFoundException{
        SocketChannel clientChannel = (SocketChannel) key.channel();

        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);

        int bytesRead = clientChannel.read(lengthBuffer);

        if (bytesRead == -1){
            logger.info("Client disconnected:" + clientChannel.getRemoteAddress());
            key.cancel();
            clientChannel.close();
            return;
        }

        if (lengthBuffer.hasRemaining()){
            return;
        }

        lengthBuffer.flip();
        int dataLength = lengthBuffer.getInt();

        ByteBuffer dataBuffer = ByteBuffer.allocate(dataLength);
        while (dataBuffer.hasRemaining()){
            if (clientChannel.read(dataBuffer) == -1) break;
        }

        if (dataBuffer.hasRemaining()){
            return;
        }

        dataBuffer.flip();
        byte[] data = new byte[dataLength];
        dataBuffer.get(data);

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Request request = (Request) ois.readObject();

        logger.info("Got request: " + request.commandType() + " from " + clientChannel.getRemoteAddress());

        Response response = serverCommandExecutor.execute(request);

        sendResponse(clientChannel, response);
    }

    private static void sendResponse(SocketChannel channel, Response response) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(response);
        oos.flush();
        byte[] data = baos.toByteArray();

        ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();

        while (buffer.hasRemaining()){
            channel.write(buffer);
        }

        logger.fine("Response is sent to client.");

    }
}
