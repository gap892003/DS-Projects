/*
 * Peer.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * Peer class representing a single peer on DHT
 * Handles incoming request processing, maintains, updates 
 * neighbor information and finger table. 
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 * 
 */

import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.osjava.norbert.NoRobotClient;

public class Peer extends Node {

	NodeInfo successor; 
	NodeInfo predecessor;
	public static final int TOTAL_IDS = 7; 
	boolean isBootstrap = false;
	boolean isJoined = false; 
	NodeInfo bootStrapServerInfo;
	Zone selfZone;
	Crawler crawler;
	NoRobotClient robotClient;
	InsertURL insertURLModule;
	boolean crawlPaused = false;
	List<String> successorURLQBackup;
	long dhtInsertMessagesCount = 0;
	long startFreeMemory = 0; 
	
	public static final String userAgent = "RIT_TERM_PROJECT_Bot/1.0 (+http://www.gauravpradipjoshi.com)";

	/**
	 * Initializes peer
	 * @param info NodeInfo object of the bootstrap server
	 */
	void initialize( NodeInfo info ){

		if ( info  == null ){

			isBootstrap = true;
			isJoined = true;
			bootStrapServerInfo = null;
			selfZone = new Zone ( 0, TOTAL_IDS ) ;	
			successor = getSelfInfo();
			predecessor = getSelfInfo();
			startBackupTimer();
		}else{

			bootStrapServerInfo = info;
		}

		crawler = new Crawler();
		crawler.parent = this;
		robotClient = new NoRobotClient( userAgent );
		insertURLModule = new InsertURL(this);
		insertURLModule.start();
		startFreeMemory = Runtime.getRuntime().freeMemory();
		System.out.println("Start free mem: " + startFreeMemory/1000 + "KB");
	}

	/**
	 * Overridden function for handling incoming request
	 */
	@Override
	synchronized void processIncomingRequest( Request reqObject ){

		dhtReceivedMessagesCount++;
		System.out.println("Processing request of type " + reqObject.requestType
				+ "from " + reqObject.senderInfo.ipAddress);
		if ( reqObject.requestType == Request.REQUEST_TYPE.Join ){

			// check if random point lies in your zone			
			if ( !selfZone.contains(reqObject.randomNumberForjoining)){

				// route to somewhere else 
				routeToNextPeer( reqObject );
				return;
			}

			Zone newZone = null;
			// if yes then split zone =
			synchronized ( selfZone ) {

				System.out.println("Spliting zones..");
				newZone = selfZone.splitZone( reqObject.randomNumberForjoining );
			}

			Request response = getRequest(reqObject.senderInfo , Request.REQUEST_TYPE.JoinAck);
			response.predecessor = getSelfInfo();
			response.successor = this.successor;
			response.zoneInfo = newZone;

			// then change successor
			this.successor = reqObject.senderInfo;
			this.successor.zone = newZone;
			sendMessage( response );			
			// else forward request 

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.JoinAck ){

			// received response from network and all the information
			this.successor = reqObject.successor;
			this.predecessor = reqObject.predecessor;
			this.selfZone = reqObject.zoneInfo;

			// convey predecessor that it has joined it needs to update
			// its neighbors
			// send update message to predecessor
			Request updateSuccessorReq = getRequest( this.successor , 
					Request.REQUEST_TYPE.UpdatePredecessor );
			updateSuccessorReq.predecessor = getSelfInfo();
			updateSuccessorReq.successor = null;
			sendMessage(updateSuccessorReq);
			startBackupTimer();
			reStartCrawling();

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.UpdateSuccessor ){

			// WARNING: DO NOT UPDATE PREDECESSOR HERE 
			this.successor = reqObject.successor;			
		}
		// this is actually not required 
		else if ( reqObject.requestType == Request.REQUEST_TYPE.UpdatePredecessor ){

			// WARNING: DO NOT UPDATE SUCCESOR HERE 
			this.predecessor = reqObject.predecessor;

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Update ){

			// check if neighbors are alive and take backup
			System.out.println("Responding to update " + selfZone);
			Request req = getRequest(reqObject.senderInfo, Request.REQUEST_TYPE.UpdateAck);
			req.zoneInfo = selfZone; 
			req.successorURLQBackup = URLInsertQueue.getInstance().getBackp();
			sendMessage(req);

			// start backup process

		}else if ( reqObject.requestType == Request.REQUEST_TYPE.UpdateAck ){

			// check if neighbors are alive and take backup
			System.out.println( " Updating zone of successor " + reqObject.zoneInfo);
			successor.zone = reqObject.zoneInfo;
			successorURLQBackup = reqObject.successorURLQBackup;
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Leave ){

			// let successor know you are leaving 
			// it will take over the zone, change its predecessor 
			// and let predecessor know about the change 
			synchronized ( selfZone ) {

				selfZone.mergeZones( reqObject.zoneInfo );
			}

			this.successor = reqObject.successor;

			// send predecessor message that I am your new successor 
			Request req = getRequest( this.successor, Request.REQUEST_TYPE.UpdatePredecessor);
			req.predecessor = getSelfInfo();
			sendMessage(req);

			Request reLeaveAck = getRequest( reqObject.senderInfo , Request.REQUEST_TYPE.LeaveAck);
			sendMessage( reLeaveAck );
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.LeaveAck ){

			System.exit(0);
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.TakeOverUpdate ){
			
			predecessor = reqObject.senderInfo;
			update();
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Insert ){

			// check if random point lies in your zone
			String domain = selfZone.extractDomain( reqObject.urlToBeInserted );

			if ( !selfZone.contains(HashCalculator.calculateHash( domain ))){

				// route to somewhere else 				
				routeToNextPeer( reqObject );
				return;
			}else{

				dhtInsertMessagesCount++; // incrementing insert message count
				if (checkWithRobotClient(reqObject.urlToBeInserted)){

					synchronized ( selfZone ) {
						
						selfZone.addUrl( reqObject.urlToBeInserted );
					}				

					reStartCrawling();
				}
			}			
		}
	}

	void startBackupTimer(){

		Timer timer = new Timer();
		timer.scheduleAtFixedRate (new TimerTask() {

			@Override
			public void run() {

				update();
			}
		}, 60000, 60000); //
	}

	void routeToNextPeer( Request request ){

		// routing to next peer normally
		Sender sendMessage = new Sender();
		sendMessage.parent = this;
		request.receiverInfo = this.successor;
		sendMessage.sendRequest(this.successor, request);
	}

	@Override
	void requestTimedOut( Request reqObject ){

		if ( reqObject.requestType == Request.REQUEST_TYPE.Insert ){			

			// insert url back into send url queue file  
			URLInsertQueue.getInstance().insertUrl(reqObject.urlToBeInserted);
			
		}else if ( reqObject.requestType == Request.REQUEST_TYPE.Update ){

			// this means node is not available
			// TAKEOVER AND Let its successor and predecessor know 
			System.out.println("TAKEOVER SUCCESSOR");
			selfZone.mergeZones( successor.zone );			
			URLInsertQueue.getInstance().mergeList(successorURLQBackup);
			NodeInfo newSuccessor = new NodeInfo();
			newSuccessor.ipAddress = successor.successorIpAddress;
			newSuccessor.portNumber = successor.portNumber;
			Request req = getRequest(newSuccessor, Request.REQUEST_TYPE.TakeOverUpdate);
			sendMessage(req);
		}
	}

	void insert ( String url ){

		// check if id is already there in your list
		// if not check in DHT
		System.out.println("Inserting url " + url + " into DHT" );
		String domain = selfZone.extractDomain(url);
		int hash = HashCalculator.calculateHash(domain);

		System.out.println("With hash value :" +  hash);
		if (selfZone.contains(hash)){

			// check if url allowed to crawl
			if (checkWithRobotClient(url)){

				System.out.println("URL allowed to crawl");
				// insert in own queue			
				selfZone.addUrl(url);	

				if (crawlPaused){

					crawlPaused = false;
					reStartCrawling();
				}
			}else{

				System.out.println("URL not allowed");
			}
		}else {

			// route further
			Request req = getRequest(this.successor, Request.REQUEST_TYPE.Insert);
			req.urlToBeInserted = url;
			routeToNextPeer(req);
		}

	}


	/**
	 * Delegate method for crawler 
	 */
	synchronized void doneCrawling (String urlCrawled ){

		System.out.println("Done crawling url: " + urlCrawled );
		selfZone.addURLCrawled( urlCrawled );
		String urlToCrawl = selfZone.getNextUrl();

		if ( null != urlToCrawl ){

			crawler.crawl( urlToCrawl );
		}else{
			
			crawlPaused = true;
		}
	}

	boolean checkWithRobotClient( String url ){

		System.out.println("Checking robot rules");

		try {

			URL urlTobeChecked = new URL(url);
			robotClient.parse(urlTobeChecked );
			if (robotClient.isUrlAllowed(urlTobeChecked )){

				return true;
			}
		} catch (Exception e) {

			return false;
		}		

		return false;
	}

	void join(){

		// generate a random number from IDs  
		int id = HashCalculator.calculateHash(_listener.getIPAddress());
		System.out.println("IP HASH : " + id);
		// join network with that id
		if ( null != bootStrapServerInfo){

			System.out.println( "Sending request to bootstrap for joining " 
					+  bootStrapServerInfo.ipAddress);
			Request req = getRequest( bootStrapServerInfo, Request.REQUEST_TYPE.Join );
			req.randomNumberForjoining = id;	
			sendMessage( req );
		}
	}

	void leave(){

		Request req = getRequest( predecessor , Request.REQUEST_TYPE.Leave);
		req.zoneInfo = selfZone;
		req.successor = this.successor;
		sendMessage( req );
	}

	void update(){

		// taking backup of both servers		
		// get their URL queue file 
		if (successor != null)
		sendMessage(successor , Request.REQUEST_TYPE.Update);
		//sendMessage(predecessor , Request.REQUEST_TYPE.Update); // not needed
	}

	@Override
	NodeID getID() {

		return null;
	}

	@Override
	void showDetails(){

		System.out.println("*****************************************************");
		System.out.println( "IP : " + _listener.getIPAddress());
		System.out.println( "Port : " + _listener.getPortNumber());

		if ( null != selfZone){

			System.out.println( "Zone start: " + selfZone.firstId);
			System.out.println( "Zone end: " + selfZone.lastId);
			System.out.println( "Domain count : " +  selfZone.domainQueue.size());
			System.out.println ( "Queued URLs count : " +  selfZone.getTotalUrlCount() );
			System.out.println ( "Seen URLs count : " +  selfZone.getSeenUrlCount() );
			System.out.println("DHT Messages received: " + dhtReceivedMessagesCount);
			System.out.println("DHT Messages sent: " + dhtSentMessagesCount);			
			System.out.println("DHT Insert Messages received count: " + dhtInsertMessagesCount);
			System.out.println("Domains it is handling: ");
			selfZone.printAllDomains();
		}
		
		System.out.println( "Memory consumed : "  +  ((startFreeMemory - Runtime.getRuntime().freeMemory())/1000
				+ " KB"));

		if (successor != null){

			System.out.println( "Successor " + successor.ipAddress + " " + successor.portNumber);			
		}

		if (predecessor != null){

			System.out.println( "Predecessor " + predecessor.ipAddress + " " + predecessor.portNumber );
		}
		printFigerTable();

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

	void printFigerTable(){


	}


	void startCrawling( String seedURL ){

		insert( seedURL );
//		crawlStarted = true;
//		crawler.crawl(seedURL);
	}

	void reStartCrawling(){

		System.out.println( "In restartCrawl method.." );
		String urlToCrawl = selfZone.getNextUrl();

		if ( null != urlToCrawl ){

			if ( crawlPaused ){

				crawlPaused = false;
			} 

			crawler.crawl( urlToCrawl );
		}
	}

	void startPrintTimer(){

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				showDetails();
			}
		}, 5000, 5000);// print after every 5 secs
	}

	/**
	 * Method to add details of own IP and port and node id 
	 * to request object 
	 * 
	 * @param request
	 * @return Request object after filling own details
	 */
	Request fillRequestObjectWithOwnDetails( Request request){

		request.senderInfo = getSelfInfo();
		return request;
	}

	NodeInfo getSelfInfo(){

		NodeInfo info = new NodeInfo(); 
		info = new NodeInfo();
		info.nodeID = this.getID();
		info.ipAddress = _listener.getIPAddress();
		info.portNumber = _listener.getPortNumber();
		info.zone = selfZone;
		if (successor != null ){
			
			info.successorIpAddress = successor.ipAddress;
			info.successorPortNumber = successor.portNumber;
		}
		return info;
	}

	/**
	 * The main function 
	 * Please refer ReadMe on using command line
     *
	 * */
	public static void main (String[] args) {

		Peer peer = null;
		peer = new Peer();

		// parse arguments
		// and send info about bootstrapServer in any 
		// parse arguments
		if (args.length > 0 ){

			peer.initialize( parseArguments(args) );
			System.out.println( "Peer initialized" );
			peer.startPrintTimer();
		}
		// check if we want to initialize bootstrap or 
		// if we want to initialize normal peer
		Scanner in = new Scanner (System.in);

		while ( true ){

			String command = null ;

			try{

				command  = in.next();
				if (command.toLowerCase().equals("view") ){

					if (null != peer){

						peer.showDetails();
					}
				}else if (command.toLowerCase().equals( "bootstrap" ) ){

					NodeInfo info = new NodeInfo(); 
					String ip  = in.next();
					String port  = in.next();
					info.ipAddress = ip;
					info.portNumber = Integer.parseInt(port);
					info.isBootStrap = true;
					peer.bootStrapServerInfo = info;
				}else if (command.toLowerCase().equals( "exit" ) ){

					break;
				}else if ( command.toLowerCase().equals("join") ){

					if (!peer.isJoined ){

						System.out.println("Joining..");
						peer.join();
					}else{

						System.out.println("Already Joined (or its the first node or bootstrap)");
					}

				}else if ( command.toLowerCase().equals("leave") ){

					peer.leave();
				}else if ( command.toLowerCase().equals("insert") ){

					System.out.println("Inserting file into DHT...");
					String url = in.next();				
					//					if (! (new File( peer.peerFilesPath +"//"+ filename).exists()))
					//					{
					//					   throw new FileNotFoundException("File not Found!");
					//					}
					//					
					peer.insert(url);

				}else if ( command.toLowerCase().equals("crawl") ){

//					if ( !peer.crawlStarted ){

						String seedUrl = in.next();						
						peer.startCrawling( seedUrl );
////					}else{
//
//						peer.reStartCrawling();
//					}
					
				}else if ( command.toLowerCase().equals("recrawl") ){

					peer.reStartCrawling();
				}else {

					System.out.println("No matching Command");
				}

			}catch( Exception e){

				System.out.println("Invalid Input, please re-enter " + e);
				e.printStackTrace();
			}
		}
		in.close();
	}

	/**
	 * function to parse arguments
	 */ 
	static NodeInfo parseArguments( String[] args ) {

		NodeInfo info = new NodeInfo();

		try{

			for (int i = 0 ; i < args.length ; ++i){

				if ( args[i].equals( "-p" ) ){

					info.isBootStrap = true;
					info.ipAddress = args[++i];
					info.portNumber = Integer.parseInt ( args[++i] );				
				}			
				else if ( args[i].equals( "-f" )){

					info  = null;
				}
			}
		}catch ( Exception e) {

			System.out.println("Invalid Input Could not start peer");
			System.exit(0);
		}

		return info; 
	}
}
