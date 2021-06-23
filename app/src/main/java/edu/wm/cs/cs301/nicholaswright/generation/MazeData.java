package edu.wm.cs.cs301.nicholaswright.generation;

import android.util.Log;

/**
 * Singleton class to control access to the maze configuration. The MazeContainer object is
 * stored in a private field and accessed by get/set methods.
 *
 * The lifecycle of this object is as follows:
 *   - GeneratingActivity creates the MazeContainer object, which is stored here
 *   - either PlayManuallyActivity or PlayAnimationActivity obtain a reference to the maze and
 *     use it to play the maze game
 *   - After the game's end, the activity sets the maze field to null
 *
 * No class besides GeneratingActivity, PlayManuallyActivity, and PlayAnimationActivity should
 * access the data here.
 */
public class MazeData {
    // Logging tag
    private static final String TAG = "MazeData";

    // holds the actual data
    private static Maze maze;

    // for making this a singleton
    private static MazeData instance;
    private MazeData(){}

    public static MazeData getInstance(){
        if(instance == null){
            instance = new MazeData();
        }
        return instance;
    }

    public Maze getMaze(){
        return maze;
    }

    public void setMaze(Maze new_maze){
        Log.v(TAG, "Setting maze to " + new_maze);
        maze = new_maze;
    }
}
