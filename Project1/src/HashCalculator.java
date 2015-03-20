/*
 * HashCalculator.java
 *
 * Version:
 *     1.0
 *
 * Revisions:
 *     1.0
 *
 */

/**
 *
 * Class provides convinience functions for calculting 
 * xHash and Yhash
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */

public class HashCalculator {

	static int calculateXHash( String fileName ){
		
		int length = fileName.length();		
		int sum = 0;
		
		for ( int i = 0 ; i < length ; i+=2){
			
			sum += fileName.charAt(i);
		}
		
		return sum%10;
	}
	
	static int calculateYHash( String fileName ){
		
		int length = fileName.length();
		int sum = 0;
				
		for ( int i = 1 ; i < length ; i+=2){
			
			sum += fileName.charAt(i);
		}
		
		return sum%10;
	}

}
