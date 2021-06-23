package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Turn;

/**
 * This is a strategy for sensing the left and forward walls when there is no functioning
 * forward directional sensor.
 * 
 * This is used when we know the left sensor is working.
 * 
 * 
 * @author Nicholas Wright
 *
 */
public class BrokenForwardWallFollowerStrategy extends DefaultWallFollowerStrategy  {

	public BrokenForwardWallFollowerStrategy(Robot r) {
		super(r);
	}
	
	/**
	 * Senses if there is a wall in front of the robot by turning right,
	 * using the left sensor, and then turning back to the original direction.
	 * 
	 * @return true if there is a wall to in front of the robot
	 */
	@Override
	public boolean senseForward() {
		boolean hasWall = false;
		robot.rotate(Turn.RIGHT);
		hasWall =  robot.distanceToObstacle(Direction.LEFT) == 0;	
		robot.rotate(Turn.LEFT);
		
		return hasWall;
	}

}
