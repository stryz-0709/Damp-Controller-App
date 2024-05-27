package com.aasolution.dapsontien;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView currentDay, currentDate;
    FragmentManager fragmentManager;
    Button[] gates = new Button[30];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    intent.putExtra("button_text", "Cổng " + (index + 1));
                    startActivity(intent);
                }
            });
        }

    }
}