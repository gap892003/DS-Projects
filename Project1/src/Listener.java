/*
 * Listener.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * Singleton class to listen to all incoming requests 
 * If it is a file transfer request it asks FileReceiverThread to 
 * receive it, for any other request it asks peer class to process
 * them.
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Listener implements Runnable {

	ServerSocket serverSocket;
	Peer parent;
	private static Listener instance = null; 
	
	protected Listener(){
		
		// no need to implement 
	}
	
	/**
	 * Method to get singleton instance
	 * 
	 * */
	public static Listener getInstance ( ) {
		
		if (instance == null ){
			
			instance = new Listener();
		}
		
		return instance;				
	}	
	
	
	/**
	 * Class to start receiving file in a new thread 
	 * */
	class FileReceiverThread implements Runnable{

		Socket socket;
		String fileName;
		
		@Override
		public void run() {
		
			File received =  new File (parent.peerFilesPath + "//" + fileName);
			
			try {
				
				if (received.exists()){
					
					received.delete();					
				}				
				
				received.createNewFile();
				OutputStream os = socket.getOutputStream();				
				System.out.println("Conveying peer ready to receive file");
				PrintWriter writer = new PrintWriter(os);
				writer.println("ready");				
				writer.flush();
				
				FileOutputStream fos = new FileOutputStream( received );
				InputStreamReader reader = new InputStreamReader( socket.getInputStream() );				
				int character;
				
				while( (character = reader.read()) != -1){
					
					fos.write(character);
				}
				
				System.out.println("File received");
				fos.close();
				reader.close();
				parent.addFileToZone(fileName);
			} catch (IOException e) {
								
				e.printStackTrace();
			}					
		}
		
		void start(Socket sock,String _filename){
			
			this.socket = sock;
			this.fileName = _filename;
			Thread fileReceivingThread = new Thread( this );
			fileReceivingThread.start();
		}
	}
	
	
	class RequestThread implements Runnable{

		Socket socket;
		
		@Override
		public void run() {
			
			// do your processing
			try {
				
				ObjectInputStream istream = new ObjectInputStream(socket.getInputStream());
				Request reqObject = (Request)istream.readObject();
				System.out.println( "Received a request from "+ reqObject.ipAddressOfSender 
						+ " " + reqObject.portNumberOfSender);
				
				if (reqObject.requestType == Request.REQUEST_TYPE.fileTxInitiate){
					
					FileReceiverThread receiverThread = new FileReceiverThread();
					receiverThread.start(socket, reqObject.fileName);					
					return;
				}
				
				synchronized ( parent ) {

					parent.processIncomingRequest(reqObject);
				}				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// return to peer
		}
		
		public void start(Socket socket){
			
			this.socket = socket;
			Thread requestThread = new Thread( this );
			requestThread.start();
		}
	}

	
	void startListening(){
		
		// Listener will run in its own thread. 
		// and start new thread for every connection
		Thread listenerThread = new Thread(this);
		listenerThread.start();
	}
	

	@Override
	public void run() {
		
		try {
			
			serverSocket = new ServerSocket( 0 );
			
			// accept connection 
			while(true){
				
				Socket sock = serverSocket.accept();
				
				// create new request thread and start communication
				RequestThread thread = new RequestThread();
				thread.start( sock );
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
	}
	
	
	String getIPAddress(){
		
		try {
						
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	int getPortNumber (){
		
		return serverSocket.getLocalPort();
	} 
}
