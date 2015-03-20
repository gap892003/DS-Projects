/*
 * Sender.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * Provides functionality to send requests to other peer. 
 * It adds current peer's information along the way  
 * 
 * Every time new instance should be created of this class
 *  
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;


// limiting the scope to send one request at a time and not multiple
// if we are to send multiple request we need to add multiple threads

public class Sender implements Runnable {

	PeerInfo serverPeer;
	Request request;
	int TIME_OUT = 100;
	
	@Override
	public void run() {

		// Create new Socket 
		System.out.println( "Sending request from" + Listener.getInstance().getIPAddress() + " "
				+ Listener.getInstance().getPortNumber()
				+ " to " + serverPeer.ipAddress + " " + serverPeer.portNumber
				+ " of type " + request.requestType 
				);
		
		Socket sock;
		try {
			
			sock = new Socket( serverPeer.ipAddress, serverPeer.portNumber );
			ObjectOutputStream oStream = new ObjectOutputStream ( sock.getOutputStream() );
			
			// send request
			oStream.writeObject( request );
			oStream.close();
		} catch (IOException e) {
				
			System.out.println( "Could not send request" );
			e.printStackTrace();
		} 
	}
	
	void sendRequest( PeerInfo serverPeer, Request request ){
		
		this.serverPeer = serverPeer;
		this.request = request;
		Thread thread = new Thread(this);		
		thread.start();
		this.request.route += Listener.getInstance().getIPAddress() + ":" 
								+ Listener.getInstance().getPortNumber() + "->";
		// this join is added as we done want to start new request 
		// till previous one is finished sending
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
