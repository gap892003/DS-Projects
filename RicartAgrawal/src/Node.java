import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Node {

	public class RequestTracker{
		
		int repliesCount;
		int requestID;
	}
	
	enum STATE{
		
		RELEASED,
		WANTED, 
		HELD,		
	}
	
	STATE currentState;
	int nodeID ;
	List <NodeInfo> knownNodes;
	boolean isCriticalSectionHolder;
	int[] criticalSection; 
	List <NodeInfo> waitingNodesQueue;
	Listener _listener;
	int waitCount;
	long waitTimeStamp = 0;
	
	// TODO: 
	List <RequestTracker> requestTrackersList; // list holds all critical section access
											   // requests ; in case when this node wants
											   // to access critical section more than once
	/**
	 * Constructor 
	 */
	Node (){
		
		// initialization
		currentState  = STATE.RELEASED;	
		knownNodes = new LinkedList<NodeInfo>();
		waitingNodesQueue = new LinkedList<NodeInfo>();
		
		// initialize listener
		_listener = Listener.getInstance();
		_listener.parent = this;
		_listener.startListening();
		System.out.println("Initializing node");
	}
	
	/**
	 * Method to generate a critical section if this node is
	 * identified by user to be critical section holder
	 */
	void generateCriticalSection (){
		
		isCriticalSectionHolder = true;
		criticalSection = new int[10];
		
		for (int i = 0 ; i < 10 ; ++i){
			
			criticalSection[i] = i;
		}
	}
	
	/**
	 * Method to simulate that node is doing something in 
	 * critical section
	 * @param _nodeID	
	 */
	void doSomethingWithCriticalSection(int _nodeID){
		
		// simulate doing something	
		System.out.println("*****************************************************");
		System.out.println(_nodeID + " Accessing critical section now....");
		
		try {
			
			for (int  i = 0 ; i < 10 ; ++i){
				
				System.out.println(" Processing in critical section... ");
			    Thread.sleep( 500 ); 
			}
			
		    System.out.println(_nodeID + " done with critical section");		    
		} catch(InterruptedException ex) {
			
		    Thread.currentThread().interrupt();
		}
		// send response that you are done 
		System.out.println("*****************************************************");
	}
	
	/**
	 * Method to send request to all nodes 
	 * that it wants to enter critical section 
	 * 
	 */
	void sendWantedRequest(){
		
		if (isCriticalSectionHolder){
			
			this.currentState = STATE.HELD;
			doSomethingWithCriticalSection( nodeID );
			this.currentState = STATE.RELEASED;
			return;
		}
		
		System.out.println("*****************************************************");		
		System.out.println("I Want to access critical section");
		System.out.println("Sending request to all");
		System.out.println("*****************************************************");		
		//Change state to wanted
		this.currentState = STATE.WANTED;
		this.waitTimeStamp = System.currentTimeMillis();
		waitCount = knownNodes.size();
		
		// send request to all nodes 		
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		while ( iterator.hasNext() ){
			
			NodeInfo nodeToBeContacted = iterator.next();
			Request req = new Request();
			req.requestType = Request.REQUEST_TYPE.AccessCS;
			req = fillRequestObjectWithOwnDetails(req);
			req.ipAddressofReceiver = nodeToBeContacted.ipAddress; 
			req.portNumberOfReceiver = nodeToBeContacted.portNumber;			
			req.nodeIDOfReceiver = nodeToBeContacted.nodeID;
			Sender sender = new Sender ();
			sender.sendRequest( req );
		}
		
		// wait for n-1 replies		
		// change state
		// and access critical section 
	}

	/**
	 * Perform action according to request received 
	 * @param	reqObject	Request object to be processed
	 * */
	void processIncomingRequest( Request reqObject ){
		
		System.out.println("Processing " + reqObject.requestType + " request" );
		// see current state
		// simulating time stamping by comparing process id s
		if ( reqObject.requestType == Request.REQUEST_TYPE.AccessCS ){
			if ( (this.currentState == STATE.HELD) || ( this.currentState == STATE.WANTED 
					&& this.waitTimeStamp  < reqObject.timeStamp )
					){
				System.out.println("Queing request");
				//  queue request 
				NodeInfo waitingNode = new NodeInfo();
				waitingNode.ipAddress = reqObject.ipAddressOfSender;
				waitingNode.portNumber = reqObject.portNumberOfSender; 
				waitingNode.nodeID = reqObject.nodeIDOfSender;
				waitingNodesQueue.add(waitingNode);
			}else{
				
				// reply to the node
				System.out.println("Sending ack to requesting node");
				sendReply(reqObject.ipAddressOfSender,reqObject.portNumberOfSender
						, reqObject.nodeIDOfSender, Request.REQUEST_TYPE.AccessCSAck);
			}
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.AccessCSAck ){
			
			waitCount--;
			System.out.println("Received reply from " + reqObject.ipAddressOfSender + " " + 
			reqObject.portNumberOfSender);
			System.out.println("Now waiting for " + waitCount +  "replies");
			// check if n-1
			if (waitCount == 0 ){
				
				System.out.println("*****************************************************");
				System.out.println("Got all replies");
				// Access Critical section 				
				System.out.println("Accessing critical section");
				this.currentState = STATE.HELD;
				waitTimeStamp = 0;
				// send request to node which contains critical section to Enter CS 
				NodeInfo node = getNodeWithCS();
				sendReply(node.ipAddress, node.portNumber, node.nodeID, Request.REQUEST_TYPE.EnterCS);				
				// when done notify all about done accessing 
				// this is handled in Processing done 
			}
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.EnterCS ){
			
			doSomethingWithCriticalSection(reqObject.nodeIDOfSender);
			
			// respond back to sender 
			sendReply(reqObject.ipAddressOfSender,reqObject.portNumberOfSender
					, reqObject.nodeIDOfSender, Request.REQUEST_TYPE.ProcessingDone);

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.ProcessingDone ){
			
			System.out.println("Leaving Critical section");
			System.out.println("*****************************************************");			
			this.currentState = STATE.RELEASED;
			
			// reply to all waiting nodes 
			replyToAllwaitingNodes();
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Join ){
			
			sendReply( reqObject.ipAddressOfSender,reqObject.portNumberOfSender,
					reqObject.nodeIDOfSender, Request.REQUEST_TYPE.JoinAck);
			NodeInfo temp = new NodeInfo();			
			temp.ipAddress = reqObject.ipAddressOfSender;
			temp.nodeID = reqObject.nodeIDOfSender;
			temp.portNumber = reqObject.portNumberOfSender;
			addNode(temp);
			sendUpdateToAll(temp);
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.JoinAck){

			addNodesFromList(reqObject.knownNodesList);
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
			sendReply(info.ipAddress, info.portNumber, info.nodeID, Request.REQUEST_TYPE.Join);
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
	 * Method to identify which nodes out of known nodes
	 * contain critical section 
	 * @return NodeInfo object of node containing Critical section
	 */
	NodeInfo getNodeWithCS(){
		
		NodeInfo node = null;
		
		Iterator<NodeInfo> iterator = knownNodes.iterator();
		
		while ( iterator.hasNext() ){
			
			NodeInfo tempnode = iterator.next();
			if (tempnode.isCSHolder){
				
				node = tempnode;
			}
		}		
		
		return node;
	}
	
	/**
	 * Method to reply back to all waiting nodes 
	 */
	void replyToAllwaitingNodes(){
		
		Iterator<NodeInfo> iterator = waitingNodesQueue.iterator();
		
		while ( iterator.hasNext() ){
			
			NodeInfo node = iterator.next();
			sendReply(node.ipAddress, node.portNumber, node.nodeID, Request.REQUEST_TYPE.AccessCSAck);		
		}
		
		waitingNodesQueue.clear();
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
	void sendReply (String ipAddressOfSender, int portNumberOfSender,int nodeIDOfSender, 
			Request.REQUEST_TYPE requestType){
		
		Request replyReq = new Request();
		replyReq.requestType = requestType;
		replyReq.ipAddressofReceiver = ipAddressOfSender;
		replyReq.portNumberOfReceiver = portNumberOfSender; 
		replyReq.nodeIDOfReceiver = nodeIDOfSender;
		replyReq = fillRequestObjectWithOwnDetails(replyReq);

		if (requestType == Request.REQUEST_TYPE.JoinAck){
			
			replyReq.knownNodesList = this.knownNodes;
		}

		// send reply
		Sender replySender = new Sender();
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
		request.timeStamp = System.currentTimeMillis();
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
		
		Iterator<NodeInfo> iterator = waitingNodesQueue.iterator();
		
		System.out.println("Waiting queue nodes: ");
		if (waitingNodesQueue.size() > 0){
			
			while ( iterator.hasNext() ){
				
				NodeInfo tempnode = iterator.next();
				System.out.println( "	" + tempnode.nodeID );
			}			
		}
		
		System.out.println("Known nodes: ");

		Iterator<NodeInfo> iteratorKnown = knownNodes.iterator();
		while ( iteratorKnown.hasNext() ){
			
			NodeInfo info = iteratorKnown.next();
			System.out.println("	" + info.ipAddress + " "
					+ info.portNumber + " " + info.nodeID);
		}

		
		System.out.println( "Enter following command in other node's terminal "
				+ "to add this node: ");
		
		String command = "node";

		if (isCriticalSectionHolder){
			
			command = "csnode" ;
		}
		
		System.out.println(command + " " + _listener.getIPAddress() + " "
				+ _listener.getPortNumber() + " " +  this.nodeID);
		System.out.println("*****************************************************");
	}
	
	public static void main ( String args[] ){
				
		Node node = new Node();		
		
		if (args.length > 0){
			
			try {
				
				if ( args [0].equals("-c")){ // that means this node has critical section 

					// generate that 
					node.isCriticalSectionHolder = true;
					int nodeID = Integer.parseInt(args[1]);
					node.nodeID = nodeID;
				}else{ // it is node id

					int nodeID = Integer.parseInt(args [0]);
					node.nodeID = nodeID;
				}
				
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
			}else if ( command.equals("ac") ){// means "access critical section"
				
				node.sendWantedRequest();
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
			}else if (command.equals("csnode")){
				
				String ipAddress = in.next();
				String port = in.next();
				String nodeID = in.next();
				
				NodeInfo newNode = new NodeInfo();
				newNode.ipAddress = ipAddress;
				newNode.portNumber = Integer.parseInt( port );
				newNode.nodeID = Integer.parseInt ( nodeID );
				newNode.isCSHolder = true;
				node.addNode(newNode);
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
