package com.liangmayong.android_preferences;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.liangmayong.preferences.Preferences;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast.makeText(this, Preferences.getDefaultPreferences().getString("key"), Toast.LENGTH_SHORT).show();
        Preferences.getDefaultPreferences().setString("key", "Main2Activity");
        startActivity(new Intent(this, Main2Activity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
