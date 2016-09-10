package com.liangmayong.android_preferences;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.liangmayong.preferences.Preferences;
import com.liangmayong.preferences.annotations.PreferenceValue;

public class MainActivity extends AppCompatActivity {

    @PreferenceValue(value = "StringKey", initValue = "hi,android")
    String string;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preferences.bind(this);
        setContentView(R.layout.activity_main);
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
