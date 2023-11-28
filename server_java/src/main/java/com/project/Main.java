package com.project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class Main {

    /**
     * static method to run server bucle
     * 
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main (String[] args) throws InterruptedException, IOException {

        int port = 8888; 
        java.lang.System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        CrazyServer server = new CrazyServer(port);
        server.runServerBucle();
    }

    /**
     * static method to retrieve local ip adress
     * 
     * @return
     * @throws SocketException
     * @throws UnknownHostException
     */
    public static String getLocalIPAddress() throws SocketException, UnknownHostException {
        String localIp = "";
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();

            while (inetAddresses.hasMoreElements()) {
                InetAddress ia = inetAddresses.nextElement();

                if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia.isSiteLocalAddress()) {
                    System.out.println(ni.getDisplayName() + ": " + ia.getHostAddress());
                    localIp = ia.getHostAddress();
                }
            }
        }

        if (localIp.compareToIgnoreCase("") == 0) {
            localIp = InetAddress.getLocalHost().getHostAddress();
        }
        return localIp;
    }
}