package edu.wm.cs.cs301.nicholaswright.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.google.android.material.snackbar.Snackbar;

import java.util.Random;

import edu.wm.cs.cs301.nicholaswright.R;
import edu.wm.cs.cs301.nicholaswright.generation.Factory;
import edu.wm.cs.cs301.nicholaswright.generation.Maze;
import edu.wm.cs.cs301.nicholaswright.generation.MazeData;
import edu.wm.cs.cs301.nicholaswright.generation.MazeFactory;
import edu.wm.cs.cs301.nicholaswright.generation.Order;

/**
 * Activity for generating a maze in a background thread and showing progress via a
 * progress bar of that generation.
 *
 * Receives information about the skill level, generation algorithm, robot driver
 * algorithm, and whether to generate a new maze (or load an old one). The driver
 * information is used to determine which activity to start next (either manual or
 * animation), and in the case of a non-manual selection, the algorithm is sent
 * to the animation activity. The other information is used as parameters for
 * generating a maze.
 *
 * This activity is started only by AMazeActivity. From this state we can either move
 * back to the previous activity or to one of PlayManuallyActivity or PlayAnimationActivity.
 *
 * Uses a Thread to generate the maze, a Handler object to update progress on the UI.
 * The Back button here is overridden to interrupt that thread.
 *
 * @author Nicholas Wright
 */
public class GeneratingActivity extends AppCompatActivity implements Order {

    // for logging
    private static final String TAG = "GeneratingActivity";

    // references to UI components
    private ProgressBar progressBar;
    private TextView progressText;
    private Handler handler = new Handler();
    private TextView title;

    private int currentProgress = 0; // current progress to display, out of 100

    // maze generation information
    // received from AMazeActivity
    private boolean newMaze; // true if we create a new maze, false if we load an existing one
    private Builder builder;  // the builder
    private String builder_name; // the name of the generation algorithm
    private int skillLevel; // complexity of the maze. In the range (0, 15)

    // other maze generation information
    private Factory factory;
    private int seed;

    // name of the driver algorithm
    // received from AMazeActivity, passed to PlayAnimationActivity if not "manual"
    private String driver;

    // reference to the global maze variable
    MazeData mazeData;

    /**
     * Creates interactive UI features, gathers mazeData from Intent,
     * defines and starts generation thread.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generating);

        init();


        if(newMaze){
            // generate a new seed and set it
            setSeed(generateSeed());
        }
        else{
            // look into the preferences and set the seed
            setSeed(getSeedFromPreferences());
        }

        // overwrite info in preferences with the current seed
        // This won't change anything if we're not generating a new maze.
        saveSeedToPreferences();

        factory = new MazeFactory();

        // implement background thread to update progressbar
        factory.order(this);



    }

    /**
     * Gathers mazeData from the intent that started this activity and initializes
     * UI elements using that mazeData.
     */
    private void init(){
        Log.v(TAG, "Starting Activity");

        // get access to the mazeData container where we'll put the maze
        mazeData = MazeData.getInstance();
        // at this point, there should be nothing stored in MazeData
        if(mazeData.getMaze() != null){
            throw new RuntimeException("MazeData should by empty!");
        }

        // gather mazeData from title activity

        Intent intent_received = getIntent();
        Bundle data = intent_received.getExtras();

        newMaze = data.getBoolean("newMaze");
        Log.v(TAG, "Creating new maze? " + newMaze);

        driver = data.getString("driver");
        builder_name = data.getString("generation");
        setBuilder(builder_name);
        skillLevel = data.getInt("skillLevel");

        Log.v(TAG,"Data received: " + driver + ", " + builder_name + ", " + skillLevel);

        Snackbar snackbar = Snackbar.make(findViewById(R.id.generation_layout),
                "Received " + driver + ", " + builder_name + ", " + skillLevel,
                Snackbar.LENGTH_SHORT);
        snackbar.show();

        // update UI based on gathered information

        title = findViewById(R.id.title_generating);
        if(!newMaze) {
            // change title text to show that we're loading a maze
            title.setText("Loading a previous maze...");
        }
        // update title text to show generation algorithm, skill level
        title.append("\n" + builder_name + " Algorithm");
        title.append("\nLevel " + skillLevel);

        progressBar = findViewById(R.id.progress_bar_generating);
        progressText = findViewById(R.id.textView_progress);
    }

    /**
     * Sets the builder variable given a string containing its name
     * @param generation_selection
     */
    private void setBuilder(String generation_selection){
        switch(generation_selection){
            case "DFS":
                builder = Builder.DFS;
                break;
            case "Prim":
                builder = Builder.Prim;
                break;
            case "Eller":
                builder = Builder.Eller;
                break;
            default:
                Log.w(TAG, "Invalid builder selected");
        }
    }

    /**
     * Checks if the driver is manual or an algorithm. Creates an intent and starts
     * the corresponding activity (PlayManuallyActivity or PlayAnimationActivity),
     * passes necessary information along.
     */
    private void switch_to_playing(){
        // once progress is done, move to the next activity
        if(driver.equals("Manual")) {
            Log.v(TAG, "Switching to PlayManuallyActivity");
            Intent intent = new Intent(GeneratingActivity.this, PlayManuallyActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            Log.v(TAG, "Switching to PlayAnimationActivity and sending driver " + driver);
            Intent intent = new Intent(GeneratingActivity.this, PlayAnimationActivity.class);
            intent.putExtra("driver", driver);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Kills the thread that's generating the maze. Finishes the current activity,
     * moving us back to the title
     */
    @Override
    public void onDestroy(){
        factory.cancel();
        mazeData.setMaze(null);
        super.onDestroy();
    }



    //////////////////////////// Methods for the Order interface /////////////////////////
    @Override
    public int getSkillLevel() {
        return skillLevel;
    }

    @Override
    public Builder getBuilder() {
        return builder;
    }

    @Override
    public boolean isPerfect() {
        return false;
    }

    /**
     * This sets
     * @param mazeConfig the maze
     */
    @Override
    public void deliver(Maze mazeConfig) {
        mazeData.setMaze(mazeConfig);
        switch_to_playing();
    }

    /**
     * Updates the UI progress bar to the given percentage
     * @param percentage current percentage of job completion
     */
    @Override
    public void updateProgress(int percentage) {
        currentProgress = percentage;
        // Update the progress bar, text view
        handler.post(new Runnable() {
            public void run() {
                progressBar.setProgress(currentProgress);
                progressText.setText(currentProgress+"%");
            }
        });
    }

    /**
     * Returns the seed for this generation
     * @return
     */
    @Override
    public int getSeed(){
        return seed;
    }

    /**
     * Sets the seed field
     * @param seed
     */
    private void setSeed(int seed){
        this.seed = seed;
    }

    /**
     * Generates a random positive integer
     * @return a random positive integer
     */
    private int generateSeed(){
        Random r = new Random();
        return r.nextInt(Integer.MAX_VALUE);
    }

    /**
     * Looks into the local preferences file for a seed given the activity's current
     * builder and skill level fields (set according to the message sent from AMazeActivity).
     *
     * The String key for the lookup is "[builderName]_[skillLevel]".
     *
     * If a value is found, we return it. Otherwise, this method generates a new seed
     * and returns it.
     *
     * @return a seed from the preferences file or a new seed in the positive integer range
     */
    private int getSeedFromPreferences(){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        // generate the string key
        String key = builder_name + "_" + skillLevel;
        int savedSeed = preferences.getInt(key, -1);

        Log.v(TAG, "found seed = " + savedSeed + " in preferences");
        if(savedSeed == -1){
            // we got the default value; i.e. nothing was there
            Log.v(TAG, "Seed key wasn't found. Generating a new seed");
            savedSeed = generateSeed();
        }

        return savedSeed;
    }

    /**
     * Opens the preferences file and saves the current generation fields (builder, skill,
     * and generation seed) to the local preferences.
     * Generates a String key for the lookup whose value is "[builderName]_[skillLevel]".
     * This method should be called every time this class is created.
     */
    private void saveSeedToPreferences(){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        //initializing editor
        SharedPreferences.Editor editor = preferences.edit();

        // generate the string key
        String key = builder_name + "_" + skillLevel;

        Log.v(TAG, "Saving " + key + ": " + seed + " to preferences");

        // put the seed into the preferences with the given
        editor.putInt(key, seed);

        // save changes
        editor.apply();
    }
}
