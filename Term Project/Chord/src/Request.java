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
import java.util.List;

public class Request implements Serializable{

	
	private static final long serialVersionUID = 1L;
	
	public enum REQUEST_TYPE{

		Join, 
		JoinAck,
		Update,
		UpdateAck,
		UpdateSuccessor,
		UpdatePredecessor,
		Leave,
		LeaveAck,
		TakeOverUpdate,
		Insert;
	}
	
	NodeInfo senderInfo;
	NodeInfo receiverInfo;	
	REQUEST_TYPE requestType;
	int randomNumberForjoining;
	Zone zoneInfo; // this is required for join, update , leave 
	NodeInfo successor;
	NodeInfo predecessor;
	String urlToBeInserted;
	List<String> successorURLQBackup;
}
