package edu.wm.cs.cs301.nicholaswright.gui;

/**
 * Class to encapsulate the notion of a robot's battery.
 * 
 * Used by BasicRobot class
 * 
 * @author Nicholas Wright
 *
 */
public class RobotBattery {

	private float maxBatteryLevel;
	private float currentBatteryLevel;
	
	public RobotBattery(float max) {
		maxBatteryLevel = max;
		currentBatteryLevel = max;
	}
	
	/**
	 * @return current battery level
	 */
	public float getBatteryLevel() {
		return currentBatteryLevel;
	}
	
	/**
	 * Sets the battery's energy to a given level
	 * @param level amount of energy the battery should have
	 */
	public void setBatteryLevel(float level) {
		currentBatteryLevel = level;
	}
	
	/**
	 * Decrements the battery's energy by a given amount
	 * @param amount to reduce the current energy level by
	 */
	public void drainBattery(float amount) {
		currentBatteryLevel -= amount;
	}
	
	/**
	 * Checks to see if the battery is depleted
	 * @return true if the current energy is non-positive
	 */
	public boolean isEmpty() {
		return currentBatteryLevel <= 0;
	}
	
	/**
	 * Sets the battery back to its max level
	 */
	public void rechargeToFull() {
		currentBatteryLevel = maxBatteryLevel;
	}
}
