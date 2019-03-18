package edu.utep.cs.mypricewatcher.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import edu.utep.cs.mypricewatcher.R;

public class MainSettingsActivity extends AbstractSettingsActivity {

    @Override
    protected PreferenceFragment createPreferenceFragment() {
        return new GradePreferenceFragment();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return super.isValidFragment(fragmentName)
                || GradePreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class GradePreferenceFragment extends AbstractPreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addNetworkSettings(R.string.pref_network);
        }

        @Override
        protected int preferenceResourceId() {
            return R.xml.settings_main;
        }

        @Override
        protected Class<?> activityClass() {
            return MainSettingsActivity.class;
        }

        @Override
        protected int[] prefKeyIds() {
            return new int[] { R.string.pref_text_size, R.string.pref_timeout};
        }
    }
}
