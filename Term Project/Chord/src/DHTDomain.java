import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DHTDomain implements Serializable,Comparable<DHTDomain>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6039903319864047244L;
	String domainName;
	long timestamp;
	int id;
	HashMap<String, Boolean> seenUrls;
	List<String> urlList;
//	List<String> seenUrls;
	
	long crawlDelay = 1000;// default 
	
	DHTDomain( String domainName ){
		
		this.domainName = domainName;
		timestamp = 10000; // some random big value
		this.id = HashCalculator.calculateHash(domainName);
		urlList = new ArrayList<String>();
		seenUrls = new HashMap<String, Boolean>();
	}
	
	synchronized void addUrl( String url){
		
		Boolean val = seenUrls.get( url );
		System.out.println("Checking if url already seen: " + val );
		
		if (  val ==  null ){
			
			urlList.add(url);
		}
	}	
	
	/**
	 * 
	 * @return
	 */
	synchronized String extractNextUrl(){
		
		if ( urlList.size() > 0){
			
			String url = urlList.get(0);
			urlList.remove(0);
			// update timeStamp 
			timestamp = System.currentTimeMillis();
			return url;
		}		
		
		return null;
	}	
	
	long getUrlCount(){
		
		return urlList.size();
	}

	@Override
	public int compareTo(DHTDomain domain) {

		if (this.timestamp == domain.timestamp){
			
			return 0;
		}else if ( this.timestamp > domain.timestamp ){
			
			return -1;
		}else if ( this.timestamp < domain.timestamp ){
			
			return 1;
		}
		return 0;
	}	
	
	synchronized void addToSeen( String urlSeen ){
		
		System.out.println("Adding url"+ urlSeen + " to seen of doamin: " + this.domainName );
		seenUrls.put(urlSeen, true);
	}
	
	long getSeenUrlsCount ( ){
		
		return seenUrls.size();
	}
}
