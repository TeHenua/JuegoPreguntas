package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        try {
            System.out.println("---SERVIDOR INICIADO---");
            ServerSocket serverSocket = new ServerSocket(PUERTO);
            Socket socket = null;

            while (true){
                socket = serverSocket.accept();
                Hilo hilo = new Hilo(socket);
                hilo.start();
                System.out.println("---CLIENTE CONECTADO "+ socket.getInetAddress().getHostName()+":"
                        +socket.getPort()+"---");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
