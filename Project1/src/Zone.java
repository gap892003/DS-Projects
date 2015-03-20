/*
 * Zone.java
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
 * Zone class is a subclass of Rectangle class and provides
 * functionality to split zones, identify neighbors. 
 * It also stores file information about zone.
 * 
 * @author    Gaurav Joshi (gpj5552@g.rit.edu)
 */


import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Zone extends Rectangle2D.Double implements Serializable{

	private static final long serialVersionUID = 1L;
	private List<String> files;
	
	Zone(){
		
		// TODO: have to validate linkedList claim
		files = new LinkedList<String>();
	}
	
	/**
	 * Splits the zone two parts, if zone width is greater than 
	 * height, then it splits vertically else horizontally
	 * 
	 * */
	Zone splitZone( Request reqObject ){
		
		// if width is greater split width wise
		// else split height wise 
		// if square then vertical
		Zone splittedZone = new Zone();
		
		// check which side is greater and split accordingly
		if (this.width <= this.height ){
			
			// splitting along x axis
			splittedZone.setRect(this.x, this.y + this.height/2 ,this.width,this.height/2 );
			this.setRect(this.x, this.y, this.width, this.height/2 );
		}else{
			// splitting along y axis			
			splittedZone.setRect(this.x+ this.width/2, this.y ,this.width/2,this.height );
			this.setRect(this.x, this.y, this.width/2, this.height );			
		}
		// TODO: add this peer to the neighbor list		
		return splittedZone;
	}
	
	/**
	 * finds if two rectangles are adjacent or not
	 * */   
	public boolean isNeighbor(Rectangle2D.Double rectToBeChecked){

	    double bottomLeftPoint_Y_1 = this.y + this.height;
	    double bottomLeftPoint_Y_2 = rectToBeChecked.y + rectToBeChecked.height;
	    double upperRightPoint_X_1 = this.x + this.width;
	    double upperRightPoint_X_2 = rectToBeChecked.x + rectToBeChecked.width;

	    
	    double bottomRightPoint_Y_1 = this.y + this.height;
	    double bottomRightPoint_Y_2 = rectToBeChecked.y + rectToBeChecked.height;
//	    double upperLeftPoint_X_1 = this.x ;
//	    double upperLeftPoint_X_2 = rectToBeChecked.x ;
	    
	    // have to account for bottom right points also	 
	    // bottom right point checked with upper right of rect to be checked
	    // checking if top edge is shared 
	    if ((bottomLeftPoint_Y_1 == rectToBeChecked.y && rectToBeChecked.x ==  this.x) ||
	    		(bottomRightPoint_Y_1 == rectToBeChecked.y &&
	    		rectToBeChecked.x+rectToBeChecked.width == this.x + this.width )){
	    	
	        return true;
	    }
	    
	    // check if  right shared
	    if( (upperRightPoint_X_1 == rectToBeChecked.x && rectToBeChecked.y== this.y) 
	    		||
	    	( rectToBeChecked.x ==  this.x + this.width 
	    	 &&  bottomRightPoint_Y_2 == this.height + this.y )){
	    	
	    	 return true;
	    }
	    
	    // bottom shared
	    if ((bottomLeftPoint_Y_2 == this.y && rectToBeChecked.x == this.x) 
	    		||
	    	(bottomRightPoint_Y_2 == this.y &&
	    		this.x + this.width == rectToBeChecked.x + rectToBeChecked.width )){
	    	
	    	 return true;
	    }
	    
	    // left shared
	    if ((upperRightPoint_X_2 == this.x && rectToBeChecked.y== this.y)
	    		||
	    	( this.x == rectToBeChecked.x + rectToBeChecked.width
	    	&&  bottomRightPoint_Y_1 == rectToBeChecked.height + rectToBeChecked.y )){
	    	
	    	
	    	return true;
	    }
	    	    
	    return false;
		}	
	
	/***
	 * Finds distance of point from this zone
	 * 
	 * */
	
	double findDistanceFromPoint( Point2D point ){
		
		double dx;
		double dy;
		
		double x = this.getCenterX();
		double y = this.getCenterY();
		
		double px = point.getX() ;
		double py = point.getY() ;
		
		dx = Math.max(Math.abs(px - x) - width / 2, 0);
		dy = Math.max(Math.abs(py - y) - height / 2, 0);
		return dx*dx + dy*dy;
	}

	
	/**
	 * Return if passed zone forms a proper shape after addition 
	 * with current zone
	 * 
	 * */
	boolean properShapeAfterAddition( Zone zoneToBeAdded ){
		
		
		// check if upper left corner align on x coordinate
		// if yes check for width
		// check if upper left corner align on y coordinate
		// if yes check for height
		
		if ((this.y == zoneToBeAdded.y && this.height == zoneToBeAdded.height) 
				||
				(this.x == zoneToBeAdded.x && this.width == zoneToBeAdded.width) ){
			
			return true;
		}
		
		return false;
	}
	
	
	double getArea (){
		
		return width*height;
	}
	
	// meant to be used with properShapeAfterAddition
	void addZones ( Zone zoneToBeAdded ){
		
		if (properShapeAfterAddition(zoneToBeAdded)){
			
			Rectangle2D.union(this, zoneToBeAdded , this);
		}
		
	}
	
	
	void addFile (String filename){
		
		files.add(filename);
	}
	
	void removeFile (String filename){
		
		files.remove(filename);
	}
	
	/**
	 * Return list of files for the zone passed
	 * 
	 * */
	List<String> getFilesForSplittedZone(Zone splittedZone){
		
		LinkedList<String> list = new LinkedList<String>();
		
		// check for each file, whether it belongs to zone or not
		Iterator<String> fileNameIterator = files.iterator();
		
		while( fileNameIterator.hasNext() ){
			
			String fileName = fileNameIterator.next();
			int x  = HashCalculator.calculateXHash(fileName);
			int y = HashCalculator.calculateYHash(fileName);
			
			Point2D.Double point = new Point2D.Double(x, y);
			if (splittedZone.contains(point)){
				
				list.add(fileName);
			}
		}
		
		return list;
	}
	
	// file list getter
	List<String> getAllFiles(){
		
		
		return files;
	}
	
	@Override	
	public String toString(){
		
		String str = super.toString();
		str = str + "\nFiles:\n\t";
		
		if (files.isEmpty()){
			
			str += "No Files";
		}else{
			
			str += files + "\n";
		}
				
		return str;
	}
		
}
