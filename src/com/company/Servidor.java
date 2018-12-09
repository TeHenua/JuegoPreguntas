package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    private static final int PUERTO = 5000;
    private static final String RUTAUSUARIOS = "Usuarios.dat";
    private static FileOutputStream fos;
    private static FileInputStream fis;
    public static ObjectOutputStream oosFile;
    public static ObjectInputStream oisFile;

    public static void main(String[] args) {
        try {
            System.out.println("---SERVIDOR INICIADO---");
            ServerSocket serverSocket = new ServerSocket(PUERTO);
            Socket socket = null;
            fos = new FileOutputStream(RUTAUSUARIOS);
            fis = new FileInputStream(RUTAUSUARIOS);
            oosFile = new ObjectOutputStream(fos);
            oisFile = new ObjectInputStream(fis);
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
