import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Profile{

	private static ServerSocket serverSocket;
	private static int port;
	private static String name;

	private static TreeMap<Integer, SocketAddress> brokersMap;

	private static ArrayList<String> connectedUserNodes;
    private static ArrayList<String> connectedTopics;

    //Necessary HashMaps for Photos
	//receiver of the notification of the photo and title in private conversation
	private static HashMap<String, String> userNodeWhoSharedPhotosInPrivateConversation;
	//receiver of the notification of the photo and title in public conversation
	private static HashMap<String, String> userNodeWhoSharedPhotosInPublicConversation;
	//owner of the photo and title in private conversation
	private static HashMap<String, ArrayList<String>> userNodesWhoReceivesPhotosInPrivateConversation;
	//owner of the photo and title in public conversation
	private static HashMap<String, ArrayList<String>> userNodesWhoReceivesPhotosInPublicConversation;
	//title of the photo and filepath
	private static HashMap<String, String> filePathsOfThePhotos;

	//Necessary HashMaps for Videos
	//receiver of the notification of the video and title in private conversation
	private static HashMap<String, String> userNodeWhoSharedVideosInPrivateConversation;
	//receiver of the notification of the video and title in public conversation
	private static HashMap<String, String> userNodeWhoSharedVideosInPublicConversation;
	//owner of the video and title in private conversation
	private static HashMap<String, ArrayList<String>> userNodesWhoReceivesVideosInPrivateConversation;
	//owner of the video and title in public conversation
	private static HashMap<String, ArrayList<String>> userNodesWhoReceivesVideosInPublicConversation;
	//title of the video and filepath
	private static HashMap<String, String> filePathsOfTheVideos;

	public Profile(){
		filePathsOfTheVideos = new HashMap<>();
		userNodeWhoSharedVideosInPrivateConversation = new HashMap<>();
		userNodeWhoSharedVideosInPublicConversation = new HashMap<>();
		userNodesWhoReceivesVideosInPrivateConversation = new HashMap<>();
		userNodesWhoReceivesVideosInPublicConversation = new HashMap<>();
		filePathsOfThePhotos = new HashMap<>();
		userNodeWhoSharedPhotosInPrivateConversation = new HashMap<>();
		userNodeWhoSharedPhotosInPublicConversation = new HashMap<>();
		userNodesWhoReceivesPhotosInPrivateConversation = new HashMap<>();
		userNodesWhoReceivesPhotosInPublicConversation = new HashMap<>();
		connectedUserNodes = new ArrayList<>();
        connectedTopics = new ArrayList<>();
		serverSocket = null;
		port = -1;
		name = "";
		brokersMap = new TreeMap<>();
	}

	public static TreeMap<Integer, SocketAddress> getBrokers(){
		return brokersMap;
	}

	public static void setBrokers(TreeMap<Integer, SocketAddress> updatedBrokers){
		brokersMap = updatedBrokers;
	}

	public static ServerSocket getServerSocket(){
		return serverSocket;
	}

	public static void setServerSocket(ServerSocket updatedServerSocket){
		serverSocket = updatedServerSocket;
	}

	public static int getPort(){
		return port;
	}

	public static void setPort(int updatedPort){
		port = updatedPort;
	}

	public static String getName(){
		return name;
	}

	public static void setName(String updatedName){
		name = updatedName;
	}

	public static ArrayList<String> getConnectedUserNodes(){
		return connectedUserNodes;
	}

	public static void setConnectedUserNodes(ArrayList<String> updatedConnectedUserNodes){
		connectedUserNodes = updatedConnectedUserNodes;
	}

	public static ArrayList<String> getConnectedTopics(){
		return connectedTopics;
	}

	public static void setConnectedTopics(ArrayList<String> updatedConnectedTopics){
		connectedTopics = updatedConnectedTopics;
	}

	public static HashMap<String, String> getUserNodeWhoSharedPhotosInPrivateConversation() {
		return userNodeWhoSharedPhotosInPrivateConversation;
	}

	public static void setUserNodeWhoSharedPhotosInPrivateConversation(HashMap<String, String> updatedUserNodeWhoSharedPhotosInPrivateConversation) {
		userNodeWhoSharedPhotosInPrivateConversation = updatedUserNodeWhoSharedPhotosInPrivateConversation;
	}

	public static HashMap<String, String> getUserNodeWhoSharedPhotosInPublicConversation() {
		return userNodeWhoSharedPhotosInPublicConversation;
	}

	public static void setUserNodeWhoSharedPhotosInPublicConversation(HashMap<String, String> updatedUserNodeWhoSharedPhotosInPublicConversation) {
		userNodeWhoSharedPhotosInPublicConversation = updatedUserNodeWhoSharedPhotosInPublicConversation;
	}

	public static HashMap<String, ArrayList<String>> getUserNodesWhoReceivesPhotosInPrivateConversation() {
		return userNodesWhoReceivesPhotosInPrivateConversation;
	}

	public static void setUserNodesWhoReceivesPhotosInPrivateConversation(HashMap<String, ArrayList<String>> updatedUserNodesWhoReceivesPhotosInPrivateConversation) {
		userNodesWhoReceivesPhotosInPrivateConversation = updatedUserNodesWhoReceivesPhotosInPrivateConversation;
	}

	public static HashMap<String, ArrayList<String>> getUserNodesWhoReceivesPhotosInPublicConversation() {
		return userNodesWhoReceivesPhotosInPublicConversation;
	}

	public static void setUserNodesWhoReceivesPhotosInPublicConversation(HashMap<String, ArrayList<String>> updatedUserNodesWhoReceivesPhotosInPublicConversation) {
		userNodesWhoReceivesPhotosInPublicConversation = updatedUserNodesWhoReceivesPhotosInPublicConversation;
	}

	public static HashMap<String, String> getFilePathsOfThePhotos() {
		return filePathsOfThePhotos;
	}

	public static void setFilePathsOfThePhotos(HashMap<String, String> updatedFilePathsOfThePhotos) {
		filePathsOfThePhotos = updatedFilePathsOfThePhotos;
	}

	public static HashMap<String, String> getUserNodeWhoSharedVideosInPrivateConversation() {
		return userNodeWhoSharedVideosInPrivateConversation;
	}

	public static void setUserNodeWhoSharedVideosInPrivateConversation(HashMap<String, String> updatedUserNodeWhoSharedVideosInPrivateConversation) {
		userNodeWhoSharedVideosInPrivateConversation = updatedUserNodeWhoSharedVideosInPrivateConversation;
	}

	public static HashMap<String, String> getUserNodeWhoSharedVideosInPublicConversation() {
		return userNodeWhoSharedVideosInPublicConversation;
	}

	public static void setUserNodeWhoSharedVideosInPublicConversation(HashMap<String, String> updatedUserNodeWhoSharedVideosInPublicConversation) {
		userNodeWhoSharedVideosInPublicConversation = updatedUserNodeWhoSharedVideosInPublicConversation;
	}

	public static HashMap<String, ArrayList<String>> getUserNodesWhoReceivesVideosInPrivateConversation() {
		return userNodesWhoReceivesVideosInPrivateConversation;
	}

	public static void setUserNodesWhoReceivesVideosInPrivateConversation(HashMap<String, ArrayList<String>> updatedUserNodesWhoReceivesVideosInPrivateConversation) {
		userNodesWhoReceivesVideosInPrivateConversation = updatedUserNodesWhoReceivesVideosInPrivateConversation;
	}

	public static HashMap<String, ArrayList<String>> getUserNodesWhoReceivesVideosInPublicConversation() {
		return userNodesWhoReceivesVideosInPublicConversation;
	}

	public static void setUserNodesWhoReceivesVideosInPublicConversation(HashMap<String, ArrayList<String>> updatedUserNodesWhoReceivesVideosInPublicConversation) {
		userNodesWhoReceivesVideosInPublicConversation = updatedUserNodesWhoReceivesVideosInPublicConversation;
	}

	public static HashMap<String, String> getFilePathsOfTheVideos() {
		return filePathsOfTheVideos;
	}

	public static void setFilePathsOfTheVideos(HashMap<String, String> updatedFilePathsOfTheVideos) {
		filePathsOfTheVideos = updatedFilePathsOfTheVideos;
	}
}
