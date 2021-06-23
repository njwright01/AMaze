package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Turn;

/**
 * This class provides a strategy for sensing whether a wall is in front of it
 * when the forward sensor is not operational. Extends the default strategy
 * by overriding the one meaningful method, hasWallInFront().
 * 
 * @author Nicholas Wright
 *
 */
public class BrokenWizardStrategy extends DefaultWizardStrategy {

	
	/**
	 * Constructor that sets the super's robot field
	 * @param r the robot to use
	 */
	public BrokenWizardStrategy(Robot r) {
		super(r);
	}
	
	/**
	 * Returns true if there is a wall in front of it.
	 * Precondition: the robot is facing the cell with the possible
	 * Picks a sensor that is working, and then turns so that the
	 * sensor is facing that cell, then senses.
	 */
	@Override
	public boolean hasWallInFront() {
		boolean hasWall = false;
		if(robot.hasOperationalSensor(Direction.LEFT)){
			robot.rotate(Turn.RIGHT); // turn so we can use the left sensor
			hasWall = robot.distanceToObstacle(Direction.LEFT) == 0;
			robot.rotate(Turn.LEFT);
		}
		else if(robot.hasOperationalSensor(Direction.RIGHT)){
			robot.rotate(Turn.LEFT); // turn so we can use the left sensor
			hasWall = robot.distanceToObstacle(Direction.RIGHT) == 0;
			robot.rotate(Turn.RIGHT);
		}
		else if(robot.hasOperationalSensor(Direction.BACKWARD)){
			robot.rotate(Turn.AROUND); // turn so we can use the left sensor
			hasWall = robot.distanceToObstacle(Direction.BACKWARD) == 0;
			robot.rotate(Turn.AROUND);
		}
		else {
			throw new UnsupportedOperationException("There are no available sensors!");
		}
		
		return hasWall;
	}

}
