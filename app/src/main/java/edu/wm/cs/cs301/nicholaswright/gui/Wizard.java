package edu.wm.cs.cs301.nicholaswright.gui;

import edu.wm.cs.cs301.nicholaswright.generation.CardinalDirection;
import edu.wm.cs.cs301.nicholaswright.generation.Distance;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Turn;

/**
 * This class implements the Wizard algorithm for solving a maze. It is intended to be
 * a baseline algorithm to which other algorithms can be compared. 
 * 
 * The Wizard strategy's goal is to minimize path length, and it follows the following rules:
 * 		- The robot checks the four spaces surrounding it and determines which has the
 * 		  smallest distance to the exit.
 * 		- It then turns to face that cell and uses its forward sensor to determine
 * 		  if there is a wall separating the two spaces.
 * 			- in the case where there is no operational forward sensor, the robot picks
 * 			  a sensor that is operational and uses it, turning the robot as necessary
 * 		- If there is a wall between the robot and the next closest neighbor, it makes
 * 		  a jump operation. Otherwise, it moves a step forward.
 * 		- These steps are repeated until the robot is at the exit.
 * 
 * This strategy relies on information about the maze found in an object of the Distance 
 * class, which is given to the robot via the setDistance() method. It also holds a
 * reference to the robot which it operates.
 * 
 * Wizard implements concurrent threads for each of the directional sensors to fail,
 * relying on the Sensor class to do so.
 * 
 * @author Nicholas Wright
 *
 */
public class Wizard implements RobotDriver {

	private final static String TAG = "Wizard";

	// Robot to exist in the maze and be controlled by this driver
	private Robot robot;
	// Distance object containing necessary information about the maze
	private Distance dists;
	// Stores the width and height of the current maze
	// dimensions[0] is width and dimensions[1] is height
	private int[] dimensions;
	// The initial battery level of the robot when solving a maze. Used
	// to calculate energy consumed during the lifetime of the algorithm.
	private float initialBattery;
	
	// Sensor objects for each direction. Might be unnecessary code.
	private Thread leftSensor;
	private Thread rightSensor;
	private Thread forwardSensor;
	private Thread backwardSensor;

	// Stores whether each directional is operational as a boolean
	// In order of (LEFT, RIGHT, FORWARD, BACKWARD)
	private boolean[] operationalSensors;

	private boolean paused = false;
	
	/**
	 * empty constructor that initializes everything to either null,
	 * zero, or true
	 */
	public Wizard() {
		robot = null;
		dists = null;
		dimensions = null;
		initialBattery = 0;
		operationalSensors = new boolean[4];
		for(int i = 0; i < 4; i++) {
			operationalSensors[i] = true;
		}
		sensorInit();
	}
	
	/**
	 * Constructor that sets only the robot,
	 * leaving everything else as default initializations.
	 * 
	 * @param r is the robot controlled by this algorithm
	 */
	public Wizard(Robot r) {
		setRobot(r);
		dists = null;
		dimensions = null;
		initialBattery = robot.getBatteryLevel();
		
		operationalSensors = new boolean[4];
		for(int i = 0; i < 4; i++) {
			operationalSensors[i] = true;
		}
		
		sensorInit();
		
	}
	
	/**
	 * Full constructor which gives a fully-operational Wizard
	 * @param r is the robot which this algorithm controls
	 * @param width is the width of the maze
	 * @param height is the height of the maze
	 * @param dists is the Distance matrix of the maze
	 */
	public Wizard(Robot r, int width, int height, Distance dists) {
		setRobot(r);
		setDimensions(width, height);
		setDistance(dists);
		initialBattery = robot.getBatteryLevel();
		
		operationalSensors = new boolean[4];
		for(int i = 0; i < 4; i++) {
			operationalSensors[i] = true;
		}
		sensorInit();
	}
	
	/**
	 * This starts the fail/repair thread for all four directional
	 * sensors. 
	 */
	public void sensorInit() {
		leftSensor = createSensorThread(Direction.LEFT);
		rightSensor = createSensorThread(Direction.RIGHT);
		forwardSensor = createSensorThread(Direction.FORWARD);
		backwardSensor = createSensorThread(Direction.BACKWARD);
	}
	
	/**
	 * Creates a thread that runs the failure/repair process for a sensor
	 * in a given direction. Makes use of the Sensor class
	 * 
	 * @param dir is a direction of a sensor
	 */
	public Thread createSensorThread(Direction dir) {
		Sensor sensor = new Sensor(this.robot, this, dir);
		Thread t = new Thread(sensor);
		return t;
	}
	
	/**
	 * Starts the failure/repair thread of the sensor in the given direction
	 * 
	 * @param dir is the direction of the sensor
	 */
	public void startSensorThread(Direction dir) {
		Thread t = null;
		switch(dir) {
		case LEFT:
			t = leftSensor; break;
		case RIGHT:
			t = rightSensor; break;
		case FORWARD:
			t = forwardSensor; break;
		case BACKWARD:
			t = backwardSensor; break;
		default:
			throw new RuntimeException("An invalid direction was given to start the thread");
		}
		
		t.start();
	}

	public void toggleSensorThread(Direction dir){
		Thread t;
		switch(dir) {
			case LEFT:
				t = leftSensor; break;
			case RIGHT:
				t = rightSensor; break;
			case FORWARD:
				t = forwardSensor; break;
			case BACKWARD:
				t = backwardSensor; break;
			default:
				throw new RuntimeException("An invalid direction was given to start the thread");
		}

		if(t.isAlive()){
			t.interrupt();
		}
		else if(t.getState().equals(Thread.State.NEW)){
			t.start();
		}
		else{
			switch(dir) {
				case LEFT:
					leftSensor = createSensorThread(dir);
					leftSensor.start(); break;
				case RIGHT:
					rightSensor = createSensorThread(dir);
					rightSensor.start(); break;
				case FORWARD:
					forwardSensor = createSensorThread(dir);
					forwardSensor.start(); break;
				case BACKWARD:
					backwardSensor= createSensorThread(dir);
					backwardSensor.start(); break;
			}
		}
	}
	
	/**
	 * method to end threads that run sensors. Should be called at the end of the
	 * playing phase of a game.
	 */
	@Override
	public void killAllSensors() {
		leftSensor.interrupt();
		rightSensor.interrupt();
		forwardSensor.interrupt();
		backwardSensor.interrupt();
	}
	
	
	/**
	 * Assigns a robot platform to the driver. 
	 * The driver uses a robot to perform, this method provides it with this necessary information.
	 * @param r robot to operate
	 */
	@Override
	public void setRobot(Robot r) {
		robot = r;
	}

	/**
	 * Provides the robot driver with information on the dimensions of the 2D maze
	 * measured in the number of cells in each direction.
	 * @param width of the maze
	 * @param height of the maze
	 * @precondition 0 <= width, 0 <= height of the maze.
	 */
	@Override
	public void setDimensions(int width, int height) {
		dimensions = new int[2];
		dimensions[0] = width;
		dimensions[1] = height;
	}

	/**
	 * Provides the robot driver with information on the distance to the exit.
	 * Only some drivers such as the wizard rely on this information to find the exit.
	 * @param distance gives the length of path from current position to the exit.
	 * @precondition null != distance, a full functional distance object for the current maze.
	 */
	@Override
	public void setDistance(Distance distance) {
		this.dists = distance;
	}

	/**
	 * Tells the driver to check its robot for operational sensor. 
	 * If one or more of the robot's distance sensor become 
	 * operational again after a repair operation, this method
	 * allows to make the robot driver aware of this change 
	 * and to bring its understanding of which sensors are operational
	 * up to date.  
	 * 
	 * This asks the robot for its understanding of the sensor situation
	 * and copies it into this class' information.
	 */
	@Override
	public synchronized void triggerUpdateSensorInformation() {
		for(Direction dir : Direction.values()) {
			operationalSensors[dir.ordinal()] = robot.hasOperationalSensor(dir);
		}
	}

	/**
	 * Toggles the paused state of the drive2Exit function
	 */
	public void togglePaused(){
		paused = !paused;
	}
	/**
	 * Drives the robot towards the exit given it exists and 
	 * given the robot's energy supply lasts long enough. 
	 * This method has the robot leave the maze.
	 * @return true if driver successfully reaches the exit, false otherwise
	 * @throws UnsupportedOperationException if there are no available sensors to use
	 * @throws IndexOutOfBoundsException possibly through position checks
	 */
	@Override
	public boolean drive2Exit() throws Exception {
		DefaultWizardStrategy withForwardSensor = new DefaultWizardStrategy(robot);
		DefaultWizardStrategy withoutForwardSensor = new BrokenWizardStrategy(robot);
		int[] curPosition = {0, 0};
		int[] nextPosition;
		
		while(!robot.isAtExit()) {

			// if the user decides to pause the algorithm, we wait until the user
			// unpauses, checking every .1 seconds
			while(paused){
				Thread.currentThread().sleep(100);
			}

			curPosition = robot.getCurrentPosition();
			nextPosition = getCloserNeighbor(curPosition[0], curPosition[1]);
			
			// check to see if it's stopped
			if(robot.hasStopped()) {return false;}
			
			// find direction of next spot
			int dx = nextPosition[0] - curPosition[0];
			int dy = nextPosition[1] - curPosition[1];
			CardinalDirection dir = CardinalDirection.getDirection(dx, dy);
			
			// rotate to face the next position
			turnToDirection(dir);

			// check to see if it's stopped
			if(robot.hasStopped()) {return false;}
			
			// conditional behavior based on robot's state
			// pick a strategy
			DefaultWizardStrategy currentStrategy = 
					operationalSensors[2] ? withForwardSensor : withoutForwardSensor;
			// make a move
			if(currentStrategy.hasWallInFront()) {
				robot.jump();
			}
			else {
				robot.move(1, false);
			}
			
			// have we crashed?
			if(robot.hasStopped()) {return false;}

			// this is beneficial for android graphics
			Thread.currentThread().sleep(30);
		}
		
		// leave the maze
		return leaveMaze();
	}
	
	
	/**
	 * This method takes a robot at the exit position, senses the exit, and leaves
	 * the maze.
	 * @precondition the robot is at the exit; i.e. robot.isAtExit() is true
	 * @return true if the robot left the maze; false otherwise
	 */
	private boolean leaveMaze() {
		assert(robot.isAtExit() == true);
		// we're at the exit, but we need to figure out which direction to go
		boolean exitFound = false;
		
		// rotate around to try out different sensors in the case where
		// the sensor we want isn't working.
		for(int i = 0; i < 3; i++) {

			// sense in all directions
			for(Direction dir : Direction.values()) {
				if(robot.hasStopped()) {return false;}
				try {
					// if we can see through the exit, turn to face it
					if(robot.canSeeThroughTheExitIntoEternity(dir)) {
						exitFound = true;
						switch(dir) {
						case FORWARD:
							break;
						case LEFT:
							robot.rotate(Turn.LEFT);
							break;
						case RIGHT:
							robot.rotate(Turn.RIGHT);
							break;
						case BACKWARD:
							robot.rotate(Turn.AROUND);
							break;
						}

						// leave the for loop
						break;
					}
				}
				// try the next direction
				catch(UnsupportedOperationException e) {
					continue;
				}
			}
			if(exitFound) {
				break;
			}
			// rotate in case some sensors are broken
			robot.rotate(Turn.LEFT);
		}

		if(exitFound && !robot.hasStopped()) {
			//Log.v(TAG, "Exiting maze at: " + robot.getCurrentPosition()[0] + ", " + robot.getCurrentPosition()[1]);
			robot.move(1, false);
			return true;
		}

		return false;
	}

	/**
	 * Returns the total energy consumption of the journey, i.e.,
	 * the difference between the robot's initial energy level at
	 * the starting position and its energy level at the exit position. 
	 * This is used as a measure of efficiency for a robot driver.
	 */
	@Override
	public float getEnergyConsumption() {
		float batteryLevel = robot.getBatteryLevel();
		return initialBattery - batteryLevel;
	}

	/**
	 * Returns the total length of the journey in number of cells traversed. 
	 * Being at the initial position counts as 0. 
	 * This is used as a measure of efficiency for a robot driver.
	 */
	@Override
	public int getPathLength() {
		return robot.getOdometerReading();
	}
	
////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////// Private Auxiliary Methods //////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Given a current (x, y) coordinate pair, this method consults with the
	 * Distance matrix to find the neighbor that is closest to the maze's exit.
	 * 
	 * Returns null if there is no such neighbor, which includes the case where
	 * the robot is at the exit position.
	 * 
	 * @param x
	 * @param y
	 * @return an int[] containing the (x, y) pair of the adjacent cell closest to the exit, or null
	 */
	private int[] getCloserNeighbor(int x, int y) {
		assert x < dimensions[0] && y < dimensions[1] : "Invalid position";
		assert x >= 0 && y >= 0 : "Invalid position";
		// corner case, (x,y) is exit position
		if (dists.isExitPosition(x, y)) {
			return null;
		}
		// find best candidate
		int dnext = dists.getDistanceValue(x, y);
		int[] result = new int[2] ;
		int[] dir;
		for (CardinalDirection cd: CardinalDirection.values()) {
			if (!isValidPosition(x, y, cd)) 
				continue; // there is a wallboard in the way
			// no wallboard, let's check the distance
			dir = cd.getDirection();
			int dn = dists.getDistanceValue(x+dir[0], y+dir[1]);
			if (dn < dnext) {
				// update neighbor position with min distance
				result[0] = x+dir[0] ;
				result[1] = y+dir[1] ;
				dnext = dn ;
			}	
		}
		// expectation: we found a neighbor that is closer
		assert(dists.getDistanceValue(x, y) > dnext) : 
			"cannot identify direction towards solution: stuck at: " + x + ", "+ y ;
		// since assert statements need not be executed, check it 
		// to avoid giving back wrong result
		return (dists.getDistanceValue(x, y) > dnext) ? result : null;
		
	}
	
	/**
	 * Helper method that turns the robot to face a given Cardinal direction.
	 * 
	 * @param dir is a Cardinal Direction the robot is to face
	 */
	private void turnToDirection(CardinalDirection dir) {
		CardinalDirection dirCurrent;

		int i = 0;
		for(; i < 3; i++) {
			dirCurrent = robot.getCurrentDirection();
			// are we facing the right direction?
			if(dir == dirCurrent) {
				break;
			}
			// check to see if it's stopped
			if(robot.hasStopped()) {return;}
			
			robot.rotate(Robot.Turn.LEFT); // arbitrary choice of direction here
		}
		// make a correction to the battery in case we did the wrong number of turns
		// i.e. we turned three times left instead of once right
		if(i == 2) {
			robot.setBatteryLevel(robot.getBatteryLevel()+ (robot.getEnergyForFullRotation()/2));
		}
	}
	
	/**
	 * Helper method that checks if a given cell adjacent to an (x, y) position in
	 * a given direction falls within the bounds of the maze.
	 * 
	 * @param x the x-coordinate of the position
	 * @param y the y-coordinate of the position
	 * @param cd the direction in which to look for a neighbor cell
	 * @return true if the given cell is within the maze; false otherwise
	 */
	private boolean isValidPosition(int x, int y, CardinalDirection cd) {
		int[] dir = cd.getDirection();
		int xPos = x + dir[0];
		int yPos = y + dir[1];

		if(xPos < 0 || yPos < 0) {
			return false;
		}
		if(xPos >= dimensions[0] || yPos >= dimensions[1]) {
			return false;
		}
		return true;
	}
	
		
	/**
	 * Drives the robot towards the exit given it exists and 
	 * given the robot's energy supply lasts long enough. 
	 * This method has the robot leave the maze.
	 * This is used for testing, since it doesn't fully leave the maze.
	 * @return true if driver successfully reaches the exit, false otherwise
	 * @throws UnsupportedOperationException if there are no available sensors to use
	 * @throws IndexOutOfBoundsException possibly through position checks
	 */
	public boolean drive2ExitButDontLeave() throws Exception {
		DefaultWizardStrategy withForwardSensor = new DefaultWizardStrategy(robot);
		DefaultWizardStrategy withoutForwardSensor = new BrokenWizardStrategy(robot);
		int[] curPosition = {0, 0};
		int[] nextPosition;
		
		while(!robot.isAtExit()) {
			curPosition = robot.getCurrentPosition();
			nextPosition = getCloserNeighbor(curPosition[0], curPosition[1]);
			
			// check to see if it's stopped
			if(robot.hasStopped()) {
				return false;
			}
			
			// find direction of next spot
			int dx = nextPosition[0] - curPosition[0];
			int dy = nextPosition[1] - curPosition[1];
			CardinalDirection dir = CardinalDirection.getDirection(dx, dy);
			
			// rotate to face the next position
			turnToDirection(dir);
			
			// conditional behavior based on robot's state
			// pick a strategy
			DefaultWizardStrategy currentStrategy = 
					operationalSensors[2] ? withForwardSensor : withoutForwardSensor;
			// make a move
			if(currentStrategy.hasWallInFront()) {
				robot.jump();
			}
			else {
				robot.move(1, false);
			}
			
			// have we crashed?
			if(robot.hasStopped()) {
				return false;
			}
			

		}
		
		return true;
		

	}

}
