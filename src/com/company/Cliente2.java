package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;

public class Cliente2 {
    private static final int PUERTO = 5000;
    private static OutputStream outputStream;
    private static InputStream inputStream;
    private static PublicKey key;
    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, SignatureException, BadPaddingException {
        Socket socket = new Socket(InetAddress.getLocalHost(),PUERTO);
        System.out.println("---CONECTADO CORRECTAMENTE AL SERVIDOR---");

        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();

        recibirKey();
        boolean continuar = false;
        do{
            System.out.print("NICK:");
            String nick = br.readLine();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE,key);
            byte[] nickCifrado = cipher.doFinal(nick.getBytes());
            boolean encontrado = comprobarUsuario(nickCifrado);
            if (encontrado){
                System.out.println("---NICK ENCONTRADO---");
                boolean correcto;
                do {
                    System.out.print("Contraseña:");
                    String pass = br.readLine(); //TODO cifrar
                    byte[] passCifrado = cipher.doFinal(pass.getBytes());
                    correcto = enviarPass(passCifrado);
                    if (correcto){
                        continuar = true;
                    }
                }while (!correcto);
            }else {
                System.out.println("Usuario no encontrado");
                System.out.println("Rellena los datos a continuación para registrarte.");
                registrarUsuario();
            }
        }while (!continuar);
        System.out.println("Reglas del juego:");
        recibirReglas();
        for (int i = 0; i < 5; i++) {
            recibirPregunta();
            enviarRespuesta();
        }
        recibirPuntos();
        recibirRecord();
    }

    private static void recibirRecord() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        boolean nueva = (boolean) objectInputStream.readObject();
        if (nueva){
            System.out.println("Enhorabuena!! Has conseguido un nuevo record");
        }else {
            System.out.println("No has conseguido superar tu record. Suerte la próxima");
        }
    }

    private static void recibirPuntos() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        int puntos = (int) objectInputStream.readObject();
        System.out.println("Puntuación: "+puntos+" puntos.");
    }

    private static void enviarRespuesta() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        String respueta = br.readLine();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] respuestaCifrada = cipher.doFinal(respueta.getBytes());
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(respuestaCifrada);
        outputStream.flush();
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        boolean correcto = (boolean) objectInputStream.readObject();
        if (correcto){
            System.out.println("Respuesta correcta.");
        }else {
            System.out.println("Respuesta incorrecta.");
        }
    }

    private static void recibirPregunta() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        System.out.print(objectInputStream.readObject());
    }

    private static void recibirReglas() throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        byte[] firma = (byte[]) objectInputStream.readObject();
        String reglas = (String) objectInputStream.readObject();
        Signature signature = Signature.getInstance("SHA1WITHRSA");
        signature.initVerify(key);
        signature.update(reglas.getBytes());
        boolean check = signature.verify(firma);
        if (check){
            System.out.println(reglas);
        }else {
            System.out.println("---ERROR AL RECIBIR LAS REGLAS---");
        }
    }

    private static boolean enviarPass(byte[] pass) throws IOException, ClassNotFoundException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(pass);
        outputStream.flush();
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        return (boolean) objectInputStream.readObject();
    }

    private static void registrarUsuario() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        Usuario usuario = pedirDatosRegistro();
        SealedObject sealedObject = new SealedObject(usuario,cipher);
        System.out.println("---USUARIO ENCRIPTADO---");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        System.out.println("---ENVIANDO USUARIO---");
        objectOutputStream.writeObject(sealedObject);
        System.out.println("---USUARIO ENVIADO---");
        outputStream.flush();
    }

    private static Usuario pedirDatosRegistro() throws IOException {
        System.out.print("Nombre:");
        String nombre = br.readLine();
        System.out.print("Apellido:");
        String apellido = br.readLine();
        boolean correcto;
        int edad = 0;
        do {
            System.out.print("Edad:");
            try {
                edad = Integer.parseInt(br.readLine());
                correcto = true;
            }catch (NumberFormatException e){
                correcto = false;
            }
        }while (!correcto);
        System.out.print("Nick:");
        String nick = br.readLine();
        System.out.print("Contraseña:");
        String pass = br.readLine();
        return new Usuario(nombre,apellido,edad,nick,pass);
    }

    private static boolean comprobarUsuario(byte[] nick) throws IOException, ClassNotFoundException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(nick);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        boolean correcto = (boolean) objectInputStream.readObject();
        outputStream.flush();
        return correcto;
    }

    private static void recibirKey() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        key = (PublicKey) objectInputStream.readObject();
        //inputStream.reset();
        System.out.println("---CLAVE RECIBIDA---");
    }
}
