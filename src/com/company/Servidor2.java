package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class Servidor2 {

    private static final int PUERTO = 5000;
    private static final String RUTAUSUARIOS = "Usuarios.dat";
    private static final String RUTAPREGUNTAS = "Preguntas.txt";
    private static final String RUTARESPUESTAS = "Respuestas.txt";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println("---SERVIDOR INICIADO---");
        ServerSocket serverSocket = new ServerSocket(PUERTO);
        Socket socket;

        while (true){
            socket = serverSocket.accept();
            Hilo2 hilo = new Hilo2(socket);
            hilo.start();
            System.out.println("---CLIENTE CONECTADO "+ socket.getInetAddress().getHostName()+":"
                    +socket.getPort()+"---");
        }
    }

    public synchronized void guardarUsuario(Usuario usuario){

    }
}
