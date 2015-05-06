import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler implements Runnable{
	
	String urlToCrawl;
	Peer parent; 
	
	void crawl( String urlToCrawl ){
		
		System.out.println("Now crawling: " + urlToCrawl);
		this.urlToCrawl = urlToCrawl;
		Thread thread = new Thread( this );
		thread.start();
//		try {
//			thread.join();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

/*	@Override
	public void run() {

		String folderName = null;		
		try {
						
			folderName = InetAddress.getLocalHost().getHostAddress().replace('.', '_');
			String filename = "URLQueue";
			File file1 = new File(folderName);
			if(!file1.exists())
			{
				file1.mkdir();
			}
			
			File file = new File(folderName+"/"+filename);
			
			if(!file.exists())
			{
			    file.createNewFile();
		    }
			FileWriter fileWritter = new FileWriter(file,true);
	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			Connection urlConnection = Jsoup.connect(urlToCrawl);
			urlConnection.userAgent( Peer.userAgent );
			//System.out.println(c1.execute().statusCode());
			Document urlDocument = urlConnection.get();
			//System.out.println(document);
			Elements urlLinks = urlDocument.select("a");
			for (Element link : urlLinks) 
			{
			    String url1 = link.absUrl("href");
			    if ( url1.equals("") )
			    	continue;
			    else
			    {
			    	bufferWritter.write( url1 + "\n");
			    }
			}
			bufferWritter.close();
			
			if ( parent != null){
				
				parent.doneCrawling( urlToCrawl );
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}*/	
	
	@Override
	public void run() {
		
		Connection urlConnection = Jsoup.connect(urlToCrawl);
		urlConnection.userAgent( Peer.userAgent );
		//System.out.println(c1.execute().statusCode());
		Document urlDocument = null;
		try {
			urlDocument = urlConnection.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(document);
		Elements urlLinks = urlDocument.select("a");
		URLInsertQueue queue = URLInsertQueue.getInstance();
		for (Element link : urlLinks) 
		{
		    String url1 = link.absUrl("href");
		    
		    if ( url1 == null || url1.equals("") ){
		    	continue;
		    }
		    else{
		    	
		    	queue.insertUrl(url1);
		    }
		}
		
		System.out.println("Extracting urls from crawled " +  urlToCrawl) ;
		
		if ( parent != null){
			
			System.out.println("Crawler: Letting peer know I am done");
			parent.doneCrawling( urlToCrawl );
		}
	}
	
	
	public static void main ( String args[] ){
		
		Crawler crwl = new Crawler();
		crwl.crawl("http://www.rit.edu");
	}
}
