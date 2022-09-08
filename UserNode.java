import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.TreeMap;

public class UserNode {

    private static ServerSocket serverSocket;
    private static String name;
    private static int hashOfTheUserNode;

    private static Socket requestSocket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    private static TreeMap<Integer, SocketAddress> brokersMap;

    public static void main(String [] args){

        name = "";
        hashOfTheUserNode = -1;

        new UserNode(Integer.parseInt(args[0]));
    }

    public UserNode(int port){
        new Profile();
        try{
            serverSocket = new ServerSocket(port, 100, InetAddress.getByName(InetAddress.getLocalHost().toString()
                    .split("/")[1]));

            Profile.setServerSocket(serverSocket);

            System.out.println("Server socket of UserNode: " + serverSocket.getInetAddress().toString().substring(1)
                    + ":" + serverSocket.getLocalPort());

            hashOfTheUserNode = calculateKeys(serverSocket.getLocalSocketAddress().toString());
            System.out.println("Hash of the UserNode is: " + hashOfTheUserNode);

            connectionToTheBrokerAndUserNodeHandler();

            try {
                objectOutputStream.writeObject("UserNode");
                objectOutputStream.flush();

                objectOutputStream.writeObject(new InetSocketAddress(serverSocket.getInetAddress().toString().substring(1), port));
                objectOutputStream.flush();

                objectOutputStream.writeInt(hashOfTheUserNode);
                objectOutputStream.flush();

                brokersMap = (TreeMap<Integer, SocketAddress>) objectInputStream.readObject();
                Profile.setBrokers(brokersMap);

                System.out.println("These are the Brokers that exists right now: " + brokersMap.toString());
            } catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            } finally {
                disconnect();
            }

            if (brokersMap != null){
                while(true){

                    System.out.println("Please add your name: ");
                    Scanner in = new Scanner(System.in);
                    name = in.nextLine();
                    int appropriateBroker = calculateKeys(name);
                    SocketAddress brokerAddress = brokersMap.firstEntry().getValue();
                    for (int hash : brokersMap.keySet()) {
                        if (appropriateBroker <= hash) {
                            brokerAddress = brokersMap.get(hash);
                            break;
                        }
                    }
                    if (brokerAddress != null)
                        connect(brokerAddress);

                    objectOutputStream.writeObject("Initialization");
                    objectOutputStream.flush();

                    //SEND CHANNEL NAME
                    objectOutputStream.writeObject(name);
                    objectOutputStream.flush();

                    objectOutputStream.writeObject(serverSocket.getInetAddress().toString().substring(1));
                    objectOutputStream.flush();

                    Profile.setPort(serverSocket.getLocalPort());
                    objectOutputStream.writeObject(serverSocket.getLocalPort());
                    objectOutputStream.flush();

                    boolean alreadyExists = objectInputStream.readBoolean();
                    if (!alreadyExists){
                        System.out.println("Welcome to What's Up.");
                        Profile.setName(name);
                        break;
                    } else {
                        System.out.println("Please choose another name.");
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        //Publisher Mode
        new Publisher(serverSocket);

        //Consumer Mode
        new Consumer();
    }

    public static int calculateKeys(String topic_or_user) {

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] byteArray = sha1.digest(topic_or_user.getBytes());

            return new BigInteger(byteArray).intValue();
        }
        catch (NoSuchAlgorithmException nsae) {
            return 0;
        }
    }

    public void connectionToTheBrokerAndUserNodeHandler(){

        try {
            Scanner input = new Scanner(System.in);
            System.out.println("Give Address Keeper IP address : ");
            String inetAddress = input.nextLine();
            requestSocket = new Socket(InetAddress.getByName(inetAddress), 4000);
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());
            System.out.println("UserNode is running...");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(SocketAddress socketAddress) {
        try {
            requestSocket = new Socket();
            requestSocket.connect(socketAddress);
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void disconnect(){

        try {
            objectInputStream.close();
            objectOutputStream.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
