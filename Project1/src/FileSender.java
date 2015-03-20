/*
 * FileSender.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * This class is explicitely used to send files from current 	
 * peer 
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FileSender implements Runnable {

	String filepath;
	PeerInfo serverPeer;
	
	FileSender (PeerInfo server, String _filePath ){
		
		this.serverPeer = server;
		this.filepath = _filePath;
	}
	
	void startTransfer(){
		
		Thread thread = new Thread( this );
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		System.out.println("Initiating file transfer");
		Request fileTxInitiateRequest = new Request();
		fileTxInitiateRequest.requestType = Request.REQUEST_TYPE.fileTxInitiate;
		fileTxInitiateRequest.ipAddressOfSender = Listener.getInstance().getIPAddress();
		fileTxInitiateRequest.portNumberOfSender = Listener.getInstance().getPortNumber();
		fileTxInitiateRequest.fileName = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
		
		Socket sock;
		try {
			
			sock = new Socket( serverPeer.ipAddress, serverPeer.portNumber );
			ObjectOutputStream oStream = new ObjectOutputStream ( sock.getOutputStream() );			
			// send request
			oStream.writeObject( fileTxInitiateRequest );
			
			BufferedReader reader = new BufferedReader(new InputStreamReader( sock.getInputStream() ));			
			String response= reader.readLine();
			
			if (response.toLowerCase().equals("ready")){
			
				System.out.println("Peer ready to receive file");
				File file = new File( filepath );
				byte byteArray[] = new byte[(int) file.length()];
				FileInputStream iStream = new FileInputStream(file);
				iStream.read(byteArray);
				iStream.close();
				
				System.out.println("Sending file....");
				OutputStream os = sock.getOutputStream();
				os.write(byteArray);
				os.flush();
				os.close();
				file.delete();
			}									
			
		} catch (IOException e) {
				
			System.out.println( "Could not send request" );
			e.printStackTrace();
		} 
	}
}
