package edu.wm.cs.cs301.nicholaswright.gui;

import android.util.Log;

import edu.wm.cs.cs301.nicholaswright.gui.Robot.Direction;

/**
 * This class implements the process for a robot's sensors failing and being repaired
 * in a separate thread. Each instance refers to a sensor in one direction.
 * 
 * This process happens at an interval of delta_t, which by default is 3 seconds.
 * 
 * Collaborates with both a Robot and a Robot Driver.
 * 
 * @author Nicholas Wright
 *
 */
public class Sensor implements Runnable {

	private static final String TAG = "Sensor";

	private Robot robot;
	private RobotDriver driver;
	private Direction dir;
	
	
	private final int delta_t = 3; // how long do we wait? (in seconds)
	
	/**
	 * Constructor which sets the necessary references for robot, driver, and direction.
	 * @param robot is the robot associated with this sensor
	 * @param driver is the driver of the robot
	 * @param dir is the direction of the sensor this runnable acts on
	 */
	public Sensor(Robot robot, RobotDriver driver, Direction dir) {
		this.robot = robot;
		this.driver = driver;
		this.dir = dir;
	}
	
	/**
	 * Causes a the sensor to fail, waits delta_t, and then repairs it.
	 * Each time a change is made, this method notifies the driver.
	 * This happens until the thread is interrupted.
	 */
	@Override
	public void run() {

		Log.v(TAG, "Starting " + dir + " thread");
		try{
			//Thread.sleep(offset);
			while(true) {
				Thread.sleep(1000*delta_t);
				Log.v(TAG, dir + " sensor failing");
				robot.triggerSensorFailure(dir);
				driver.triggerUpdateSensorInformation();
				Thread.sleep(1000*delta_t);
				Log.v(TAG, "Repairing " + dir + " sensor");
				robot.repairFailedSensor(dir);
				driver.triggerUpdateSensorInformation();
			}
		}
		// if something happens to this thread; we set the sensor to operational
		// before we end things
		catch(InterruptedException e) {
			robot.repairFailedSensor(dir);
			driver.triggerUpdateSensorInformation();
		}
	}
	


}
