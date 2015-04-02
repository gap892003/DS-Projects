/*
 * Node.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * Implementation of Node class provides central functionality
 * for it to communicate with other node. 
 * This class takes input from user, accordingly makes request 
 * object and sends it to the Sender class
 * 
 * This class also processes requests which Listener receives 
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Node {
	
	int nodeID ;
	List <NodeInfo> knownNodes;
	Listener _listener;
	int waitCount = -1;
	NodeInfo leader = null;
	int higherNodesCount = 0;
	Timer timer;
	int failureCount = -1;
	
	/**
	 * Constructor
	 * */
	Node (){
		
		// initialization
		knownNodes = new LinkedList<NodeInfo>();
		
		// initialize listener
		_listener = Listener.getInstance();
		_listener.parent = this;
		_listener.startListening();
		System.out.println("Initializing node");		
	}
	
	/**
	 * Perform action according to request received 
	 * @param	reqObject	Request object to be processed
	 * */
	void processIncomingRequest( Request reqObject ){
		
		System.out.println("Processing " + reqObject.requestType + " request" );
		// see current state
		// simulating time stamping by comparing process id s
		if ( reqObject.requestType == Request.REQUEST_TYPE.Election ){
			
			// if timer is running stop that for a while
			stopTimer();
			// I am alive // send reply 			
			sendMessage( reqObject.ipAddressOfSender,reqObject.portNumberOfSender,
					reqObject.nodeIDOfSender, Request.REQUEST_TYPE.Answer);			
			// send message to all higher nodes 
			initiateElection();
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Answer ){
			
			// do not have to wait for any more replies
			// terminate that		
			// clear election queue
			System.out.println("*****************************************************");
			System.out.println("I am not the leader, a higher node alive");
			System.out.println("*****************************************************");

//			cancelElectionForMe();
			failureCount = -1;
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Coordinator ){
		
			
			if ( reqObject.nodeIDOfSender > this.nodeID){
				
				NodeInfo temp = new NodeInfo();
				temp.ipAddress = reqObject.ipAddressOfSender;
				temp.nodeID = reqObject.nodeIDOfSender;
				temp.portNumber = reqObject.portNumberOfSender;
				this.leader = temp;
				System.out.println("*****************************************************");
				System.out.println("My leader is " + leader.nodeID);
				System.out.println("*****************************************************");

				// start alive timer
				startAliveTimer();		
			}else{
				
				initiateElection();
			}
						
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Alive ){
			
			sendMessage( reqObject.ipAddressOfSender,reqObject.portNumberOfSender,
					reqObject.nodeIDOfSender, Request.REQUEST_TYPE.AliveAck);			
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.AliveAck ){

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Join ){
			
			sendMessage( reqObject.ipAddressOfSender,reqObject.portNumberOfSender,
					reqObject.nodeIDOfSender, Request.REQUEST_TYPE.JoinAck);
			NodeInfo temp = new NodeInfo();			
			temp.ipAddress = reqObject.ipAddressOfSender;
			temp.nodeID = reqObject.nodeIDOfSender;
			temp.portNumber = reqObject.portNumberOfSender;
			addNode(temp);
			sendUpdateToAll(temp);
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.JoinAck){

			addNodesFromList(reqObject.knownNodesList);
			this.leader = reqObject.currentLeader;
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Update ){
			
			if (reqObject.knownNodesList.size() > 0){
				
				addNode( reqObject.knownNodesList.get(0) );
			}
		}
			
	}
	
	/**
	 * Send join request to known peer
	 */
	void join(){
		
		if ( knownNodes.size() > 0){
			
			NodeInfo info = knownNodes.get(0);
			sendMessage(info.ipAddress, info.portNumber, info.nodeID, Request.REQUEST_TYPE.Join);
		}
	}
	
	/**
	 * Send update request to all known nodes 
	 * about new node 
	 * @param newNode	New Node added to the network   
	 */
	void sendUpdateToAll( NodeInfo newNode ){
		
		// create list to add all nodes 
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();
			if (!(info.nodeID == newNode.nodeID && info.ipAddress.equals(newNode.ipAddress)
					&& info.portNumber == newNode.portNumber)){

				sendUpdateMessage(info.ipAddress, info.portNumber, info.nodeID, newNode);
			}
		}
	}
	
	/**
	 * Method to start timer to periodically check if other 
	 * nodes are alive
	 * The time interval varies according to 
	 */
	void startAliveTimer(){

		timer = new Timer();
		timer.scheduleAtFixedRate( new TimerTask() {
			
			@Override
			public void run() {
				
				sendAliveRequest();
			}
		} , 15000 + nodeID*1000 , 15000 + nodeID*1000);
	}
	
	/**
	 * Stops timer
	 */
	void stopTimer (){
		
		if (timer != null){
			
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * Send alive request to all known nodes  
	 */
	void sendAliveRequest(){
		
		System.out.println("Checking if all nodes Alive");
		cancelAllPrevAliveRequests();
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		
		if (knownNodes.size() == 0){
			
			System.out.println("No nodes, no need to send alive requests");
		}
		
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();			
			sendMessage( info.ipAddress,info.portNumber,info.nodeID, Request.REQUEST_TYPE.Alive);
		}
			
	}
	
	void cancelAllPrevAliveRequests(){
		
	}
	
	/**
	 * Method to start election 
	 */
	void initiateElection(){
		
		// stopping heartbeat check
		// to avoid redundant checks
		stopTimer();
		failureCount = 0;
		// send election message to all nodes with higher ID
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		int requestSent = 0;
		
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();
			if (info.nodeID > this.nodeID){
								
				sendMessage( info.ipAddress,info.portNumber,info.nodeID, Request.REQUEST_TYPE.Election);
				requestSent++;
				// wait for replies 
			}
		}		
		
		if ( requestSent == 0 ){
			
			// elect itself as leader and let everyone know
			electSelf();			
		}
	}
		
	/**
	 * Send Coordinator ( win ) message to all Nodes 
	 */
	void sendCoordinatorMessage(){
				
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();								
			sendMessage( info.ipAddress,info.portNumber,info.nodeID, Request.REQUEST_TYPE.Coordinator);
		}		
	}
	
	/**
	 * Callback method for sender objects in case of 
	 * failure and timeout
	 *  
	 * @param reqObject	 
	 */
	void requestTimedOut( Request reqObject ){
		
		System.out.println("Request Timed out");
		
		if ( reqObject.requestType == Request.REQUEST_TYPE.Election ){
			
			failureCount++;
			
			if ( higherNodesCount == failureCount ){
				
				electSelf();
				failureCount = 0;
			}

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Alive ){
			
			if ( leader.nodeID == reqObject.nodeIDOfReceiver ){
				
				System.out.println("*****************************************************");
				System.out.println("Leader is down , initiate new election");
				System.out.println("*****************************************************");				
				initiateElection();
			}
		}
	}

	/**
	 * Method to perform actions for electing self 
	 */
	void electSelf(){
		
		System.out.println("*****************************************************");
		System.out.println("I AM THE LEADER");
		System.out.println("*****************************************************");
		leader = getSelf();
		startAliveTimer();
		sendCoordinatorMessage();
	}
	
	/**
	 * 
	 * Method sends request to given node of given type.
	 * 
	 * @param ipAddress		IP of Node to whom request is to be sent
	 * @param portNumber	Port Number of destination node 
	 * @param _nodeID		Node id of destination node
	 * @param requestType	Type of request to be sent
	 */
	void sendMessage (String ipAddress, int portNumber,int _nodeID, 
			Request.REQUEST_TYPE requestType){
		
		Request replyReq = new Request();
		replyReq.requestType = requestType;
		replyReq.ipAddressofReceiver = ipAddress;
		replyReq.portNumberOfReceiver = portNumber; 
		replyReq.nodeIDOfReceiver = _nodeID;
		replyReq = fillRequestObjectWithOwnDetails(replyReq);
		
		if (requestType == Request.REQUEST_TYPE.JoinAck){
			
			replyReq.knownNodesList = this.knownNodes;
			replyReq.currentLeader = leader;
		}
		
		// send reply
		Sender replySender = new Sender();
		replySender.parent = this;
		replySender.sendRequest(replyReq);		
	}
	
	/**
	 * Method to send update message to all known nodes 
	 * 
	 * @param ipAddress		IP of Node to whom request is to be sent
	 * @param portNumber	Port Number of destination node 
	 * @param _nodeID		Node id of destination node
	 * @param newNode		New Node added to the system 
	 */
	void sendUpdateMessage (String ipAddress, int portNumber,int _nodeID, 
			NodeInfo newNode){
		
		Request replyReq = new Request();
		replyReq.requestType = Request.REQUEST_TYPE.Update ;
		replyReq.ipAddressofReceiver = ipAddress;
		replyReq.portNumberOfReceiver = portNumber; 
		replyReq.nodeIDOfReceiver = _nodeID;
		replyReq = fillRequestObjectWithOwnDetails(replyReq);
		replyReq.knownNodesList = new LinkedList<NodeInfo>();
		replyReq.knownNodesList.add(newNode);
		
		// send reply
		Sender replySender = new Sender();
		replySender.parent = this;
		replySender.sendRequest(replyReq);				
	}
	
	/**
	 * Method to add details of own IP and port and node id 
	 * to request object 
	 * 
	 * @param request
	 * @return Request object after filling own details
	 */
	Request fillRequestObjectWithOwnDetails( Request request){
		
		request.nodeIDOfSender = this.nodeID;
		request.ipAddressOfSender = _listener.getIPAddress();
		request.portNumberOfSender = _listener.getPortNumber();
		return request;
	}
	
	/**
	 * Remove node from known Nodes list
	 * @param _nodeID	id of node to be removed
	 */
	void removeNode(int _nodeID){
		
		// remove node with that ipAddress from list
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();
			if ( info.nodeID == _nodeID){
				
				knownNodes.remove(info);
			}
		}
		
		updateHigherNodesCount();
	}
	
	/**
	 * Add nodes from the list passed to known nodes 
	 * list
	 * 
	 * @param list	list from which nodes are to be copied	
	 */
	void addNodesFromList ( List<NodeInfo> list ) {

		Iterator<NodeInfo> iterator = list.iterator();
		
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();
			
			if ( !knownNodes.contains(info) ){
				
				knownNodes.add(info);
			}
		}
		
		updateHigherNodesCount();
	}
	
	/**
	 * Add node to known nodes list
	 * 
	 * @param newNode
	 */
	void addNode (NodeInfo newNode){
		
		if ( !knownNodes.contains(newNode) ||
				!(this.nodeID == newNode.nodeID && _listener.getIPAddress().equals(newNode.ipAddress)
				&& _listener.getPortNumber() == newNode.portNumber)){
			
			knownNodes.add(newNode);
		}
		
		updateHigherNodesCount();
	}
	
	/**
	 * Method to traverse through list and get count 
	 * of higher nodes 
	 */
	void updateHigherNodesCount () {
		
		// updating higherNodesCount
		higherNodesCount = 0;
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();
			if ( info.nodeID > this.nodeID){
				
				higherNodesCount++;
			}
		}		
	}
	
	/**
	 * Method to get Node info object for self  
	 * @return	NodeInfo object containing details of self 
	 */
	NodeInfo getSelf(){

		NodeInfo self = new NodeInfo();
		self.ipAddress =  _listener.getIPAddress();
		self.portNumber =  _listener.getPortNumber();
		self.nodeID = nodeID;
		return self;
	}
	
	/**
	 * Print details of node 
	 */
	void showDetails(){
		
		System.out.println("*****************************************************");
		System.out.println( "IP : " + _listener.getIPAddress());
		System.out.println( "Port : " + _listener.getPortNumber());
		System.out.println( "Node ID : " + this.nodeID);
		
		System.out.println("Known nodes: ");

		Iterator<NodeInfo> iterator = knownNodes.iterator();
		while ( iterator.hasNext() ){
			
			NodeInfo info = iterator.next();
			System.out.println("	" + info.ipAddress + " "
					+ info.portNumber + " " + info.nodeID);
		}
		
		System.out.println( "Enter following command in other node's terminal "
				+ "to add this node: ");
		
		String command = "node";		
		System.out.println(command + " " + _listener.getIPAddress() + " "
				+ _listener.getPortNumber() + " " +  this.nodeID);
		
		if (leader != null){
			
			System.out.println("Leader :" + leader.nodeID);
		}
		System.out.println("*****************************************************");
	}
	
	public static void main ( String args[] ){
				
		Node node = new Node();		
		
		if (args.length > 0){
			
			try {
				
					int nodeID = Integer.parseInt(args [0]);
					node.nodeID = nodeID;
				
			}catch( Exception e){
				
				System.out.println("Command not recognised, exiting..");
				System.exit(1);
			}			
		}
		
		// start accepting commands
		Scanner in = new Scanner (System.in);
		while(true){
			
			String command  = in.next();
			
			if ( command.equals("exit") ){
				
				break;	
			}else if ( command.equals("sf") ){ // this means simulate failure 
				
				System.exit(1);
			}else if ( command.equals("ie") ){// means "access critical section"
				
				node.initiateElection();
			}else if ( command.equals("node") ){ // means "add node information"
				
				String ipAddress = in.next();
				String port = in.next();
				String nodeID = in.next();
				
				NodeInfo newNode = new NodeInfo();
				newNode.ipAddress = ipAddress;
				newNode.portNumber = Integer.parseInt(port);
				newNode.nodeID = Integer.parseInt (nodeID);
				newNode.isCSHolder = false;
				node.addNode(newNode);
			}else if ( command.equals("rmnode") ){// to remove node 
				
				String nodeID = in.next();
				node.removeNode(Integer.parseInt (nodeID));
			}else if ( command.equals( "view" ) ){
				
				node.showDetails();
			}else if ( command.equals( "join" ) ){
				
				if(node.knownNodes.size() > 0){
					
					node.join();
				}else{
					
					System.out.println("Please add at least 1 known node");
				}
			}	
		}
		
		in.close();
	}
}
