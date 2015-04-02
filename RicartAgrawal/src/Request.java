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
 * Serializable request object. It contains information 
 * about whom to send request, which Node sending, 
 * request type and additional information according to  
 * that request type
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */

import java.io.Serializable;
import java.util.List;

public class Request implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public enum REQUEST_TYPE{
		
		AccessCS,
		AccessCSAck,
		EnterCS,
		ProcessingDone,
		Join, 
		JoinAck,
		Update,
	}
	
	String ipAddressOfSender;
	int portNumberOfSender;
	String ipAddressofReceiver;
	int nodeIDOfSender;
	int nodeIDOfReceiver;
	int portNumberOfReceiver;	
	REQUEST_TYPE requestType;
	List<NodeInfo> knownNodesList;
	long timeStamp;
}
