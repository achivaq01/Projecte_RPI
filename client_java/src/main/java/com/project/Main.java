package com.project;

import java.net.URI;
import java.net.URISyntaxException;

/*
    Examples d'instruccions:

    exit                > Exit the program
    reconnect           > Reconnect to the server
    list                > Get list of clients
    to(6a2)message      > Send a private message to a client
    broadcast message   > Send a message to everyone
    
 */

public class Main {
    public static void main(String[] args) {
        int port = 8888;
        String host = "localhost";
        String location = "ws://" + host + ":" + port;

        ChatClient chatClient;
        try {
            chatClient = new ChatClient(new URI(location));
            chatClient.runClientBucle();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}