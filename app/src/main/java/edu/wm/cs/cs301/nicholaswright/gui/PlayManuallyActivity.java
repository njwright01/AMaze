package edu.wm.cs.cs301.nicholaswright.gui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Intent;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import edu.wm.cs.cs301.nicholaswright.gui.Constants.UserInput;

/**
 * UI activity for a user to navigate through a maze.
 *
 * This activity is started by the GeneratingActivity when it is given a driver
 * that is manual.
 *
 * Based on the success/failure of the user, this activity either starts
 * WinningActivity or LosingActivity. The back button returns to AMazeActivity.
 * Data about the user's performance is passed along.
 *
 * @author Nicholas Wright
 */
import edu.wm.cs.cs301.nicholaswright.R;
import edu.wm.cs.cs301.nicholaswright.generation.MazeData;

public class PlayManuallyActivity extends AppCompatActivity {

    // for logging
    private static final String TAG = "PlayManuallyActivity";

    // performace mazeData
    private float energy_consumed = 0;
    private int path_length = 0;
    private int min_path_length = 0;

    // reference to the global maze variable
    MazeData mazeData;

    // UI components
    private MazePanel panel;
    private Button forward;
    private Button backward;
    private Button left;
    private Button right;
    private Button show_maze;
    private Button show_walls;
    private Button show_solution;
    private Button zoom_in;
    private Button zoom_out;
    private ProgressBar energyBar;

    // Controller which handles UI and updates graphics
    private Controller controller;

    private GestureDetectorCompat detector;

    /**
     * Creates navigation buttons for project 6
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting Activity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_manually);

        initUI();

        // get a reference to the maze object
        mazeData = MazeData.getInstance();

        // sets up swipe gesture listener
        detector = new GestureDetectorCompat(this, new SwipeListener());

        // starts the game
        controller = new Controller(this);
        controller.start(mazeData.getMaze());
    }

    /**
     * Accessor method to give access to the panel
     */
    public MazePanel getPanel(){
        return panel;
    }

    /**
     * Initializes UI components for the activity. Should be called early on
     * in onCreate();
     */
    private void initUI(){
        // get reference to ProgressBar
        energyBar = findViewById(R.id.progressBar_manual);

        // get references to buttons
        forward = findViewById(R.id.button_up);
        backward = findViewById(R.id.button_down);
        right = findViewById(R.id.button_right);
        left = findViewById(R.id.button_left);
        show_maze = findViewById(R.id.button_show_full);
        show_walls = findViewById(R.id.button_show_walls);
        show_solution = findViewById(R.id.button_show_solution);
        zoom_in = findViewById(R.id.button_zoom_in);
        zoom_out = findViewById(R.id.button_zoom_out);

        // set listeners
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "forward button tapped");
                controller.keyDown(UserInput.Up, 0);
                updateProgress();
            }
        });
        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "backward button tapped");
                controller.keyDown(UserInput.Down, 0);
                updateProgress();
            }
        });
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "left button tapped");
                controller.keyDown(UserInput.Left, 0);
                updateProgress();
            }
        });
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "right button tapped");
                controller.keyDown(UserInput.Right, 0);
                updateProgress();
            }
        });
        show_maze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "show full map button tapped");
                controller.keyDown(UserInput.ToggleFullMap, 0);
            }
        });
        show_walls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "show visible walls button tapped");
                controller.keyDown(UserInput.ToggleLocalMap, 0);
            }
        });
        show_solution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "solution button tapped");
                controller.keyDown(UserInput.ToggleSolution, 0);
            }
        });
        zoom_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "zoom in button tapped");
                for(int i = 0; i < 8; i++)
                    controller.keyDown(UserInput.ZoomIn, 0);
            }
        });
        zoom_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "zoom out button tapped");
                for(int i = 0; i < 8; i++)
                    controller.keyDown(UserInput.ZoomOut, 0);
            }
        });

        panel = findViewById(R.id.maze_panel_manual);
    }

    /**
     * Creates an intent to start WinningActivity and passes relevant mazeData
     */
    public void win(int pathLength, float energyConsumed){
        // reset global variable
        setMazetoNull();

        path_length = pathLength;
        energy_consumed = energyConsumed;

        Intent intent = new Intent(PlayManuallyActivity.this, WinningActivity.class);
        intent.putExtras(bundleGameData());
        Log.v(TAG, "Switching to WinningActivity");
        startActivity(intent);
        finish();
    }

    /**
     * Creates an intent to start LosingActivity and passes relevant mazeData
     */
    public void lose(int pathLength, float energyConsumed){
        // reset global variable
        setMazetoNull();

        path_length = pathLength;
        energy_consumed = energyConsumed;

        Intent intent = new Intent(PlayManuallyActivity.this, LosingActivity.class);
        intent.putExtras(bundleGameData());
        Log.v(TAG, "Switching to LosingActivity");
        startActivity(intent);
        finish();
    }

    /**
     * Bundles information about the user's performance to send to the next activity
     * @return a Bundle object containing energy consumed, path length, and the minimum path
     * length for this maze
     */
    private Bundle bundleGameData(){
        Log.v(TAG, "Bundling mazeData to pass forward. Energy consumed: " + energy_consumed +
                ", path length: " + path_length + "minimum path length: " + min_path_length);
        Bundle b = new Bundle();
        b.putFloat("energy_consumed", energy_consumed);
        b.putInt("path_length", path_length);
        b.putInt("min_path_length", min_path_length);
        return b;
    }

    /**
     * This accesses the global MazeData object and sets the maze to
     * null. This is essential for maintaining the global variable and
     * must be called before ending this activity.
     */
    private void setMazetoNull(){
        mazeData.setMaze(null);
    }

    /**
     * Updates the progress bar by asking the controller for the
     * current energy consumed
     */
    private void updateProgress(){
        float energy_remaining = controller.getEnergyConsumed();
        energy_remaining = 3000 - energy_remaining; // 3000 is default energy
        energyBar.setProgress((int)energy_remaining);
    }


    //////////////////////////////////// Gesture Events ////////////////////////////////////////////

    /**
     * Class to listen for swipe gestures. Should be used by a GestureDetectorCompat object
     */
    private class SwipeListener extends GestureDetector.SimpleOnGestureListener{
        private static final int SWIPE_MIN_DISTANCE = 150;
        private static final int SWIPE_THRESHOLD_VELOCITY = 100;

        /**
         * Determines the direction of the fling and calls the appropriate method
         */
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            if (event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                onLeftSwipe();
                return true;
            }
            else if (event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                onRightSwipe();
                return true;
            }

            if (event1.getY() - event2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                onUpSwipe();
                return true;
            }
            else if (event2.getY() - event1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                onDownSwipe();
                return true;
            }
            return false;
        }

    }

    /**
     * Connects the GestureDetectorCompat object to the activity
     * @param event a touch event
     * @return true sometimes and false some other times
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.detector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    // These methods work just like the button presses
    /**
     * Simulates a right keystroke
     */
    private void onRightSwipe(){
        Log.v(TAG, "swiped right");
        controller.keyDown(UserInput.Right, 0);
        updateProgress();
    }
    /**
     * Simulates a left keystroke
     */
    private void onLeftSwipe(){
        Log.v(TAG, "swiped left");
        controller.keyDown(UserInput.Left, 0);
        updateProgress();
    }
    /**
     * Simulates an up keystroke
     */
    private void onUpSwipe(){
        Log.v(TAG, "swiped up");
        controller.keyDown(UserInput.Up, 0);
        updateProgress();
    }
    /**
     * Simulates a down keystroke
     */
    private void onDownSwipe(){
        Log.v(TAG, "swiped down");
        controller.keyDown(UserInput.Down, 0);
        updateProgress();
    }

}
