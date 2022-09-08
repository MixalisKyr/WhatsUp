import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Consumer {

    private static Socket requestSocket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    public Consumer(){
        startUserInterface();
    }

    public void startUserInterface(){
        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("\nMenu");
            //Methods
            System.out.println("0. Exit");
            System.out.println("1. Subscribe UserNode to topic or another UserNode");
            System.out.println("2. Start Conversation");
            System.out.println("3. Check Profile");
            System.out.println("4. Pull Photos");
            System.out.println("5. Pull Videos");
            String choice = input.nextLine();
            switch (choice) {
                case "0":
                    System.out.println("Disconnecting you from other UserNodes...");
                    notifyUserNodesForDisconnection();
                    System.out.println("Disconnecting you from Brokers...");
                    notifyBrokerForDisconnection();
                    System.out.println("\nThank you for joining our app!!!");
                    System.exit(0);

                case "1": {
                    System.out.print("Please select a topic or a user that you want to connect: ");
                    String topic_or_user = input.nextLine();

                    System.out.print("Please specify if the above is a topic or a user (Answer must be topic or user): ");
                    String decision = input.nextLine();

                    if (!Profile.getConnectedUserNodes().contains(topic_or_user) &&
                            !Profile.getConnectedTopics().contains(topic_or_user)) {

                        int hashOfTHeTopicOrUserNode = UserNode.calculateKeys(topic_or_user);
                        SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
                        TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                        for (int hash : brokers.keySet()) {
                            if (hashOfTHeTopicOrUserNode <= hash) {
                                brokerAddress = brokers.get(hash);
                                break;
                            }
                        }
                        if (brokerAddress != null)
                            connectToTopicOrUser(brokerAddress, topic_or_user, decision);
                    } else {
                        if (decision.toUpperCase(Locale.ROOT).equals("USER"))
                            System.out.println("You are already connected with " + topic_or_user + ".");
                        else
                            System.out.println("You are already subscribed in " + topic_or_user + ".");
                    }
                    break;
                }
                case "2": {
                    System.out.print("Please specify if this conversation references to topic or user (Answer must be topic or user): ");
                    String decision = input.nextLine();

                    if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                        if (!Profile.getConnectedUserNodes().isEmpty()) {
                            System.out.println("Your connections:");
                            int counter = 1;
                            for (String conversationUserNode : Profile.getConnectedUserNodes()) {
                                System.out.println(counter++ + ". " + conversationUserNode);
                            }
                            System.out.println("Please give me the number of the conversation that you want: ");
                            int number = input.nextInt();
                            if (number >= 1 && number <= Profile.getConnectedUserNodes().size()) {
                                String selectedUserNode = Profile.getConnectedUserNodes().get(--number);
                                int hashOfTheAppropriateBroker = UserNode.calculateKeys(selectedUserNode);
                                SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();;
                                TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                                for (int hash : brokers.keySet()) {
                                    if (hashOfTheAppropriateBroker <= hash) {
                                        brokerAddress = brokers.get(hash);
                                        break;
                                    }
                                }
                                SocketAddress socketAddressOfTheConversation = null;
                                if (brokerAddress != null)
                                    socketAddressOfTheConversation = getUserNodeConversationSocketAddress(brokerAddress, selectedUserNode, decision);

                                if (socketAddressOfTheConversation != null) {
                                    startConversation(socketAddressOfTheConversation, selectedUserNode, decision);
                                }
                            } else {
                                System.out.println("You choose wrong number.");
                            }
                        } else{
                            System.out.println("Your are not connected with any users.");
                        }
                    } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                        if (!Profile.getConnectedTopics().isEmpty()) {
                            System.out.println("Your connections:");
                            int counter = 1;
                            for (String conversationTopic : Profile.getConnectedTopics()) {
                                System.out.println(counter++ + ". " + conversationTopic);
                            }
                            System.out.println("Please give me the number of the conversation that you want: ");
                            int number = input.nextInt();
                            if (number >= 1 && number <= Profile.getConnectedTopics().size()) {
                                String selectedTopic = Profile.getConnectedTopics().get(--number);
                                int hashOfTheAppropriateBroker = UserNode.calculateKeys(selectedTopic);
                                SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
                                TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                                for (int hash : brokers.keySet()) {
                                    if (hashOfTheAppropriateBroker <= hash) {
                                        brokerAddress = brokers.get(hash);
                                        break;
                                    }
                                }
                                if (brokerAddress != null) {
                                    sendToBrokerTheMessageForPublicConversation(brokerAddress, selectedTopic, decision);
                                    startConversation(brokerAddress, selectedTopic, decision);
                                }
                            } else {
                                System.out.println("You choose wrong number.");
                            }
                        } else {
                            System.out.println("Your are not connected with any topics.");
                        }
                    }
                    break;
                }
                case "3": {
                    StringBuilder stringBuilder = new StringBuilder("Photos that you have shared in private conversations with:\n");
                    for (Map.Entry<String, String> item : Profile.getUserNodeWhoSharedPhotosInPrivateConversation().entrySet()){
                        stringBuilder.append("\t").append(item.getKey()).append(" :").append(item.getValue()).append("\n");
                    }
                    stringBuilder.append("Photos that you have shared in public conversations with:\n");
                    for (Map.Entry<String, String> item : Profile.getUserNodeWhoSharedPhotosInPublicConversation().entrySet()){
                        stringBuilder.append("\t").append(item.getKey()).append(" :").append(item.getValue()).append("\n");
                    }
                    stringBuilder.append("Videos that you have shared in private conversations with:\n");
                    for (Map.Entry<String, String> item : Profile.getUserNodeWhoSharedVideosInPrivateConversation().entrySet()){
                        stringBuilder.append("\t").append(item.getKey()).append(" :").append(item.getValue()).append("\n");
                    }
                    stringBuilder.append("Videos that you have shared in public conversations with:\n");
                    for (Map.Entry<String, String> item : Profile.getUserNodeWhoSharedVideosInPublicConversation().entrySet()){
                        stringBuilder.append("\t").append(item.getKey()).append(" :").append(item.getValue()).append("\n");
                    }
                    System.out.println(stringBuilder);
                    break;
                }
                case "4": {
                    System.out.print("Please specify if this Photo belongs in topic or user (Answer must be topic or user): ");
                    String decision = input.nextLine();

                    if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                        if (!Profile.getUserNodesWhoReceivesPhotosInPrivateConversation().isEmpty()) {
                            System.out.println("Users that shared photos with you and their titles:");
                            int counter = 1;
                            for (Map.Entry<String, ArrayList<String>> item :
                                    Profile.getUserNodesWhoReceivesPhotosInPrivateConversation().entrySet()) {
                                int insideCounter = 1;
                                System.out.println(counter++ + ". " + item.getKey() + ": ");
                                for (String titleOfThePhoto : item.getValue()) {
                                    System.out.println("\t" + insideCounter++ + ". " + titleOfThePhoto);
                                }
                            }
                            System.out.println("Please give me the number of the owner that you want: ");
                            int number = input.nextInt();
                            String[] keys =
                                    Profile.getUserNodesWhoReceivesPhotosInPrivateConversation().keySet().toArray(String[]::new);
                            if (number >= 1 && number <= Profile.getUserNodesWhoReceivesPhotosInPrivateConversation().size()) {
                                int hashOfTheAppropriateBroker = UserNode.calculateKeys(keys[--number]);
                                SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
                                TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                                for (int hash : brokers.keySet()) {
                                    if (hashOfTheAppropriateBroker <= hash) {
                                        brokerAddress = brokers.get(hash);
                                        break;
                                    }
                                }
                                if (brokerAddress != null) {
                                    String owner = keys[number];
                                    System.out.println("Please give me the number of the photo that you want: ");
                                    int insideNumber = input.nextInt();
                                    if (insideNumber >= 1 && insideNumber <=
                                            Profile.getUserNodesWhoReceivesPhotosInPrivateConversation().get(owner).size()) {
                                        String title = Profile.
                                                getUserNodesWhoReceivesPhotosInPrivateConversation().get(owner).get(--insideNumber);
                                        pullPhoto(brokerAddress, owner, title);
                                    }
                                }
                            }
                        } else {
                            System.out.println("None has shared with you any photo.");
                        }
                    } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                        if (!Profile.getUserNodesWhoReceivesPhotosInPublicConversation().isEmpty()) {
                            System.out.println("Users that shared photos with in conversation and their titles:");
                            int counter = 1;
                            for (Map.Entry<String, ArrayList<String>> item :
                                    Profile.getUserNodesWhoReceivesPhotosInPublicConversation().entrySet()) {
                                int insideCounter = 1;
                                System.out.println(counter++ + ". " + item.getKey() + ": ");
                                for (String titleOfThePhoto : item.getValue()) {
                                    System.out.println("\t" + insideCounter++ + ". " + titleOfThePhoto);
                                }
                            }
                            System.out.println("Please give me the number of the owner that you want: ");
                            int number = input.nextInt();
                            String[] keys =
                                    Profile.getUserNodesWhoReceivesPhotosInPublicConversation().keySet().toArray(String[]::new);
                            if (number >= 1 && number <= Profile.getUserNodesWhoReceivesPhotosInPublicConversation().size()) {
                                int hashOfTheAppropriateBroker = UserNode.calculateKeys(keys[--number]);
                                SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
                                TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                                for (int hash : brokers.keySet()) {
                                    if (hashOfTheAppropriateBroker <= hash) {
                                        brokerAddress = brokers.get(hash);
                                        break;
                                    }
                                }
                                if (brokerAddress != null) {
                                    String owner = keys[number];
                                    System.out.println("Please give me the number of the photo that you want: ");
                                    int insideNumber = input.nextInt();
                                    if (insideNumber >= 1 && insideNumber <=
                                            Profile.getUserNodesWhoReceivesPhotosInPublicConversation().get(owner).size()) {
                                        String title = Profile
                                                .getUserNodesWhoReceivesPhotosInPublicConversation().get(owner).get(--insideNumber);
                                        pullPhoto(brokerAddress, owner, title);
                                    }
                                }
                            }
                        } else {
                            System.out.println("None in a public conversation has shared a photo with you.");
                        }
                    }
                    break;
                }
                case "5": {
                    System.out.print("Please specify if this Video belongs in topic or user (Answer must be topic or user): ");
                    String decision = input.nextLine();

                    if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                        if (!Profile.getUserNodesWhoReceivesVideosInPrivateConversation().isEmpty()) {
                            System.out.println("Users that shared videos with you and their titles:");
                            int counter = 1;
                            for (Map.Entry<String, ArrayList<String>> item :
                                    Profile.getUserNodesWhoReceivesVideosInPrivateConversation().entrySet()) {
                                int insideCounter = 1;
                                System.out.println(counter++ + ". " + item.getKey() + ": ");
                                for (String titleOfThePhoto : item.getValue()) {
                                    System.out.println("\t" + insideCounter++ + ". " + titleOfThePhoto);
                                }
                            }
                            System.out.println("Please give me the number of the owner that you want: ");
                            int number = input.nextInt();
                            String[] keys =
                                    Profile.getUserNodesWhoReceivesVideosInPrivateConversation().keySet().toArray(String[]::new);
                            if (number >= 1 && number <= Profile.getUserNodesWhoReceivesVideosInPrivateConversation().size()) {
                                int hashOfTheAppropriateBroker = UserNode.calculateKeys(keys[--number]);
                                SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
                                TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                                for (int hash : brokers.keySet()) {
                                    if (hashOfTheAppropriateBroker <= hash) {
                                        brokerAddress = brokers.get(hash);
                                        break;
                                    }
                                }
                                if (brokerAddress != null) {
                                    String owner = keys[number];
                                    System.out.println("Please give me the number of the video that you want: ");
                                    int insideNumber = input.nextInt();
                                    if (insideNumber >= 1 && insideNumber <=
                                            Profile.getUserNodesWhoReceivesVideosInPrivateConversation().get(owner).size()) {
                                        String title = Profile
                                                .getUserNodesWhoReceivesVideosInPrivateConversation().get(owner).get(--insideNumber);
                                        pullVideo(brokerAddress, owner, title);
                                    }
                                }
                            }
                        } else {
                            System.out.println("None has shared a video with you.");
                        }
                    } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                        if (!Profile.getUserNodesWhoReceivesVideosInPublicConversation().isEmpty()) {
                            System.out.println("Users that shared videos with in conversation and their titles:");
                            int counter = 1;
                            for (Map.Entry<String, ArrayList<String>> item :
                                    Profile.getUserNodesWhoReceivesVideosInPublicConversation().entrySet()) {
                                int insideCounter = 1;
                                System.out.println(counter++ + ". " + item.getKey() + ": ");
                                for (String titleOfThePhoto : item.getValue()) {
                                    System.out.println("\t" + insideCounter++ + ". " + titleOfThePhoto);
                                }
                            }
                            System.out.println("Please give me the number of the owner that you want: ");
                            int number = input.nextInt();
                            String[] keys = Profile.getUserNodesWhoReceivesVideosInPublicConversation().keySet().toArray(String[]::new);
                            if (number >= 1 && number <= Profile.getUserNodesWhoReceivesVideosInPublicConversation().size()) {
                                int hashOfTheAppropriateBroker = UserNode.calculateKeys(keys[--number]);
                                SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
                                TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
                                for (int hash : brokers.keySet()) {
                                    if (hashOfTheAppropriateBroker <= hash) {
                                        brokerAddress = brokers.get(hash);
                                        break;
                                    }
                                }
                                if (brokerAddress != null) {
                                    String owner = keys[number];
                                    System.out.println("Please give me the number of the video that you want: ");
                                    int insideNumber = input.nextInt();
                                    if (insideNumber >= 1 && insideNumber <=
                                            Profile.getUserNodesWhoReceivesVideosInPublicConversation().get(owner).size()) {
                                        String title = Profile
                                                .getUserNodesWhoReceivesVideosInPublicConversation().get(owner).get(--insideNumber);
                                        pullVideo(brokerAddress, owner, title);
                                    }
                                }
                            }
                        } else {
                            System.out.println("None in a public conversation has shared a video with you.");
                        }
                    }
                    break;
                }
            }
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

    public void connectToTopicOrUser(SocketAddress socketAddress, String topic_or_user, String decision){
        connect(socketAddress);

        try {
            objectOutputStream.writeObject("Connect to Topic or User");
            objectOutputStream.flush();

            objectOutputStream.writeObject(Profile.getName());
            objectOutputStream.flush();

            objectOutputStream.writeObject(topic_or_user);
            objectOutputStream.flush();

            objectOutputStream.writeObject(decision);
            objectOutputStream.flush();

            objectOutputStream.writeObject(Profile.getServerSocket().getInetAddress().toString().substring(1));
            objectOutputStream.flush();

            objectOutputStream.writeObject(Profile.getPort());
            objectOutputStream.flush();

            String response = (String) objectInputStream.readObject();
            System.out.println(response);

            if (response.contains("successfully")){
                if (decision.toUpperCase(Locale.ROOT).equals("USER")){
                    ArrayList<String> connectedUserNodes = Profile.getConnectedUserNodes();
                    connectedUserNodes.add(topic_or_user);
                    Profile.setConnectedUserNodes(connectedUserNodes);
                } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")){
                    ArrayList<String> connectedTopics = Profile.getConnectedTopics();
                    connectedTopics.add(topic_or_user);
                    Profile.setConnectedTopics(connectedTopics);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public SocketAddress getUserNodeConversationSocketAddress(SocketAddress socketAddress, String topic_or_user, String decision){
        connect(socketAddress);

        SocketAddress socketAddressOfTheConversation;
        try {
            objectOutputStream.writeObject("Start Conversation");
            objectOutputStream.flush();

            objectOutputStream.writeObject(topic_or_user);
            objectOutputStream.flush();

            objectOutputStream.writeObject(decision);
            objectOutputStream.flush();

            String stringIPAndPort = (String) objectInputStream.readObject();
            String ip = stringIPAndPort.substring(1).split(":")[0];
            int port = Integer.parseInt(stringIPAndPort.split(":")[1]);
            socketAddressOfTheConversation = new InetSocketAddress(ip, port);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            disconnect();
        }
        return socketAddressOfTheConversation;
    }

    public void sendToBrokerTheMessageForPublicConversation(SocketAddress brokerAddress, String topic_or_user, String decision){
        connect(brokerAddress);

        try {
            objectOutputStream.writeObject("Start Conversation");
            objectOutputStream.flush();

            objectOutputStream.writeObject(topic_or_user);
            objectOutputStream.flush();

            objectOutputStream.writeObject(decision);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public void startConversation(SocketAddress socketAddress, String topic_or_user, String decision){
        try {

            Scanner inputConversation = new Scanner(System.in);
            String subChoice;
            do{
                connect(socketAddress);
                objectOutputStream.writeObject("Conversation");
                objectOutputStream.flush();

                System.out.println("SubMenu for conversations");
                System.out.println("1. Send Message");
                System.out.println("2. Send Photo");
                System.out.println("3. Send Video");
                System.out.println("0. Exit");

                subChoice = inputConversation.nextLine();

                if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                    objectOutputStream.writeObject(Profile.getName());
                    objectOutputStream.flush();
                } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                    objectOutputStream.writeObject(Profile.getName());
                    objectOutputStream.flush();
                }

                switch (subChoice) {
                    case "1":

                        System.out.println("Enter your message: ");
                        String message = inputConversation.nextLine();

                        objectOutputStream.writeObject("Message");
                        objectOutputStream.flush();

                        if (decision.toUpperCase(Locale.ROOT).equals("USER"))
                            message = " leave a private message to you: " + message;
                        else
                            message = " leave a message in " + topic_or_user + " public conversation: " + message;
                        objectOutputStream.writeObject(message);
                        objectOutputStream.flush();

                        if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                            objectOutputStream.writeObject(topic_or_user);
                            objectOutputStream.flush();

                            objectOutputStream.writeObject(Profile.getServerSocket().getInetAddress().toString().substring(1));
                            objectOutputStream.flush();

                            objectOutputStream.writeObject(Profile.getPort());
                            objectOutputStream.flush();
                        }
                        disconnect();
                        break;
                    case "2":
                        System.out.println("Please enter the absolute path of the photo that you want to upload: ");
                        String absolutePath = inputConversation.nextLine();

                        File photo = new File(absolutePath);
                        if (!photo.exists()) {
                            System.out.println("This path is not valid!");
                        } else {
                            objectOutputStream.writeObject("Photo");
                            objectOutputStream.flush();

                            int lastBackslashIndex = absolutePath.lastIndexOf('\\');
                            String title = absolutePath.substring(lastBackslashIndex + 1).split("\\.")[0];
                            objectOutputStream.writeObject(title);
                            objectOutputStream.flush();

                            if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                                objectOutputStream.writeObject(Profile.getName() + " shared a photo with you in private conversation.");
                                objectOutputStream.flush();
                            } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                                objectOutputStream.writeObject(Profile.getName() + " shared a photo with you in " + topic_or_user + " public conversation.");
                                objectOutputStream.flush();

                                objectOutputStream.writeObject(topic_or_user);
                                objectOutputStream.flush();

                                objectOutputStream.writeObject(Profile.getServerSocket().getInetAddress().toString().substring(1));
                                objectOutputStream.flush();

                                objectOutputStream.writeObject(Profile.getPort());
                                objectOutputStream.flush();
                            }
                            HashMap<String, String> temp;
                            if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                                temp = Profile.getUserNodeWhoSharedPhotosInPrivateConversation();
                                temp.put(topic_or_user, title);
                                Profile.setUserNodeWhoSharedPhotosInPrivateConversation(temp);
                            }
                            else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                                temp = Profile.getUserNodeWhoSharedPhotosInPublicConversation();
                                temp.put(topic_or_user, title);
                                Profile.setUserNodeWhoSharedPhotosInPublicConversation(temp);
                            }

                            HashMap<String, String> tempFilePathMapToTitles = Profile.getFilePathsOfThePhotos();
                            tempFilePathMapToTitles.put(title, absolutePath);
                            Profile.setFilePathsOfThePhotos(tempFilePathMapToTitles);
                        }
                        disconnect();
                        break;
                    case "3":
                        System.out.println("Please enter the absolute path of the video that you want to upload: ");
                        String absolutePathOfVideo = inputConversation.nextLine();

                        File video = new File(absolutePathOfVideo);
                        if (!video.exists()) {
                            System.out.println("This path is not valid!");
                        } else {
                            objectOutputStream.writeObject("Video");
                            objectOutputStream.flush();

                            int lastBackslashIndex = absolutePathOfVideo.lastIndexOf('\\');
                            String title = absolutePathOfVideo.substring(lastBackslashIndex + 1).split("\\.")[0];
                            objectOutputStream.writeObject(title);
                            objectOutputStream.flush();

                            if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                                objectOutputStream.writeObject(Profile.getName() + " shared a video with you in private conversation.");
                                objectOutputStream.flush();
                            } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                                objectOutputStream.writeObject(Profile.getName() + " shared a video with you in " + topic_or_user + " public conversation.");
                                objectOutputStream.flush();

                                objectOutputStream.writeObject(topic_or_user);
                                objectOutputStream.flush();

                                objectOutputStream.writeObject(Profile.getServerSocket().getInetAddress().toString().substring(1));
                                objectOutputStream.flush();

                                objectOutputStream.writeObject(Profile.getPort());
                                objectOutputStream.flush();
                            }
                            HashMap<String, String> temp;
                            if (decision.toUpperCase(Locale.ROOT).equals("USER")) {
                                temp = Profile.getUserNodeWhoSharedVideosInPrivateConversation();
                                temp.put(topic_or_user, title);
                                Profile.setUserNodeWhoSharedVideosInPrivateConversation(temp);
                            } else if (decision.toUpperCase(Locale.ROOT).equals("TOPIC")) {
                                temp = Profile.getUserNodeWhoSharedVideosInPublicConversation();
                                temp.put(topic_or_user, title);
                                Profile.setUserNodeWhoSharedVideosInPublicConversation(temp);
                            }

                            HashMap<String, String> tempFilePathMapToTitles = Profile.getFilePathsOfTheVideos();
                            tempFilePathMapToTitles.put(title, absolutePathOfVideo);
                            Profile.setFilePathsOfTheVideos(tempFilePathMapToTitles);
                        }
                        disconnect();
                        break;
                }
            }while (!subChoice.equals("0"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notifyUserNodesForDisconnection(){
        for (String userNode : Profile.getConnectedUserNodes()){
            int hashOfTheAppropriateBroker = UserNode.calculateKeys(userNode);
            SocketAddress brokerAddress = Profile.getBrokers().firstEntry().getValue();
            TreeMap<Integer, SocketAddress> brokers = Profile.getBrokers();
            for (int hash : brokers.keySet()) {
                if (hashOfTheAppropriateBroker <= hash) {
                    brokerAddress = brokers.get(hash);
                    break;
                }
            }
            if (brokerAddress != null) {
                connect(brokerAddress);
                SocketAddress disconnectFromUserNode = null;
                try {
                    objectOutputStream.writeObject("User Node wants to disconnect From User Node");
                    objectOutputStream.flush();

                    objectOutputStream.writeObject(userNode);
                    objectOutputStream.flush();

                    String ip = (String) objectInputStream.readObject();
                    int port = Integer.parseInt((String) objectInputStream.readObject());

                    disconnectFromUserNode = new InetSocketAddress(ip, port);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }

                if (disconnectFromUserNode != null) {
                    connect(disconnectFromUserNode);

                    try {
                        objectOutputStream.writeObject("User Node wants to disconnect");
                        objectOutputStream.flush();

                        objectOutputStream.writeObject(Profile.getName());
                        objectOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        disconnect();
                    }
                }
            }
        }
    }

    public void notifyBrokerForDisconnection(){
        for (Map.Entry<Integer, SocketAddress> item : Profile.getBrokers().entrySet()) {
            connect(item.getValue());

            try {
                objectOutputStream.writeObject("User Node wants to disconnect From Broker");
                objectOutputStream.flush();

                objectOutputStream.writeObject(Profile.getName());
                objectOutputStream.flush();

                objectOutputStream.writeObject(Profile.getServerSocket().getInetAddress().toString().substring(1));
                objectOutputStream.flush();

                objectOutputStream.writeObject(Profile.getPort());
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }
    }

    public void pullPhoto(SocketAddress brokerAddress, String owner, String title){
        connect(brokerAddress);

        try {
            objectOutputStream.writeObject("Pull Photo");
            objectOutputStream.flush();

            objectOutputStream.writeObject(owner);
            objectOutputStream.flush();

            objectOutputStream.writeObject(title);
            objectOutputStream.flush();

            int size = (int) objectInputStream.readObject();
            byte [] photo = new byte[size];
            objectInputStream.readFully(photo);

            ByteArrayInputStream bais = new ByteArrayInputStream(photo);
            BufferedImage bufferedImage = ImageIO.read(bais);
            ImageIO.write(bufferedImage, "PNG",
                    new FileOutputStream("PulledPhotos\\"
                            + owner + "_To_" + Profile.getName() + "_" + title + ".PNG"));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public void pullVideo(SocketAddress brokerAddress, String owner, String title){
        connect(brokerAddress);

        try {
            objectOutputStream.writeObject("Pull Video");
            objectOutputStream.flush();

            objectOutputStream.writeObject(owner);
            objectOutputStream.flush();

            objectOutputStream.writeObject(title);
            objectOutputStream.flush();

            int size = (int) objectInputStream.readObject();
            byte [] chunk;
            ArrayList<byte []> videoChunks = new ArrayList<>();
            for (int counterOfChunks = 0;counterOfChunks < size;counterOfChunks++){
                chunk = new byte[2048];
                objectInputStream.readFully(chunk);
                videoChunks.add(chunk);
            }
            try {
                File videoFile = new File("PulledVideos\\"
                        + owner + "_To_" + Profile.getName() + "_" + title + ".mp4");
                for (byte [] chunkOfTheVideo : videoChunks){
                    try (FileOutputStream fos = new FileOutputStream(videoFile, true)) {
                        fos.write(chunkOfTheVideo);
                    }
                }
            } catch (IOException ioException){
                ioException.printStackTrace();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }
}
