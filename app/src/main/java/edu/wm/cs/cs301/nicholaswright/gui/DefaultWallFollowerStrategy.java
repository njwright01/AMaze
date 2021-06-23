package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;

public class DefaultWallFollowerStrategy implements WallFollowerStrategy {

	// stores a reference to the robot object that creates it so it can act
	protected Robot robot;
	
	/**
	 * default constructor. Should not be used
	 */
	public DefaultWallFollowerStrategy() {
		robot = null;
	}
	
	/**
	 * Constructor that gives the strategy a proper robot
	 * @param r is a robot on which this strategy should act
	 */
	public DefaultWallFollowerStrategy(Robot r) {
		robot = r;
	}
	
	/**
	 * @return true if this robot has a wall to the left; false otherwise
	 */
	@Override
	public boolean senseLeft() {
		return robot.distanceToObstacle(Direction.LEFT) == 0;		
	}

	/**
	 * @return true if this robot has a wall in front; false otherwise
	 */
	@Override
	public boolean senseForward() {
		return robot.distanceToObstacle(Direction.FORWARD) == 0;		
	}

}
