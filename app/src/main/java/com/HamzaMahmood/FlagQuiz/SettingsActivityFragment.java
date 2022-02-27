package com.HamzaMahmood.FlagQuiz;

import android.preference.PreferenceFragment;
import android.os.Bundle;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {
    //subclass of PreferenceFragment for managing app settings
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        //load from XML
        addPreferencesFromResource(R.xml.preferences);
    }
}
