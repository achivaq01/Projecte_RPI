package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import org.json.JSONArray;

public class CrazyServer extends WebSocketServer {
    private final String RESET = "\u001B[0m";
    private final String RED = "\u001B[31m";
    private final String GREEN = "\u001B[32m";
    private final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    
    private String IP;
    private final int SERVER_SLEEP_TIME = 3;
    private final int ERROR = -1;
    private final int UPDATE = 0;
    private final int CONNECTION = 1;
    private final int DISCONNECTION = 2;
    private final int NOTIFICATION = 3;
    private final String PRINT_STRING = "string";
    private final String PRINT_IMAGE = "image";
    private final String LOGIN = "login";
    private final String LIST = "list";
    private final String SERVER_PREFIX = "[" + BRIGHT_MAGENTA + "SERVER" + RESET + "]: ";
    private final String PRINT_MOVING_MESSAGE_ON_SCREEN = "cd ~/dev/rpi-rgb-led-matrix && pwd && text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=2 --led-no-hardware-pulse 'MESSAGE'";
    private final String PRINT_IMAGE_ON_SCREEN = "cd ~/dev/rpi-rgb-led-matrix && pwd && led-image-viewer -C --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse ~/dev/server/Projecte_RPI/server_java/screenimage.png";
    private final String USERS_JSON_PATH = "data/users.json";

    static BufferedReader serverInput;
    private final ThreadManager threadManager;
    private final HashMap<WebSocket,Client> clientList;

    public CrazyServer (int port) {
        super(new InetSocketAddress(port));
        
        threadManager = new ThreadManager();
        clientList = new HashMap<>();
        serverInput = new BufferedReader(new InputStreamReader(System.in));

        IP = "0.0.0.0";
        try{
            IP = Main.getLocalIPAddress();
        } catch (SocketException | UnknownHostException ex) {
            log("ERROR There was an issue getting the local IP", ERROR);
        }
    }

    @Override
    public void onStart() {
        final String HOST = getAddress().getAddress().getHostAddress();
        final int PORT = getAddress().getPort();
        
        threadManager.start();
        String printIpOnScreen = PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", IP);

        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
        threadManager.addQueue("clear");
        
        try {
            TimeUnit.SECONDS.sleep(SERVER_SLEEP_TIME);
        } catch (InterruptedException e) {
        }
        log("WebSockets server running at: ws://" + HOST + ":" + PORT, UPDATE);
        log("Type 'exit' to stop and exit server.", UPDATE);

        threadManager.addQueue(printIpOnScreen);
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        String clientId = getConnectionId(connection);
        Client unauthorizedClient = new Client(clientId, connection, null);
        String lastCommand = threadManager.getLastCommand();

        JSONObject welcomeMessage = new JSONObject("{}");
        welcomeMessage.put("type", "private");
        welcomeMessage.put("from", "server");
        welcomeMessage.put("value", "Welcome to CrazyDisplay");
        connection.send(welcomeMessage.toString()); 

        //broadcast(sendList(connection).toString());

        JSONObject connectedMessage = new JSONObject("{}");
        connectedMessage.put("type", "connected");
        connectedMessage.put("from", "server");
        connectedMessage.put("id", clientId);
        connection.send(connectedMessage.toString());

        clientList.put(connection, unauthorizedClient);
        String host = connection.getRemoteSocketAddress().getAddress().getHostAddress();

        if(!clientList.isEmpty() && lastCommand.equals(PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", IP))) {
            log("Removing the IP from display...", UPDATE);
            threadManager.interrupt();
        }
        
        log("New client (" + clientId + "): " + host, CONNECTION);

    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        Client clientConnection = clientList.get(connection);
        
        JSONObject connectionClosedMessage = new JSONObject("{}");
        connectionClosedMessage.put("type", "disconnected");
        connectionClosedMessage.put("from", "server");
        connectionClosedMessage.put("id", clientConnection.getId());
        broadcast(connectionClosedMessage.toString());
        
        clientList.remove(connection);
        if(clientList.isEmpty()) {
            threadManager.addQueue(PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", IP));
            log("The client list is now empty", UPDATE);
        }

        sendClientList();
        if(!clientList.isEmpty() && !clientConnection.getPlatform().equals(null)) {
            JSONObject newConnection = new JSONObject();
            newConnection.put("type", "new disconnection");
            newConnection.put("id", clientConnection.getId());

            for(Map.Entry<WebSocket, Client> connected : clientList.entrySet()) {
                if(!connected.getKey().equals(connection)) {
                    connected.getKey().send(newConnection.toString());
                }
            }
        }
        log("Client disconnected '" + clientConnection.getId() + "'", DISCONNECTION);
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        Client clientConnection = clientList.get(connection);
        JSONObject receivedMessage = new JSONObject(message);
        String printMessage;

        log("Mensaje recivido de " + clientConnection.getId(), NOTIFICATION);        
        switch(receivedMessage.getString("type")) {
            case PRINT_STRING:

                printMessage = PRINT_MOVING_MESSAGE_ON_SCREEN.replace("MESSAGE", receivedMessage.getString("text"));
                notifyMessage(connection);
                threadManager.addQueue(printMessage);
                break;
            
            case LOGIN:
                String user = receivedMessage.getString("user");
                String password = receivedMessage.getString("password");
                String platform = receivedMessage.getString("platform");
                String id = receivedMessage.getString("id");

                JSONObject loggedInMessage = new JSONObject();
                loggedInMessage.put("type", "login");

                if(!login(user, password)) {
                    log("Client " + clientConnection.getId() + " rejected.", CONNECTION);
                    loggedInMessage.put("success", false);
                    connection.send(loggedInMessage.toString());

                    break;
                }

                loggedInMessage.put("success", true);
                connection.send(loggedInMessage.toString());
                clientList.put(connection, new Client(id, connection, platform));
                
                sendClientList();
                if(!clientList.isEmpty()) {
                    JSONObject newConnection = new JSONObject();
                    newConnection.put("type", "new connection");
                    newConnection.put("id", clientConnection.getId());

                    for(Map.Entry<WebSocket, Client> connected : clientList.entrySet()) {
                        if(!connected.getKey().equals(connection)) {
                            connected.getKey().send(newConnection.toString());
                        }
                    }
                }
                
                //clientList.add(new String[]{clientId, platform});
                log("Client " + clientConnection.getId() + " has succesfully logged in from " + platform + ".", CONNECTION);
                break;
            
            case LIST:
                log("Client list send to client " + clientConnection.getId(), NOTIFICATION);
                //connection.send(sendList(connection).toString());
                break;

            case PRINT_IMAGE:
                byte[] decodedImage = Base64.getDecoder().decode(receivedMessage.getString("img"));
                try {
                    Files.write(Paths.get("screenimage.png"), decodedImage);
                } catch (IOException e) {
                    log("ERROR loading an image :" + e.getMessage(), ERROR);
                }
                printMessage = PRINT_IMAGE_ON_SCREEN;
                notifyMessage(connection);
                threadManager.addQueue(printMessage);
                break;

        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log("ERROR" + ex.getMessage(), ERROR);

    }

    public void sendClientList() {
        JSONObject message = new JSONObject();
        JSONArray clientList = new JSONArray();
        
        message.put("type", "list");
        for(Map.Entry<WebSocket, Client> connection : this.clientList.entrySet()) {
            Client connectionClient = connection.getValue();
            String connectionId = connectionClient.getId();
            String connectionPlatform = connectionClient.getPlatform();

            JSONObject client = new JSONObject();
            client.put("id", connectionId);
            client.put("platform", connectionPlatform);

            clientList.put(client);
        }
        message.put("list", clientList);

        log("Sending client list...", NOTIFICATION);
        broadcast(message.toString());
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
            threadManager.interrupt();
            threadManager.stop();

            log("Stopping server...", UPDATE);
            stop(1000);
        } 
        catch (IOException | InterruptedException e) 
        {
            log("ERROR " + e.getMessage(), ERROR);
        }  
    }
    public void notifyMessage(WebSocket connection) {
        Client clientConnection = clientList.get(connection);
        JSONObject notifyMessageSent = new JSONObject();

        notifyMessageSent.put("type", "new message");
        notifyMessageSent.put("id", clientConnection.getId());

        for(Map.Entry<WebSocket, Client> currentConnection : this.clientList.entrySet()) {
            if(!currentConnection.getKey().equals(connection)) {
                currentConnection.getValue().getConnection().send(notifyMessageSent.toString());
            }
        }
    }

    public String getConnectionId (WebSocket connection) {
        String name = connection.toString();
        return name.replaceAll("org.java_websocket.WebSocketImpl@", "").substring(0, 3);
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
            log("ERROR " + e.getMessage(), ERROR);
            return false;
        }

    }
    
    private void log(String log, int type) {
        String serverMessage;
        
        switch(type) {
            case ERROR:
                serverMessage = SERVER_PREFIX + RED + log + RESET;
                break;
            
            case UPDATE:
                serverMessage = SERVER_PREFIX + GREEN + log + RESET;
                break;
                
            case CONNECTION:
                serverMessage = SERVER_PREFIX + CYAN + log + RESET;
                break;
            
            case DISCONNECTION:
                serverMessage = SERVER_PREFIX + YELLOW + log + RESET;
                break;
            
            case NOTIFICATION:
                serverMessage = SERVER_PREFIX + MAGENTA + log + RESET;
                break;
                
            default:
                serverMessage = SERVER_PREFIX + log;
                break;
        }
        
        System.out.println(serverMessage);
    }
}