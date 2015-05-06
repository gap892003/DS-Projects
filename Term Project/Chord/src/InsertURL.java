import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;

public class InsertURL implements Runnable{

	Peer parent = null;

	InsertURL ( Peer _parent ) {

		parent = _parent;
	}
	
	void insertURLInDHT(){
		
		URLInsertQueue queue = URLInsertQueue.getInstance();
		String url = queue.getNextUrl();
		
		if (url != null){
			
			System.out.println("Entering url " + url + " in DHT");
			parent.insert(url);
		}
	}

//	void insertURLInDHT(){
//		
//		String folderName = null;
//
//		try {
//
//			folderName = InetAddress.getLocalHost().getHostAddress().replace(".", "_");
//			String fileName = "URLQueue";
//			File file1 = new File(folderName);
//
//			if(!file1.exists()){
//
//				System.out.println("No such directory");
//			}
//
//			File file = new File(folderName+"/"+fileName+ ".txt");
//			
//			if(!file.exists()){
//
//				System.out.println("No such file found");
//			}
//
//			BufferedReader br = new BufferedReader(new FileReader(file));
//			String line = br.readLine();
//			
//			if (line == null){
//				
//				br.close();
//				return;
//			}
//			
//			System.out.println("Entering url " + line + " in DHT");
//			this.removeEntry();
//			//parent.insert(line);
//			br.close();
//
//		} catch (UnknownHostException e) {
//
//			e.printStackTrace();
//		} catch (FileNotFoundException e) {
//
//			e.printStackTrace();
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		}
//	}

	public void removeEntry() throws IOException
	{
//		System.out.println("Removing entry");
//		String folderName = InetAddress.getLocalHost().getHostAddress().replace('.', '_');;
//		String fileName = "URLQueue.txt";
//		String file =  folderName+"/"+fileName;
//		System.out.println(file);
//		String command  = "sed -i _bak \'1d\' " + file;
//		System.out.println(command);
//		Runtime.getRuntime().exec(command);
//		Process prc = new ProcessBuilder(command).start();
//		System.out.println(prc.exitValue());
		
		
		String folderName = InetAddress.getLocalHost().getHostAddress().replace('.', '_');;
		String fileName = "URLQueue.txt";
		String file =  folderName+"/"+fileName;
		File file1 = new File(file);
		BufferedWriter br = new BufferedWriter(new FileWriter(file1));
		br.close();
	}

	@Override
	public void run() {

		while ( true ) {

			try {
				synchronized (this) {
					
					wait(1000);
					insertURLInDHT();					
				}
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
	}
	
	void start(){
		
		Thread thread = new Thread( this );
		thread.start();
	}
	
	public static void main ( String args[] ){
		
		InsertURL module = new InsertURL(null);
		module.start();
	}
}
