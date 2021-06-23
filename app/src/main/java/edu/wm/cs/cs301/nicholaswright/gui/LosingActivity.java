package edu.wm.cs.cs301.nicholaswright.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.wm.cs.cs301.nicholaswright.R;

/**
 * Activity that displays a failure message and information about
 * performance in trying to solve a maze.
 *
 * This activity is started by either PlayAnimationActivity or PlayManuallyActivity,
 * and it is given energy consumption, path length, and minimum path length for the
 * maze just failed.
 *
 * From this activity the only option is to return to the title screen (AMazeActivity)
 * and start again.
 *
 * @author Nicholas Wright
 */
public class LosingActivity extends AppCompatActivity {

    // for logging
    private static final String TAG = "LosingActivity";

    // UI Components
    private TextView pathLengthView;
    private TextView minPathLengthView;
    private TextView energyConsumedView;
    private Button to_title_button;

    // data about the game
    private float energyConsumed;
    private int pathLength;
    private int minPathLength;

    /**
     * Creates buttons to navigate back to the title, displays results from the game
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting Activity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_losing);


        initUI();
        getDataFromPlaying();

        pathLengthView.append(" "+pathLength);
        minPathLengthView.append(" "+minPathLength);
        energyConsumedView.append(" "+energyConsumed);

        minPathLengthView.setVisibility(View.INVISIBLE); // until I can get this right

    }

    /**
     * Initializes UI components
     */
    private void initUI(){
        // Navigation Button

        to_title_button = findViewById(R.id.button_lose2title);
        to_title_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "to title button tapped");
                Intent intent = new Intent(LosingActivity.this, AMazeActivity.class);
                Log.v(TAG, "Switching to title");
                startActivity(intent);
                finish();
            }
        });

        pathLengthView = findViewById(R.id.path_lose);
        minPathLengthView = findViewById(R.id.min_path_lose);
        energyConsumedView = findViewById(R.id.energy_consumed_lose);
    }

    /**
     * Gathers data from the intent sent by one of the Playing activities
     */
    private void getDataFromPlaying(){
        Intent intent = getIntent();
        Bundle data = intent.getExtras();

        pathLength = data.getInt("path_length");
        minPathLength = data.getInt("min_path_length");
        energyConsumed = data.getFloat("energy_consumed");
    }
}
