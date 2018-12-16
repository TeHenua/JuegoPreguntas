package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.*;

public class Cliente {

    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private static final int PUERTO = 5000;
    private static OutputStream outputStream;
    private static InputStream inputStream;
    private static PublicKey key;
    private static Cipher cipher;
    private static ObjectInputStream objectInputStream;
    private static ObjectOutputStream objectOutputStream;
    private static boolean correcto;
    private static String opcion;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(),PUERTO);
            System.out.println("---CONECTADO CORRECTAMENTE AL SERVIDOR---");
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            objectOutputStream = new ObjectOutputStream(outputStream);
            objectInputStream = new ObjectInputStream(inputStream);

            key = (PublicKey) objectInputStream.readObject();

            System.out.println("---CLAVE PÚBLICA RECIBIDA---");
            menuUsuario();
            menu();
            //cerrar conexiones
            objectInputStream.close();
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void iniciarCipher(){
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE,key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

    }

    private static void menu(){
        try {
            //recibo el nick
            String nick = objectInputStream.readUTF();
            System.out.println("Bienvenido "+nick);
            //recibo las reglas y la firma
            String reglas = (String) objectInputStream.readObject();
            byte[] firma = (byte[]) objectInputStream.readObject();
            System.out.println("---RECIBIDAS REGLAS Y FIRMA---");
            Signature dsa = Signature.getInstance("SHA1WITHRSA");
            dsa.initVerify(key);
            dsa.update(reglas.getBytes());
            boolean check = dsa.verify(firma);
            if (check){
                //la firma es correcta
                System.out.println(reglas);
            }else{
                //la firma NO es correcta
                System.out.println("La reglas no se han recibido correctamente. No se puede verificar la firma.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

    }

    private static void menuUsuario(){
        do {
            try {
                System.out.println("Escribe 'SI' para hacer login si ya estás registrado\n" +
                        "En caso contrario serás redirigido al formulario de registro");
                opcion = br.readLine();
                correcto = false;
                //envio la opción del menú
                objectOutputStream.writeUTF(opcion);
                if (opcion.equalsIgnoreCase("SI")){
                    login();
                }else {
                    registro();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }while (!correcto);
    }

    private static void login(){
        try {
            System.out.println("Escribe tu usuario:");
            String nick = br.readLine();
            //cifro el nick y lo envio
            iniciarCipher();
            byte[] nickCifrado = cipher.doFinal(nick.getBytes());
            System.out.println("---NICK CIFRADO---");
            objectOutputStream.writeObject(nickCifrado);
            System.out.println("---NICK ENVIADO---");
            //recibo la confirmación de que el nick existe
            boolean nickCorrecto = (boolean) objectInputStream.readObject();
            System.out.println("---RECIBIDA CONFIRMACIÓN---");
            if (nickCorrecto) {
                System.out.println("Escribe tu contraseña:");
                //cifro la contraseña y la envio
                byte[] passCifrado = cipher.doFinal(br.readLine().getBytes());
                objectOutputStream.writeObject(passCifrado);
                //recibo confirmación de la contraseña
                boolean passCorrecto = (boolean) objectInputStream.readObject();
                if (passCorrecto) {
                    System.out.println("---LOGIN REALIZADO CORRECTAMENTE---");
                    correcto = true;
                } else {
                    correcto = false;
                }
            } else {
                System.out.println("Usuario no encontrado");
                correcto = false;
            }
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void registro(){ //TODO cifrar antes de enviar
        try {
            /*System.out.println("Escribe tu nombre:");
            objectOutputStream.writeUTF(br.readLine());
            System.out.println("Escribe tu apellido");
            objectOutputStream.writeUTF(br.readLine());
            System.out.println("Escribe tu edad:");
            objectOutputStream.writeInt(Integer.parseInt(br.readLine()));
            System.out.println("Escribe tu nick:");
            objectOutputStream.writeUTF(br.readLine());
            System.out.println("Escribe tu contraseña:");
            objectOutputStream.writeObject(cipher.doFinal(br.readLine().getBytes()));*/

            System.out.println("Escribe tu nombre:");
            String nombre = br.readLine();
            System.out.println("Escribe tu apellido");
            String apellido = br.readLine();
            System.out.println("Escribe tu edad:");
            int edad = Integer.parseInt(br.readLine());
            System.out.println("Escribe tu nick:");
            String nick = br.readLine();
            System.out.println("Escribe tu contraseña:");
            String pass = br.readLine();
            Usuario usuario = new Usuario(nombre,apellido,edad,nick,pass);
            SealedObject sealedObject = new SealedObject(usuario,cipher);
            iniciarCipher();
            CipherOutputStream cos = new CipherOutputStream(outputStream,cipher);
            ObjectOutputStream oosCip = new ObjectOutputStream(cos);
            oosCip.writeObject(sealedObject);
            outputStream.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }  catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NumberFormatException e){
            System.out.println("Número incorrecto");
        }
    }
}
