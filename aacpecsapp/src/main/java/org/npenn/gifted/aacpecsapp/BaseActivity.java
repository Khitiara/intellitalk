package org.npenn.gifted.aacpecsapp;

import android.app.Activity;
import android.os.Bundle;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class BaseActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(getDefaultSharedPreferences(this).getBoolean("pref_useDarkTheme", false) ? android.R.style.Theme_Holo : android.R.style.Theme_Holo_Light);
        super.onCreate(savedInstanceState);
    }
}
