package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
    private static final int PUERTO = 5000;
    private static final String RUTAUSUARIOS = "Usuarios.dat";
    private static final String RUTAPREGUNTAS = "Preguntas.txt";
    private static final String RUTARESPUESTAS = "Respuestas.txt";
    public static FileOutputStream fos;
    public static FileInputStream fis;
    public static ObjectOutputStream oosFile;
    public static ObjectInputStream oisFile;
    public static BufferedReader readerPre;
    public static BufferedReader readerRes;

    public static void main(String[] args) {
        try {
            System.out.println("---SERVIDOR INICIADO---");
            ServerSocket serverSocket = new ServerSocket(PUERTO);
            Socket socket = null;
            fos = new FileOutputStream(RUTAUSUARIOS,true);
            fis = new FileInputStream(RUTAUSUARIOS);
            oosFile = new ObjectOutputStream(fos);
            oisFile = new ObjectInputStream(fis);
            File file = new File(RUTAUSUARIOS);
            System.out.println(file.length());
            BufferedInputStream bisPre = new BufferedInputStream(new FileInputStream(new File(RUTAPREGUNTAS)));
            BufferedInputStream bisRes = new BufferedInputStream(new FileInputStream(RUTARESPUESTAS));
            readerPre = new BufferedReader(new InputStreamReader(bisPre));
            readerRes = new BufferedReader(new InputStreamReader(bisRes));
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
