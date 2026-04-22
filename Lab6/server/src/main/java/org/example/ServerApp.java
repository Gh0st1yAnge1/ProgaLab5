package org.example;

import org.example.manager.CollectionManager;
import org.example.manager.FileManager;
import org.example.manager.ServerCommandExecutor;
import org.example.model.Route;
import org.example.request_and_response.CommandType;
import org.example.request_and_response.Request;
import org.example.request_and_response.Response;
import org.example.utils.LoggerUtil;
import org.example.utils.TcpUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
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

            clientChannel.register(selector, SelectionKey.OP_READ, new ClientSession());
            logger.info("New connection from: " + clientChannel.getRemoteAddress());
        }
    }


    private static void handleRead(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientSession session = (ClientSession) key.attachment();
        if (session == null) {
            session = new ClientSession();
            key.attach(session);
        }

        while (true){
            if (session.remainingChunkBytes == null){
                int bytesRead = clientChannel.read(session.chunkLengthBuffer);
                if (bytesRead == -1){
                    closeClient(key);
                    return;
                }

                if (session.chunkLengthBuffer.hasRemaining()){
                    return;
                }

                session.chunkLengthBuffer.flip();
                int chunkLength = session.chunkLengthBuffer.getInt();
                session.chunkLengthBuffer.clear();

                if (chunkLength < 0 || chunkLength > TcpUtil.CHUNK_SIZE){
                    logger.warning("Invalid chunk size from " + clientChannel.getRemoteAddress() + ": " + chunkLength);
                    closeClient(key);
                    return;
                }

                if (chunkLength == 0){
                    session.payloadOutputStream.flush();
                    Request request = (Request) TcpUtil.desirealizeFromFile(session.payloadFile);
                    Response response = serverCommandExecutor.execute(request);
                    logger.info("Got request: " + request.commandType() + " from " + clientChannel.getRemoteAddress());
                    sendResponse(clientChannel, response);
                    session.reset();
                    return;
                }

                session.remainingChunkBytes = chunkLength;
            }

            session.payloadBuffer.clear();
            int portion = Math.min(session.remainingChunkBytes, session.payloadBuffer.capacity());
            session.payloadBuffer.limit(portion);

            int bytesRead = clientChannel.read(session.payloadBuffer);
            if (bytesRead == -1) {
                closeClient(key);
                return;
            }
            if (bytesRead == 0) {
                return;
            }

            session.payloadOutputStream.write(session.payloadBuffer.array(), 0, bytesRead);
            session.remainingChunkBytes -= bytesRead;

            if (session.remainingChunkBytes == 0) {
                session.remainingChunkBytes = null;
            }
        }
    }

    private static void sendResponse(SocketChannel channel, Response response) throws IOException {

        Path payloadFile = TcpUtil.serializeToTempFile(response);
        try {
            TcpUtil.writeChunckedFromFile(channel, payloadFile);
            logger.fine("Response is sent to client.");
        } finally {
            Files.deleteIfExists(payloadFile);
        }

    }

    private static void closeClient(SelectionKey key) throws IOException{
        SocketChannel channel = (SocketChannel) key.channel();
        logger.info("Client disconnected: " + channel.getRemoteAddress());

        ClientSession session = (ClientSession) key.attachment();
        if (session != null) {
            session.cleanUp();
        }

        key.cancel();
        channel.close();
    }

    private static class ClientSession{
        private ByteBuffer chunkLengthBuffer;
        private ByteBuffer payloadBuffer;
        private Integer remainingChunkBytes;
        private Path payloadFile;
        private OutputStream payloadOutputStream;

        private ClientSession() throws IOException{
            init();
        }

        private void init() throws IOException {
            this.chunkLengthBuffer = ByteBuffer.allocate(4);
            this.remainingChunkBytes = null;
            this.payloadBuffer = ByteBuffer.allocate(TcpUtil.CHUNK_SIZE);
            this.payloadFile = Files.createTempFile("tcp-request", ".bin");
            this.payloadOutputStream = Files.newOutputStream(this.payloadFile);
        }

        private void cleanUp() throws IOException {
            if (this.payloadOutputStream != null){
                this.payloadOutputStream.close();
            }
            if (this.payloadFile != null) {
                Files.deleteIfExists(this.payloadFile);
            }
        }

        private void reset() throws IOException{
            cleanUp();
            init();
        }
    }
}
