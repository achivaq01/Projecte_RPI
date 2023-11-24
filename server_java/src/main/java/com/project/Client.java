package com.project;

import org.java_websocket.WebSocket;

public class Client {
    private final String id;
    private final WebSocket connection;
    private final String platform;

    public Client(String id, WebSocket connection, String platform) {
        super();

        this.id = id;
        this.connection = connection;
        this.platform = platform;
    }

    public String getId() {
        return id;
    }

    public WebSocket getConnection() {
        return connection;
    }

    public String getPlatform() {
        return platform;
    }

    
}
