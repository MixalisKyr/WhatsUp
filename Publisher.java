import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Publisher {

    public Publisher(ServerSocket serverSocket){
        new HandleRequests(serverSocket).start();
    }
    static class HandleRequests extends Thread{

        ServerSocket serverSocket;
        Socket connectionSocket;

        HandleRequests(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run(){
            try {

                while(true){
                    connectionSocket = serverSocket.accept();
                    new ServiceIncomingRequests(connectionSocket).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ServiceIncomingRequests extends Thread{

        Socket socket;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        ServiceIncomingRequests(Socket s) {
            socket = s;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.flush();
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                String choice = (String) objectInputStream.readObject();

                switch (choice) {
                    case "New Broker":
                        Profile.getBrokers().clear();
                        TreeMap<Integer, SocketAddress> updatedBrokers = (TreeMap<Integer, SocketAddress>) objectInputStream.readObject();
                        Profile.setBrokers(updatedBrokers);

                        System.out.println("Brokers currently running: " + Profile.getBrokers());
                        break;
                    case "Conversation":
                        String senderUserNode = (String) objectInputStream.readObject();
                        String message_or_photo_or_video = (String) objectInputStream.readObject();

                        switch (message_or_photo_or_video) {
                            case "Message":
                                String message = (String) objectInputStream.readObject();
                                System.out.println(senderUserNode + message);
                                break;
                            case "Photo": {
                                String title = (String) objectInputStream.readObject();
                                String update = (String) objectInputStream.readObject();

                                HashMap<String, ArrayList<String>> temp;
                                if (update.contains("private")) {
                                    temp = Profile
                                            .getUserNodesWhoReceivesPhotosInPrivateConversation();
                                } else {
                                    temp = Profile
                                            .getUserNodesWhoReceivesPhotosInPublicConversation();
                                }
                                ArrayList<String> titlesOfThePhotos = temp.get(senderUserNode);
                                if (titlesOfThePhotos == null)
                                    titlesOfThePhotos = new ArrayList<>();
                                titlesOfThePhotos.add(title);
                                temp.put(senderUserNode, titlesOfThePhotos);
                                if (update.contains("private")) {
                                    Profile.setUserNodesWhoReceivesPhotosInPrivateConversation(temp);
                                } else {
                                    Profile.setUserNodesWhoReceivesPhotosInPublicConversation(temp);
                                }

                                System.out.println(update);
                                break;
                            }
                            case "Video": {
                                String title = (String) objectInputStream.readObject();
                                String update = (String) objectInputStream.readObject();

                                HashMap<String, ArrayList<String>> temp;
                                if (update.contains("private")) {
                                    temp = Profile.getUserNodesWhoReceivesVideosInPrivateConversation();
                                } else {
                                    temp = Profile.getUserNodesWhoReceivesVideosInPublicConversation();
                                }
                                ArrayList<String> titlesOfTheVideos = temp.get(senderUserNode);
                                if (titlesOfTheVideos == null)
                                    titlesOfTheVideos = new ArrayList<>();
                                titlesOfTheVideos.add(title);
                                temp.put(senderUserNode, titlesOfTheVideos);
                                if (update.contains("private")){
                                    Profile.setUserNodesWhoReceivesVideosInPrivateConversation(temp);
                                } else {
                                    Profile.setUserNodesWhoReceivesVideosInPublicConversation(temp);
                                }

                                System.out.println(update);
                                break;
                            }
                        }
                        break;
                    case "User Node wants to connect": {
                        ArrayList<String> connectedUserNodes = Profile.getConnectedUserNodes();
                        String name = (String) objectInputStream.readObject();
                        connectedUserNodes.add(name);
                        Profile.setConnectedUserNodes(connectedUserNodes);
                        break;
                    }
                    case "User Node wants to disconnect": {
                        ArrayList<String> connectedUserNodes = Profile.getConnectedUserNodes();
                        String name = (String) objectInputStream.readObject();
                        connectedUserNodes.remove(name);
                        Profile.setConnectedUserNodes(connectedUserNodes);
                        break;
                    }
                    case "Pull Photo":
                        String title = (String) objectInputStream.readObject();
                        File photo = new File(Profile.getFilePathsOfThePhotos().get(title));

                        byte[] photoData = Files.readAllBytes(photo.toPath());

                        objectOutputStream.writeObject(String.valueOf(photoData.length));
                        objectOutputStream.flush();

                        objectOutputStream.write(photoData);
                        objectOutputStream.flush();
                        break;
                    case "Pull Video":
                        String titleOfTheVideo = (String) objectInputStream.readObject();
                        File video = new File(Profile.getFilePathsOfTheVideos().get(titleOfTheVideo));

                        byte[] videoData = Files.readAllBytes(video.toPath());

                        //Generating Chunks
                        ArrayList<byte []> chunks = new ArrayList<>();
                        int counterOfAllBytes = 0;
                        while (counterOfAllBytes < videoData.length){
                            byte [] bufferTransfer = new byte[2048];//2KB
                            for (int counterOfBufferTransfer = 0; counterOfBufferTransfer < bufferTransfer.length;counterOfBufferTransfer++){
                                if (counterOfAllBytes < videoData.length)
                                    bufferTransfer[counterOfBufferTransfer] = videoData[counterOfAllBytes++];
                            }
                            chunks.add(bufferTransfer);
                        }
                        //End of Generating Chunks

                        objectOutputStream.writeObject(String.valueOf(chunks.size()));
                        objectOutputStream.flush();

                        while (!chunks.isEmpty()){
                            objectOutputStream.write(chunks.remove(0));
                            objectOutputStream.flush();
                        }
                        break;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
