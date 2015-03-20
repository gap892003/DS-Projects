/*
 * Request.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * Serializable request object to pass around the peers 
 * It also defines enum to indicate type of request
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */


import java.io.Serializable;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class Request implements Serializable{

	
	private static final long serialVersionUID = 1L;
	
	// enum to indicate type of request
	public enum REQUEST_TYPE{
		
		join,
		insert, 	
		leave,
		search,
		update,
		fileTxInitiate,
		joinAck, 
		joinSuccess,
		searchAck,
		insertAck,
		leaveAck;
	}

	REQUEST_TYPE requestType;
	Point2D.Double pointToBeLookedFor;
	String fileName;
	String ipAddressOfSender;
	int portNumberOfSender;
	List<Zone> zoneInfo; // used for join request to convey its zone to peer
	List<PeerInfo> peerList; // used only for join or update request 
				  // to give more information about neighbors
	boolean takeOverAsFirstNode = false;		
	String route;
	
	Request(){
		
		zoneInfo = new LinkedList<Zone>();
		route = "";
	}
		
}
