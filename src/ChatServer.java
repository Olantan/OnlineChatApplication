import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;  // <-- Add this import

public class ChatServer {
    private static final int PORT = 9090;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static Map<ClientHandler, String> clientUserIds = new ConcurrentHashMap<>();
    private static AtomicInteger userIdCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                String userId = "User" + userIdCounter.getAndIncrement();
                clientUserIds.put(clientHandler, userId);
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress() + " with ID " + userId);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    broadcast(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientHandlers.remove(this);
                clientUserIds.remove(this);
                System.out.println("Client disconnected.");
            }
        }

        private void broadcast(String message) {
            for (ClientHandler handler : clientHandlers) {
                handler.out.println(clientUserIds.get(this) + ": " + message);
            }
        }
    }
}
