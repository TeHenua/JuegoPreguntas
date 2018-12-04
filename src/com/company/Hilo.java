package com.company;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;

public class Hilo extends Thread{

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private KeyPairGenerator keyPairGenerator;
    private KeyPair keyPair;
    private Cipher cipher;
    private final String RUTAUSUARIOS = "Usuarios.txt";
    private Usuario usuario;
    private ArrayList<Usuario> usuarios;

    public Hilo(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run(){

        try {

            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPair = keyPairGenerator.generateKeyPair();
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate());

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            ObjectOutputStream oosFile = new ObjectOutputStream(new FileOutputStream(new File(RUTAUSUARIOS)));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            ObjectInputStream oisFile = new ObjectInputStream(new FileInputStream(new File(RUTAUSUARIOS)));
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            

            objectOutputStream.writeObject(keyPair.getPublic());
            System.out.println("---CLAVE PÚBLICA ENVIADA---");
            //recibo la opcion del menú SI para login, otros para registro
            String opcion = objectInputStream.readUTF();

            boolean correcto = false;
            if (opcion.equalsIgnoreCase("SI")) {    //LOGIN
                while (!correcto){
                    //recibo el nick y lo descifro
                    byte[] nickCifrado = (byte[]) objectInputStream.readObject();
                    String nick = new String(cipher.doFinal(nickCifrado));
                    //compruebo si existe y recojo el pass
                    try {
                        while (true) {
                            Usuario usuarioTemp = (Usuario) oisFile.readObject();
                            if (usuarioTemp.getNick().equals(nick)) {
                                usuario = usuarioTemp;
                            }
                        }
                    } catch (EOFException e) {
                    } catch (StreamCorruptedException x) {
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    //envio la confirmacion del nick
                    if (usuario != null) {
                        objectOutputStream.writeBoolean(true);
                        //recibo el pass y lo comparo con el guardado
                        byte[] passRecibido = (byte[]) objectInputStream.readObject();
                        if (Arrays.equals(usuario.getPass(), passRecibido)) {
                            objectOutputStream.writeBoolean(true);
                            System.out.println("---USUARIO " + usuario.getNombre() + "//" + usuario.getNick() + " LOGEADO CORRECTAMENTE---");
                            correcto = true;
                        } else {
                            objectOutputStream.writeBoolean(false);
                            correcto = false;
                        }
                    } else {
                        objectOutputStream.writeBoolean(false);
                        correcto = false;
                    }
                }
            }else {                                                 //REGISTRO
                System.out.println("---COMIENZA EL REGISTRO DE USUARIO---");
                String nombre = objectInputStream.readUTF();
                String apellido = objectInputStream.readUTF();
                int edad = objectInputStream.readInt();
                String nick = objectInputStream.readUTF();
                outputStream.flush();
                byte[] pass = (byte[]) objectInputStream.readObject();
                usuario = new Usuario(nombre,apellido,edad,nick,pass);
                System.out.println("---USUARIO CREADO---");
                //leo el archivo y guardo todos los usuarios en el array
                try {
                    while (true) {
                        Usuario usuarioTemp = (Usuario) oisFile.readObject();
                        usuarios.add(usuarioTemp);
                    }
                } catch (EOFException e) {
                } catch (StreamCorruptedException x) {
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                //añado el usuario nuevo al arraylist
                if (usuarios==null){
                    usuarios = new ArrayList<>();
                }
                usuarios.add(usuario);
                for (Usuario usu:usuarios){
                    objectOutputStream.writeObject(usu);
                }
                System.out.println("---ARCHIVO DE USUARIOS ACTUALIZADO---");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }


    }
}