package org.npenn.gifted.aacpecsapp;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    protected void onResume() {
        boolean useDarkTheme = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_useDarkTheme", false);
        if (useDarkTheme) {
            this.setTheme(android.R.style.Theme_Holo);
        } else {
            this.setTheme(android.R.style.Theme_Holo_Light);
        }
        super.onResume();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String THEME_KEY = "pref_useDarkTheme";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            findPreference(THEME_KEY).setSummary(String.format("The %s theme is selected.", getPreferenceManager().getSharedPreferences().getBoolean(THEME_KEY, false) ? "light" : "dark"));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(THEME_KEY)) {
                Preference theme = findPreference(key);
                theme.setSummary(String.format("The %s theme is selected.", sharedPreferences.getBoolean(THEME_KEY, false) ? "dark" : "light"));
                if (sharedPreferences.getBoolean(THEME_KEY, false)) {
                    getActivity().getApplication().setTheme(android.R.style.Theme_Holo);
                    getActivity().setTheme(android.R.style.Theme_Holo);
                    getActivity().recreate();
                } else {
                    getActivity().getApplication().setTheme(android.R.style.Theme_Holo_Light);
                    getActivity().setTheme(android.R.style.Theme_Holo_Light);
                    getActivity().recreate();
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);

        }
    }
}
