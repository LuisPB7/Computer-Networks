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
import java.text.*;

class TES{
	
	private static int TESport = 59000;
	private static int ECPport = 58006;
	private static String ECPname = "localhost";
	private static int SID;
	private static int topicNumber;
	private static BufferedReader inFromUser;
    private static DatagramSocket serverSocket;
    private static Socket connectionSocket;
    private static BufferedReader inFromClient;
    private static DataOutputStream outToClient;
    private static String QID;
    private static String topic_name;
    private static String dueTime;
    private static String questName;
    
    /**
     * Delivers a random file from a chosen topic.
     * @return the chosen file.
     */
    public static String choseRandomFile(){
		Random rand = new Random();
		String topicN = Integer.toString(topicNumber);
		if(topicNumber < 10){
			topicN = "0" + topicNumber;
		}
		String topicModel = "T" + topicN + "QF";
		File folder = new File(".");
		File[] listOfFiles = folder.listFiles();
		List<String> topicFiles = new ArrayList<String>();
		for (int i = 0; i < listOfFiles.length; i++){
			if (((listOfFiles[i].isFile()) && (listOfFiles[i].getName().startsWith(topicModel))) && listOfFiles[i].getName().contains(".pdf")){
				topicFiles.add(listOfFiles[i].getName());
			}
		}
		int fileIndex = rand.nextInt(topicFiles.size());
		questName = (topicFiles.get(fileIndex)).split("\\.")[0];
		return questName;	
	}
    
    /**
     * Verifies if the user submitted its answer after the deadline.
     * @return true if that happened and false otherwise.
     */
    public static boolean submittedAfterDeadline() throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyy'_'hh:mm:ss");
        Date rightNow = new Date();
        String currentDate = sdf.format(rightNow);
        rightNow = sdf.parse(currentDate);
        Date dueDate = sdf.parse(dueTime);
        return rightNow.after(dueDate);
	}
	
	 /**
     * Verifies if a match exists between the SID and the QID.
     * @return true if that happened and false otherwise.
     */
	public static boolean SIDQIDmatch(){
		return String.valueOf(SID).equals(QID.substring(0,5));
	}
	
	/**
	 * Gets the RQS input
	 * @return true if the RQS is badly formulated and false otherwise.
	 */
	public static boolean badRQS(String input){
		String[] splitRQS = input.split(" ");
		if(splitRQS.length != 8){
			return true;
		}
		try{
			int SIDisInt = Integer.parseInt(splitRQS[1]);
		}
		catch(Exception e){
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the user's/ECP input.
	 * Processes its input according to the given rules.
	 */ 
	public static void processInput(String input) throws Exception{
		String[] splitInput = input.split(" ");
		if(splitInput[0].trim().equals("RQT")){
			try{
				if(splitInput.length != 2){
					byte[] error = "ERR".getBytes();
					outToClient.write(error, 0, error.length);
					outToClient.flush();
					outToClient.close();
				}
				SID = Integer.parseInt(splitInput[1]);
				InetAddress userIP = connectionSocket.getInetAddress();
				System.out.println("Request from: " + SID + " " + userIP.getHostName() + " " + connectionSocket.getPort());
				//Opens a questionnaire and sends User the AQT command
				File myFile = new File(choseRandomFile() + ".pdf");
				long size = myFile.length();
				byte[] mybytearray = new byte[(int) myFile.length()];
				FileInputStream fis = null;
				fis = new FileInputStream(myFile);
				BufferedInputStream bis = new BufferedInputStream(fis);
				bis.read(mybytearray, 0, mybytearray.length);
				//Before sending, concatenates QID time size data with AQT
				Date currentTime = new Date();
				Calendar cal = Calendar.getInstance();
				cal.setTime(currentTime);
				cal.add(Calendar.MINUTE, 1);
				SimpleDateFormat ft = new SimpleDateFormat("ddMMMyyy'_'hh:mm:ss");
				QID = SID + "_" + ft.format(currentTime);
				dueTime = ft.format(cal.getTime());
				byte[] info = ("AQT " + QID + " " + dueTime + " " + size + " ").getBytes();
				byte[] completeMessage = new byte[info.length + mybytearray.length + "\n".getBytes().length];
				System.arraycopy(info, 0, completeMessage, 0, info.length);
				System.arraycopy(mybytearray, 0, completeMessage, info.length, mybytearray.length);
				System.arraycopy("\n".getBytes(), 0, completeMessage, mybytearray.length, "\n".getBytes().length);
				outToClient.write(completeMessage, 0, completeMessage.length);
				outToClient.flush();
				outToClient.close();
			}
			catch(Exception e){
				byte[] error = "ERR".getBytes();
				outToClient.write(error, 0, error.length);
				outToClient.flush();
				outToClient.close();
			} 
			
		}
		if(splitInput[0].trim().equals("RQS")){
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];
			DatagramSocket clientSocket = new DatagramSocket(TESport);
			DatagramPacket sendPacket;
			if(badRQS(input)){
				sendData = "ERR".getBytes();
				outToClient.write(sendData, 0, sendData.length);
				outToClient.flush();
				outToClient.close();
				clientSocket.close();
			}
			else if(submittedAfterDeadline()){
				sendData = "-1".getBytes();
				outToClient.write(sendData, 0, sendData.length);
				outToClient.flush();
				outToClient.close();
				clientSocket.close();
			}
			else if(!SIDQIDmatch()){
				sendData = "-2".getBytes();
				outToClient.write(sendData, 0, sendData.length);
				outToClient.flush();
				outToClient.close();
				clientSocket.close();
			}
			else{
				double score = 0.0;
				double right = 0.0;
				BufferedReader solutionFile = new BufferedReader(new FileReader(questName + "A.txt"));
				String line = solutionFile.readLine();
				String[] splitLine = line.split(" ");
				for(int i = 0; i < 5; i++){
					if((splitInput[i+3].trim()).equals(splitLine[i])){
						right = right + 1;
					}
				}
				score = (right / 5)*100;
				System.out.println("Student " + SID + " Score: " + (int)score + "%");
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				outToClient.writeBytes("AQS" + " " + QID + " " + (int)score + "\n");
				//UDP socket is created here to send the result to the ECP server
				BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
				InetAddress IPAddress = InetAddress.getByName(ECPname);
				String sentence = "IQR " + SID + " " + QID + " " + topic_name + " " + (int)score;
				sendData = sentence.getBytes();
				sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ECPport);
				clientSocket.send(sendPacket);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				String confirmation = new String(receivePacket.getData());
				processInput(confirmation);
				clientSocket.close();
			}
		}
		if(splitInput[0].trim().equals("AWI")){
			System.out.println("ECP received results of questionnaire " + QID);
		}
		if(splitInput[0].trim().equals("INF")){
			topic_name = splitInput[1];
			topicNumber = Integer.parseInt(splitInput[2].trim());
		}
	}
	
    public static void main(String args[]) throws Exception {
		 try{
			 TESport = Integer.parseInt(args[1]);
			 ECPname = args[3];
			 ECPport = Integer.parseInt(args[5]);
		 }
		 catch(ArrayIndexOutOfBoundsException exception){}
         ServerSocket welcomeSocket = new ServerSocket(TESport);
         while(true){
		try{
            connectionSocket = welcomeSocket.accept();
            inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
            outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            String clientInput = inFromClient.readLine();
            processInput(clientInput);
		}
		catch(Exception e){
			System.out.println("ERROR: Program will terminate");
			System.exit(0);
		}
         } 
	}
}
