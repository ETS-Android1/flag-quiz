package com.HamzaMahmood.FlagQuiz;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class MainActivityFragment extends DialogFragment {

    //TAG used for logging error messages
    private static final String TAG = "FlagQuiz Activity";
    //Number of flags in the quiz
    private static final int FLAGS_IN_QUIZ = 10;

    //private static DialogFragment quizResults;
    //Flag file names
    //flagNameList holds the flag-image file names for the current
    private List<String> fileNameList;
    //countries in current quiz
    private List<String> quizCountriesList;
    //world regions in current quiz
    private Set<String> regionsSet;


    //correct country for the current flag
    private String correctAnswer;
    //number of guesses made
    private int totalGuesses;
    //number of correct guesses so far, this will eventually be equal to
    private int correctAnswers;
    //number of rows displaying guess Buttons
    private int guessRows;
    //random is used to randomize the quiz
    private SecureRandom random;

    //handler used to delay loading next flag
    private Handler handler;
    //shakeAnimation used for animation for incorrect guess
    private Animation shakeAnimation;

    //layout that contains the quiz
    private LinearLayout quizLinearLayout;
    //Textview that shows current question number
    private TextView questionNumberTextView;
    //Imageview that displays a flag
    private ImageView flagImageView;
    //Array holds rows of answer Buttons
    private LinearLayout[] guessLinearLayouts;
    //TextView that displays correct answer
    private TextView answerTextView;

    public MainActivityFragment() {
        // required empty constructor
    }

    //configures the MainActivityFragment when its View is created
    //onCreateView inflates the GUI and initializes most of MAFragment's instance variables.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //regions and countries in the current quiz.
        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //load the shake animation thats used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); //animation repeats 3 times

        //get references to GUI components
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);


        //Here we get references to various GUI components that we'll manipulate.
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);


        //configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        //set questionNumberTextView's text
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        //return the fragments view for display
        return view;

    }

    //updateGuessRows will update guessRows based on value in sharedPreferences
    //called from the app's MainActivity when the app is launched and each
    //time the user changes the # of guess buttons to display with each flag.
    public void updateGuessRows(SharedPreferences sharedPreferences) {

        //get the number of guess buttons that should be displayed.
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);

        guessRows = Integer.parseInt(choices) / 2;

        //hide all guess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        //display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);

    }


    //Method updateRegions is called from the app's MainActivity when app is launched
    public void updateRegions(SharedPreferences sharedPreferences) {
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }


    //resetQuiz() sets up and starts a quiz
    public void resetQuiz() {

        //use AssetManager to get image file names for enabled regions
        //we used AssetManager to access the folders contents
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); //empty list of image file names

        try {
            //loop through each region
            for (String region : regionsSet) {
                //get a list of all flag image files in this region
                String[] paths = assets.list(region);

                //here we remove the .png extension from each file name
                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));

            }
        } //AssetManagers list method throws an exception that we must catch
        catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

        //reset the number of correct answers made
        correctAnswers = 0;
        //reset the total number of guesses the user made
        totalGuesses = 0;
        //clear prior list of quiz countries
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //add FLAGS_IN_QUIZ random file names to the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            //get the random file name
            String filename = fileNameList.get(randomIndex);

            //if the region is enabled it hasnt already been chosen
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename); //add the file to the list
                ++flagCounter;
            }
        }

        //start the quiz by loading the first flag
        loadNextFlag();
    }

    //Method loadNextFlag loads and displays the next flag and the corresponding
    //set of answer Buttons.
    private void loadNextFlag() {
        //get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        //update the correct answer
        correctAnswer = nextImage;

        //clear the answerTextView
        //Next we clear the textview and display the current question number
        answerTextView.setText("");
        //display the current question number
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));


        //extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));
        AssetManager assets = getActivity().getAssets();

        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")) {
            //load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
            animate(false);
        } catch (IOException exception) {
            Log.e(TAG, "Error loading" + nextImage, exception);
        }

        //Then we shuffle the fileNameList
        Collections.shuffle(fileNameList);

        //put the correct answer at the end of the fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        for (int row = 0; row < guessRows; row++){
            //place buttons in currentTablerow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){
                //get reference to button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);


                //get country name and set it as newGuessbuttons'text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //randomly replace one Button with the correct Answer
        int row = random.nextInt(guessRows); //pick random row
        int column = random.nextInt(2); //pick random col
        LinearLayout randomRow = guessLinearLayouts[row]; //get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }


    //Method getCountryName parses the country name from the image file name.
    private String getCountryName(String name){
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    //Method Animate executes the circular reveal animation on the entire layout of the quiz
    //to tranition between questions.
    private void animate(boolean animateOut){

        //prevent animation into the UI for the first flag
        if (correctAnswers == 0)
            return;
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        if (animateOut){
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX,centerY,radius,0);
            animator.addListener(new AnimatorListenerAdapter() {
                //called when the animation finishes
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });
        }
        else {
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout,centerX,centerY,0,radius);
        }

        //set animation duration to 500ms
        animator.setDuration(500);
        //start the animation
        animator.start();
    }

    //Anonymous inner class guessButtonListen implements OnClickListener
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            Button guessButton = ((Button) v);
            //We get the buttons texts
            String guess = guessButton.getText().toString();
            //and the parsed country's name
            String answer = getCountryName(correctAnswer);
            //increment number of guesses the user has made
            ++totalGuesses;

            if (guess.equals(answer)){
                //if the guess is correct increment number of correct answers
                ++correctAnswers;

                //display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));

                //disable all guess buttons
                disableButtons();

                if (correctAnswers == FLAGS_IN_QUIZ){
                    onCreateDialog(getArguments()).show();
                }
                else {
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    animate(true); //animate the flag off the screen
                                }
                            }, 2000); //2000millisecond for 2second delay
                }
            }
            else {
                //answer was incorrect
                flagImageView.startAnimation(shakeAnimation); //play shake
                //display "Incorrect" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
                //disable incorrect answer
                guessButton.setEnabled(false);
            }
        }
    };


    @Override
    public Dialog onCreateDialog(Bundle bundle){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.results, totalGuesses, (1000/(double) totalGuesses)));
        //this dialog box is not cancellable so user has to interact with it
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener(){
            //We override onClick to respond to the event when the user touches the corresponding button.
            public void onClick(DialogInterface dialog, int id){
                resetQuiz();
            }
        });
        return builder.create(); //return the AlertDialog
    }

    //method disableButtons iterates through the guess Buttons and disables them.
    //This method is called when the user makes a correct guess.
    private void disableButtons(){
        for (int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }

}