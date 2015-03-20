
public class BootStrapServer extends Peer {

    PeerInfo _knownPeer;
    Peer bootStrapPeer;
    
//	void initializeBootStrap( PeerInfo knownPeer){
//		
//		_knownPeer = knownPeer;
//		bootStrapPeer = new Peer();
//		System.out.println("Bootstrap initialize");
//		bootStrapPeer.initialize(null);
//		
//	}
	
    
    void initialize ( PeerInfo knownPeer ){

		_knownPeer = knownPeer;
		System.out.println("Bootstrap initializing..");
		super.initialize( null );
    }
    
    @Override
	void printInfo(){
		
    	System.out.println( "" );
    	System.out.println("**********************************************************");    	
		System.out.println( "This is a bootstrap server" );
		System.out.println( "My address " + Listener.getInstance().getIPAddress() + 
				":" + Listener.getInstance().getPortNumber());
		System.out.println( "Peer I know is " + _knownPeer.ipAddress + ":" 
				+ _knownPeer.portNumber);
		System.out.println("**********************************************************");		
	}
    
    @Override	
	void processIncomingRequest( Request reqObject ){
		
    	if (reqObject.requestType == Request.REQUEST_TYPE.update){
    		
    		_knownPeer = reqObject.peerList.get(0);
    		return;
    	}
    	
		synchronized (_knownPeer) {
			
			_sender.sendRequest(_knownPeer, reqObject);
		}		
	}
}
