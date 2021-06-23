package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Turn;

/**
 * Strategy for sensing walls to the left and right of a robot when both of those
 * sensors are not operational.
 * 
 * 
 * @author Nicholas Wright
 *
 */
public class BrokenLandFWallFollowerStrategy extends DefaultWallFollowerStrategy  {

	
	public BrokenLandFWallFollowerStrategy(Robot r) {
		super(r);
	}
	
	/**
	 * Senses whether there is a wall to the left of the robot by finding a
	 * sensor that does work and using it, turning to face that direction
	 * and then turning back.
	 * 
	 * @return true if there is a wall to the left; false otherwise
	 */
	@Override
	public boolean senseLeft() {
		boolean hasWall = false;
		if(robot.hasOperationalSensor(Direction.RIGHT)){
			robot.rotate(Turn.AROUND); // turn so we can use the left sensor
			hasWall = robot.distanceToObstacle(Direction.RIGHT) == 0;
			robot.rotate(Turn.AROUND);
		}
		else if(robot.hasOperationalSensor(Direction.BACKWARD)){
			robot.rotate(Turn.RIGHT); // turn so we can use the left sensor
			hasWall = robot.distanceToObstacle(Direction.BACKWARD) == 0;
			robot.rotate(Turn.LEFT);
		}
		else {
			throw new UnsupportedOperationException("There are no available sensors!");
		}
		return hasWall;
	}

	/**
	 * Senses whether there is a wall in front of the robot by finding a
	 * sensor that does work and using it, turning to face that direction
	 * and then turning back.
	 * 
	 * @return true if there is a wall in front; false otherwise
	 */
	@Override
	public boolean senseForward() {
		boolean hasWall = false;
		if(robot.hasOperationalSensor(Direction.RIGHT)){
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
