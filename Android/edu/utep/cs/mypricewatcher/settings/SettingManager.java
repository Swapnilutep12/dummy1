package edu.utep.cs.mypricewatcher.settings;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import edu.utep.cs.mypricewatcher.R;

/** Stateless class providing a set of utility methods to access shared preferences. */
public class SettingManager {

    private AppCompatActivity activity;
    private SharedPreferences prefs;

    public SettingManager(AppCompatActivity activity) {
        this.activity = activity;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public boolean useFirebase() {
        boolean defaultValue = getBoolean(R.string.pref_use_firebase_default);
        return prefs.getBoolean(getString(R.string.pref_use_firebase), defaultValue);
    }

    public boolean useFile() {
        boolean defaultValue = getBoolean(R.string.pref_use_file_default);
        return prefs.getBoolean(getString(R.string.pref_use_file), defaultValue);
    }

    public int timeout() {
        String defaultValue = getString(R.string.pref_timeout_default_value);
        return Integer.parseInt(prefs.getString(getString(R.string.pref_timeout), defaultValue));
    }

    private String getString(int resId) {
        return activity.getString(resId);
    }

    private Boolean getBoolean(int resId) {
        return Boolean.parseBoolean(getString(resId));
    }
}
