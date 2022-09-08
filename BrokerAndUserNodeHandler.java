import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

public class BrokerAndUserNodeHandler {

    private static ServerSocket serverSocket;
    private static TreeMap<Integer, SocketAddress> brokersMap;
    private static TreeMap<Integer, SocketAddress> userNodesMap = null;

    private static Socket requestSocket;
    private static ObjectInputStream objectInputStream;
    private static ObjectOutputStream objectOutputStream;

    public void initialize(){
        Socket connectionSocket;
        try {
            serverSocket = new ServerSocket(4000, 100, InetAddress.getByName(InetAddress.getLocalHost().toString().split("/")[1]));
            System.out.println("Broker & UserNode handler ip: " + serverSocket.getInetAddress().toString().substring(1));

            while(true) {
                connectionSocket = serverSocket.accept();
                new Handler(connectionSocket).start();
            }
        } catch(IOException error) {
            throw new RuntimeException("Unable to start ServerSocket ", error);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public synchronized void addBrokerToBrokerMap(ObjectInputStream objectInputStream){
        SocketAddress socketAddress;
        try {
            socketAddress = (SocketAddress) objectInputStream.readObject();
            System.out.println("Socket address: " + socketAddress);
            int hashOfTheIncomingBroker = objectInputStream.readInt();
            brokersMap.put(hashOfTheIncomingBroker, socketAddress);
            System.out.println("Brokers:" + brokersMap);
        } catch (IOException | ClassNotFoundException error) {
            error.printStackTrace();
        }
    }

    public synchronized void addUserNodeToUserNodesMap(ObjectInputStream objectInputStream){
        SocketAddress socketAddress;
        try {
            socketAddress = (SocketAddress) objectInputStream.readObject();
            System.out.println("Socket address: " + socketAddress);
            int hashOfTheIncomingUserNode = objectInputStream.readInt();
            userNodesMap.put(hashOfTheIncomingUserNode, socketAddress);
            System.out.println("User Nodes:" + userNodesMap);
        } catch (IOException | ClassNotFoundException error) {
            error.printStackTrace();
        }
    }

    class Handler extends Thread {

        Socket socket;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        Handler(Socket inputSocket) {
            socket = inputSocket;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.flush();
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException error) {
                error.printStackTrace();
            }
        }

        public void run() {

            try {
                String BrokerOrUserNode = (String) objectInputStream.readObject();

                if (BrokerOrUserNode.equals("Broker")){
                    //Broker
                    addBrokerToBrokerMap(objectInputStream);
                    if (userNodesMap.size() != 0){
                        for (Map.Entry<Integer, SocketAddress> userNode : userNodesMap.entrySet()){
                            notifyUserNodesForNewBrokers(userNode.getValue());
                        }
                    }
                } else if (BrokerOrUserNode.equals("UserNode")){
                    //UserNode
                    addUserNodeToUserNodesMap(objectInputStream);
                    objectOutputStream.writeObject(brokersMap);
                    objectOutputStream.flush();
                }
            } catch (IOException | ClassNotFoundException error) {
                error.printStackTrace();
            }
            try {
                objectInputStream.close();
                objectOutputStream.close();
                socket.close();
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }

    public void notifyUserNodesForNewBrokers(SocketAddress socketAddress){
        connectToUserNode(socketAddress);

        try{
            objectOutputStream.writeObject("New Broker");
            objectOutputStream.flush();

            objectOutputStream.writeObject(brokersMap);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        disconnectFromUserNode();
    }

    public void connectToUserNode(SocketAddress socketAddress){

        try {
            requestSocket = new Socket();
            requestSocket.connect(socketAddress);
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectFromUserNode(){
        try {
            objectInputStream.close();
            objectOutputStream.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BrokerAndUserNodeHandler brokerAndUserNodeHandler = new BrokerAndUserNodeHandler();
        brokersMap = new TreeMap<>();
        userNodesMap = new TreeMap<>();
        brokerAndUserNodeHandler.initialize();
    }
}
