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
 * Implementation of peer class provides central functionality
 * for it to communicate with other peers. 
 * This class takes input from user, accordingly makes request 
 * object and sends it to the Sender class
 * 
 * This class also processes requests which Listener receives 
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Peer {

	protected PeerInfo bootStrapServerInfo;
	private List<Zone> zoneList;
	protected Listener _listener;
	protected Sender _sender;
	static boolean isFirstNode = false; 
	private List<PeerInfo> neighboursList;
	boolean isJoined = false;
	public String peerFilesPath = null;
	
	void initialize( PeerInfo bootStrapInfo ){

		//save bootstrapInfo
		bootStrapServerInfo = bootStrapInfo;

		// initialize zone list
		zoneList = new LinkedList<Zone>();

		// initialize neighbor list
		neighboursList = new LinkedList<PeerInfo>();

		// initialize listener 
		_listener = Listener.getInstance();
		_listener.parent = this;
		_listener.startListening();

		// initialize Sender
		_sender = new Sender();

		// if id indicates it is first peer acquire whole zone
		if ( isFirstNode ){

			System.out.println("Initializing first peer");
			Zone newZone = new Zone();
			newZone.setRect( new Rectangle2D.Double (0,0,10,10) );
			zoneList.add(newZone);			
		}
		
		// create folder for its files 
		peerFilesPath = ".//" + _listener.getIPAddress() +"."+ _listener.getPortNumber();
		File file = new File(peerFilesPath);
		
		if (!file.exists()) {
			
			if (file.mkdir()) {
				
				System.out.println("Directory is created!");
			} else {
				
				System.out.println("Failed to create directory!");
			}
		}
	}

	/**
	 * Join functionality
	 * */
	void join (){

		// contact bootstrap to join 
		System.out.println("Contacting bootstrap...");		
		Request reqObject = new Request();
		reqObject.requestType = Request.REQUEST_TYPE.join;
		reqObject.ipAddressOfSender = _listener.getIPAddress();
		reqObject.portNumberOfSender = _listener.getPortNumber();

		// generate random point
		Random randomGenerator = new Random();
		int  randomX = randomGenerator.nextInt(10);
		int  randomY = randomGenerator.nextInt(10);
		reqObject.pointToBeLookedFor = new Point2D.Double(randomX, randomY);
		_sender.sendRequest( bootStrapServerInfo, reqObject );
	}
	
	/**
	 * Search functionality
	 * */
	void search (String fileName){

		// make request for point 
		// right now hard coding 
		System.out.println("Searching file " + fileName);
		int x = HashCalculator.calculateXHash( fileName );
		int y = HashCalculator.calculateYHash(fileName);
		Point2D.Double pointToLook = new Point2D.Double(x,y);

		// check if file is within peer
		if (null != checkForPointInZoneList(pointToLook)){

			System.out.println("File is here at peer "+ _listener.getIPAddress() 
					+" " + _listener.getPortNumber());
			return;
		}

		PeerInfo info = findNearestPeer( pointToLook );		
		Request reqObject = new Request();
		reqObject.requestType = Request.REQUEST_TYPE.search;
		reqObject.fileName = fileName;
		reqObject.pointToBeLookedFor = pointToLook;
		reqObject.ipAddressOfSender = _listener.getIPAddress();
		reqObject.portNumberOfSender = _listener.getPortNumber();
		_sender.sendRequest( info, reqObject );
	}

	/**
	 * view Functionality
	 * */
	void printInfo(){
		
		System.out.println("**********************************************************");
		System.out.println( "My address:" + _listener.getIPAddress()+ ":" + _listener.getPortNumber());
		System.out.println("");
		System.out.println("Zones controlled:");

		if (!zoneList.isEmpty()){
			
			Iterator<Zone> zoneIterator = zoneList.iterator();
			
			while( zoneIterator.hasNext() ){
				
				System.out.println( "	"	 + zoneIterator.next().toString());
			}
		}		
		
		Iterator<PeerInfo> iterator = neighboursList.iterator(); 

		System.out.println("");		
		System.out.println("Neighbors: ");
		
		if (neighboursList.isEmpty()){
			
			System.out.println("	No Neighbors");
		}
		
		while ( iterator.hasNext() ){
			
			String str = "";
			PeerInfo _info = iterator.next();
			str = str + _info.ipAddress;
			str = str + " " + _info.portNumber;
			str +=  ": ";
			System.out.println( str );
			
			if (!_info.zoneList.isEmpty()){
									
				Iterator<Zone> zoneIterator = _info.zoneList.iterator();
				
				while( zoneIterator.hasNext() ){
					
					System.out.println( "		" + zoneIterator.next().toString());					
				}
			}
						
		}
		
		System.out.println("**********************************************************");		
	}
	
	
	
	void addZone( Zone zoneToBeAdded){

		synchronized ( zoneList ) {

			zoneList.add(zoneToBeAdded);
		}		
	} 

	void removeZone( Zone zoneToBeRemoved){

		synchronized ( zoneList ) {

			zoneList.remove(zoneToBeRemoved);
		}
	}

	
	/**
	 * Function to process incoming requests
	 * 
	 * */
	void processIncomingRequest( Request reqObject ){

		System.out.println( "Processing request at peer :" + _listener.getIPAddress() +" "
				+ _listener.getPortNumber() + " "
				+ " Request type is " + reqObject.requestType );

		synchronized (zoneList) {

			// first we will check for point in the request 
			// because that is a common part in every request

			if ( reqObject.requestType == Request.REQUEST_TYPE.joinAck){

				isJoined = true;
				System.out.println( "Joining done now sending success to parent peer"
						+ " " + reqObject.ipAddressOfSender
						+ " " + reqObject.portNumberOfSender );
				
				printRoutingInfo(reqObject);
				zoneList = reqObject.zoneInfo;				
				this.setNeighbors( reqObject.peerList );

				// send Join success to peer so that it can convey its neighbors
				// about updation of zone
				Request newReq = new Request();
				newReq.requestType = Request.REQUEST_TYPE.joinSuccess;
				newReq.ipAddressOfSender = _listener.getIPAddress();
				newReq.portNumberOfSender = _listener.getPortNumber();

				PeerInfo server = new PeerInfo();
				server.ipAddress = reqObject.ipAddressOfSender;
				server.portNumber = reqObject.portNumberOfSender;
				Sender newSender = new Sender();
				newSender.sendRequest(server, newReq);

				// update neighbors about its zone info
				sendNeighborsUpdatedZoneInfo( server );				

			}else if ( reqObject.requestType == Request.REQUEST_TYPE.joinSuccess){

				PeerInfo temp = new PeerInfo();
				temp.ipAddress = reqObject.ipAddressOfSender;
				temp.portNumber = reqObject.portNumberOfSender;
				sendNeighborsUpdatedZoneInfo( temp );
				removePeersfromNeigbors();
			}else if ( reqObject.requestType == Request.REQUEST_TYPE.update ){

				// for leave zoneinfo is null				
				if ( reqObject.zoneInfo.get(0).isEmpty() ){
					
					removeNeighbor( reqObject.ipAddressOfSender, reqObject.portNumberOfSender);
					return;
				}
				
				//updateNeighborInfo(reqObject.zoneInfo);
				// go through each neighbor one 
				// by one and observe its zones
				// we are checking here if the peer is still the neighbor 
				// if its not we want to remove it , if it is still 
				// a neighbor we just update its information
				Iterator<PeerInfo> iterator = neighboursList.iterator(); 

				while ( iterator.hasNext() ){

					PeerInfo info  = iterator.next();

					if (info.ipAddress.equals(reqObject.ipAddressOfSender) &&
							info.portNumber == reqObject.portNumberOfSender	){

						// update zone info 
						// there is a possibility of probable deletion of node 
						// check that too
						info.zoneList = reqObject.zoneInfo;
						Iterator<Zone> zoneiterator = info.zoneList.iterator(); 

						// checking if node is still a neighbor or not
						while ( zoneiterator.hasNext() ){

							Zone zone  = (Zone) zoneiterator.next();
							if ( checkIfNeighborToAnyZone(zone) ){

								// even if one zone matches
								// we update peer information
								info.zoneList  = reqObject.zoneInfo;
								return;
							}
						}

						// if no zone are no more neighbors then remove that
						neighboursList.remove(info);
						return;
					}					
				}				

				//add new peer info
				PeerInfo info = new PeerInfo();
				info.ipAddress = reqObject.ipAddressOfSender;
				info.portNumber = reqObject.portNumberOfSender;
				info.zoneList = reqObject.zoneInfo;
				neighboursList.add( info );

			}else if (reqObject.requestType == Request.REQUEST_TYPE.join){

				System.out.println(" Looking for " + reqObject.pointToBeLookedFor.x
						+ " " + reqObject.pointToBeLookedFor.y + " point");

				System.out.println("Join request processing..");
				System.out.println("Splitting zones..");

				Zone zone = null;
				zone = this.checkForPointInZoneList( reqObject.pointToBeLookedFor );

				if ( null != zone){

					// split zone 
					Zone splittedZone = zone.splitZone( reqObject );

					// send request to requester to update its zone as this
					// TODO: seperate request making
					Request newRequestObject = new Request();
					newRequestObject.requestType = Request.REQUEST_TYPE.joinAck;
					newRequestObject.ipAddressOfSender = _listener.getIPAddress();
					newRequestObject.portNumberOfSender = _listener.getPortNumber();					

					PeerInfo peerToBecontacted  = new PeerInfo();
					peerToBecontacted.ipAddress = reqObject.ipAddressOfSender;
					peerToBecontacted.portNumber = reqObject.portNumberOfSender;

					newRequestObject.zoneInfo.add(splittedZone);
					newRequestObject.peerList = getNeighborsforSplitZone(splittedZone);							
					newRequestObject.route = reqObject.route;
					//finally add this new peer to our neighborList
					// add zone info to it
					// since its a new node it will have only one zone
					peerToBecontacted.zoneList.add( splittedZone );
					this.addNeighbor( peerToBecontacted );

					System.out.println("Sending information to requesting peer..");
					_sender.sendRequest( peerToBecontacted, newRequestObject );

					// start file transfer
					List<String> filesToTransfer = zone.getFilesForSplittedZone(splittedZone);
					transferFiles(peerToBecontacted, filesToTransfer, zone) ;
					
				}else {

					routeToNearestPeer(reqObject);
				}
			}else if (reqObject.requestType == Request.REQUEST_TYPE.search
					|| reqObject.requestType == Request.REQUEST_TYPE.insert){

				System.out.println( "Searching for file or routing ahead" );
				Zone searchZone = checkForPointInZoneList( reqObject.pointToBeLookedFor );
				PeerInfo neighborPeer = null;
				Request searchReq = null;
				
				if ( null != searchZone){

					System.out.println("Found file here");
					//respond back and send file
					neighborPeer = new PeerInfo();
					neighborPeer.ipAddress = reqObject.ipAddressOfSender;
					neighborPeer.portNumber = reqObject.portNumberOfSender;
					neighborPeer.zoneList = zoneList;

					searchReq = new Request(); 
					
					if (reqObject.requestType == Request.REQUEST_TYPE.search){
						
						searchReq.requestType = Request.REQUEST_TYPE.searchAck;
					}else{
						
						searchReq.requestType = Request.REQUEST_TYPE.insertAck;
					}
					
					searchReq.fileName = reqObject.fileName;
					searchReq.ipAddressOfSender = _listener.getIPAddress();
					searchReq.portNumberOfSender = _listener.getPortNumber();
					searchReq.route = reqObject.route;
					_sender.sendRequest(neighborPeer, searchReq);
				}else{

					System.out.println("Sending search request to nearest peer");
					routeToNearestPeer(reqObject);						
				}										

			}else if (reqObject.requestType == Request.REQUEST_TYPE.searchAck){

				System.out.println("File " + reqObject.fileName +" is with Peer "
						+ reqObject.ipAddressOfSender+ " " + reqObject.portNumberOfSender);	
				printRoutingInfo(reqObject);
			}else if ( reqObject.requestType == Request.REQUEST_TYPE.leave ){
				
				handOverZones(reqObject.zoneInfo);
				addNeighborsToList(reqObject.peerList);
				
				if (reqObject.takeOverAsFirstNode){
					
					isFirstNode = true;
				}
				
				PeerInfo temp = new PeerInfo();
				temp.ipAddress = reqObject.ipAddressOfSender;
				temp.portNumber = reqObject.portNumberOfSender;
								
				Request leaveAckReq = new Request();
				leaveAckReq.ipAddressOfSender = _listener.getIPAddress();
				leaveAckReq.portNumberOfSender = _listener.getPortNumber();				
				leaveAckReq.requestType = Request.REQUEST_TYPE.leaveAck;
				Sender leaveAckSender = new Sender();
				leaveAckSender.sendRequest(temp, leaveAckReq);				
				removeNeighbor(reqObject.ipAddressOfSender, reqObject.portNumberOfSender);
				sendNeighborsUpdatedZoneInfo( temp );
			}else if ( reqObject.requestType == Request.REQUEST_TYPE.leaveAck ){
				
				PeerInfo temp = new PeerInfo();
				temp.ipAddress = reqObject.ipAddressOfSender;
				temp.portNumber = reqObject.portNumberOfSender;
				List<String> files = new LinkedList<String>();
				files.addAll(zoneList.get(0).getAllFiles());
				transferFiles(temp, files,zoneList.get(0));				
				zoneList.clear();
				Zone emptyRect = new Zone();
				emptyRect.setRect(0, 0, 0, 0);
				zoneList.add(emptyRect);
				sendNeighborsUpdatedZoneInfo( temp );
				System.exit(0);
			}else if ( reqObject.requestType == Request.REQUEST_TYPE.insertAck ){
				
				// send file to that peer
				System.out.println("Send file " + reqObject.fileName + " to " + reqObject.ipAddressOfSender + ":"
									+ reqObject.portNumberOfSender);
				printRoutingInfo(reqObject);
				PeerInfo peerToRespondTo = new PeerInfo();
				peerToRespondTo.ipAddress = reqObject.ipAddressOfSender;
				peerToRespondTo.portNumber = reqObject.portNumberOfSender;
				FileSender sender = new FileSender( peerToRespondTo ,peerFilesPath+ "//" + reqObject.fileName );
				sender.startTransfer();
			}
		}
	}
	
	/**
	 * Prints routing information
	 * */
	void printRoutingInfo(Request reqObject){
		
		System.out.println("**********************************************************");
		System.out.println("Routing Information: ");
		System.out.println("\t"+ reqObject.route.substring(0, reqObject.route.length()-2));
		System.out.println("**********************************************************");
	}
	
	/**
	 * Transfers a list of files to a peer one by one
	 * */
	void transferFiles(PeerInfo peerToBecontacted,List<String> filesToTransfer,
			Zone zoneToBeTransferredFrom){

		Iterator<String> filesIterator = filesToTransfer.iterator();
		
		while( filesIterator.hasNext() ){
			
			String fileName = filesIterator.next();
			FileSender sender = new FileSender( peerToBecontacted ,peerFilesPath + "//" + fileName );
			sender.startTransfer();
			zoneToBeTransferredFrom.removeFile(fileName);
		}
	}
	
	/**
	 * Checks if file's coordinates lies in zone
	 * and adds it to the zone
	 * */
	void addFileToZone(String fileName){
		
		int x = HashCalculator.calculateXHash(fileName);
		int y = HashCalculator.calculateYHash(fileName);
		Point2D.Double point  = new Point2D.Double(x,y);
		Zone zone = checkForPointInZoneList(point);
		
		if (null!= zone){
			
			zone.addFile(fileName);
		}
	}
	
	/**
	 * Insert File into DHT 
	 * It finds the Hash of the file,tries to locates point
	 * in current peer , if not forwards request. 
	 * 
	 * */	
	void insertFile ( String filepath ){
		
		String filename = filepath.substring(filepath.lastIndexOf("/")+1, filepath.length());
		int x = HashCalculator.calculateXHash(filename);
		int y = HashCalculator.calculateYHash(filename);
		
		Point2D.Double point  = new Point2D.Double(x,y);
		System.out.println("File mapped to point " + point);
		// check first if point lies here
		// else
		// find point in hash table 
		// after getting acknowledgment send file there
		
		Zone zone  = checkForPointInZoneList(point);
		
		if (null != zone){
			
			System.out.println("Keeping file here");
			zone.addFile(filename);
			
			// copy files to received files
			// temp arrangement
			// no need to send file anywhere
		}else{
			
			System.out.println("Searching for point in system");
			Request insertRequest = new Request();
			insertRequest.ipAddressOfSender = _listener.getIPAddress();
			insertRequest.portNumberOfSender = _listener.getPortNumber();
			insertRequest.requestType = Request.REQUEST_TYPE.insert;
			insertRequest.pointToBeLookedFor = point;
			insertRequest.fileName = filename;
			
			PeerInfo neighbor = findNearestPeer( point );
			Sender localSender = new Sender();
			localSender.sendRequest(neighbor, insertRequest);
		}		
	}
	
	/**
	 * Remove neighbor with ipAdress and portnumber
	 * */
	void removeNeighbor (String ipAddress, int portNumber){
		
		Iterator<PeerInfo> peersIterator = neighboursList.iterator();
		
		while ( peersIterator.hasNext() ){
			
			PeerInfo info = peersIterator.next();
			if (info.ipAddress.equals( ipAddress ) && 
				 info.portNumber == portNumber ){
				
				peersIterator.remove();
			}
		}
	}
	
	/**
	 * Used for Leave
	 * Adds zone passed to one of its zone else
	 * just handles it seperately
	 * */
	void handOverZones(List<Zone> zoneToHandOver){
		
		// check which zone adds up to which zone 
		// temporarily just considering first zones
		Zone zoneToMerge = zoneToHandOver.get(0);
		Zone primaryZone = zoneList.get(0);
		
		if (primaryZone.properShapeAfterAddition(zoneToMerge)){
			
			primaryZone.addZones(zoneToMerge);
		}else{
			
			zoneList.add(zoneToMerge);
		}		
	}

	/**
	 * Adds peerinfo objects to neighborslist 
	 * If peer already is present it ignores
	 */
	void addNeighborsToList(List<PeerInfo> newNeighbors){
		
		// appending all nodes
		// check for duplicates  				
		Iterator<PeerInfo> newPeersIterator = newNeighbors.iterator();
				
		while( newPeersIterator.hasNext() ){
			
			Iterator<PeerInfo> peersIterator = neighboursList.iterator();
			PeerInfo info = (PeerInfo) newPeersIterator.next();
			boolean alreadyExists = false;
			while ( peersIterator.hasNext() ){
				
				PeerInfo peerToBeComparedWith = (PeerInfo)peersIterator.next();
				if ( (info.ipAddress.equals( peerToBeComparedWith.ipAddress )
						&& info.portNumber == peerToBeComparedWith.portNumber) || 
						(_listener.getIPAddress().equals( info.ipAddress )
								&& _listener.getPortNumber() == info.portNumber )){
					
					alreadyExists = true;
				}				
			}
			
			if (!alreadyExists){
				
				neighboursList.add( info );
			}
		}
				
	}
	
	/**
	 * It removes the peerinfo objects from neighborsList 
	 * if they are no more neighbors of the zones that this peer has
	 * 
	 * */
	void removePeersfromNeigbors(){

		synchronized (neighboursList) {

			Iterator<PeerInfo> iterator = neighboursList.iterator(); 

			while ( iterator.hasNext() ){

				PeerInfo info  = iterator.next();
				Iterator<Zone> zoneIterator = info.zoneList.iterator();
				boolean stillNeighbor = false;

				while (zoneIterator.hasNext()){

					Zone zone =  zoneIterator.next();
					if ( checkIfNeighborToAnyZone(zone) ){

						stillNeighbor = true;
					}

				}

				if (!stillNeighbor){

					iterator.remove();
				}
			}	
		}
	}

	/**
	 * Function to check whether given zone is neighbor of 
	 * any zone that current peer contains
	 * */
	boolean checkIfNeighborToAnyZone( Zone zone ){

		Iterator<Zone> iterator = zoneList.iterator();

		while(iterator.hasNext()){

			Zone thisZone = (Zone) iterator.next();
			if (thisZone.isNeighbor(zone)){

				return true;
			}

		}

		return false;
	}

	/**
	 * Routes the request to neighbor which is closest to or which 
	 * contains the point mentioned in request object
	 * 
	 * */
	void routeToNearestPeer (Request reqObject ){

		PeerInfo nearestPeer = findNearestPeer(reqObject.pointToBeLookedFor);

		if (null != nearestPeer){

			System.out.println("Routing to " + nearestPeer.ipAddress + " " + nearestPeer.portNumber);	
			_sender.sendRequest(nearestPeer, reqObject);
		}

	}

	/**
	 * Check if point under consideration lies within the zone
	 * that current peer has
	 * 
	 * */
	Zone checkForPointInZoneList ( Point2D pointToBeLookedFor){

		System.out.println("Checking in which zone point lies..");
		
		synchronized (zoneList) {

			Iterator<Zone> iterator = zoneList.iterator(); 
			
			while ( iterator.hasNext() ){

				Zone zone  = (Zone)iterator.next();
				if (zone.contains ( pointToBeLookedFor) ){

					return zone;
				}
			}
		}
		
		return null;
	}

	/**
	 * For a point under consideration it finds which is the neighbor
	 * which has a zone which is closest to the point or contains 
	 * that point 
	 * */
	PeerInfo findNearestPeer (Point2D.Double pointToBeLookedFor ){

		System.out.println("Finding nearest peer for point" + pointToBeLookedFor );
		Iterator<PeerInfo> iterator = neighboursList.iterator(); 
		PeerInfo nearestPeer = null ;
		double minDistance = 100000;
		
		while ( iterator.hasNext() ){

			PeerInfo info  = iterator.next();
			Iterator<Zone> zoneIterator = info.zoneList.iterator(); 			
			while ( zoneIterator.hasNext() ){

				Zone zone  = (Zone)zoneIterator.next();
				double outcode = zone.findDistanceFromPoint ( pointToBeLookedFor);
				System.out.println("Distance of peer " + info.ipAddress
						+ " " + info.portNumber + " from point is " + outcode );					
				if ( outcode < minDistance ){
					
					minDistance = outcode;
					nearestPeer = info;
				}
			}

			// go through all zones of that peer and check for point
		}

		System.out.println("Choosing of peer " + nearestPeer.ipAddress
						+ " " + nearestPeer.portNumber);
		return nearestPeer;
	}

	/**
	 * Iterates over list of neighbors and sends them update
	 * message. It excludes the neighbor which is passed in as parameter
	 *	
	 * */
	void sendNeighborsUpdatedZoneInfo( PeerInfo nodeToExclude ){

		synchronized ( neighboursList ) {

			Iterator<PeerInfo> iterator = neighboursList.iterator();

			while( iterator.hasNext() ){

				PeerInfo neighborInfo = iterator.next();

				if ( null!= nodeToExclude && 
						neighborInfo.ipAddress.equals(nodeToExclude.ipAddress) 
						&& neighborInfo.portNumber == nodeToExclude.portNumber){

					continue;
				}

				Request reqObject = new Request();
				reqObject.requestType = Request.REQUEST_TYPE.update;
				reqObject.ipAddressOfSender = _listener.getIPAddress();
				reqObject.portNumberOfSender = _listener.getPortNumber();
				reqObject.zoneInfo = zoneList;
				Sender localSender = new Sender();
				localSender.sendRequest(neighborInfo, reqObject);	

			}
		}
	}

	/**
	 * Function checks which neighbors has zones which are neighbor to
	 * the splitted zone and returns the list 
	 * 
	 * */
	public List<PeerInfo> getNeighborsforSplitZone( Zone newZone ){

		List<PeerInfo> list = new LinkedList<PeerInfo>();
		Iterator<PeerInfo> iterator = neighboursList.iterator();

		while( iterator.hasNext() ){

			PeerInfo neighborInfo = iterator.next();

			// TODO: implementing for primary zone only then
			// will see for multiple zones		
			Iterator<Zone> neighborZoneList = neighborInfo.zoneList.iterator();
			
			while ( neighborZoneList.hasNext() ){
				
				Zone neighborZone = (Zone) neighborZoneList.next();

				if (newZone.isNeighbor(neighborZone)){

					list.add( neighborInfo );
				}
			}
		}

		// add self as neighbor to new zone 
		list.add( getSelfInfo() );
		return list;
	}

	// function to create PeerInfo object for current peer
	PeerInfo getSelfInfo(){

		PeerInfo selfInfo = new PeerInfo();
		selfInfo.ipAddress = _listener.getIPAddress();
		selfInfo.portNumber = _listener.getPortNumber();
		selfInfo.zoneList = this.zoneList;
		selfInfo.isBootStrap = false;
		return selfInfo;
	}

	// change neighbor list to given listOfNeighbours
	void setNeighbors( List<PeerInfo> listOfNeighbours){

		neighboursList = listOfNeighbours;
	}

	// add a neighbor to neighbor list
	void addNeighbor ( PeerInfo peer){

		neighboursList.add(peer);
	}

	// unimplemented
	void removeNeighbor (  PeerInfo peer ){

		// TODO:
		// iterate over the list and remove the neighbour
		//neighboursList.remove()
	}


	/**
	 * Update Neighbor information
	 * When update signal is received from a peer 
	 * it searches for that peer in the neighbor list 
	 * and updates its information.
	 * */ 
	void updateNeighborInfo (PeerInfo peerInfo) {

		// find that peer in the neighbor list 
		Iterator<PeerInfo> iterator = neighboursList.iterator();

		while( iterator.hasNext() ){

			PeerInfo neighborInfo = iterator.next();

			if ( neighborInfo.ipAddress.equals( peerInfo.ipAddress) &&
					neighborInfo.portNumber == peerInfo.portNumber){

				neighborInfo.zoneList = peerInfo.zoneList;
			}
		}
	}

	
	/**
	 * Function to start leave procedure
	 * 
	 * It selects neighbor which can form proper shape with its zone
	 * if no neighbor is able to geive proper shape, it selects neighbor
	 * which has smallest zone
	 * 
	 * It checks if number of zones that peer contains is more
	 * than 1, and cancels leave.
	 * 
	 * 
	 * */ 
	void startLeaveProcedure (){
		
		if (this.zoneList.size() > 1){
			
			System.out.println("ERROR: Cant Leave... Peer has multiple zones");
			return;
		}
		
		// creating request object
		Zone leavingZone = this.zoneList.get(0);
		Sender localSender = new Sender();
		Request leaveRequest = new Request();
		leaveRequest.requestType = Request.REQUEST_TYPE.leave;
		leaveRequest.ipAddressOfSender = _listener.getIPAddress();
		leaveRequest.portNumberOfSender = _listener.getPortNumber();
		leaveRequest.zoneInfo = this.zoneList;
		leaveRequest.peerList = neighboursList;
		Sender requestSenderForBootstrap = null;
		Request bootStrapUpdateRequest = null;
		
		if (isFirstNode){
			
			leaveRequest.takeOverAsFirstNode = true;
			// let bootstrap know about change of start peer				
			requestSenderForBootstrap = new Sender();
			bootStrapUpdateRequest = new Request();
			bootStrapUpdateRequest.requestType = Request.REQUEST_TYPE.update;			
			bootStrapUpdateRequest.ipAddressOfSender = _listener.getIPAddress();
			bootStrapUpdateRequest.portNumberOfSender = _listener.getPortNumber();
			bootStrapUpdateRequest.peerList = new LinkedList<PeerInfo>();				
		}

		// go through neighbors List and check which neighbor can take 
		// over the zone and send request 
		
		// TODO: currently implementing for primary zone only
		// find smallest zone on the way
		PeerInfo smallestZonePeer = null;
		double minArea = 100000;
		Iterator<PeerInfo> iterator = neighboursList.iterator();		
		while( iterator.hasNext() ){

			PeerInfo neighborInfo = iterator.next();

			// TODO: implementing for primary zone only then
			// will see for multiple zones			
			Zone neighborZone = (Zone) neighborInfo.zoneList.get(0);
			double area = neighborZone.getArea();

			if ( area < minArea){
				
				minArea = area;
				smallestZonePeer = neighborInfo;
			}
			
			if (leavingZone.properShapeAfterAddition(neighborZone)){
				
				// give control of leavingZone zone to this neighbor
				localSender.sendRequest(neighborInfo, leaveRequest);
				if (isFirstNode){
					
					bootStrapUpdateRequest.peerList.add(neighborInfo);
					requestSenderForBootstrap.sendRequest(bootStrapServerInfo, bootStrapUpdateRequest);
				}
				return;
			}			
		}
		
		// if reached here that means no neighbor forms proper shape after addition 
		// just give control to neighbor which is less loaded 
		localSender.sendRequest(smallestZonePeer, leaveRequest);
		
		if (isFirstNode){
			
			bootStrapUpdateRequest.peerList.add(smallestZonePeer);
			requestSenderForBootstrap.sendRequest(bootStrapServerInfo, bootStrapUpdateRequest);
		}
	}
		
	// applies only for first peer convinience method
	void updateBootStrap( PeerInfo peerInfo){

		bootStrapServerInfo = peerInfo;
	}

	/**
	 * The main function 
	 * Please refer ReadMe on using command line
	 * 
	 * */
	public static void main (String[] args){

		Peer peer = null;
		BootStrapServer bootstrap = null;
		// check if we want to initialize bootstrap or 
		// if we want to initialize normal peer
		if (args.length > 0 && args[0].equals( "-p" )){

			// if we are initializing bootstrap it needs at least one 
			// peer already initialized , and info of that peer
			bootstrap = new BootStrapServer();

			//parse arguments
			bootstrap.initialize ( parseArguments(args) );
			System.out.println( "Bootstrap initialized" );
		}else{

			peer = new Peer();

			// parse arguments
			// and send info about bootstrapServer in any 
			// parse arguments
			peer.initialize( parseArguments(args) );
			System.out.println( "Peer initialized" );
		}

		Scanner in = new Scanner (System.in);

		while ( true ){

			String command = null ;

			try{

				command  = in.next();
				if (command.toLowerCase().equals("view") ){

					if (null != peer){

						peer.printInfo();
					}else{

						bootstrap.printInfo();
					}

				}else if (command.toLowerCase().equals( "bootstrap" ) ){

					PeerInfo info = new PeerInfo(); 
					String ip  = in.next();
					String port  = in.next();
					info.ipAddress = ip;
					info.portNumber = Integer.parseInt(port);
					info.isBootStrap = true;
					peer.bootStrapServerInfo = info;
				}else if (command.toLowerCase().equals( "exit" ) ){

					break;
				}else if ( Request.REQUEST_TYPE.valueOf(command) == 
						Request.REQUEST_TYPE.join){

					if (!peer.isJoined && !isFirstNode ){
						
						System.out.println("Joining..");
						peer.join();
					}else{
						
						System.out.println("Already Joined (or its the first node or bootstrap)");
					}

				}else if ( Request.REQUEST_TYPE.valueOf(command) == 
						Request.REQUEST_TYPE.leave){

					peer.startLeaveProcedure();
				}else if ( Request.REQUEST_TYPE.valueOf(command) == 
						Request.REQUEST_TYPE.search){

					// take filename as input from user
					String filename = in.next();										
					peer.search (filename);
				}else if ( Request.REQUEST_TYPE.valueOf(command) == 
						Request.REQUEST_TYPE.insert){
					
					System.out.println("Inserting file into DHT...");
					String filename = in.next();
					
					if (! (new File( peer.peerFilesPath +"//"+ filename).exists()))
					{
					   throw new FileNotFoundException("File not Found!");
					}
					
					peer.insertFile(filename);
					
				}else {

					System.out.println("No matching Command");
				}

			}catch( FileNotFoundException e){
				
				System.out.println("File not found");
				e.printStackTrace();
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
	static PeerInfo parseArguments( String[] args ) {

		PeerInfo info = new PeerInfo();

		try{
			
			for (int i = 0 ; i < args.length ; ++i){

				if ( args[i].equals( "-p" ) || args[i].equals("-b") ){

					info.isBootStrap = args[i].equals( "-b" );
					info.ipAddress = args[++i];
					info.portNumber = Integer.parseInt ( args[++i] );				
				}			
				else if ( args[i].equals( "-f" )){

					isFirstNode = true; 
				}
			}
		}catch ( Exception e) {

			System.out.println("Invalid Input Could not start peer");
			System.exit(0);
		}
		return info; 
	}
}
