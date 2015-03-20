import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


public class PeerInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String ipAddress; 
	int portNumber; 
	List<Zone> zoneList;
	String nodeID;	
	boolean isBootStrap;
	
	PeerInfo(){
		
		zoneList = new LinkedList<Zone>();
	}
}
