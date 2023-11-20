package com.project;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Base64;

public class CrazyServer extends WebSocketServer {

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private Process process;
    private ThreadManager manager;
    private List<String[]> cList;

    public CrazyServer (int port) {
        super(new InetSocketAddress(port));
        manager = new ThreadManager();
        cList = new ArrayList<>();

    }

    public void cdRPI(String text) {
        try {
            // Set the working directory to "~/dev/rpi-rgb-led-matrix".
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", "-c", "cd ~/dev/rpi-rgb-led-matrix && pwd && text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse '" + text + "'");
            builder.inheritIO();

            process = builder.start();
            process.waitFor();

            try (InputStream inputStream = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Current Directory: " + line);
                }
            }

            process = builder.start();
            process.waitFor();

            // Check the exit code of the process.
            int exitCode = process.exitValue();

            // If the exit code is 0, the process finished successfully.
            if (exitCode == 0) {
                System.out.println("The process finished successfully.");
            } else {
                System.out.println("The process failed with exit code " + exitCode);
            }

            // ...
            // Rest of your code
        } catch (Exception e) {
            System.out.println("Process Destroyed");
        }
    }

    @Override
    public void onStart() {
        // Quan el servidor s'inicia
        String host = getAddress().getAddress().getHostAddress();
        int port = getAddress().getPort();
        manager.start();

        System.out.println("WebSockets server running at: ws://" + host + ":" + port);
        System.out.println("Type 'exit' to stop and exit server.");

        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
        
        manager.addQueue("echo first_message");
        try {
            String ip = Main.getLocalIPAddress();
            String command = "cd ~/dev/rpi-rgb-led-matrix && pwd && text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=2 --led-no-hardware-pulse '"+ ip +"'";

            manager.addQueue(command);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // TODO
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Quan un client es connecta
        String clientId = getConnectionId(conn);

        // Saludem personalment al nou client
        JSONObject objWlc = new JSONObject("{}");
        objWlc.put("type", "private");
        objWlc.put("from", "server");
        objWlc.put("value", "Welcome to CrazyDisplay");
        conn.send(objWlc.toString()); 

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

        // Esborrem la IP que es mostra en la pantalla de la RPI

        manager.interrupt();

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
        /*
        executor.submit(() -> {
            cdRPI();
        });

         */
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Quan arriba un missatge
        String clientId = getConnectionId(conn);
        System.out.println("Llega mensaje");
        try {
            JSONObject objRequest = new JSONObject(message);

            if(objRequest.has("text")) {
                String text = objRequest.getString("text");
                String command = "cd ~/dev/rpi-rgb-led-matrix && pwd && text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse '"+ text +"'";
                manager.addQueue(command);

            } else if (objRequest.has("platform")) {
                System.out.println(clientId + " is from " + objRequest.getString("platform"));

                cList.add(new String[]{clientId, objRequest.getString("platform")});

            } else if (objRequest.get("type") == "list") {
                sendList(conn);
            } else if (objRequest.get("type").equals("image")) {
                System.out.println("Esta en una imagen");
                
                byte[] decodedBytes = Base64.getDecoder().decode(objRequest.getString("img"));
                Files.write(Paths.get("screenimage.png"), decodedBytes);
                String command = "cd ~/dev/rpi-rgb-led-matrix && pwd && led-image-viewer -C --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse ~/dev/server/Projecte_RPI/server_java/screenimage.png";

                System.out.print("Antes de enviar comando imagen");
                manager.addQueue(command);
                System.out.print("Despues de enviar");
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
        long[] count = getCounts(cList);

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
}