package com.project;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Scanner;

import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.json.JSONObject;

public class ChatClient extends WebSocketClient {
    private Scanner sc = new Scanner(System.in);
    private LinkedList<String> _last5Messages = new LinkedList<>();

    public ChatClient(URI uri) throws URISyntaxException {
        super(uri, (Draft) new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        // Quan el client es connecta
        this.onMessage("Connection opened");
    }

    @Override
    public void onMessage(String message) {
        // Quan el client rep un missatge
        _last5Messages.add("Message received > " + message);
        if (_last5Messages.size() > 5) {
            _last5Messages.removeFirst();
        }
        displayPrompt();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // Quan el client es desconnecta
        this.onMessage("Connection closed");
    }

    @Override
    public void onError(Exception ex) {
        // Quan hi ha un error
        this.onMessage("Error: " + ex.getMessage());
    }

    public void runClientBucle() {
        boolean running = true;

        connect();
        while (running) {
            displayPrompt();
            String text = sc.nextLine();
            if (text.equalsIgnoreCase("exit")) {
                running = false;
                close();
                break;
            } else if (text.equalsIgnoreCase("list")) {
                JSONObject obj = new JSONObject();
                obj.put("type", "list");
                send(obj.toString());
            } else if (text.startsWith("to(")) {
                int endIdx = text.indexOf(")");
                if (endIdx > -1) {
                    String destId = text.substring(3, endIdx);
                    String msg = text.substring(endIdx + 1);

                    JSONObject obj = new JSONObject();
                    obj.put("type", "private");
                    obj.put("destination", destId);
                    obj.put("value", msg);
                    send(obj.toString());
                }
            } else if (text.startsWith("broadcast ")) {
                int endIdx = text.indexOf(" ");
                if (endIdx > -1) {
                    String msg = text.substring(endIdx + 1);

                    JSONObject obj = new JSONObject();
                    obj.put("type", "broadcast");
                    obj.put("value", msg);
                    send(obj.toString());
                }
            }
        }
        close();
    }

    public void displayPrompt() {
        clearConsole();
        System.out.println("Connection: " + getReadyState());
        for (String msg : _last5Messages) {
            System.out.println(msg);
        }
        System.out.print("Type a message (list, exit, to(id)message, broadcast message): ");
    }

    public void clearConsole() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else if (os.contains("mac") || os.contains("unix") || os.contains("linux")) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            } else {
                // Fallback per si no es pot detectar l'OS
                for (int i = 0; i < 50; i++) {
                    System.out.println();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected () {
        return (getReadyState() == ReadyState.OPEN);
    }
}