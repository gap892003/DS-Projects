
public class RequestFactory {

	
	static Request getRequest( Request.REQUEST_TYPE type){
		
		Request reqObject = new Request();
		reqObject.requestType = type;
		Listener listener = Listener.getInstance();
		reqObject.ipAddressOfSender = listener.getIPAddress();
		reqObject.portNumberOfSender = listener.getPortNumber();
				
		switch ( type ) {
		
		case join:
			
			break;
		case joinAck:
			
			break;
		case insert:

			break;
		case leave:

			break;
		case search:

			break;
		case update:
			
			break;
		case searchAck:
			
			break;
		case leaveAck:
			
			break;
		default:
			break;		
		}	
		
		return null;
	}
}
