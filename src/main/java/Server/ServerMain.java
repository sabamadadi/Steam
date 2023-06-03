package Server;

import Shared.Response;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ServerMain {
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException, SQLException {
        int portNumber = 1234;
        ServerMain server = new ServerMain(portNumber);
        server.start();
    }

    public ServerMain(int portNumber) throws IOException {
        this.serverSocket = new ServerSocket(portNumber);
    }

    public void start() throws SQLException {
        System.out.println("Server started.");
        Connection connection = connectSQL();

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, connection);
                clients.add(handler);
                handler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Connection connectSQL() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/STEAM";
        String user = "postgres";
        String pass = "0000";
        Connection connection = DriverManager.getConnection(url, user, pass);
        System.out.println("Connected .....");
        return connection;
    }

    public class ClientHandler extends Thread {
        private Socket socket;
        private Connection connection;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket, Connection connection) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.connection = connection;
        }

        public void run() {
            try {
                Statement statement = connection.createStatement();
                this.out.println(Response.MenuResponse());
                String request;

                while ((request = in.readLine()) != null) {
                    if (!request.equals("null")) {
                        JSONObject jsonRequest = new JSONObject(request);
                        if (jsonRequest.getString("type").equals("exit")) {
                            socket.close();
                            clients.remove(this);
                        } else if (jsonRequest.getString("type").equals("download")) {
                            sendFiles(new JSONObject(request).getString("id"), socket);
                        }
                        String response = Response.responseCreator(jsonRequest, statement);
                        this.out.println(response);
                    }
                }
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    clients.remove(this);
                    connection.close();
                    System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void sendFiles(String id, Socket clientSocket) {
        File file = new File("Eighth-Assignment-Steam/src/main/java/Server/Resources/" + id + ".png");
        long fileSize = file.length();

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream())) {

            outputStream.writeLong(fileSize);
            outputStream.flush();

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
