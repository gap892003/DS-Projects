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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Listener implements Runnable {

	ServerSocket serverSocket;
	Node parent;
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

	/**
	 * This method is required to be called at the beginning 
	 * of program to start listening.
	 * 
	 **/
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
	
	/**
	 * Method to return IP address of self 
	 * */
	String getIPAddress(){
		
		try {
						
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Method to return Port number of self 
	 * */
	int getPortNumber (){
		
		return serverSocket.getLocalPort();
	} 
}
