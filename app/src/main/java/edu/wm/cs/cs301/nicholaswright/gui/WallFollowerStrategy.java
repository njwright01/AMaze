package edu.wm.cs.cs301.nicholaswright.gui;

/**
 * Interface describing sensor-specific behavior necessary to the 
 * Wall-Follower algorithm.
 * 
 * @author Nicholas Wright
 *
 */
public interface WallFollowerStrategy {

	boolean senseLeft();
	
	boolean senseForward();
	
}
