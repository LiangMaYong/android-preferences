package com.liangmayong.android_preferences;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.liangmayong.preferences.Preferences;

public class MainActivity extends AppCompatActivity implements Preferences.OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Preferences.getDefaultPreferences().registerOnPreferenceChangeListener(this);
        Preferences.getDefaultPreferences().setInt("IntKey", 1).setString("StringKey", "hi,android").setString("StringKey", "hi,preferences");

        Toast.makeText(this, "StringKey:" + Preferences.getDefaultPreferences().getString("StringKey"), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChange(Preferences preference, String key) {
        Toast.makeText(MainActivity.this, "onChange:" + Preferences.getDefaultPreferences().getString(key), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Preferences.getDefaultPreferences().unregisterOnPreferenceChangeListener(this);
    }
}
