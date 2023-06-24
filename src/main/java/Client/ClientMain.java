package Client;

import Shared.Request;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    private static final String DOWNLOADS_FOLDER = "Eighth-Assignment-Steam/src/main/java/Client/Downloads/";

    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 1234;

        try (Socket socket = new Socket(hostname, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scan = new Scanner(System.in)) {

            String response;
            while ((response = reader.readLine()) != null) {
                if (!response.equals("null")) {
                    JSONObject jsonResponse = new JSONObject(response);
                    String request = Request.createRequest(jsonResponse, scan);
                    JSONObject jsonRequest = new JSONObject(request);
                    writer.println(request);
                    if (jsonRequest.getString("type").equals("download")) {
                        receiveFile(socket, jsonRequest.getString("id"));
                    }
                }
            }
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void receiveFile(Socket socket, String id) {
        try (DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {
            long fileSize = inputStream.readLong();
            String uniqueId = getUniqueFileName(id);

            String filePath = DOWNLOADS_FOLDER + uniqueId + ".png";
            Files.copy(inputStream, Path.of(filePath));

            System.out.println("Download completed");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUniqueFileName(String id) {
        File folder = new File(DOWNLOADS_FOLDER);
        File[] listOfFiles = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.getName().endsWith(".png")) {
                    fileNames.add(file.getName().substring(0, file.getName().length() - 4));
                }
            }
        }

        String uniqueId = id;
        int count = 1;
        while (fileNames.contains(uniqueId)) {
            uniqueId = id + " (" + count + ")";
            count++;
        }

        return uniqueId;
    }
}
