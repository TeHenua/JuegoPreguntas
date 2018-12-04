package com.company;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class Cliente {

    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private static final int PUERTO = 5000;
    private static OutputStream outputStream;
    private static InputStream inputStream;
    private static PublicKey key;
    private static Cipher cipher;

    public static void main(String[] args) {
        try {
            cipher = Cipher.getInstance("RSA");
            Socket socket = new Socket(InetAddress.getLocalHost(),PUERTO);
            System.out.println("---CONECTADO CORRECTAMENTE AL SERVIDOR---");
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            key = (PublicKey) objectInputStream.readObject();
            cipher.init(Cipher.ENCRYPT_MODE,key);
            System.out.println("---CLAVE PÚBLICA RECIBIDA---");

            System.out.println("Escribe 'SI' para hacer login si ya estás registrado\n" +
                    "En caso contrario serás redirigido al formulario de registro");
            String opcion = br.readLine();
            //envio la opción del menú
            objectOutputStream.writeUTF(opcion);

            boolean correcto = false;
            if (opcion.equalsIgnoreCase("SI")){     //LOGIN
                while (!correcto){
                    System.out.println("Escribe tu usuario:");
                    String nick = br.readLine();
                    //cifro el nick y lo envio
                    byte[] nickCifrado = cipher.doFinal(nick.getBytes());
                    objectOutputStream.writeObject(nickCifrado);
                    //recibo la confirmación de que el nick existe
                    boolean nickCorrecto = objectInputStream.readBoolean();
                    if (nickCorrecto){
                        System.out.println("Escribe tu contraseña:");
                        //cifro la contraseña y la envio
                        byte[] passCifrado = cipher.doFinal(br.readLine().getBytes());
                        objectOutputStream.writeObject(passCifrado);
                        //recibo confirmación de la contraseña
                        boolean passCorrecto = objectInputStream.readBoolean();
                        if (passCorrecto){
                            System.out.println("---LOGIN REALIZADO CORRECTAMENTE---");
                            correcto = true;
                        }else {
                            correcto = false;
                        }
                    }else {
                         correcto = false;
                    }
                }
            }else {                                                 //REGISTRO
                System.out.println("Escribe tu nombre:");
                objectOutputStream.writeUTF(br.readLine());
                System.out.println("Escribe tu apellido");
                objectOutputStream.writeUTF(br.readLine());
                System.out.println("Escribe tu edad:");
                objectOutputStream.writeInt(Integer.parseInt(br.readLine()));
                System.out.println("Escribe tu nick:");
                objectOutputStream.writeUTF(br.readLine());
                System.out.println("Escribe tu contraseña:");
                objectOutputStream.writeObject(cipher.doFinal(br.readLine().getBytes()));

            }
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
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }
}
