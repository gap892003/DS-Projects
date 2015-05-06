import java.io.Serializable;

public class PeerInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String ipAddress; 
	int portNumber; 
	String nodeID;	
	boolean isBootStrap;
	PeerInfo successor;
	PeerInfo predecessor;
	Zone zone;
}
