package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;

/**
 * This class provides a strategy for sensing whether a wall is in front of it
 * when the forward sensor is operational
 * 
 * This is the default behavior
 * 
 * @author Nicholas Wright
 *
 */
public class DefaultWizardStrategy {

	// reference to the robot whose sensors this class uses
	protected Robot robot;
	
	/**
	 * Constructor that provides a robot reference.
	 * @param r a robot to use
	 */
	public DefaultWizardStrategy(Robot r) {
		this.robot = r;
	}
	
	/**
	 * Uses the front sensor to check whether there's a wall in front
	 * @return true if there is a wall in front of the robot; false otherwise
	 */
	public boolean hasWallInFront() {
		return robot.distanceToObstacle(Direction.FORWARD) == 0;		
	}

}
