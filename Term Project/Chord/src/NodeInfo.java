/*
 * NodeInfo.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 * Data structure to store information about other nodes
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */

import java.io.Serializable;

public class NodeInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String ipAddress; 
	int portNumber; 
	Node.NodeID nodeID;
	boolean isBootStrap;
	Zone zone;
	String successorIpAddress; 
	int successorPortNumber; 
}
