package Shared;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Request {
    public static String createRequest(JSONObject response, Scanner scan) {
        String type = response.getString("type");
        String result = null;

        switch (type) {
            case "menu":
                result = createMenuRequests(scan);
                break;

            case "user menu":
                result = createUserMenuRequests(scan, response);
                break;

            case "sign up":
                if (response.getBoolean("status")) {
                    result = showUserMenuRequest(response);
                } else {
                    System.out.println("A user with this username already exists. Do you want to try again? y/n");
                    if (scan.nextLine().equals("y")) {
                        result = createSignUpRequest(scan);
                    } else {
                        result = showMenuRequest();
                    }
                }
                break;

            case "log in":
                if (response.getBoolean("status")) {
                    result = showUserMenuRequest(response);
                } else {
                    System.out.println(response.getString("reason"));
                    System.out.println("Do you want to try again? y/n");
                    if (scan.nextLine().equals("y")) {
                        result = createLogInRequest(scan);
                    } else {
                        result = showMenuRequest();
                    }
                }
                break;

            case "view game list":
            case "search":
                result = gameListRequest(response, scan);
                break;

            case "view details":
                printGameDetails(response);
                System.out.println("Do you want to download this game? y/n");
                if (scan.nextLine().equals("y")) {
                    result = downloadGameRequest(response.getString("id"), response);
                } else {
                    result = showUserMenuRequest(response);
                }
                break;
        }

        return result;
    }

    private static String gameListRequest(JSONObject response, Scanner scan) {
        return null;
    }

    private static String createLogInRequest(Scanner scan) {
        return null;
    }

    private static String createMenuRequests(Scanner scan) {
        return null;
    }

    private static String createUserMenuRequests(Scanner scan, JSONObject response) {
        return null;
    }

    private static String createSignUpRequest(Scanner scan) {
        return null;
    }

    private static String showMenuRequest() {
        return null;
    }

    private static void printGameDetails(JSONObject response) {
    }

    private static String downloadGameRequest(String id, JSONObject response) {
        return id;
    }

    private static String showUserMenuRequest(JSONObject response) {
        return null;
    }
}
