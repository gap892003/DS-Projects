import java.util.ArrayList;
import java.util.List;


public class URLInsertQueue {

	private static URLInsertQueue instance = null; 
	private  ArrayList<String> urlQueue = null;
	
	protected URLInsertQueue(){
		
		// no need to implement 
	}
	
	public static URLInsertQueue getInstance ( ) {
		
		if ( instance == null ){
			
			instance = new URLInsertQueue();
			instance.initialize();
		}
		
		return instance;				
	}	
	
	void initialize(){
		
		urlQueue = new ArrayList<String>();

	}
	
	synchronized String getNextUrl(){
		
		String url = null;
		
		if (urlQueue.size() > 0){
			
			url = urlQueue.get(0);
			urlQueue.remove(0);
		}
		
		return url;
	}
	
	synchronized void insertUrl( String url ){
		
		urlQueue.add(url);
	}
	
	List<String> getBackp(){
		
		return urlQueue;
	}
	
	synchronized void mergeList( List<String> listToBeMerged){
		
		urlQueue.addAll(listToBeMerged);
	}
}
