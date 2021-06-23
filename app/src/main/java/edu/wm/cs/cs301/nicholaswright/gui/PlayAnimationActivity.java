package edu.wm.cs.cs301.nicholaswright.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import edu.wm.cs.cs301.nicholaswright.R;
import edu.wm.cs.cs301.nicholaswright.generation.MazeData;
import edu.wm.cs.cs301.nicholaswright.gui.Constants.UserInput;

/**
 * This activity runs a driver_string algorithm through a maze and shows the animation.
 *
 * This activity is started by the GeneratingActivity when it is given a driver_string
 * that is not manual. That selection is passed to this activity by GeneratingActivity
 *
 * Based on the success/failure of the algorithm, this activity either starts
 * WinningActivity or LosingActivity. The back button returns to AMazeActivity.
 * Data about the algorithm's performance is sent to the next screen for display.
 *
 * @author Nicholas Wright
 */
public class PlayAnimationActivity extends AppCompatActivity {

    // for logging
    private static final String TAG = "PlayAnimationActivity";

    // UI components
    private TextView title;
    private MazePanel panel;
    private Button show_maze;
    private Button show_walls;
    private Button show_solution;
    private Button zoom_in;
    private Button zoom_out;
    private Button forward;
    private Button backward;
    private Button left;
    private Button right;
    private ToggleButton pause;

    // mazeData received from GeneratingActivity
    private String driver_string;

    // information about the algorithm's performance
    // sent to the winning/losing activity
    private float energy_consumed = 0;
    private int path_length = 0;
    private int min_path_length = 0;

    // maze traversal
    private Controller controller;
    private Robot robot;
    private RobotDriver driver;
    driveAsync driveTask;

    // reference to the global maze variable
    MazeData mazeData;

    /**
     * Gets information from the Intent, creates buttons to
     * navigate to winning/losing (just for Project 6)
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting Activity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_animation);

        getDataFromGenerating();
        initUI();


        mazeData = MazeData.getInstance();

        controller = new Controller(this);
        controller.initRobotAndDriver(driver_string);

        controller.start(mazeData.getMaze());


        driver = controller.getDriver();
        driveTask = new driveAsync();
        driveTask.execute();
    }

    /**
     * Handles creation of the animation thread and its interaction with the
     * UI thread.
     */
    private class driveAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Log.v(TAG, "Starting driver algorithm");
                // drive through the maze
                boolean mazeSolved = driver.drive2Exit();
                // did we reach the end?
                if(!mazeSolved) {
                    controller.lose();
                }
            }
            catch (UnsupportedOperationException e) {
                Log.v(TAG, "in drive2Exit: Sensor failure");
                controller.lose();
            }
            catch(Exception e){
                Log.v(TAG, "In drive2exit: an error");
                controller.lose();
            }


            return null;
        }

    }

    /**
     * Initialized UI components. Should be called after
     * getDataFromGenerating, as the driver_string information
     * is used here.
     */
    private void initUI(){
        panel = findViewById(R.id.maze_panel_animation);

        // update title to display driver_string
        title = findViewById(R.id.title_animation);
        title.setText(driver_string);

        // get references
        show_maze = findViewById(R.id.button_show_full);
        show_walls = findViewById(R.id.button_show_walls);
        show_solution = findViewById(R.id.button_show_solution);
        zoom_in = findViewById(R.id.button_zoom_in);
        zoom_out = findViewById(R.id.button_zoom_out);
        forward = findViewById(R.id.button_up);
        backward = findViewById(R.id.button_down);
        right = findViewById(R.id.button_right);
        left = findViewById(R.id.button_left);
        pause = findViewById(R.id.toggleButton_animation);

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
        forward.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.v(TAG, "forward button tapped. toggling sensor thread");
                driver.toggleSensorThread(Robot.Direction.FORWARD);
            }
        });
        backward.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.v(TAG, "backward button tapped. toggling sensor thread");
                driver.toggleSensorThread(Robot.Direction.BACKWARD);
            }
        });
        left.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.v(TAG, "left button tapped. toggling sensor thread");
                driver.toggleSensorThread(Robot.Direction.LEFT);
            }
        });
        right.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.v(TAG, "right button tapped. toggling sensor thread");
                driver.toggleSensorThread(Robot.Direction.RIGHT);
            }
        });

        // Toggle button makes a call to the pause functionality of the driver
        pause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.v(TAG, "Unpausing animation");
                    driver.togglePaused();
                } else {
                    Log.v(TAG, "Pausing animation");
                    driver.togglePaused();
                }
            }
        });

    }



    /**
     * Gathers data sent by the intent from GeneratingActivity
     */
    private void getDataFromGenerating(){
        // get driver_string information from generation activity
        Intent intent_received = getIntent();
        driver_string = intent_received.getStringExtra("driver");

        Log.v(TAG, "Received driver " + driver_string + " from generating state");
        Snackbar snackbar = Snackbar.make(findViewById(R.id.animation_layout),
                "Received " + driver_string,
                Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    /**
     * Accessor method to give access to the panel
     */
    public MazePanel getPanel(){
        return panel;
    }

    /**
     * Creates an intent to start WinningActivity and passes relevant mazeData
     */
    public void win(int pathLength, float energyConsumed){
        // reset global variable
        setMazetoNull();

        path_length = pathLength;
        energy_consumed = energyConsumed;

        Intent intent = new Intent(PlayAnimationActivity.this, WinningActivity.class);
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

        Intent intent = new Intent(PlayAnimationActivity.this, LosingActivity.class);
        intent.putExtras(bundleGameData());
        Log.v(TAG, "Switching to LosingActivity");
        startActivity(intent);
        finish();
    }

    /**
     * Bundles information about the algorithm's performance to send to the next activity
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
}
