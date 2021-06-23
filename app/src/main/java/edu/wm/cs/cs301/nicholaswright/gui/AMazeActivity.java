package edu.wm.cs.cs301.nicholaswright.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.google.android.material.snackbar.Snackbar;

import edu.wm.cs.cs301.nicholaswright.R;

/**
 * Main activity for this application. Holds the title screen, with spinners and
 * a seek bar to select robot driver, generation algorithm, and maze complexity
 * (skill level).
 *
 * Passes those mazeData to GeneratingActivity with the tap of either of two buttons. One
 * generates a new maze, and the other finds an existing maze to load. The distinction
 * is made by passing a "newMaze" boolean to GeneratingActivity
 *
 * This activity is started by the system on the application's startup, via buttons on
 * WinningActivity and LosingActivity, or by pressing the back button anywhere during
 * the application.
 *
 * @author Nicholas Wright
 */
public class AMazeActivity extends AppCompatActivity {

    // for logging
    private static final String TAG = "AMazeActivity";

    // references to UI components
    private Spinner driver_spinner;
    private Spinner generation_spinner;
    private SeekBar skillBar;
    private Button generate_button;
    private Button load_maze_button;

    // mazeData to be passed to the next activity
    private String driver_selection;
    private String generation_selection;
    private int skillLevel;

    /**
     * called when the activity is started; i.e. at the application's start,
     * from Losing/Winning activities, or the "back" button
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting Activity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amaze);

        // set text content on driver spinner
        driver_spinner = findViewById(R.id.spinner_driver);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.driver_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        driver_spinner.setAdapter(adapter);

        // set text content on generation spinner
        generation_spinner = findViewById(R.id.spinner_generation);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.generation_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        generation_spinner.setAdapter(adapter);

        skillBar = findViewById(R.id.skillBar);

        // make the generate button work
        generate_button = findViewById(R.id.button_generate);
        generate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "generate button tapped");
                Bundle data = getSelections();
                data.putBoolean("newMaze", true); // tell the next activity to generate a new maze


                Intent intent = new Intent(AMazeActivity.this, GeneratingActivity.class);
                intent.putExtras(data);
                Log.v(TAG, "Sending: " + driver_selection +
                        ", " + generation_selection + ", " + skillLevel);
                startActivity(intent);
            }
        });

        // make the load maze button work
        load_maze_button = findViewById(R.id.button_load);
        load_maze_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "load maze button tapped");
                Bundle data = getSelections();
                data.putBoolean("newMaze", false); // tell the next activity to load an old maze

                Intent intent = new Intent(AMazeActivity.this, GeneratingActivity.class);
                intent.putExtras(data);
                Log.v(TAG, "Sending: " + driver_selection +
                        ", " + generation_selection + ", " + skillLevel);
                Snackbar snackbar = Snackbar.make(findViewById(R.id.title_layout),
                        "Sending " + driver_selection + ", " + generation_selection + ", " + skillLevel,
                        Snackbar.LENGTH_SHORT);
                snackbar.show();
                startActivity(intent);
            }
        });

    }

    /**
     * Gathers selections from the seek bar and the spinners and bundles them.
     * @return a Bundle containing the driver, generation algorithm, and skill level
     */
    private Bundle getSelections(){
        // get current selections
        driver_selection     = driver_spinner.getSelectedItem().toString();
        generation_selection = generation_spinner.getSelectedItem().toString();
        skillLevel           = skillBar.getProgress();

        // bundle up the information to send
        Bundle b = new Bundle();
        b.putString("driver", driver_selection);
        b.putString("generation", generation_selection);
        b.putInt("skillLevel", skillLevel);

        return b;
    }
}
