/** 
 * Instituto Superior Técnico
 * Redes de Computadores
 * Projecto 1
 *
 * António Tavares - 78122
 * Luís Borges - 78349
 * Paulo Ritto - 78929 
 */


import java.io.*;
import java.net.*;
import java.util.*;

public class ECP{
	
	private static int port = 58006;
	private static int nTopics;
	private static BufferedReader inFromUser;
	private static byte[] receiveData;
    private static byte[] sendData;
    private static InetAddress IPAddress;
    private static int clientPort;
    private static DatagramPacket receivePacket;
    private static DatagramPacket sendPacket;
    private static DatagramSocket serverSocket;
    private static int TESport;
    private static InetAddress TESIP;
    private static String writeToStats = "";
    

    /**
     * Gets the topic name and the number of available topics.
     * Informs the TES server by giving it the number of available topics and the chosen topic.
     */
    public static void informTES(String topicName, int nTopic) throws Exception{
		Socket TCPsocket = new Socket(TESIP.getHostName(), TESport);
		DataOutputStream outToServer = new DataOutputStream(TCPsocket.getOutputStream());
		outToServer.writeBytes("INF " + topicName + " " + nTopic);
		TCPsocket.close();
	}
    
    /**
     * Reads from a .txt file how many topics are available.
     * @return a String with the topic list and the number of available topics.
     */
    public static String readTopics() throws Exception{
		 String thisLine = null;
		 String topics = "";
		 nTopics = 0;
		 BufferedReader topicsFile = new BufferedReader(new FileReader("topics.txt"));
		 while ((thisLine = topicsFile.readLine()) != null){
		    topics = topics + thisLine.split(" ")[0] + " ";
		    nTopics = nTopics + 1;
         	}
         return nTopics + " " + topics.substring(0,topics.length() -1);    
	}
	
	 /**
     * Gets the topic number as a string.
     * @return the TES's server information.
     */
	public static String readTESinfo(String Tn) throws Exception{
		int n = Integer.parseInt(Tn.trim());
		String thisLine = null;
		String TESinfo = "";
		BufferedReader topicsFile = new BufferedReader(new FileReader("topics.txt"));
		for(int i = 0; i < n; i++){
			thisLine = topicsFile.readLine();
		}
		TESIP = InetAddress.getByName(thisLine.split(" ")[1]);
		TESport = Integer.parseInt(thisLine.split(" ")[2]);
		informTES(thisLine.split(" ")[0], n);
        return TESinfo + thisLine.split(" ")[1] + " " + thisLine.split(" ")[2];
	}
    
    /**
	 * Gets the user's/TES input.
	 * Processes its input according to the given rules.
	 */ 
    public static void processInput(String input) throws Exception{
		String[] splitInput = input.split(" ");
		if(splitInput[0].trim().equals("TQR")){
			try{
				String fail = splitInput[1];
				sendData = "ERR\n".getBytes();
			} 
			catch(Exception e){
				System.out.println("List request: " + IPAddress.getHostName() + " " + clientPort);
				sendData = ("AWT " + readTopics()+"\n").getBytes();
				if(nTopics == 0){
					sendData = "EOF\n".getBytes();
				}
			}
            sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientPort);
            serverSocket.send(sendPacket);
		}
		if(splitInput[0].trim().equals("TER")){ 
			System.out.println("Topic request: " + IPAddress.getHostName() + " " + clientPort);
			try{
				if(Integer.parseInt(splitInput[1].trim()) > nTopics){
					sendData = "EOF\n".getBytes();
				}
				else{
					String TESinfo = readTESinfo(splitInput[1]);
					System.out.println("Desired TES: " + TESIP.getHostName() + " " + TESport);
					sendData = ("AWTES " + TESinfo + "\n").getBytes();
				}
			}
			catch(NumberFormatException e){
				sendData = "ERR\n".getBytes();
			}
            sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientPort);
            serverSocket.send(sendPacket);
		}
		if(splitInput[0].trim().equals("IQR")){
			if(splitInput.length != 5){
				byte[] toSend = "ERR\n".getBytes();
				sendPacket = new DatagramPacket(toSend, toSend.length, TESIP, TESport);
				serverSocket.send(sendPacket);
			}
			try{
				int intSID = Integer.parseInt(splitInput[1]);
			}
			catch(Exception e){
				byte[] toSend = "ERR\n".getBytes();
				sendPacket = new DatagramPacket(toSend, toSend.length, TESIP, TESport);
				serverSocket.send(sendPacket);
			}
			if(splitInput.length == 5){
				String SID = splitInput[1];
				String QID = splitInput[2];
				String topic_name = splitInput[3];
				String score = splitInput[4];
				System.out.println("Received student score: " + SID + ", " + topic_name + ", " + score + "%");
				File stats = new File("stats.txt");
				BufferedWriter writer = new BufferedWriter(new FileWriter(stats));
				writeToStats = writeToStats + "-> Student " + SID + " answered questionnaire " + QID + " on " + topic_name + ", and obtained " + score.trim() + "%" + "\n";
				writer.write(writeToStats);
				writer.flush();
				writer.close();
				byte[] toSend = ("AWI " + QID + "\n").getBytes();
				sendPacket = new DatagramPacket(toSend, toSend.length, TESIP, TESport);
				serverSocket.send(sendPacket);
			}
		}
	}
	
   public static void main(String args[]) throws Exception {
	   try{
		   port = Integer.parseInt(args[1]);
	   }
	   catch(ArrayIndexOutOfBoundsException exception){}

		 serverSocket = new DatagramSocket(port);
         while(true){
		try{
			receiveData = new byte[1024];
			sendData = new byte[1024];
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            String input = new String(receivePacket.getData());
            IPAddress = receivePacket.getAddress();
            clientPort = receivePacket.getPort();
            processInput(input);
		   }
		catch(Exception e){
			System.out.println("ERROR: Program will terminate");
			System.exit(0);
		}
         } 
	}
} 
