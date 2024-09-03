package com.aasolution.dapsontien;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView currentDay, currentDate;
    FragmentManager fragmentManager;
    Button[] gates = new Button[30];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        ///SHOW DATE////
        currentDay = findViewById(R.id.current_day);
        currentDate = findViewById(R.id.current_date);
        currentDay.setText(new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date()));
        currentDate.setText(new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date()));

        /////TOOLBAR//////
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        setSupportActionBar(toolbar);

        for (int i = 0; i < 30; i++) {
            String buttonID = "gate" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            gates[i] = findViewById(resID);

            final int index = i;
            gates[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("BUTTON","PRESSED");
                    Intent intent = new Intent(MainActivity.this, ButtonActivity.class);
                    intent.putExtra("button_text", String.valueOf(index + 1));
                    intent.putExtra("button_number", index + 1);
                    startActivity(intent);
                }
            });
        }

    }



}