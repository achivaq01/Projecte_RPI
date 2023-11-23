package com.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Base64;

public class CrazyServer extends WebSocketServer {
    private final String RESET = "\u001B[0m";
    private final String RED = "\u001B[31m";
    private final String GREEN = "\u001B[32m";
    private final String YELLOW = "\u001B[33m";
    
    private final int ERROR = -1;
    private final int UPDATE = 0;
    private final int CONNECTION = 1;
    private final String PRINT_STRING = "string";
    private final String PRINT_IMAGE = "image";
    private final String LOGIN = "login";
    private final String LIST = "list";
    private final String SERVER_PREFIX = "[SERVER]: ";
    private final String PRINT_MOVING_MESSAGE_ON_SCREEN = "cd ~/dev/rpi-rgb-led-matrix && pwd && text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=2 --led-no-hardware-pulse 'MESSAGE'";
    private final String PRINT_IMAGE_ON_SCREEN = "cd ~/dev/rpi-rgb-led-matrix && pwd && led-image-viewer -C --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse ~/dev/server/Projecte_RPI/server_java/screenimage.png";
    private final String USERS_JSON_PATH = "data/users.json";

    static BufferedReader serverInput;
    private final ThreadManager threadManager;
    private  final List<String[]> clientList;

    public CrazyServer (int port) {
        super(new InetSocketAddress(port));
        
        threadManager = new ThreadManager();
        clientList = new ArrayList<>();
        serverInput = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void onStart() {
        final String HOST = getAddress().getAddress().getHostAddress();
        final int PORT = getAddress().getPort();
        final String IP;
        
        try{
            IP = Main.getLocalIPAddress();
        } catch (SocketException | UnknownHostException ex) {
            return;
            
        }
        threadManager.start();
        String printIpOnScreen = PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", IP);

        log("WebSockets server running at: ws://" + HOST + ":" + PORT, UPDATE);
        log("Type 'exit' to stop and exit server.", UPDATE);

        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
        
        threadManager.addQueue("echo ThreadManager started!");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
        }

        threadManager.addQueue(printIpOnScreen);
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        String clientId = getConnectionId(connection);

        JSONObject welcomeMessage = new JSONObject("{}");
        welcomeMessage.put("type", "private");
        welcomeMessage.put("from", "server");
        welcomeMessage.put("value", "Welcome to CrazyDisplay");
        connection.send(welcomeMessage.toString()); 

        broadcast(sendList(connection).toString());

        JSONObject connectedMessage = new JSONObject("{}");
        connectedMessage.put("type", "connected");
        connectedMessage.put("from", "server");
        connectedMessage.put("id", clientId);
        broadcast(connectedMessage.toString());

        String host = connection.getRemoteSocketAddress().getAddress().getHostAddress();
        log("New client (" + clientId + "): " + host, CONNECTION);

        threadManager.interrupt();

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String clientId = getConnectionId(conn);

        JSONObject connectionClosedMessage = new JSONObject("{}");
        connectionClosedMessage.put("type", "disconnected");
        connectionClosedMessage.put("from", "server");
        connectionClosedMessage.put("id", clientId);
        broadcast(connectionClosedMessage.toString());

        log("Client disconnected '" + clientId + "'", CONNECTION);
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        String clientId = getConnectionId(connection);
        JSONObject receivedMessage = new JSONObject(message);
        String printMessage;

        log("Mensaje recivido de " + clientId, CONNECTION);        
        switch(receivedMessage.getString("type")) {
            case PRINT_STRING:
                printMessage = PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", receivedMessage.getString("text"));
                threadManager.addQueue(printMessage);
                break;
            
            case LOGIN:
                String user = receivedMessage.getString("user");
                String password = receivedMessage.getString("password");
                String platform = receivedMessage.getString("platform");

                JSONObject loggedInMessage = new JSONObject();
                loggedInMessage.put("type", "login");

                if(!login(user, password)) {
                    log("Client " + clientId + " rejected.", CONNECTION);
                    loggedInMessage.put("success", false);
                    connection.send(loggedInMessage.toString());

                    break;
                }
                
                loggedInMessage.put("success", true);
                connection.send(loggedInMessage.toString());

                clientList.add(new String[]{clientId, platform});
                log("Client " + clientId + "has succesfully logged in from " + platform + ".", CONNECTION);
                break;
            
            case LIST:
                log("Client list send to client " + clientId, CONNECTION);
                connection.send(sendList(connection).toString());
                break;

            case PRINT_IMAGE:
                byte[] decodedImage = Base64.getDecoder().decode(receivedMessage.getString("img"));
                try {
                    Files.write(Paths.get("screenimage.png"), decodedImage);
                } catch (IOException e) {
                    log("ERROR loading an image : \n" + e.getMessage(), ERROR);
                }
                printMessage = PRINT_IMAGE_ON_SCREEN;
                threadManager.addQueue(printMessage);
                break;

        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log("ERROR" + ex.getMessage(), ERROR);

    }

    public void runServerBucle () {
        boolean running = true;
        try 
        {
            log("Starting Server...", UPDATE);
            start();
            while (running) {
                String line;
                line = serverInput.readLine();
                if (line.equals("exit")) {
                    running = false;
                }
            } 
            log("Stopping server...", UPDATE);
            stop(1000);
        } 
        catch (IOException | InterruptedException e) 
        {
            log("ERROR \n" + e.getMessage(), ERROR);
        }  
    }

    public JSONObject sendList (WebSocket conn) {
        long[] count = getCounts(clientList);

        JSONObject list = new JSONObject("{}");
        list.put("type", "list");
        list.put("flutter", count[0]);
        list.put("android", count[1]);
        
        return list;
    }

    public String getConnectionId (WebSocket connection) {
        String name = connection.toString();
        return name.replaceAll("org.java_websocket.WebSocketImpl@", "").substring(0, 3);
    }

    public String[] getClients () {
        int length = getConnections().size();
        String[] clients = new String[length];
        int cnt = 0;

        for (WebSocket ws : getConnections()) {
            clients[cnt] = getConnectionId(ws);               
            cnt++;
        }
        return clients;
    }

    public WebSocket getClientById (String clientId) {
        for (WebSocket webSocket : getConnections()) {
            String wsId = getConnectionId(webSocket);
            if (clientId.compareTo(wsId) == 0) {
                return webSocket;
            }               
        }
        
        return null;
    }

    private long[] getCounts(List<String[]> arrayList) {
        Map<String, Long> valueCounts = arrayList.stream()
                .filter(pair -> "Flutter".equals(pair[1]) || "Android".equals(pair[1]))
                .collect(Collectors.groupingBy(pair -> pair[1], Collectors.counting()));

        long[] counts = {0, 0};

        valueCounts.forEach((value, count) -> {
            if ("Flutter".equals(value)) {
                counts[0] = count;
            } else if ("Android".equals(value)) {
                counts[1] = count;
            }
        });

        return counts;
    }

    private boolean login(String user, String password) {
        try {
            String usersContent = new String(Files.readAllBytes(Path.of(USERS_JSON_PATH)));
            JSONArray usersArray = new JSONArray(usersContent);

            for(int i = 0; i < usersArray.length(); i++) {
                final JSONObject arrayObject = usersArray.getJSONObject(i);
                
                if(user.equals(arrayObject.getString("user")) && password.equals(arrayObject.getString("password"))) {
                    return true;
                }
            }

            return false;

        } catch (IOException e) {
            log("ERROR\n" + e.getMessage(), ERROR);
            return false;
        }

    }
    
    private void log(String log, int type) {
        String serverMessage;
        
        switch(type) {
            case ERROR:
                serverMessage = RED + SERVER_PREFIX + log + RESET;
                break;
            
            case UPDATE:
                serverMessage = GREEN + SERVER_PREFIX + log + RESET;
                break;
                
            case CONNECTION:
                serverMessage = YELLOW + SERVER_PREFIX + log + RESET;
                break;
                
            default:
                serverMessage = SERVER_PREFIX + log;
                break;
        }
        
        System.out.print(serverMessage + "\n");
    }
}