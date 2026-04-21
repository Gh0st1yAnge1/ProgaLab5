package org.example;

import org.example.manager.CollectionManager;
import org.example.manager.FileManager;
import org.example.manager.ServerCommandExecutor;
import org.example.model.Route;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;
import org.example.utils.LoggerUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApp
{
    private static final int PORT = 12345;
    private static final Logger logger = LoggerUtil.getLogger();

    private static CollectionManager collectionManager;
    private static FileManager fileManager;
    private static ServerCommandExecutor serverCommandExecutor;

    public static void main(String[] args ){
        logger.setLevel(Level.INFO);
        logger.info("=== LOADING SERVER ===");

        String filePath = System.getenv("FILE_PATH");
        if (filePath == null || filePath.trim().isEmpty()){
            logger.severe("ERROR: FILE_PATH environmental variable is not set.");
            return;
        }
        logger.info("File path: " + filePath);

        fileManager = new FileManager(filePath);
        collectionManager = new CollectionManager();
        serverCommandExecutor = new ServerCommandExecutor(collectionManager, fileManager);


        logger.info("Try to load collection file.");
        LinkedHashMap<Integer, Route> loadedData = fileManager.loadCollection();
        collectionManager.load(loadedData);
        logger.info("Collection loaded. Elements count: " + loadedData.size());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Response saveResult = serverCommandExecutor.execute(
                    new Request(CommandType.SAVE_SERVER, null,null)
            );
            logger.info("Shutdown save status: " + saveResult.message());
        }));

        logger.info("Starting network server loop...");
        runServer();
    }

    private static void runServer(){
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){

            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server started on port: " + PORT);

            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                var iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()){
                        continue;
                    }

                    try {
                        if (key.isAcceptable()){
                            handleAccept(key, selector);
                        }

                        if (key.isReadable()){
                            handleRead(key);
                        }
                    } catch (IOException | ClassNotFoundException e){
                        logger.severe("Error while handling client event: " + e.getMessage());
                        closeClient(key);
                    }

                }
            }

        } catch (IOException e) {
            logger.severe("Server startup error: " + e.getMessage());
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);

            clientChannel.register(selector, SelectionKey.OP_READ); new ClientSession();
            logger.info("New connection from: " + clientChannel.getRemoteAddress());
        }
    }


    private static void handleRead(SelectionKey key) throws IOException, ClassNotFoundException{
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientSession session = (ClientSession) key.attachment();
        if (session == null){
            session = new ClientSession();
            key.attach(session);
        }

        if (session.lengthBuffer.hasRemaining()){
            int bytesRead = clientChannel.read(session.lengthBuffer);
            if (bytesRead == -1){
                closeClient(key);
                return;
            }

            if (session.lengthBuffer.hasRemaining()){
                return;
            }
            session.lengthBuffer.flip();
            int dataLength = session.lengthBuffer.getInt();
            if (dataLength < 0 || dataLength > 10_000_000){
                logger.warning("Invalid request size from " + clientChannel.getRemoteAddress() + ": " + dataLength);
                closeClient(key);
                return;
            }

            session.databuffer = ByteBuffer.allocate(dataLength);
        }

        Objects.requireNonNull(session.databuffer, "Data buffer must be initialized");
        int bytesRead = clientChannel.read(session.databuffer);
        if (bytesRead == -1){
            closeClient(key);
            return;
        }

        if (session.databuffer.hasRemaining()){
            return;
        }

        session.databuffer.flip();
        byte[] data = new byte[session.databuffer.remaining()];
        session.databuffer.get(data);

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Request request = (Request) ois.readObject();


        Response response = serverCommandExecutor.execute(request);
        logger.info("Got request: " + request.commandType() + " from " + clientChannel.getRemoteAddress());
        sendResponse(clientChannel, response);
        session.reset();
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

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        logger.fine("Response is sent to client.");

    }

    private static void closeClient(SelectionKey key) throws IOException{
        SocketChannel channel = (SocketChannel) key.channel();
        logger.info("Client disconnected: " + channel.getRemoteAddress());
        key.cancel();
        channel.close();
    }

    private static class ClientSession{
        private ByteBuffer lengthBuffer  = ByteBuffer.allocate(4);
        private ByteBuffer databuffer;

        private void reset(){
            this.lengthBuffer = ByteBuffer.allocate(4);
            this.databuffer = null;
        }
    }
}
