package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Turn;

/**
 * This is a strategy for sensing the left and forward walls when there is no functioning
 * left directional sensor.
 * 
 * This is used when we know the forward sensor is working.
 * 
 * 
 * @author Nicholas Wright
 *
 */
public class BrokenLeftWallFollowerStrategy extends DefaultWallFollowerStrategy {


	public BrokenLeftWallFollowerStrategy(Robot r) {
		super(r);
	}
	
	/**
	 * This senses left by turning to face left, using the forward sensor,
	 * and then turning to face the original direction
	 * 
	 * @return true if there is a wall to the left of the robot
	 */
	@Override
	public boolean senseLeft() {
		boolean hasWall = false;
		robot.rotate(Turn.LEFT);
		hasWall =  robot.distanceToObstacle(Direction.FORWARD) == 0;	
		robot.rotate(Turn.RIGHT);
		
		return hasWall;

	}


}
