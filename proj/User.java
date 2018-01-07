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

public class User{
	
	private static String ECPname = "localhost";
	private static int ECPport = 58006;
	private static int SID;
	private static int topicNumber;
	private static byte[] sendData;
	private static byte[] receiveData;
	private static DatagramSocket clientSocket;
	private static InetAddress IPAddress;
	private static InetAddress TESIP;
	private static int TESport;
	private static BufferedReader inFromUser;
	private static Socket TCPsocket;
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static String QID;
	private static String dueTime;
	
	/**
	 * Creates a UDP connection.
	 */ 
	public static void createUDP() throws Exception{
		 clientSocket = new DatagramSocket();
		 IPAddress = InetAddress.getByName(ECPname);
		 sendData = new byte[1024];
		 receiveData = new byte[1024]; 
	}
	
	/**
	 * Creates a TCP connection.
	 */ 
	public static void createTCP() throws Exception{
		TCPsocket = new Socket(TESIP.getHostName(), TESport);
		outToServer = new DataOutputStream(TCPsocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(TCPsocket.getInputStream()));
	}
	
	/**
	 * Gets the user's input.
	 * Processes its input according to the given rules.
	 */ 
	public static void processInput(String input) throws Exception{
		String[] splitInput = input.split(" ");
		if(splitInput[0].compareTo("list") == 0){
			createUDP();
			sendData = "TQR\n".getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ECPport);
			clientSocket.send(sendPacket);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			String receivedSentence = new String(receivePacket.getData());
			processAnswer(receivedSentence);
			clientSocket.close();
		}
		if(splitInput[0].compareTo("submit") == 0){
			createTCP();
			String answers = input.split(" ", 2)[1];
			outToServer.writeBytes("RQS " + SID + " " + QID + " " + answers + '\n');
			String receivedSentence = inFromServer.readLine();
			processAnswer(receivedSentence);
			TCPsocket.close();
		}
		if(splitInput[0].compareTo("request") == 0){
			createUDP();
			sendData = ("TER " + splitInput[1] + "\n").getBytes();
			topicNumber = Integer.parseInt(splitInput[1]);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ECPport);
			clientSocket.send(sendPacket);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			String receivedSentence = new String(receivePacket.getData());
			processAnswer(receivedSentence);
			clientSocket.close();
		}
		if(splitInput[0].compareTo("exit") == 0){
			System.exit(0);
		}
	}
	
	/**
	 * Gets the user's answers.
	 * Processes its answers, by compairing them to the available answer file.
	 */ 
	public static void processAnswer(String answer) throws Exception{
		String[] splitAnswer = answer.split(" ");
		if(splitAnswer[0].trim().equals("AWT")){
			for(int i = 0; i < Integer.parseInt(splitAnswer[1]); i++){
				System.out.println((i+1) + " - " + splitAnswer[i+2]);
			}
		}
		if(splitAnswer[0].trim().equals("AWTES")){
			TESIP = InetAddress.getByName(splitAnswer[1]);
			TESport = Integer.parseInt(splitAnswer[2].trim());
			System.out.println("Receiving from: " + TESIP.getHostName() + " " + TESport);
			//Now that that questionnaire's information was obtained from its TES server, time to establish a TCP connection to fetch it
			createTCP();
			outToServer.writeBytes("RQT " + SID + "\n");
			byte[] aByte = new byte[1];
			int bytesRead;
			InputStream is = null;
			try {
				is = TCPsocket.getInputStream();
			} catch (IOException ex) {
				
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (is != null) {
				FileOutputStream fos = null;
				BufferedOutputStream bos = null;
				try {
					fos = new FileOutputStream("Questionario Topico " + topicNumber + ".pdf");
					bos = new BufferedOutputStream(fos);
					bytesRead = is.read(aByte, 0, aByte.length);
					do {
							baos.write(aByte);
							bytesRead = is.read(aByte);
					} while (bytesRead != -1);
					String wholeCommand = new String(baos.toByteArray(), "utf-8");
					String[] splitCommand = wholeCommand.split(" ");
					QID = splitCommand[1];
					dueTime = splitCommand[2];
					bos.write(baos.toByteArray());
					bos.flush();
					bos.close();
					TCPsocket.close();
					System.out.println("Received file: " + "Questionario Topico " + topicNumber + ".pdf");
				} catch (IOException ex) {
				}
			}
		}
		if(splitAnswer[0].trim().equals("AQS")){
			System.out.println("Obtained score " + splitAnswer[2] + "%");
		}
		if(splitAnswer[0].trim().equals("-1")){
			System.out.println("Questionnaire " + QID + " not submitted, due time has past");
		}
		if(splitAnswer[0].trim().equals("-2")){
			System.out.println("QID and SID don't match!");
		}
		if(splitAnswer[0].trim().equals("ERR")){
			System.out.println("Request incorrectly formulated");
		}
	}
	
	public static void main(String args[]) throws Exception {
		 SID = Integer.parseInt(args[0]);
		 try{
			 ECPport = Integer.parseInt(args[4]);
			 ECPname = args[2];
		 }
		 catch(ArrayIndexOutOfBoundsException exception){}
		 while(true){
			 try{
				 inFromUser = new BufferedReader(new InputStreamReader(System.in));
				 String input = inFromUser.readLine();
				 processInput(input);
			 }
			 catch(Exception e){
				 e.printStackTrace();
				 System.out.println("ERROR: Program will terminate");
			 }
		 }
	}
}
