package Shared;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Response {
    public static String responseCreator(JSONObject request, Statement statement) throws SQLException, IOException {
        String type = request.getString("type");

        switch (type) {
            case "menu":
                return MenuResponse();
            case "user menu":
                return userMenuResponse(request);
            case "sign up":
                return signUpResponseCreator(doesUserExist(request, statement), statement, request);
            case "log in":
                return logInResponseCreator(doesUserExist(request, statement), statement, request);
            case "view games":
                return viewGameListResponse(statement, request);
            case "view details":
                return viewDetailsResponse(request, statement);
            case "download":
                return downloadResponse(request, statement);
            case "search":
                return searchResponse(request, statement);
            default:
                throw new IllegalArgumentException("Invalid request type: " + type);
        }
    }

    public static List<String> columnNames(Statement statement) throws SQLException {
        List<String> columns = new ArrayList<>();

        for (int i = 2; i <= 9; i++) {
            String sql = "SELECT column_name FROM information_schema.columns\n" +
                    "WHERE table_name = 'games' AND ordinal_position = " + i + ";";
            try (ResultSet result = statement.executeQuery(sql)) {
                result.next();
                columns.add(result.getString("column_name"));
            }
        }

        return columns;
    }

    private static String searchResponse(JSONObject request, Statement statement) throws SQLException {
        JSONObject json = new JSONObject();
        JSONObject result = new JSONObject();
        List<String> columns = columnNames(statement);

        json.put("type", "search");
        json.put("user", request.getJSONObject("user"));

        String searchTitle = request.getString("title").toLowerCase(Locale.ROOT);
        String query = "SELECT * FROM games WHERE LOWER(title) LIKE '%" + searchTitle + "%'";
        try (ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                JSONObject details = new JSONObject();
                for (String column : columns) {
                    details.put(column, resultSet.getString(column));
                }
                result.put(resultSet.getString("id"), details);
            }
        }
        json.put("games", result);

        return json.toString();
    }

    public static String viewDetailsResponse(JSONObject request, Statement statement) throws SQLException {
        String id = request.getString("id");
        JSONObject json = new JSONObject();
        json.put("type", "view details");
        json.put("id", id);
        json.put("user", request.getJSONObject("user"));

        JSONObject details = new JSONObject();
        String query = "SELECT * FROM games WHERE id = '" + id + "'";
        try (ResultSet result = statement.executeQuery(query)) {
            if (result.next()) {
                details.put("title", result.getString("title"));
                details.put("developer", result.getString("developer"));
                details.put("genre", result.getString("genre"));
                details.put("price", result.getString("price"));
                details.put("release_year", result.getString("release_year"));
                details.put("controller_support", result.getString("controller_support"));
                details.put("reviews", result.getString("reviews"));
                details.put("size", result.getString("size") + " GB");
            }
        }

        json.put("details", details);
        return json.toString();
    }

    private static String viewGameListResponse(Statement statement, JSONObject request) throws SQLException {
        JSONObject json = new JSONObject();
        json.put("type", "view game list");
        json.put("user", request.getJSONObject("user"));
        List<String> columns = columnNames(statement);

        try (ResultSet result = statement.executeQuery("SELECT * FROM games")) {
            JSONObject games = new JSONObject();
            while (result.next()) {
                JSONObject details = new JSONObject();
                for (String column : columns) {
                    details.put(column, result.getString(column));
                }
                games.put(result.getString("id"), details);
            }
            json.put("games", games);
        }

        return json.toString();
    }

    private static String logInResponseCreator(String doesUserExist, Statement statement, JSONObject request) throws SQLException {
        JSONObject json = new JSONObject();
        json.put("type", "log in");
        JSONObject user = request.getJSONObject("user");

        if (doesUserExist.equals("true")) {
            String username = user.getString("username");
            String password = user.getString("password");
            ResultSet result = statement.executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
            if (result.next()) {
                String storedPassword = result.getString("password");
                if (password.equals(storedPassword)) {
                    user.put("date", result.getString("date_of_birth"));
                    user.put("id", result.getString("id"));
                    json.put("user", user);
                    json.put("status", "true");
                } else {
                    json.put("status", "false");
                    json.put("reason", "password is incorrect");
                }
            } else {
                json.put("status", "false");
                json.put("reason", "no user found with such username");
            }
        } else {
            json.put("status", "false");
            json.put("reason", "no user found with such username");
        }

        return json.toString();
    }

    public static String MenuResponse() {
        JSONObject json = new JSONObject();
        json.put("type", "menu");
        return json.toString();
    }

    public static String userMenuResponse(JSONObject request) {
        JSONObject json = new JSONObject();
        json.put("type", "user menu");
        json.put("user", request.getJSONObject("user"));
        return json.toString();
    }

    public static String downloadResponse(JSONObject request, Statement statement) throws IOException, SQLException {
        insertDownload(request, statement, downloadCount(request, statement));
        JSONObject json = new JSONObject();
        json.put("type", "user menu");
        json.put("user", request.getJSONObject("user"));

        String id = request.getString("id");
        String filePath = "Eighth-Assignment-Steam/src/main/java/Server/Resources/" + id + ".png";
        FileChannel src = new FileInputStream(filePath).getChannel();

        File folder = new File("Eighth-Assignment-Steam/src/main/java/Client/Downloads/");
        File[] listOfFiles = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.getName().endsWith(".png")) {
                    fileNames.add(file.getName().substring(0, file.getName().length() - 4));
                }
            }
        }

        int i = 1;
        String plainId = id;
        while (fileNames.contains(id)) {
            id = plainId + " (" + i + ")";
            i++;
        }

        return json.toString();
    }

    public static String signUpResponseCreator(String doesUserExist, Statement statement, JSONObject request) throws SQLException {
        JSONObject json = new JSONObject();

        if (doesUserExist.equals("false")) {
            insertUser(request, statement);
        }

        json.put("type", "sign up");
        json.put("status", doesUserExist.equals("false"));
        json.put("user", request.getJSONObject("user"));

        return json.toString();
    }

    public static String doesUserExist(JSONObject json, Statement statement) throws SQLException {
        String username = json.getJSONObject("user").getString("username");
        ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users WHERE username = '" + username + "'");
        result.next();

        if (result.getInt("count") == 0) {
            return "false";
        }
        return "true";
    }

    public static int downloadCount(JSONObject request, Statement statement) throws SQLException {
        String accountId = request.getJSONObject("user").getString("id");
        String gameId = request.getString("id");
        ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM downloads WHERE account_id = '" +
                accountId + "' AND game_id = '" + gameId + "'");
        result.next();

        if (result.getInt("count") == 0) {
            return 0;
        } else {
            result = statement.executeQuery("SELECT * FROM downloads WHERE account_id = '" +
                    accountId + "' AND game_id = '" + gameId + "'");
            result.next();
            return result.getInt("download_count");
        }
    }

    public static void insertUser(JSONObject request, Statement statement) throws SQLException {
        JSONObject user = request.getJSONObject("user");
        String sql = "INSERT INTO users VALUES ('" + user.getString("id") + "','" + user.getString("username") + "', '" +
                user.getString("password") + "','" + user.getString("date") + "')";
        statement.executeUpdate(sql);
    }

    public static void insertDownload(JSONObject request, Statement statement, int download_count) throws SQLException {
        JSONObject user = request.getJSONObject("user");

        String sql;

        if (download_count == 0) {
            sql = "INSERT INTO downloads VALUES ('" + user.getString("id") + "','" + request.getString("id") +
                    "','" + 1 + "')";
        } else {
            sql = "UPDATE downloads SET download_count = " + (download_count + 1) + " WHERE account_id = '"
                    + user.getString("id") + "' AND game_id = '" + request.getString("id") + "'";
        }

        statement.executeUpdate(sql);
    }
}