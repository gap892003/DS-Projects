import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCalculator {

	static int calculateHash ( String url ){
		
		try {
			
			MessageDigest md = MessageDigest.getInstance( "SHA-1" );
			md.update( url.getBytes() );
			byte[] sha1hash = md.digest();
			sha1hash[0] = 0;
			BigInteger bigInt = new BigInteger( sha1hash ) ;			
			return ( bigInt.mod(new BigInteger("8")) ).intValue();
			
		} catch (NoSuchAlgorithmException e) {

			e.printStackTrace();
		}	
		
		return -1;
	}
	
	static int calculateHashSimple( String url ){
		
		int length = url.length();		
		int sum = 0;
		
		for ( int i = 0 ; i < length ; i+=2){
			
			sum += url.charAt(i);			
		}

		return sum%8;
	}
	
	
//	public static void main(String args[]){
//		
//		System.out.println( calculateHash( "129.21.37.35" ));
//	}
}
