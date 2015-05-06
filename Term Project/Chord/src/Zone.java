import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Zone implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final long DOMAIN_THROTTLING_DELAY = 10000; 
	int firstId = 0;
	int lastId = 0 ;
	PriorityQueue<DHTDomain> domainQueue;
	
	public Zone( int firstId, int lastId ) {
		
		this.firstId = firstId;
		this.lastId = lastId;
		domainQueue = new PriorityQueue<DHTDomain>();
	}

	/**
	 * Splits the current zone in half
	 * 
	 * @return zone splittedZone
	 */
	Zone splitZone(int fromID){
				
		Iterator<DHTDomain> urlIterator = domainQueue.iterator();
//		int newFirstId = (firstId+lastId)/2 + 1 ;
//	    int newLastId =  lastId;
//	    this.lastId = (firstId+lastId)/2;
	    
		int newFirstId = fromID ;
	    int newLastId =  lastId;
	    this.lastId = fromID-1;

		Zone zone = new Zone( newFirstId, newLastId );
		
		while ( urlIterator.hasNext() ){
			
			DHTDomain url = urlIterator.next();
			
			// if new zone maps contains url id add it to 
			// that zone and remove from here
			
			if (zone.contains( url.id )){
				
				zone.domainQueue.add(url);
				this.domainQueue.remove(url);
			}
		}
		
		return zone;
	}
		
	/**
	 * Function to check whether id belongs to zone
	 * 
	 * @return boolean value indicating if id is in this zone
	 */
	public boolean contains ( int idTocheck){
		
		if ( idTocheck >= this.firstId && idTocheck <= this.lastId ){
			
			return true;
		} 
		
		return false;
	} 
	
	/**
	 * Method to get top of the heap 
	 */
	synchronized String getNextUrl(){
				
		DHTDomain  domain =  domainQueue.poll();
		
		if (domain == null){
			
			return null;
		}
				
		long diff = System.currentTimeMillis() - domain.timestamp;
		
		if ( diff  < DOMAIN_THROTTLING_DELAY){
			
			try {
				
				System.out.println("Waiting till DOMAIN_THROTTLING_DELAY: " + diff);
				wait(diff);
				System.out.println("Done Waiting till DOMAIN_THROTTLING_DELAY");
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
		
		String url = domain.extractNextUrl();	
		System.out.println("Returning url to be crawled next: " + url);
		domainQueue.offer(domain); // domain back to queue with updated timestamp
		return url;
	}
	
	
	/**
	 * Add domain to priority queue
	 * @param	 url	url to be added
	 */
	synchronized void addUrl (  String url ){		
		
		System.out.println("Adding url to domain" + url);
		if ( url == null ){
			
			return;
		}
		
		String domain = extractDomain(url);
		DHTDomain newDomain = new DHTDomain( domain );
		// check if domain already there 
		// if not then only insert
		Iterator<DHTDomain> iterator =  domainQueue.iterator();
		
		while ( iterator.hasNext() ){
			
			DHTDomain nextDomain = iterator.next();			
			if ( nextDomain.domainName.equals( domain )){
				
				System.out.println("Inserting url into domain " + domain);					
				nextDomain.addUrl(url);
				return;
			}
		}

		newDomain.addUrl(url);
		domainQueue.offer(newDomain);
	}
	
	/**
	 * 
	 * @param url
	 * @return	domain string 
	 */
	String extractDomain ( String url ){
		
		URL urlObj;
		try {
			
			urlObj = new URL(url);
		} catch (MalformedURLException e) {
			
			System.out.println("Can not add url, it is malformed");
			return null;
		}
		
		return urlObj.getHost();
	}
	
	long getTotalUrlCount(){
		
		long count = 0;
		Iterator<DHTDomain> urlIterator = domainQueue.iterator();
		
		while( urlIterator.hasNext() ){
			
			count += urlIterator.next().getUrlCount();
		}
		
		return count;
	}
	
	void printAllDomains(){
		
		Iterator<DHTDomain> urlIterator = domainQueue.iterator();
		
		while( urlIterator.hasNext() ){
			
			System.out.println("	" + urlIterator.next().domainName );
		}
				
	}
	
	long getSeenUrlCount(){
		
		long count = 0;
		Iterator<DHTDomain> urlIterator = domainQueue.iterator();
		
		while( urlIterator.hasNext() ){
			
			count += urlIterator.next().getSeenUrlsCount();
		}
		
		return count;

	}
	
	void mergeZones( Zone zoneToBeMerged ){
		
		if (zoneToBeMerged.firstId > this.lastId ){
			
			lastId = zoneToBeMerged.lastId;
		}else{
			
			firstId = zoneToBeMerged.firstId;
		}

		Iterator<DHTDomain> urlIterator = zoneToBeMerged.domainQueue.iterator();
		
		while( urlIterator.hasNext() ){
			
			domainQueue.offer( urlIterator.next() );
		}		
	}

	synchronized void addURLCrawled( String crawledUrl){
		
		System.out.println("Searching domain to add url to seen ");
		String domain = extractDomain( crawledUrl );

		// check if domain already there 
		// if not then only insert
		Iterator<DHTDomain> iterator =  domainQueue.iterator();
		
		while ( iterator.hasNext() ){
			
			DHTDomain nextDomain = iterator.next();	
			
			if ( nextDomain.domainName.equals( domain )){
								
				nextDomain.addToSeen(crawledUrl);
				return;
			}
		}
	
	}
}

