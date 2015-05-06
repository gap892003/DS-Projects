/*
 * Node.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 */

/**
 * Abstract class representing a Node in the network.  
 *  
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 * 
 */

public abstract class Node {

	Listener _listener;
	NodeInfo selfInfo = null;
	long dhtReceivedMessagesCount = 0; 
	long dhtSentMessagesCount = 0;

	public enum NodeID {

	}

	/**
	 * Constructor
	 * */
	Node (){

		// initialize listener
		_listener = Listener.getInstance();
		_listener.parent = this;
		_listener.startListening();
		System.out.println("Initializing node");		
	}


	/**
	 * 
	 * Method sends request to given node of given type.
	 * @param	receiverInfo	NodeInfo object of receiver
	 * @param 	requestType		Type of request to be sent
	 */
	void sendMessage (NodeInfo receiverInfo, 
			Request.REQUEST_TYPE requestType){

		Request replyReq = new Request();
		replyReq.requestType = requestType;
		replyReq.receiverInfo = receiverInfo;
		replyReq = fillRequestObjectWithOwnDetails(replyReq);

		// send reply
		Sender replySender = new Sender();
		replySender.parent = this;
		replySender.sendRequest(replyReq);		
	}

	/**
	 * Creates basic request object 
	 * 
	 * @param	receiverInfo	NodeInfo object of receiver	
	 * @param 	requestType		Type of request to be sent
	 * @return basic request object 
	 */
	Request getRequest ( NodeInfo receiverInfo, Request.REQUEST_TYPE requestType ){
		
		Request replyReq = new Request();
		replyReq.requestType = requestType;
		replyReq.receiverInfo = receiverInfo;
		replyReq = fillRequestObjectWithOwnDetails(replyReq);
		return replyReq;
	}	
	
	
	void sendMessage ( Request replyReq ){
		
		Sender replySender = new Sender();
		replySender.parent = this;
		replySender.sendRequest(replyReq);		
	}
	
	abstract NodeID getID();
	abstract void processIncomingRequest( Request reqObject );	
	abstract Request fillRequestObjectWithOwnDetails( Request request);
	
	/**
	 * Method to handle request time outs 
	 * @param reqObject	Request that failed
	 */
	void requestTimedOut( Request reqObject ){

		System.out.println("Time Out");
	}

	void incrementSentMessageCount (){
		
		dhtSentMessagesCount++;
	}

	
	/**
	 * Printing details of the node
	 */
	void showDetails(){

		System.out.println("*****************************************************");
		System.out.println( "IP : " + _listener.getIPAddress());
		System.out.println( "Port : " + _listener.getPortNumber());
		System.out.println( "Node ID : " + this.getID());
		System.out.println( "System free memory : "  + 		
		Runtime.getRuntime().freeMemory());
				
//		if (this.getID() == NodeID.Supplier){
//			
//			System.out.println( "Enter following command in other node's terminal "
//					+ "to add this node: ");
//
//			String command = "supplier";		
//			System.out.println(command + " " + _listener.getIPAddress() + " "
//					+ _listener.getPortNumber());
//		}
		
		System.out.println("*****************************************************");
	}
}
