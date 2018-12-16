package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Hilo2 extends Thread{

    private final String RUTAUSUARIOS = "Usuarios.dat";
    private final String RUTAPREGUNTAS = "Preguntas.txt";
    private final String RUTARESPUESTAS = "Respuestas.txt";
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private KeyPair keyPair;
    private ArrayList<String> preguntas;
    private ArrayList<String> respuestas;
    private Usuario usuarioLogeado;

    public Hilo2(Socket socket) throws IOException, NoSuchAlgorithmException {
        this.socket = socket;
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        keyPair = generarLlave();
    }

    @Override
    public void run(){
        try {
            enviarLlave();
            boolean continuar;
            do{
                continuar = comprobarUsuario();
            }while (!continuar);
            enviarReglas();
            cargarPreguntas();
            int puntuación = 0;
            Random r = new Random();
            for (int i=0;i<5;i++) {
                int n = r.nextInt(25);
                enviarPregunta(n);
                puntuación += comprobarRespuesta(n);
            }
            enviarPuntos(puntuación);
            boolean nueva = comprobarRecord(puntuación);
            enviarRecord(nueva);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
    }

    private void enviarRecord(boolean nueva) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(nueva);
        outputStream.flush();
    }

    private boolean comprobarRecord(int puntuacion) throws IOException, ClassNotFoundException {
        if (usuarioLogeado.getRecord()<puntuacion){
            usuarioLogeado.setRecord(puntuacion);
            ArrayList<Usuario> usuarios = new ArrayList<>();
            ObjectInputStream oisFile;
            try {
                oisFile = new ObjectInputStream(new FileInputStream(RUTAUSUARIOS));
            }catch (FileNotFoundException e){
                oisFile = null;
            }
            boolean salir = false;
            if (oisFile!=null){
                do {
                    try {
                        usuarios.add((Usuario) oisFile.readObject());
                    }catch (EOFException e){
                        salir = true;
                    }
                }while (!salir);
            }
            oisFile.close();
            ObjectOutputStream oosFile = new ObjectOutputStream(new FileOutputStream(RUTAUSUARIOS,false));
            for (Usuario u:usuarios) {
                if (u.getNick().equals(usuarioLogeado.getNick())){
                    u.setRecord(puntuacion);
                }
                oosFile.writeObject(u);
            }
            oosFile.close();
            return true;
        }else {
            return false;
        }
    }

    private void enviarPuntos(int puntuación) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(puntuación);
        outputStream.flush();
    }

    private int comprobarRespuesta(int n) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        byte[] respuestaCifrada = (byte[]) objectInputStream.readObject();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate());
        String respuesta = new String(cipher.doFinal(respuestaCifrada));
        if (respuesta.equalsIgnoreCase(respuestas.get(n))){
            objectOutputStream.writeObject(true);
            return 1;
        }else {
            objectOutputStream.writeObject(false);
            return 0;
        }
    }

    private void enviarPregunta(int n) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(preguntas.get(n));
        outputStream.flush();
    }

    private void cargarPreguntas() throws FileNotFoundException {
        Scanner sPre = new Scanner(new File(RUTAPREGUNTAS),"UTF-8");
        preguntas = new ArrayList<>();
        while (sPre.hasNextLine()){
            preguntas.add(sPre.nextLine());
        }
        sPre.close();
        Scanner sRes = new Scanner(new File(RUTARESPUESTAS),"UTF-8");
        respuestas = new ArrayList<>();
        while (sRes.hasNextLine()){
            respuestas.add(sRes.nextLine());
        }
        sRes.close();
    }

    private void enviarReglas() throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        String reglas = "REGLAS PENDIENTES DE ESCRIBIR";
        Signature signature = Signature.getInstance("SHA1WITHRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(reglas.getBytes());
        byte[] firma = signature.sign();
        objectOutputStream.writeObject(firma);
        objectOutputStream.writeObject(reglas);
        outputStream.flush();
    }

    private boolean comprobarUsuario() throws IOException, ClassNotFoundException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        byte[] nick = (byte[]) objectInputStream.readObject();
        Usuario usuario = buscarUsuario(nick);
        boolean encontrado = false;
        if (usuario!=null){
            encontrado = true;
        }
        objectOutputStream.writeObject(encontrado);
        if (encontrado){
            System.out.println("---USUARIO ENCONTRADO---");
            do {
                encontrado = comprobarPass(usuario);
            }while (!encontrado);
            if (encontrado){
                System.out.println("---PASS CORRECTO---");
            }
        }else {
            System.out.println("---USUARIO NO ENCONTRADO---");
            registrarUsuario();
        }
        outputStream.flush();
        return encontrado;
    }

    private boolean comprobarPass(Usuario usuario) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        boolean correcto;
        byte[] passCifrado = (byte[]) objectInputStream.readObject();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate());
        String pass = new String(cipher.doFinal(passCifrado));
        if (usuario.getPass().equals(pass)){
            correcto = true;
            usuarioLogeado = usuario;
        }else {
            correcto = false;
            System.out.println("---PASS INCORRECTO---");
        }
        objectOutputStream.writeObject(correcto);
        outputStream.flush();
        return correcto;
    }

    private void registrarUsuario() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException {
        ArrayList<Usuario> usuarios = new ArrayList<>();
        ObjectInputStream oisFile;
        try {
            oisFile = new ObjectInputStream(new FileInputStream(RUTAUSUARIOS));
        }catch (FileNotFoundException e){
            oisFile = null;
        }
        boolean salir = false;
        if (oisFile!=null){
            do {
                try {
                    usuarios.add((Usuario) oisFile.readObject());
                }catch (EOFException e){
                    salir = true;
                }
            }while (!salir);
        }
        oisFile.close();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate());
        ObjectOutputStream oosFile = new ObjectOutputStream(new FileOutputStream(RUTAUSUARIOS,false));
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        System.out.println("---RECIBIENDOO USUARIO---");
        SealedObject sealedObject = (SealedObject) objectInputStream.readObject();
        System.out.println("---USUARIO RECIBIDO---");
        Usuario usuario = (Usuario) sealedObject.getObject(cipher);
        System.out.println("---USUARIO DESCIFRADO---");
        usuarios.add(usuario);
        for (Usuario u:usuarios){
            oosFile.writeObject(u);
        }
        System.out.println("--USUARIO ALMACENADO---");
        oosFile.close();

    }

    private Usuario buscarUsuario(byte[] nickCifrado) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        ObjectInputStream oisFile = null;
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate());
        String nick = new String(cipher.doFinal(nickCifrado));
        Usuario usuario;
        try {
            oisFile = new ObjectInputStream(new FileInputStream(RUTAUSUARIOS));
        }catch (FileNotFoundException e){
            usuario = null;
        }
        boolean correcto;
        do {
            try {
                usuario = (Usuario) oisFile.readObject();
                if (usuario.getNick().equals(nick)){
                    return usuario;
                }else {
                    usuario = null;
                    correcto = false;
                }
            }catch (EOFException e){
                correcto = true;
                usuario = null;
            } catch (ClassNotFoundException e) {
                correcto = false;
                usuario = null;
            } catch (NullPointerException e){
                correcto = true;
                usuario = null;
            }
        }while (!correcto);
        oisFile.close();
        return usuario;
    }

    private void enviarLlave() throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(keyPair.getPublic());
        outputStream.flush();
        System.out.println("---LLAVE ENVIADA---");
    }

    private KeyPair generarLlave() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        return keyPairGenerator.generateKeyPair();
    }


}
