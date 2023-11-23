package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Base64;
import org.json.JSONException;

public class CrazyServer extends WebSocketServer {
    private final String RESET = "\u001B[0m";
    private final String RED = "\u001B[31m";
    private final String GREEN = "\u001B[32m";
    private final String YELLOW = "\u001B[33m";
    
    private final int ERROR = -1;
    private final int UPDATE = 0;
    private final int CONNECTION = 1;
    private final String PRINT = "print";
    private final String LOGIN = "login";
    private final String LIST = "list";
    
    private final String SERVER_PREFIX = "[SERVER]: ";
    private final String PRINT_MOVING_MESSAGE_ON_SCREEN = "cd "
            + "~/dev/rpi-rgb-led-matrix && pwd && text-scroller " 
            + "-f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf "
            + "--led-cols=64 "
            + "--led-rows=64 "
            + "--led-slowdown-gpio=2 "
            + "--led-no-hardware-pulse "
            + "'MESSAGE'";

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
        String printIpOnScreen = PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", IP);
        
        log("WebSockets server running at: ws://" + HOST + ":" + PORT, UPDATE);
        log("Type 'exit' to stop and exit server.", UPDATE);

        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
        
        threadManager.start();
        threadManager.addQueue("echo ThreadManager started.");
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

        sendList(connection);

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
            case PRINT:
                printMessage = PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", receivedMessage.getString("text"));
                threadManager.addQueue(printMessage);
                break;
            
            case LOGIN:
                clientList.add(new String[]{clientId, receivedMessage.getString("platform")});
                log("Client " + clientId + "has succesfully logged in.", UPDATE);
                break;
            
            case LIST:
                log("Client list send to client " + clientId, CONNECTION);
                sendList(connection);
                break;
        }
        
        try {

            if(receivedMessage.has("text")) {
                String text = receivedMessage.getString("text");
                String command = "cd ~/dev/rpi-rgb-led-matrix && pwd && text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse '"+ text +"'";
                threadManager.addQueue(command);

            } else if (receivedMessage.has("platform")) {
                System.out.println(clientId + " is from " + receivedMessage.getString("platform"));

                clientList.add(new String[]{clientId, receivedMessage.getString("platform")});

            } else if (receivedMessage.get("type") == "list") {
                sendList(connection);
            } else if (receivedMessage.get("type").equals("image")) {
                System.out.println("Esta en una imagen");
                
                byte[] decodedBytes = Base64.getDecoder().decode(receivedMessage.getString("img"));
                Files.write(Paths.get("screenimage.png"), decodedBytes);
                String command = "cd ~/dev/rpi-rgb-led-matrix && pwd && led-image-viewer -C --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse ~/dev/server/Projecte_RPI/server_java/screenimage.png";

                System.out.print("Antes de enviar comando imagen");
                threadManager.addQueue(command);
                System.out.print("Despues de enviar");
            }
            


        } catch (IOException | JSONException e) {
        }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log(ex.getMessage(), ERROR);

    }

    public void runServerBucle () {
        boolean running = true;
        try {
            System.out.println("Starting server");
            start();
            while (running) {
                String line;
                line = serverInput.readLine();
                if (line.equals("exit")) {
                    running = false;
                }
            } 
            System.out.println("Stopping server");
            stop(1000);
        } catch (IOException | InterruptedException e) {
        }  
    }

    public void sendList (WebSocket conn) {
        long[] count = getCounts(clientList);

        JSONObject list = new JSONObject("{}");
        list.put("type", "list");
        list.put("flutter", count[0]);
        list.put("android", count[1]);
        conn.send(list.toString()); 
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
        for (WebSocket ws : getConnections()) {
            String wsId = getConnectionId(ws);
            if (clientId.compareTo(wsId) == 0) {
                return ws;
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
    
    private void printMessage(JSONObject printRequest) {
        //String context 
    }
}