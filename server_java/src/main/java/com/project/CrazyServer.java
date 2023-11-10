package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

public class CrazyServer extends WebSocketServer {

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public CrazyServer (int port) {
        super(new InetSocketAddress(port));
    }

    public void cdRPI() {

        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", "-c", "cd ~/dev/rpi-rgb-led-matrix");
            builder.inheritIO();

            Process process;
            process= builder.start();
            process.waitFor();
            builder.command("bash", "-c", "./examples-api-use/demo -D0 --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse");
            process = builder.start();
            process.waitFor();

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                System.out.println("The process finished successfully.");
            } else {
                System.out.println("The process failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public void ordersRPI(String cmd[]) {
        
        System.out.println("Iniciant comanda...");
 
        try {
            // Objecte global Runtime
            Runtime rt = java.lang.Runtime.getRuntime();
            // Executar comanda en subprocess
            Process p = rt.exec(cmd);
            // Donem un temps d'execució
            TimeUnit.SECONDS.sleep(5);
            // El matem si encara no ha acabat
            if( p.isAlive() ) p.destroy();
            p.waitFor();
            // Comprovem el resultat de l'execució
            System.out.println("Comanda 1 exit code = " + p.exitValue());
 
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Finish
        System.out.println("Comandes finalitzades.");
    }

    @Override
    public void onStart() {
        // Quan el servidor s'inicia
        String host = getAddress().getAddress().getHostAddress();
        int port = getAddress().getPort();
        System.out.println("WebSockets server running at: ws://" + host + ":" + port);
        System.out.println("Type 'exit' to stop and exit server.");
        
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);

        String[] initiate = {"cd", "./../../rpi-rgb-led-matrix"};
        String[] execute = {"examples-api-use/demo", "-D0", "--led-cols=64", "--led-rows=64", "--led-slowdown-gpio=4", "--led-no-hardware-pulse"};
        /*
        ordersRPI(initiate);
        ordersRPI(execute);
        */
        cdRPI();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Quan un client es connecta
        String clientId = getConnectionId(conn);

        // Saludem personalment al nou client
        JSONObject objWlc = new JSONObject("{}");
        objWlc.put("type", "private");
        objWlc.put("from", "server");
        objWlc.put("value", "Welcome to the chat server");
        conn.send(objWlc.toString()); 

        // Li enviem el seu identificador
        JSONObject objId = new JSONObject("{}");
        objId.put("type", "id");
        objId.put("from", "server");
        objId.put("value", clientId);
        conn.send(objId.toString()); 

        // Enviem al client la llista amb tots els clients connectats
        sendList(conn);

        // Enviem la direcció URI del nou client a tothom 
        JSONObject objCln = new JSONObject("{}");
        objCln.put("type", "connected");
        objCln.put("from", "server");
        objCln.put("id", clientId);
        broadcast(objCln.toString());

        // Mostrem per pantalla (servidor) la nova connexió
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println("New client (" + clientId + "): " + host);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Quan un client es desconnecta
        String clientId = getConnectionId(conn);

        // Informem a tothom que el client s'ha desconnectat
        JSONObject objCln = new JSONObject("{}");
        objCln.put("type", "disconnected");
        objCln.put("from", "server");
        objCln.put("id", clientId);
        broadcast(objCln.toString());

        // Mostrem per pantalla (servidor) la desconnexió
        System.out.println("Client disconnected '" + clientId + "'");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Quan arriba un missatge
        String clientId = getConnectionId(conn);
        try {
            JSONObject objRequest = new JSONObject(message);
            String type = objRequest.getString("type");

            if (type.equalsIgnoreCase("list")) {
                // El client demana la llista de tots els clients
                System.out.println("Client '" + clientId + "'' requests list of clients");
                sendList(conn);

            } else if (type.equalsIgnoreCase("private")) {
                // El client envia un missatge privat a un altre client
                System.out.println("Client '" + clientId + "'' sends a private message");

                JSONObject objResponse = new JSONObject("{}");
                objResponse.put("type", "private");
                objResponse.put("from", clientId);
                objResponse.put("value", objRequest.getString("value"));

                String destination = objRequest.getString("destination");
                WebSocket desti = getClientById(destination);

                if (desti != null) {
                    desti.send(objResponse.toString()); 
                }
                
            } else if (type.equalsIgnoreCase("broadcast")) {
                // El client envia un missatge a tots els clients
                System.out.println("Client '" + clientId + "'' sends a broadcast message to everyone");

                JSONObject objResponse = new JSONObject("{}");
                objResponse.put("type", "broadcast");
                objResponse.put("from", clientId);
                objResponse.put("value", objRequest.getString("value"));
                broadcast(objResponse.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // Quan hi ha un error
        ex.printStackTrace();
    }

    public void runServerBucle () {
        boolean running = true;
        try {
            System.out.println("Starting server");
            start();
            while (running) {
                String line;
                line = in.readLine();
                if (line.equals("exit")) {
                    running = false;
                }
            } 
            System.out.println("Stopping server");
            stop(1000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }  
    }

    public void sendList (WebSocket conn) {
        JSONObject objResponse = new JSONObject("{}");
        objResponse.put("type", "list");
        objResponse.put("from", "server");
        objResponse.put("list", getClients());
        conn.send(objResponse.toString()); 
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
}