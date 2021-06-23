package edu.wm.cs.cs301.nicholaswright.gui;

import android.util.Log;

import edu.wm.cs.cs301.nicholaswright.generation.Distance;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;
import edu.wm.cs.cs301.nicholaswright.gui.Robot.Turn;

/**
 * This class implements the Wall Follower algorithm for solving a maze. Its goal is to 
 * minimize path length, and it follows the following rules:
 * 		- If there is a Wall to the robot's left:
 * 			- Move forward one step if there is no wall in front.
 * 			- Otherwise, turn the robot right so it doesn't hit the wall.
 * 		- Otherwise, turn left and move forward one step.
 * 		- Continue these steps until the robot is at the maze's exit.
 * 
 * This strategy holds a reference to the robot which it operates.
 * 
 * WallFollower implements concurrent threads for each of the directional sensors to fail,
 * relying on the Sensor class to do so.
 * 
 * @author Nicholas Wright
 *
 */
public class WallFollower implements RobotDriver {

	private final static String TAG = "WallFollower";

	// Robot to exist in the maze and be controlled by this driver
	private Robot robot;
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
	public WallFollower() {
		robot = null;
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
	public WallFollower(Robot r) {
		setRobot(r);
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
	public WallFollower(Robot r, int width, int height, Distance dists) {
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

		if(!t.isAlive()) {
			t.start();
		}
	}

	public void endSensorThread(Direction dir){
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

		if(!t.isAlive()) {
			t.interrupt();
		}
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
		this.robot = r;
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
	 * WallFollower does not make use of Distance, so this method does nothing.
	 */
	@Override
	public void setDistance(Distance distance) {
		Log.v(TAG, "WallFollower has no distance attribute");
	}

	/**
	 * Tells the driver to check its robot for operational sensor. 
	 * If one or more of the robot's distance sensor become 
	 * operational again after a repair operation, this method
	 * allows to make the robot driver aware of this change 
	 * and to bring its understanding of which sensors are operational
	 * up to date.  
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
		// initialize strategies
		WallFollowerStrategy defaultStrat = new DefaultWallFollowerStrategy(robot);
		WallFollowerStrategy brokenLStrat = new BrokenLeftWallFollowerStrategy(robot);
		WallFollowerStrategy brokenFStrat = new BrokenForwardWallFollowerStrategy(robot);
		WallFollowerStrategy brokenLFStrat = new BrokenLandFWallFollowerStrategy(robot);
		
		// by default, we assume forward and left are operational
		WallFollowerStrategy currentStrategy = defaultStrat;
		
		// while we're not done
		while(!robot.isAtExit()) {

		    // if the user decides to pause the algorithm, we wait until the user
            // unpauses, checking every .1 seconds
		    while(paused){
		        Thread.currentThread().sleep(100);
            }
			
			// check to see if it's stopped
			if(robot.hasStopped()) {
				return false;
			}

			// pick a strategy
			

			// both forward and left sensors are not operational
			if(!operationalSensors[2] && !operationalSensors[0]) {
				currentStrategy = brokenLFStrat;
			}
			// just the left is not operational
			else if(!operationalSensors[0]) {
				currentStrategy = brokenLStrat;
			}
			// just the forward is not operational
			else if(!operationalSensors[2]) {
				currentStrategy = brokenFStrat;
			}
			
			
			// if there's a wall to the left, we move forward
			if(currentStrategy.senseLeft()) {
				if(robot.hasStopped()) {return false;} // check for a stopped robot
				// but, if there's a wall in front, too, then we turn right
				if(currentStrategy.senseForward()) {
					if(robot.hasStopped()) {return false;} // check for a stopped robot
					robot.rotate(Turn.RIGHT);
				}
				else {
					if(robot.hasStopped()) {return false;} // check for a stopped robot
					robot.move(1, false);
				}
			}
			// if we're at a corner, we turn left and move forward
			else {
				if(robot.hasStopped()) {return false;} // check for a stopped robot
				robot.rotate(Turn.LEFT);
				if(robot.hasStopped()) {return false;} // check for a stopped robot
				robot.move(1, false);
			}

			// wait, for graphics' sake
			Thread.currentThread().sleep(30);
		}
		if(robot.hasStopped()) {return false;} // check for a stopped robot
		
		// leave the maze
		return leaveMaze();
	}
	
	/**
	 * This method takes a robot at the exit position, senses the exit, and leaves
	 * the maze.
	 * @precondition the robot is at the exit; i.e. robot.isAtExit() is true
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
				try {
					if(robot.hasStopped()) {return false;} // check for a stopped robot
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

		if(exitFound) {
			if(robot.hasStopped()) {return false;} // check for a stopped robot
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
	
	
	/**
	 * Drives the robot towards the exit given it exists and 
	 * given the robot's energy supply lasts long enough. 
	 * 
	 * The robot does not leave the maze in this, so it's useful for testing
	 * 
	 * @return true if driver successfully reaches the exit, false otherwise
	 * @throws UnsupportedOperationException if there are no available sensors to use
	 * @throws IndexOutOfBoundsException possibly through position checks
	 */
	public boolean drive2ExitButDontLeave() throws Exception {
		// initialize strategies
		WallFollowerStrategy defaultStrat = new DefaultWallFollowerStrategy(robot);
		WallFollowerStrategy brokenLStrat = new BrokenLeftWallFollowerStrategy(robot);
		WallFollowerStrategy brokenFStrat = new BrokenForwardWallFollowerStrategy(robot);
		WallFollowerStrategy brokenLFStrat = new BrokenLandFWallFollowerStrategy(robot);
		
		// by default, we assume forward and left are operational
		WallFollowerStrategy currentStrategy = defaultStrat;
		
		// while we're not done
		while(!robot.isAtExit()) {
			
			// check to see if it's stopped
			if(robot.hasStopped()) {
				return false;
			}
			
			// pick a strategy
			

			// both forward and left sensors are not operational
			if(!operationalSensors[2] && !operationalSensors[0]) {
				currentStrategy = brokenLFStrat;
			}
			// just the left is not operational
			else if(!operationalSensors[0]) {
				currentStrategy = brokenLStrat;
			}
			// just the forward is not operational
			else if(!operationalSensors[2]) {
				currentStrategy = brokenFStrat;
			}
			
			
			// if there's a wall to the left, we move forward
			if(currentStrategy.senseLeft()) {
				// but, if there's a wall in front, too, then we turn right
				if(currentStrategy.senseForward()) {
					robot.rotate(Turn.RIGHT);
				}
				else {
					robot.move(1, false);
				}
			}
			// if we're at a corner, we turn left and move forward
			else {
				robot.rotate(Turn.LEFT);
				robot.move(1, false);
			}
			
		}
		
		return true;
	}

}
