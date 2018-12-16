package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;

import static com.company.Servidor.fos;
import static com.company.Servidor.oosFile;
import static com.company.Servidor.fis;
import static com.company.Servidor.oisFile;

public class Hilo extends Thread {


    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private KeyPairGenerator keyPairGenerator;
    private KeyPair keyPair;
    private Cipher cipher;
    private Signature dsa;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private boolean correcto;
    private String reglas = "Reglas del juego:";

    private Usuario usuario;
    private ArrayList<Usuario> usuarios;

    public Hilo(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {

            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPair = keyPairGenerator.generateKeyPair();
            iniciarCipher();
            dsa = Signature.getInstance("SHA1WITHRSA");
            dsa.initSign(keyPair.getPrivate());

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            objectOutputStream = new ObjectOutputStream(outputStream);
            objectInputStream = new ObjectInputStream(inputStream);

            objectOutputStream.writeObject(keyPair.getPublic());
            System.out.println("---CLAVE PÚBLICA ENVIADA---");
            do {
                //recibo la opcion del menú SI para login, otros para registro
                String opcion = objectInputStream.readUTF();

                correcto = false;
                if (opcion.equalsIgnoreCase("SI")) {
                    login();
                } else {
                    registro();
                }
            }while (!correcto);
            //envio nick para el saludo
            objectOutputStream.writeUTF(usuario.getNick());
            System.out.println("---ENVIADO NICK "+usuario.getNick()+"---");
            //firmo las reglas
            dsa.update(reglas.getBytes());
            byte[] firma = dsa.sign();
            //envio las reglas firmadas y la firma
            objectOutputStream.writeObject(reglas);
            objectOutputStream.writeObject(firma);

            //cerrar conexiones
            objectOutputStream.close();
            objectInputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }


    }

    private void iniciarCipher(){
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE,keyPair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

    }

    private void login(){
        try{
            //recibo el nick y lo descifro
            byte[] nickCifrado = (byte[]) objectInputStream.readObject();
            System.out.println("---NICK CIFRADO RECIBIDO---");
            iniciarCipher();
            String nick = new String(cipher.doFinal(nickCifrado));
            System.out.println("---NICK DESCIFRADO---");
            //compruebo si existe y recojo el pass
            leerNickArchivo(nick);

            //envio la confirmacion del nick
            if (usuario != null) {
                objectOutputStream.writeObject(true);
                //recibo el pass y lo comparo con el guardado
                byte[] passRecibido = (byte[]) objectInputStream.readObject();
                if (usuario.getPass().equals(new String(cipher.doFinal(passRecibido)))) {
                    objectOutputStream.writeObject(true);
                    System.out.println("---USUARIO " + usuario.getNombre() + "//" + usuario.getNick() + " LOGEADO CORRECTAMENTE---");
                    correcto = true;
                } else {
                    objectOutputStream.writeObject(false);
                    System.out.println("---PASS INCORRECTO---");
                    correcto = false;
                }
            } else {
                objectOutputStream.writeObject(false);
                correcto = false;
            }
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registro(){
        try {
            System.out.println("---COMIENZA EL REGISTRO DE USUARIO---");
            /*String nombre = objectInputStream.readUTF();
            String apellido = objectInputStream.readUTF();
            int edad = objectInputStream.readInt();
            String nick = objectInputStream.readUTF();
            byte[] pass = (byte[]) objectInputStream.readObject();
            usuario = new Usuario(nombre, apellido, edad, nick, new String(cipher.doFinal(pass)));
            System.out.println("---USUARIO CREADO---");*/
            iniciarCipher();
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream,cipher);
            ObjectInputStream oisCip = new ObjectInputStream(cipherInputStream);

            SealedObject sealedObject = (SealedObject) oisCip.readObject();
            usuario = (Usuario) sealedObject.getObject(cipher);
            //leo el archivo y guardo todos los usuarios en el array
            boolean guardado = leerGuardarUsuario();
            if (guardado) {
                System.out.println("---USUARIO GUARDADO---");
            } else {
                System.out.println("---ERROR USUARIO NO GUARDADO---");
            }
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean leerGuardarUsuario() {
        try {

            //oisFile = new ObjectInputStream(fis);
            /*try {
                while (true) {
                    Usuario usuarioTemp = (Usuario) oisFile.readObject();
                    usuarios.add(usuarioTemp);
                }
            } catch (EOFException e){}
            catch(IOException e){
                e.printStackTrace();
            }

            //añado el usuario nuevo al arraylist
            if (usuarios == null) {
                usuarios = new ArrayList<>();
            }
            usuarios.add(usuario);
*/
            //for (Usuario usu : usuarios) {
            oosFile.flush();
            oosFile.writeObject(usuario);
            //}
            System.out.println("---ARCHIVO DE USUARIOS ACTUALIZADO---");
        //}  catch (ClassNotFoundException e) {
            //e.printStackTrace();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } /*finally {
            try {
                oosFile.flush();
                //fis.close();
                //oisFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/


    }

    private synchronized void leerNickArchivo(String nick){
        try {

            while (true) {
                Usuario usuarioTemp = (Usuario) oisFile.readObject();
                System.out.println("--BUSCANDO NICK EN EL ARCHIVO:" + usuarioTemp.getNick() + "---");
                if (usuarioTemp.getNick().equals(nick)) {
                    usuario = usuarioTemp;
                    System.out.println("---NICK ENCONTRADO---");
                }
            }
        } catch (EOFException e) {
        } catch (StreamCorruptedException x) {
            x.printStackTrace();
        } catch (ClassNotFoundException o) {
            o.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }/*finally {
            try {
                fis.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }
}