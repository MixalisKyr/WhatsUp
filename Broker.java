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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Broker {

    private static ServerSocket serverSocket;
    private static int hashOfTheBroker;

    private static Socket connectionSocket;

    private static Socket requestSocket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    private static HashMap<String, SocketAddress> userNodes;

    private static HashMap<String, ArrayList<SocketAddress>> userNodeRelatedWithOtherUserNodes;
    private static HashMap<String, ArrayList<SocketAddress>> topicsInTheCurrentBroker;

	public static void main(String [] args){

	    connectionSocket = null;
	    hashOfTheBroker = -1;
	    userNodes = new HashMap<>();
        userNodeRelatedWithOtherUserNodes = new HashMap<>();
        topicsInTheCurrentBroker = new HashMap<>();

        new Broker(Integer.parseInt(args[0]));
    }

    public Broker(int port){

	    try{
            serverSocket = new ServerSocket(port, 100, InetAddress.getByName(InetAddress.getLocalHost().toString().split("/")[1]));
            System.out.println("Server socket of Broker: " + serverSocket.getInetAddress().toString().substring(1)
                    + ":" + serverSocket.getLocalPort());

            hashOfTheBroker = calculateKeys(serverSocket.getLocalSocketAddress().toString());
            System.out.println("Hash of the Broker is: " + hashOfTheBroker);

            connectionToTheBrokerAndUserNodeHandler();

            objectOutputStream.writeObject("Broker");
            objectOutputStream.flush();

            objectOutputStream.writeObject(new InetSocketAddress(serverSocket.getInetAddress().toString().substring(1), port));
            objectOutputStream.flush();

            objectOutputStream.writeInt(hashOfTheBroker);
            objectOutputStream.flush();

            disconnectFromTheBrokerAndUserNodeHandler();

            while(true) {
                connectionSocket = serverSocket.accept();
                System.out.println(connectionSocket.getRemoteSocketAddress());
                new Handler(connectionSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public int calculateKeys(String brokerIP) {

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] byteArray = sha1.digest(brokerIP.getBytes());

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
            System.out.println("Broker is running...");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectFromTheBrokerAndUserNodeHandler(){

        try {
            objectInputStream.close();
            objectOutputStream.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Handler extends Thread {

        Socket socket;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        Handler(Socket s) {
            socket = s;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                String choice = (String) objectInputStream.readObject();

                switch (choice) {
                    case "Initialization": {
                        String name = (String) objectInputStream.readObject();
                        String socketIP = (String) objectInputStream.readObject();
                        int port = (int) objectInputStream.readObject();

                        SocketAddress userNodeAddress = new InetSocketAddress(socketIP, port);

                        if (userNodes.containsKey(name)) {
                            objectOutputStream.writeBoolean(true);
                            objectOutputStream.flush();
                        } else {
                            userNodes.put(name, userNodeAddress);
                            objectOutputStream.writeBoolean(false);
                            objectOutputStream.flush();
                        }

                        break;
                    }
                    case "Connect to Topic or User": {
                        String userNodeName = (String) objectInputStream.readObject();
                        String topic_or_user = (String) objectInputStream.readObject();
                        String decision = (String) objectInputStream.readObject();
                        String ip = (String) objectInputStream.readObject();
                        int port = (int) objectInputStream.readObject();

                        SocketAddress userNodeAddress = new InetSocketAddress(ip, port);

                        if (decision.toUpperCase(Locale.ROOT).equalsIgnoreCase("USER")) {
                            //Conversation with user
                            if (userNodes.containsKey(topic_or_user)) {
                                if (userNodeRelatedWithOtherUserNodes.get(userNodeName) != null) {
                                    ArrayList<SocketAddress> socketAddresses = userNodeRelatedWithOtherUserNodes.get(userNodeName);
                                    socketAddresses.add(userNodes.get(topic_or_user));
                                    userNodeRelatedWithOtherUserNodes.put(userNodeName, socketAddresses);
                                } else {
                                    ArrayList<SocketAddress> socketAddresses = new ArrayList<>();
                                    socketAddresses.add(userNodes.get(topic_or_user));
                                    userNodeRelatedWithOtherUserNodes.put(userNodeName, socketAddresses);
                                }

                                //Add also the request user in the list of the wanted user
                                if (userNodeRelatedWithOtherUserNodes.get(topic_or_user) != null) {
                                    ArrayList<SocketAddress> socketAddresses1 = userNodeRelatedWithOtherUserNodes.get(topic_or_user);
                                    socketAddresses1.add(userNodeAddress);
                                    userNodeRelatedWithOtherUserNodes.put(topic_or_user, socketAddresses1);
                                } else {
                                    ArrayList<SocketAddress> socketAddresses1 = new ArrayList<>();
                                    socketAddresses1.add(userNodeAddress);
                                    userNodeRelatedWithOtherUserNodes.put(topic_or_user, socketAddresses1);
                                }

                                //Also notify the other user that he has a new connection
                                notifyNewConnection(userNodes.get(topic_or_user), userNodeName);

                                objectOutputStream.writeObject("Joined you with " + topic_or_user + " successfully.");
                                objectOutputStream.flush();
                            } else {
                                objectOutputStream.writeObject("Unable to join you in the conversation with " + topic_or_user +
                                        ", because this user " + "doesn't exists.");
                                objectOutputStream.flush();
                            }
                        } else if (decision.toUpperCase(Locale.ROOT).equalsIgnoreCase("TOPIC")) {
                            //Conversation with team
                            ArrayList<SocketAddress> socketAddresses;
                            if (topicsInTheCurrentBroker.containsKey(topic_or_user)) {
                                socketAddresses = topicsInTheCurrentBroker.get(topic_or_user);
                            } else {
                                socketAddresses = new ArrayList<>();
                            }
                            socketAddresses.add(userNodeAddress);
                            topicsInTheCurrentBroker.put(topic_or_user, socketAddresses);

                            objectOutputStream.writeObject("Joined you in " + topic_or_user + " conversation successfully.");
                            objectOutputStream.flush();
                        }
                        break;
                    }
                    case "Start Conversation": {
                        String topic_or_user = (String) objectInputStream.readObject();
                        String decision = (String) objectInputStream.readObject();

                        if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                            objectOutputStream.writeObject((userNodes.get(topic_or_user)).toString());
                            objectOutputStream.flush();
                        }
                        //If decision.toUpperCase(Locale.ROOT).equals("TOPIC")
                        //Do nothing (user will not send something or broker to user)
                        //User is going to come back and send the message to the broker
                        //to share it to the rest UserNodes in the public conversation
                        //(SEE BELOW IN "Conversation" choice)
                        break;
                    }
                    case "Conversation":
                        String senderUserNode = (String) objectInputStream.readObject();
                        String message_or_photo_or_video = (String) objectInputStream.readObject();

                        switch (message_or_photo_or_video) {
                            case "Message": {
                                String message = (String) objectInputStream.readObject();
                                String topic = (String) objectInputStream.readObject();
                                String ip = (String) objectInputStream.readObject();
                                int port = (int) objectInputStream.readObject();

                                SocketAddress publisherAddress = new InetSocketAddress(ip, port);

                                if (topicsInTheCurrentBroker.containsKey(topic)) {
                                    for (SocketAddress socketAddress : topicsInTheCurrentBroker.get(topic)) {
                                        //Filter User Nodes to not send the message to Publisher
                                        if (!socketAddress.toString().equals(publisherAddress.toString())) {
                                            //Connect to subscribed in the team UserNodes and share them the message
                                            notifyNewMessage(socketAddress, senderUserNode, message_or_photo_or_video, message);
                                        }
                                    }
                                }
                                break;
                            }
                            case "Photo": {
                                String title = (String) objectInputStream.readObject();
                                String update = (String) objectInputStream.readObject();
                                String topic = (String) objectInputStream.readObject();
                                String ip = (String) objectInputStream.readObject();
                                int port = (int) objectInputStream.readObject();

                                SocketAddress publisherAddress = new InetSocketAddress(ip, port);

                                if (topicsInTheCurrentBroker.containsKey(topic)) {
                                    for (SocketAddress socketAddress : topicsInTheCurrentBroker.get(topic)) {
                                        if (!socketAddress.toString().equals(publisherAddress.toString())) {
                                            notifyNewImage(socketAddress, senderUserNode, message_or_photo_or_video, title, update);
                                        }
                                    }
                                }
                                break;
                            }
                            case "Video": {
                                String title = (String) objectInputStream.readObject();
                                String update = (String) objectInputStream.readObject();
                                String topic = (String) objectInputStream.readObject();
                                String ip = (String) objectInputStream.readObject();
                                int port = (int) objectInputStream.readObject();

                                SocketAddress publisherAddress = new InetSocketAddress(ip, port);

                                if (topicsInTheCurrentBroker.containsKey(topic)) {
                                    for (SocketAddress socketAddress : topicsInTheCurrentBroker.get(topic)) {
                                        if (!socketAddress.toString().equals(publisherAddress.toString())) {
                                            notifyNewVideo(socketAddress, senderUserNode, message_or_photo_or_video, title, update);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    case "User Node wants to disconnect From User Node":
                        String userNode = (String) objectInputStream.readObject();

                        if (userNodes.containsKey(userNode)) {
                            objectOutputStream.writeObject((userNodes.get(userNode)).toString().substring(1).split(":")[0]);
                            objectOutputStream.flush();

                            objectOutputStream.writeObject((userNodes.get(userNode)).toString().split(":")[1]);
                            objectOutputStream.flush();
                        }
                        break;
                    case "User Node wants to disconnect From Broker": {
                        String userNodeName = (String) objectInputStream.readObject();
                        String userNodeIP = (String) objectInputStream.readObject();
                        int userNodePort = (int) objectInputStream.readObject();

                        SocketAddress userNodeSocketAddress = new InetSocketAddress(userNodeIP, userNodePort);

                        userNodes.remove(userNodeName);

                        for (Map.Entry<String, ArrayList<SocketAddress>> item : userNodeRelatedWithOtherUserNodes.entrySet()) {
                            if (item.getValue().contains(userNodeSocketAddress)) {
                                ArrayList<SocketAddress> socketAddresses = item.getValue();
                                socketAddresses.remove(userNodeSocketAddress);
                                userNodeRelatedWithOtherUserNodes.put(item.getKey(), socketAddresses);
                            }
                        }

                        for (Map.Entry<String, ArrayList<SocketAddress>> item : topicsInTheCurrentBroker.entrySet()) {
                            if (item.getValue().contains(userNodeSocketAddress)) {
                                ArrayList<SocketAddress> socketAddresses = item.getValue();
                                socketAddresses.remove(userNodeSocketAddress);
                                topicsInTheCurrentBroker.put(item.getKey(), socketAddresses);
                            }
                        }
                        break;
                    }
                    case "Pull Photo":
                        String owner = (String) objectInputStream.readObject();
                        String title = (String) objectInputStream.readObject();

                        String ip = (userNodes.get(owner)).toString().substring(1).split(":")[0];
                        int port = Integer.parseInt((userNodes.get(owner)).toString().split(":")[1]);

                        SocketAddress ownerSocketAddress = new InetSocketAddress(ip, port);

                        byte [] photo = pullPhoto(ownerSocketAddress, title);

                        objectOutputStream.writeObject(photo.length);
                        objectOutputStream.flush();

                        objectOutputStream.write(photo);
                        objectOutputStream.flush();
                    case "Pull Video":
                        String ownerOfTheVideo = (String) objectInputStream.readObject();
                        String titleOfTheVideo = (String) objectInputStream.readObject();

                        String ipOfTheVideoHolder = (userNodes.get(ownerOfTheVideo)).toString().substring(1).split(":")[0];
                        int portOfTheVideoHolder = Integer.parseInt((userNodes.get(ownerOfTheVideo)).toString().split(":")[1]);

                        SocketAddress ownerSocketAddressOfTheVideo = new InetSocketAddress(ipOfTheVideoHolder, portOfTheVideoHolder);

                        ArrayList<byte []> chunks = pullVideo(ownerSocketAddressOfTheVideo, titleOfTheVideo);

                        objectOutputStream.writeObject(chunks.size());
                        objectOutputStream.flush();

                        while (!chunks.isEmpty()){
                            objectOutputStream.write(chunks.remove(0));
                            objectOutputStream.flush();
                        }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private Socket notifySocket;
        private ObjectOutputStream notifyObjectOutputStream;
        private ObjectInputStream notifyObjectInputStream;

        public void connect(SocketAddress socketAddress) {
            try {
                notifySocket = new Socket();
                notifySocket.connect(socketAddress);
                notifyObjectOutputStream = new ObjectOutputStream(notifySocket.getOutputStream());
                notifyObjectOutputStream.flush();
                notifyObjectInputStream = new ObjectInputStream(notifySocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void disconnect(){
            try {
                notifyObjectInputStream.close();
                notifyObjectOutputStream.close();
                notifySocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void notifyNewConnection(SocketAddress socketAddress, String nameOfUserNode){
            String ip = socketAddress.toString().substring(1).split(":")[0];
            int port = Integer.parseInt(socketAddress.toString().split(":")[1]);
            SocketAddress validSocketAddress = new InetSocketAddress(ip, port);
            connect(validSocketAddress);

            try {
                notifyObjectOutputStream.writeObject("User Node wants to connect");
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(nameOfUserNode);
                notifyObjectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        public void notifyNewMessage(SocketAddress notifiedAddress, String senderUserNode, String decision, String message){
            connect(notifiedAddress);

            try {
                notifyObjectOutputStream.writeObject("Conversation");
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(senderUserNode);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(decision);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(message);
                notifyObjectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        public void notifyNewImage(SocketAddress notifiedAddress, String senderUserNode, String decision, String title, String update){
            connect(notifiedAddress);

            try {
                notifyObjectOutputStream.writeObject("Conversation");
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(senderUserNode);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(decision);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(title);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(update);
                notifyObjectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        public void notifyNewVideo(SocketAddress notifiedAddress, String senderUserNode, String decision, String title, String update){
            connect(notifiedAddress);

            try {
                notifyObjectOutputStream.writeObject("Conversation");
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(senderUserNode);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(decision);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(title);
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(update);
                notifyObjectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        public byte [] pullPhoto(SocketAddress publisherAddress, String title){
            connect(publisherAddress);
            byte [] photo = null;

            try {
                notifyObjectOutputStream.writeObject("Pull Photo");
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(title);
                notifyObjectOutputStream.flush();

                String size = (String) notifyObjectInputStream.readObject();
                photo = new byte[Integer.parseInt(size)];
                notifyObjectInputStream.readFully(photo);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return photo;
            } finally {
                disconnect();
            }
            return photo;
        }

        public ArrayList<byte []> pullVideo(SocketAddress publisherAddress, String title){
            connect(publisherAddress);
            ArrayList<byte []> videoChunks = new ArrayList<>();

            try {
                notifyObjectOutputStream.writeObject("Pull Video");
                notifyObjectOutputStream.flush();

                notifyObjectOutputStream.writeObject(title);
                notifyObjectOutputStream.flush();

                String size = (String) notifyObjectInputStream.readObject();

                byte [] chunk;
                for (int counterOfChunks = 0;counterOfChunks < Integer.parseInt(size);counterOfChunks++){
                    chunk = new byte[2048];
                    notifyObjectInputStream.readFully(chunk);
                    videoChunks.add(chunk);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            } finally {
                disconnect();
            }
            return videoChunks;
        }
    }
}
