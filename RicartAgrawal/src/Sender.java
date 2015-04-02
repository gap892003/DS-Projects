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
import java.net.InetSocketAddress;
import java.net.Socket;


// limiting the scope to send one request at a time and not multiple
// if we are to send multiple request we need to add multiple threads

public class Sender implements Runnable {

	NodeInfo serverPeer;
	Request request;
	int TIME_OUT = 100;
	Node parent ;

	@Override
	public void run() {

		// Create new Socket 
		
		Socket sock;
		try {
			
			if (serverPeer != null){
				
				sock = new Socket( serverPeer.ipAddress, serverPeer.portNumber );
				System.out.println( "Sending request from" + Listener.getInstance().getIPAddress() + " "
						+ Listener.getInstance().getPortNumber()
						+ " to " + serverPeer.ipAddress + " " + serverPeer.portNumber
						+ " of type " + request.requestType 
						);

			}else {
				
				sock = new Socket();
				sock.connect( new InetSocketAddress (request.ipAddressofReceiver, request.portNumberOfReceiver), 5000);
				System.out.println( "Sending request from" + Listener.getInstance().getIPAddress() + " "
						+ Listener.getInstance().getPortNumber()
						+ " to " + request.ipAddressofReceiver + " " + request.portNumberOfReceiver
						+ " of type " + request.requestType 
						);

			}
			
			
			ObjectOutputStream oStream = new ObjectOutputStream ( sock.getOutputStream() );
			
			// send request
			oStream.writeObject( request );
			oStream.close();
		} catch (IOException e) {
				
			System.out.println( "Could not send request " + e.getMessage());
			e.printStackTrace();
		} 
	}
	
	void sendRequest( NodeInfo serverPeer, Request request ){
		
		this.serverPeer = serverPeer;
		this.request = request;
		Thread thread = new Thread(this);		
		thread.start();
		
//		this.request.route += Listener.getInstance().getIPAddress() + ":" 
//								+ Listener.getInstance().getPortNumber() + "->";
		// this join is added as we done want to start new request 
		// till previous one is finished sending
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Overloaded method which accepts only request object
	 * It expects ip and port number of node which is supposed to receive
	 * this request embedded in request object  
	 * 
	 * @param request
	 */
	void sendRequest( Request request ){
		
		this.request = request;
		this.serverPeer = null;
		Thread thread = new Thread(this);		
		thread.start();
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
