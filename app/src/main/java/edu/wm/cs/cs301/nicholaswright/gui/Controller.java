package edu.wm.cs.cs301.nicholaswright.gui;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import edu.wm.cs.cs301.nicholaswright.generation.CardinalDirection;
import edu.wm.cs.cs301.nicholaswright.generation.Maze;
import edu.wm.cs.cs301.nicholaswright.gui.Constants.UserInput;

/**
 * The Controller class handles the interaction between the Playing activities and the StatePlaying
 * object. For PlayAnimationActivity, this also handles the robot and driver mechanisms.
 * This is mostly a legacy from the old Maze Game codebase.
 */
public class Controller {

    // for logging
    private static final String TAG = "Controller";

    /**
     * Either PlayManuallyActivity or PlayAnimationActivity
     */
    private AppCompatActivity activity;
    /**
     * The current state of the controller and the game.
     * All state objects share the same interface and can be
     * operated in the same way, although the behavior is
     * vastly different.
     * currentState is never null and only updated by
     * switchFrom .. To .. methods.
     */
    private StatePlaying currentState;
    /**
     * The panel is used to draw on the screen for the UI.
     * It can be set to null for dry-running the controller
     * for testing purposes but otherwise panel is never null.
     */
    private MazePanel panel;
    /**
     * The filename is optional, may be null, and tells
     * if a maze is loaded from this file and not generated.
     */
    private String fileName;

    /**
     * Specifies if the maze's robot has lost the maze game;
     * i.e. it has run out of energy. This affects the winning
     * screen.
     */
    boolean lost;

    public Controller(AppCompatActivity playing_activity) {
        currentState = new StatePlaying();
        activity = playing_activity;
        if(activity instanceof PlayManuallyActivity) {
            Log.v(TAG, "instance of PlayManually");
            panel = ((PlayManuallyActivity) activity).getPanel();
        }
        else {
            Log.v(TAG, "instance of PlayAnimation");
            panel = ((PlayAnimationActivity) activity).getPanel();
        }

        Log.v(TAG, "Panel is not null: " + (panel != null));
        fileName = null;
        lost = false;
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public MazePanel getPanel() {
        return panel;
    }
    /**
     * Starts the controller and begins the game
     * with the title screen.
     */
    public void start(Maze config) {

        // the following code comes from the previous SwitchfromGeneratingToPlaying

        lost = false; // in case it doesn't get re-initialized after a failed game
        currentState.setMazeConfiguration(config);

        // these are initialized if something other than a manual driver is selected
        if(robot != null && driver != null) {
            robot.setMaze(this);
            driver.setDimensions(config.getWidth(), config.getHeight());
            driver.setDistance(config.getMazedists());
        }

        currentState.start(this, panel);

        // By default, have the map and the solution show up
        currentState.keyDown(UserInput.ToggleLocalMap, 0);
        currentState.keyDown(UserInput.ToggleFullMap, 0);
        currentState.keyDown(UserInput.ToggleSolution, 0);
        // crude fix to the map showing really small
        for(int i = 0; i < 50; i++)
            currentState.keyDown(UserInput.ZoomIn,0);
    }

    /**
     * Switches the controller to the final screen
     * @param pathLength gives the length of the path
     */
    public void switchFromPlayingToWinning(int pathLength) {
        float energyConsumed = currentState.getEnergyConsumed();

        if(driver != null){
            driver.killAllSensors();
        }

        // send a message to the activity to win or lose

        if(lost) {
            if (activity instanceof PlayManuallyActivity) {
                Log.v("Controller", "instance of PlayManually");
                ((PlayManuallyActivity) activity).lose(pathLength, energyConsumed);
            } else {
                Log.v("Controller", "instance of PlayAnimation");
                ((PlayAnimationActivity) activity).lose(pathLength, energyConsumed);
            }
        }
        else{
            if (activity instanceof PlayManuallyActivity) {
                Log.v("Controller", "instance of PlayManually");
                ((PlayManuallyActivity) activity).win(pathLength, energyConsumed);
            } else {
                Log.v("Controller", "instance of PlayAnimation");
                ((PlayAnimationActivity) activity).win(pathLength, energyConsumed);
            }
        }
    }


    /**
     * Method incorporates all reactions to keyboard input in original code.
     * The simple key listener calls this method to communicate input.
     */
    public boolean keyDown(Constants.UserInput key, int value) {
        // delegated to state object
        return currentState.keyDown(key, value);
    }
    /**
     * Turns of graphics to dry-run controller for testing purposes.
     * This is irreversible.
     */
    public void turnOffGraphics() {
        panel = null;
    }
      //// Extension in preparation for Project 3: robot and robot driver //////
    /**
     * The robot that interacts with the controller starting from P3
     */
    Robot robot;
    /**
     * The driver that interacts with the robot starting from P3
     */
    RobotDriver driver;

    /**
     * Takes the name of an driver type (Wizard, Wall Follower, or Manual),
     * creates a driver of that type and a BasicRobot, and sets them to
     * this controller.
     *
     * This method allows the selection screen to set these variables.
     *
     * @param robotDriver is a string holding the name of the driver type
     */
    public void initRobotAndDriver(String robotDriver) {

        Log.v(TAG, "initializing driver " + robotDriver);
        RobotDriver driver = null;
        Robot robot = null;

        switch(robotDriver) {
            case "Manual":
                return;
            case "Wizard":
                robot = new BasicRobot();
                driver = new Wizard(robot);
                break;
            case "Wall Follower":
                robot = new BasicRobot();
                driver = new WallFollower(robot);
                break;
            default:
                throw new RuntimeException("Invalid robot driver selected");
        }

        setRobotAndDriver(robot, driver);
    }

    /**
     * Sets the robot and robot driver
     * @param robot
     * @param robotdriver
     */
    public void setRobotAndDriver(Robot robot, RobotDriver robotdriver) {
        this.robot = robot;
        driver = robotdriver;

    }
    /**
     * @return the robot, may be null
     */
    public Robot getRobot() {
        return robot;
    }
    /**
     * @return the driver, may be null
     */
    public RobotDriver getDriver() {
        return driver;
    }
    /**
     * Provides access to the maze configuration.
     * This is needed for a robot to be able to recognize walls
     * for the distance to walls calculation, to see if it
     * is in a room or at the exit.
     * Note that the current position is stored by the
     * controller. The maze itself is not changed during
     * the game.
     * This method should only be called in the playing state.
     * @return the MazeConfiguration
     */
    public Maze getMazeConfiguration() {
        return currentState.getMazeConfiguration();
    }
    /**
     * Provides access to the current position.
     * The controller keeps track of the current position
     * while the maze holds information about walls.
     * This method should only be called in the playing state.
     * @return the current position as [x,y] coordinates,
     * 0 <= x < width, 0 <= y < height
     */
    public int[] getCurrentPosition() {
        return currentState.getCurrentPosition();
    }
    /**
     * Provides access to the current direction.
     * The controller keeps track of the current position
     * and direction while the maze holds information about walls.
     * This method should only be called in the playing state.
     * @return the current direction
     */
    public CardinalDirection getCurrentDirection() {
        return currentState.getCurrentDirection();
    }

    /**
     * Mimics a left turn keystroke for the playing state
     * This method should only be called in the playing state
     *
     * Can trigger the end of the game if the robot is stopped
     */
    public void rotateLeft() {
        // first, check to see if the robot is still working
        if(getRobot().hasStopped()) {
            lose();
        }
        currentState.keyDown(UserInput.Left, 0);
    }

    /**
     * Mimics a right turn keystroke for the playing state
     * This method should only be called in the playing state
     *
     * Can trigger the end of the game if the robot is stopped
     */
    public void rotateRight() {
        // first, check to see if the robot is still working
        if(getRobot().hasStopped()) {
            lose();
        }
        currentState.keyDown(UserInput.Right, 0);
    }

    /**
     * Mimics a forward move keystroke for the playing state
     * This method should only be called in the playing state
     *
     * Can trigger the end of the game if the robot is stopped
     */
    public void move() {
        // first, check to see if the robot is still working
        if(getRobot().hasStopped()) {
            lose();
        }
        currentState.keyDown(UserInput.Up, 0);
    }

    /**
     * Mimics a jump keystroke for the playing state
     * This method should only be called in the playing state
     *
     * Can trigger the end of the game if the robot is stopped
     */
    public void jump() {
        // first, check to see if the robot is still working
        if(getRobot().hasStopped()) {
            lose();
        }
        currentState.keyDown(Constants.UserInput.Jump, 0);
    }

    /**
     * @return the total energy consumed by the robot/user
     */
    public float getEnergyConsumed(){
        return currentState.getEnergyConsumed();
    }

    /**
     * Called if the robot has run out of energy. Triggers
     * the switch to the Losing Activity.
     */
    public void lose() {
        Log.v(TAG, "Losing");
        lost = true;
        this.switchFromPlayingToWinning(currentState.getPathLength());
    }

    /**
     * Called if the robot/user successfully leaves the maze.
     * Triggers the switch to the WinningActivity
     */
    public void win(){
        Log.v(TAG, "Winning");
        lost = false;
        this.switchFromPlayingToWinning(currentState.getPathLength());
    }

}
