package edu.wm.cs.cs301.nicholaswright.gui;

import android.util.Log;

import edu.wm.cs.cs301.nicholaswright.generation.CardinalDirection;
import edu.wm.cs.cs301.nicholaswright.generation.Floorplan;
import edu.wm.cs.cs301.nicholaswright.generation.Maze;

/**
 * 
 * This class is a basic implementation of the Robot interface that provides
 * an API for traversing a maze, either manually or automatically. All directional
 * sensors, as well as both the room sensor and exit sensor, are implemented and
 * operational. The robot has an initial amount of energy that is depleted with 
 * operations as follows:
 *     Initial energy: 3000
 *     Energy to use directional sensors: 1
 *     Energy for other sensors: 0
 *     Energy to rotate: 3
 *     Energy to move one space: 5
 *     Energy to jump over a wall: 50
 * Once the robot runs out of energy, it stops and can no longer take actions.
 * In the controller class this should trigger the end of the maze game.
 * The robot also keeps track of the distance traveled. Sensors can fail and
 * be fixed on demand.
 * 
 * Internally, this class stores the current position and cardinal direction
 * of the robot, a reference to the Controller object that holds the maze,
 * a reference to the floorplan of the maze (for convenience), and an array
 * which keeps track of which sensors are operational. Also, this
 * class relies on a RobotBattery class which encapsulates the notion of
 * energy and energy consumption.
 * 
 * Note that directionally, North and South are flipped from their usual spots;
 * i.e. turning left from East gives South, and turning right gives North.
 * 
 * @author Nicholas Wright
 *
 */
public class BasicRobot implements Robot {
	// for logging
	private static final String TAG = "BasicRobot";

	/**
	 * Stores the current (x, y) position of the robot. The Map graphics
	 * place (0, 0) in the bottom-left corner of the maze. This is updated
	 * with every movement operation
	 */
	private int[] currentPosition;

	/**
	 * Stores the current absolute direction of the robot. This is updated
	 * with every rotation. The Map graphics place East to the right, West
	 * to the left, as one would expect. However, North and South are flipped
	 * graphically, so implementations seem wacky.
	 */
	private CardinalDirection currentAbsoluteDirection;
	
	/**
	 * This controller is the link between the robot and the UI/graphics
	 */
	private Controller controller;
	/**
	 * Comes from the Controller, stores information about the maze
	 */
	private Floorplan floorplan;
	
	/**
	 * stores information about the battery and methods to change it
	 */
	private RobotBattery battery;
	/**
	 *  incremented every time a robot moves (or jumps) a space
	 */
	private int distanceTraveled;
	
	/**
	 *  Battery level constants
	 *  These are determined by Project 3 specs
	 */
	private final float INITIAL_ENERGY = 3000;
	private final float SENSE_ENERGY = 1;  // energy to use one of the sensors
	private final float ROTATE_ENERGY = 3; // energy to rotate 90 degrees in either direction
	private final float MOVE_ENERGY = 5;   // energy to move forward or backward one space
	private final float JUMP_ENERGY = 50;  // energy to jump over a wall
	
	/**
	 *  Holds information of whether directional sensors are working or not.
	 *  Ordered in the same way as Robot.Direction is (Left, Right, Forward, Backward)
	 */ 
	private boolean[] directionalSensors;
	
	/**
	 * Initially true, but set to false when battery runs out.
	 */
	private boolean stopped;
	
	/**
	 * Constructor that creates a robot with all possible sensors operational
	 * and a fully charged battery.
	 */
	public BasicRobot() {
		battery = new RobotBattery(INITIAL_ENERGY);
		distanceTraveled = 0;
		directionalSensors = new boolean[4];
		for(int i = 0; i < 4; i++) {
			directionalSensors[i] = true;
		}

		stopped = false;
		
		currentPosition = null;
		currentAbsoluteDirection = null;
	}
	
	/**
	 * Constructor that takes a controller as an argument. Creates a robot with
	 * all possible sensors operational and a fully charged battery. Also sets
	 * the robot's controller.
	 */
	public BasicRobot(Controller controller) {
		this();
		setMaze(controller);
	}

	/**
	 * Provides the current position as (x,y) coordinates for the maze cell as an array of length 2 with [x,y].
	 * @postcondition 0 <= x < width, 0 <= y < height of the maze. 
	 * @return array of length 2, x = array[0], y=array[1]
	 * @throws Exception if position is outside of the maze
	 */
	@Override
	public int[] getCurrentPosition() throws Exception {
		// check for stuff
		assert (currentPosition != null) : "current position might not have been initialized";
		
		// what if controller is null?
		int mazeWidth = controller.getMazeConfiguration().getWidth();
		int mazeHeight = controller.getMazeConfiguration().getHeight();
		if(currentPosition[0] < 0 || currentPosition[0] >= mazeWidth) {
			throw new IndexOutOfBoundsException("Robot is out of bounds");
		}
		if(currentPosition[1] < 0 || currentPosition[1] >= mazeHeight) {
			throw new IndexOutOfBoundsException("Robot is out of bounds");
		}
		
		return currentPosition;
	}

	/**
	 * Provides the current cardinal direction.
	 * @return cardinal direction is robot's current direction in absolute terms
	 */	
	@Override
	public CardinalDirection getCurrentDirection() {
		// check for initialization
		assert (currentAbsoluteDirection != null) : "current direction might not have been initialized";	
		return currentAbsoluteDirection;
	}

	/**
	 * Provides the robot with a reference to the controller to cooperate with.
	 * The robot memorizes the controller such that this method is most likely called only once
	 * and for initialization purposes. The controller serves as the main source of information
	 * for the robot about the current position, the presence of walls, the reaching of an exit.
	 * The controller is assumed to be in the playing state.
	 * @param controller is the communication partner for robot
	 * @precondition controller != null, controller is in playing state and has a maze
	 */
	@Override
	public void setMaze(Controller controller) {
		this.controller = controller;
		Maze mazeconfig = controller.getMazeConfiguration();
		floorplan = mazeconfig.getFloorplan();
		assert(floorplan != null);
		currentPosition = controller.getCurrentPosition();
		currentAbsoluteDirection = controller.getCurrentDirection();
	}
	
	/**
	 * Returns the current battery level.
	 * The robot has a given battery level (energy level) 
	 * that it draws energy from during operations. 
	 * The particular energy consumption is device dependent such that a call 
	 * for distance2Obstacle may use less energy than a move forward operation.
	 * If battery level <= 0 then robot stops to function and hasStopped() is true.
	 * @return current battery level, level is > 0 if operational. 
	 */
	@Override
	public float getBatteryLevel() {
		// check for initialization
		assert(battery != null) : "battery might not have been initialized";
		
		return battery.getBatteryLevel();
	}

	/**
	 * Sets the current battery level.
	 * The robot has a given battery level (energy level) 
	 * that it draws energy from during operations. 
	 * The particular energy consumption is device dependent such that a call 
	 * for distance2Obstacle may use less energy than a move forward operation.
	 * If battery level <= 0 then robot stops to function and hasStopped() is true.
	 * @param level is the current battery level
	 * @precondition level >= 0 
	 */
	@Override
	public void setBatteryLevel(float level) {
		// we're assuming this has been initialized
		assert(battery != null) : "battery might not have been initialized";
		
		battery.setBatteryLevel(level);
	}

	/** 
	 * Gets the distance traveled by the robot.
	 * The robot has an odometer that calculates the distance the robot has moved.
	 * Whenever the robot moves forward, the distance 
	 * that it moves is added to the odometer counter.
	 * The odometer reading gives the path length if its setting is 0 at the start of the game.
	 * The counter can be reset to 0 with resetOdomoter().
	 * @return the distance traveled measured in single-cell steps forward
	 */
	@Override
	public int getOdometerReading() {
		return distanceTraveled;
	}

	/** 
     * Resets the odomoter counter to zero.
     * The robot has an odometer that calculates the distance the robot has moved.
     * Whenever the robot moves forward, the distance 
     * that it moves is added to the odometer counter.
     * The odometer reading gives the path length if its setting is 0 at the start of the game.
     */
	@Override
	public void resetOdometer() {
		distanceTraveled = 0;
	}

	/**
	 * Gives the energy consumption for a full 360 degree rotation.
	 * Scaling by other degrees approximates the corresponding consumption. 
	 * @return energy for a full rotation
	 */
	@Override
	public float getEnergyForFullRotation() {
		return 4*ROTATE_ENERGY;
	}

	/**
	 * Gives the energy consumption for moving forward for a distance of 1 step.
	 * For simplicity, we assume that this equals the energy necessary 
	 * to move 1 step backwards and that scaling by a larger number of moves is 
	 * approximately the corresponding multiple.
	 * @return energy for a single step forward
	 */
	@Override
	public float getEnergyForStepForward() {
		return MOVE_ENERGY;
	}
	
	/**
	 * Gives the energy consumption for jumping over a wall.
	 * @return energy for a single step forward
	 */
	public float getEnergyForJump() {
		return JUMP_ENERGY;
	}
	
	///////////////////////////////////////////////////////////////////
	/////////////////// Sensors   /////////////////////////////////////
	///////////////////////////////////////////////////////////////////

	/**
	 * Tells if current position (x,y) is right at the exit but still inside the maze. 
	 * Used to recognize termination of a search.
	 * @return true if robot is at the exit, false otherwise
	 */
	@Override
	public boolean isAtExit() {
		return floorplan.isExitPosition(currentPosition[0], currentPosition[1]);
	}

	/**
	 * Tells if a sensor can identify the exit in the given direction relative to 
	 * the robot's current forward direction from the current position.
	 * 
	 * This drains the battery.
	 * 
	 * @return true if the exit of the maze is visible in a straight line of sight,
	 * false if either we ran out of energy or the exit is not visible
	 * @throws UnsupportedOperationException if robot has no sensor in this direction
	 */
	@Override
	public boolean canSeeThroughTheExitIntoEternity(Direction direction) throws UnsupportedOperationException {
		// check for sensors
		if(!hasOperationalSensor(direction)) {
			throw new UnsupportedOperationException("Sensor in " + direction + " direction is not operational");
		}
		
		int distanceToWall = distanceToObstacle(direction);
		
		// The robot didn't have enough energy to sense
		if(distanceToWall == -1) {
			return false;
		}
		
		return distanceToWall == Integer.MAX_VALUE;
	}

	/**
	 * Tells if current position is inside a room. Makes use of the floorplan to determine this.
	 * @return true if robot is inside a room, false otherwise
	 * @throws UnsupportedOperationException if not supported by robot
	 */	
	@Override
	public boolean isInsideRoom() throws UnsupportedOperationException {
		if(!hasRoomSensor()) {
			throw new UnsupportedOperationException("This robot does not have a room sensor");
		}
		
		return floorplan.isInRoom(currentPosition[0], currentPosition[1]);

	}

	/**
	 * Tells if the robot has a room sensor. This implementation has one, so returns true
	 * 
	 * @return true always
	 */
	@Override
	public boolean hasRoomSensor() {
		return true;
	}

	/**
	 * Tells if the robot has stopped for reasons like lack of energy, hitting an obstacle, etc.
	 * @return true if the robot has stopped, false otherwise
	 */
	@Override
	public boolean hasStopped() {
		return stopped;
	}

	/**
	 * Tells the distance to an obstacle (a wall) 
	 * in the given direction.
	 * The direction is relative to the robot's current forward direction.
	 * Distance is measured in the number of cells towards that obstacle, 
	 * e.g. 0 if the current cell has a wallboard in this direction, 
	 * 1 if it is one step forward before directly facing a wallboard,
	 * Integer.MaxValue if one looks through the exit into eternity.
	 * 
	 * Drains the battery
	 * 
	 * @param direction specifies the direction of the sensor
	 * @return number of steps towards obstacle if obstacle is visible 
	 * in a straight line of sight, Integer.MAX_VALUE if we can see into eternity,
	 * -1 if there was no energy to do this
	 * @throws UnsupportedOperationException if the robot does not have
	 * an operational sensor for this direction
	 */
	@Override
	public int distanceToObstacle(Direction direction) throws UnsupportedOperationException {
		// do we have the energy to sense?
		if(hasStopped()) {
			Log.v(TAG, "Robot has stopped. Cannot sense.");
			return -1;
		}
		
		// check for sensors
		if(!hasOperationalSensor(direction)) {
			throw new UnsupportedOperationException("Sensor in " + direction + " direction is not operational");
		}
		
		// convert relative direction to absolute direction
		CardinalDirection absoluteDirection = relativeDirectionToAbsoluteDirection(direction);
		// walk in that direction until we hit a wall
		int stepCount = 0;
		int curX = currentPosition[0];
		int curY = currentPosition[1];
		// for checking bounds
		int mazeWidth = controller.getMazeConfiguration().getWidth();
		int mazeHeight = controller.getMazeConfiguration().getHeight();
		
		// step in the direction towards a wall until it hits the wall and
		// keep track  of the number of steps it takes to do that
		while(!floorplan.hasWall(curX, curY, absoluteDirection)) {
			int[] dir = absoluteDirection.getDirection();
			curX += dir[0];
			curY += dir[1];
			stepCount++;
			
			// if we've left the maze, then we're seeing into eternity
			if(curX < 0 || curX >= mazeWidth || curY < 0 || curY >= mazeHeight){
				stepCount = Integer.MAX_VALUE;
				break;
			}
		}
		
		battery.drainBattery(SENSE_ENERGY);
		
		// have we run out of energy?
		if(battery.isEmpty()) {
			stop();
		}
		
		return stepCount;
	}
	
	/**
	 * Tells if the robot has an operational distance sensor for the given direction.
	 * 
	 * returns boolean value stored in directionalSensors array corresponding to the
	 * desired direction 
	 * 
	 * @param direction specifies the direction of the sensor
	 * @return true if robot has operational sensor, false otherwise
	 */
	@Override
	public synchronized boolean hasOperationalSensor(Direction direction) {
		//return directionalSensors[direction.ordinal()];
		return directionalSensors[direction.ordinal()];
	}
	
	/**
	 * Makes the robot's distance sensor for the given direction fail.
	 * Subsequent calls to measure the distance to an obstacle in 
	 * this direction will return with an exception.
	 * 
	 * Sets boolean value stored in directionalSensors array corresponding to the
	 * desired direction to false
	 * 
	 * @param direction specifies the direction of the sensor
	 */
	@Override
	public synchronized void triggerSensorFailure(Direction direction) {
		directionalSensors[direction.ordinal()] = false;
	}

	/**
	 * Makes the robot's distance sensor for the given direction
	 * operational again. 
	 * A method call for an already operational sensor has no effect
	 * but returns true as the robot has an operational sensor
	 * for this direction.
	 * 
	 * Sets boolean value stored in directionalSensors array corresponding to the
	 * desired direction to true
	 * 
	 * @param direction specifies the direction of the sensor
	 * @return true if robot has operational sensor, false otherwise
	 */
	@Override
	public boolean repairFailedSensor(Direction direction) {
		directionalSensors[direction.ordinal()] = true;
		return true;
	}
	
	///////////////////////////////////////////////////////////////////
	/////////////////// Actuators /////////////////////////////////////
	///////////////////////////////////////////////////////////////////

	/**
	 * Turn robot on the spot for amount of degrees. Uses the controller's access
	 * to the current playing state to make the rotation
	 * 
	 * This changes the currentAbsoluteDirection value of the robot.
	 * 
	 * If robot runs out of energy, it stops. Rotate around is treated as two
	 * left rotations, and the robot can run out of energy halfway through this.
	 * 
	 * which can be checked by hasStopped() == true and by checking the battery level. 
	 * @param turn direction to turn and relative to current forward direction.
	 */
	@Override
	public void rotate(Turn turn) {
		// do we have the energy to turn?
		if(hasStopped()) {
			Log.v(TAG, "Robot has stopped. Cannot rotate.");
			return;
		}
		
		// stop it before it tries to rotate
		if(battery.isEmpty()) {
			stop();
			return;
		}
		
		// pick a direction and turn, update values
		switch(turn) {
		case LEFT:
			controller.rotateLeft();
			battery.drainBattery(ROTATE_ENERGY);
			break;
		case RIGHT:
			controller.rotateRight();
			battery.drainBattery(ROTATE_ENERGY);
			break;
		case AROUND: // this counts as two left turns
			this.rotate(Turn.LEFT);		
			this.rotate(Turn.LEFT);
			break;
		}
		
		// update direction and battery
		currentAbsoluteDirection = controller.getCurrentDirection();
		
		// have we run out of energy?
		if(battery.isEmpty()) {
			stop();
		}
	}

	/**
	 * Moves robot forward a given number of steps. A step matches a single cell.
	 * If the robot runs out of energy somewhere on its way, it stops, 
	 * which can be checked by hasStopped() == true and by checking the battery level. 
	 * If the robot hits an obstacle like a wall, it depends on the mode of operation
	 * what happens. If an algorithm drives the robot, it remains at the position in front 
	 * of the obstacle and also hasStopped() == true as this is not supposed to happen.
	 * This is also helpful to recognize if the robot implementation and the actual maze
	 * do not share a consistent view on where walls are and where not.
	 * If a user manually operates the robot, this behavior is inconvenient for a user,
	 * such that in case of a manual operation the robot remains at the position in front
	 * of the obstacle but hasStopped() == false and the game can continue.
	 * 
	 * This interacts with the controller, who controls access to the statePlaying object,
	 * which actually makes the changes.
	 * Alters currentPosition and battery
	 * 
	 * @param distance is the number of cells to move in the robot's current forward direction 
	 * @param manual is true if robot is operated manually by user, false otherwise
	 * @precondition distance >= 0
	 */
	@Override
	public void move(int distance, boolean manual) {
		assert(distance >= 0) : "Distance must be non-negative";
		// do we have the energy to move?
		if(hasStopped()) {
			Log.v(TAG, "Robot has stopped. Cannot begin moving.");
			return;
		}

		// move a certain number of steps
		for(int i = 0; i < distance; i++) {
			// check for energy
			if(battery.isEmpty()) {
				stop();
				return;
			}
			// if we've hit a wall, stop things depending on whether we're manual or not
			if(floorplan.hasWall(currentPosition[0], currentPosition[1], currentAbsoluteDirection)) {
				if(manual) {
					Log.v(TAG, "You've hit a wall!");
					break;
				}
				else {
					stop();
					break;
				}
			}
			// otherwise, make the move and update things accordingly
			controller.move();
			currentPosition = controller.getCurrentPosition();
			battery.drainBattery(MOVE_ENERGY);
			distanceTraveled++;
		}
		
	}

	/**
	 * Makes robot move in a forward direction even if there is a wall
	 * in front of it. In this sense, the robot jumps over the wall
	 * if necessary. The distance is always 1 step and the direction
	 * is always forward.
	 * 
	 * Uses controller object to make the actual move. Alters currentPosition
	 * and battery instance variables.
	 * 
	 * @throws Exception is thrown if the chosen wall is an exterior wall 
	 * and the robot would land outside of the maze that way. 
	 * The current location remains set at the last position, 
	 * same for direction but the game is supposed
	 * to end with a failure.
	 */
	@Override
	public void jump() throws Exception {
		// do we have the energy to move?
		if(hasStopped()) {
			Log.v(TAG, "Robot has stopped. Cannot jump.");
			return;
		}

		// stop it before it tries to move
		if(battery.isEmpty()) {
			stop();
			return;
		}
		
		// check to see if the move is valid
		int mazeWidth = controller.getMazeConfiguration().getWidth();
		int mazeHeight = controller.getMazeConfiguration().getHeight();
		// find position after jump
		int jumpX = currentPosition[0];
		int jumpY = currentPosition[1];
		int[] dir = getCurrentDirection().getDirection();
		jumpX += dir[0];
		jumpY += dir[1];

		// check if it's in bounds
		if(jumpX < 0 || jumpX >= mazeWidth || jumpY < 0 || jumpY >= mazeHeight) {
			throw new IndexOutOfBoundsException("Robot is trying to jump outside the maze");
		}
		
		// make the actual jump
		controller.jump();
		currentPosition = controller.getCurrentPosition();
		battery.drainBattery(JUMP_ENERGY);
		distanceTraveled++;
			
	}
	
	/**
	 * Method to encapsulate stopping a robot. Sets stopped flag to true and
	 * prints a message
	 */
	private void stop() {
		Log.v(TAG, "Stopping robot");
		stopped = true;
	}
	
	////////////////////////////Private Methods for Converting Directions////////////////////////
	
	/**
	 * Converts the relative direction passed in to an absolute direction by comparing
	 * it to the current absolute direction of the robot
	 * @param direction relative to the current absolute direction
	 * @return an absolute direction 
	 */
	private CardinalDirection relativeDirectionToAbsoluteDirection(Direction direction) {
		switch(direction) {
		case FORWARD:
			return currentAbsoluteDirection;
		case BACKWARD:
			return currentAbsoluteDirection.oppositeDirection();
		case LEFT:
			return leftAbsoluteDirection(currentAbsoluteDirection);
		case RIGHT:
			return rightAbsoluteDirection(currentAbsoluteDirection);
		}
		return null;
	}
	
	/**
	 * Gives the absolute direction to the left of the given direction
	 * @param direction is an absolute direction
	 * @return the direction to the left of the given one
	 */
	private CardinalDirection leftAbsoluteDirection(CardinalDirection direction) {
		switch(direction) {
		case North:
			return CardinalDirection.East;
		case South:
			return CardinalDirection.West;
		case East:
			return CardinalDirection.South;
		case West:
			return CardinalDirection.North;
		}
		return null;
	}
	
	/**
	 * Gives the absolute direction to the right of the given direction
	 * @param direction is an absolute direction
	 * @return the direction to the right of the given one
	 */
	private CardinalDirection rightAbsoluteDirection(CardinalDirection direction) {
		switch(direction) {
		case North:
			return CardinalDirection.West;
		case South:
			return CardinalDirection.East;
		case East:
			return CardinalDirection.North;
		case West:
			return CardinalDirection.South;
		}
		return null;
	}

}
