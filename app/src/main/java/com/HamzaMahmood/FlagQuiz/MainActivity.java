package com.HamzaMahmood.FlagQuiz;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.util.Set;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    //Here we define constants for the preference keys
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    //phoneDevice specifies whether the app is running on a phone
    //if so, the app will only allow portrait mode
    private boolean phoneDevice = true;
    //preferencesChanged speficies whether the apps preferences have changed
    //initially set to true so app starts using default preferences
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set default values in the app's SharedPReferences
        PreferenceManager.setDefaultValues(this,R.xml.preferences, false);


        //register listener for SharedPreferences Changes
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        //determine screensize
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        //if the device is a tablet, set phoneDevice to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            phoneDevice = false; //not a phonesized device
        }

        //if device is phone sized, allow only portrait mode
        if (phoneDevice){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged){
            //now that the default preferences have been set
            //initialize main activity fragment and start the quiz
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    //show menu if app is running on a phone or a portrait-oriented tablet
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        //get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        //display the app's menu only in portrait orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            //inflate the menu
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else {
            return false;
        }

    }


    //displays the SettingsActivity when running on a phone
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    //listener for changes to the app's SharedPreferences
    private OnSharedPreferenceChangeListener preferencesChangeListener = new OnSharedPreferenceChangeListener() {
        //called when the user changes the app's preferences
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            preferencesChanged = true; //user changed app settings
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);

            if (key.equals(CHOICES)){
                //#of choices to display changed
                quizFragment.updateGuessRows(sharedPreferences);
                quizFragment.resetQuiz();
            } else if (key.equals(REGIONS)){
                Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                if (regions != null && regions.size() > 0 ){
                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();
                } else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    regions.add(getString(R.string.default_region));
                    editor.putStringSet(REGIONS, regions);
                    editor.apply(); //the apply method commits the changes


                    //Now we use a Toast to indicate the default region was set.
                    Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                }
            }

            //Regardless of which pref was changed, we display a Toast indicating the quiz will be reset with new prefs.
            Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }
    };







}
