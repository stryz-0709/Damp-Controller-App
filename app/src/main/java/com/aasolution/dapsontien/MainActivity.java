package com.aasolution.dapsontien;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.Manifest;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int INTERNET_SETTINGS_REQUEST = 0;
    private static final int NUM_OF_IPs = 11;
    Toolbar toolbar;
    int[] isManager = new int[20];
    public int selectedGate = -1, inLevel = 0, outLevel = 0, top_val = 0, bot_val = 0,
        h1 = -1, h2 = -1, debugIn = 0, debugOut = 0;
    public String gateMode = "", gateStatus = "";
    private final OkHttpClient client = new OkHttpClient();
    FragmentManager fragmentManager;
    private Toast currentToast;
    Button[] gates = new Button[20];
    int[] isGatePressed = new int[20];
    String[] colors = {"#F70D1A", "#FDD017", "#00c4a6"};
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    public interface PasswordCheckCallback {
        void onPasswordChecked(boolean isSuccess);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        for (int i = 0; i < 20; i++) isManager[i] = 0;

        fragmentManager = getSupportFragmentManager();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }


        toolbar();
        buttons();
        refreshStatus();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with your logic
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    private void toolbar(){
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        setSupportActionBar(toolbar);
    }

    private void buttons(){
        for (int i = 0; i < 20; i++) {
            String buttonID = "gate" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName()); // use requireContext() in Fragment
            gates[i] = findViewById(resID);

            final int index = i;
            isGatePressed[index] = 0;

            gates[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isGatePressed[index] == 0) {
                        startActivityForResult(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), INTERNET_SETTINGS_REQUEST);
                    }
                    else if (isGatePressed[index] == 1) {
                        if (isManager[index] == 0) passwordPopup("height", h1, h2);
                        else heightPopup(h1, h2);
                    }
                    else if (isGatePressed[index] == 2) {
                        openFragment(new ButtonFragment());
                    }
                }
            });
        }
    }


    public void showToast(String message, int duration) {
        if (currentToast != null) currentToast.cancel();

        final int delay = Toast.LENGTH_SHORT;
        final int iterations = duration / 3500;

        currentToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);

        final Handler handler = new Handler();
        for (int i = 0; i < iterations; i++) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    currentToast.show();
                }
            }, i * delay);
        }
    }

    private void refreshStatus() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    // Make button RED if not connected to Wifi,
                    // Make button YELLOW if device is connected but not set up
                    // Make button GREEN if device is ready

                    if (checkWifi("Cổng " + String.valueOf(i + 1))){
                        getWaterInfo(i + 1);
                        gateSelect(i, (h1 < 0 || h2 < 0)? 1 : 2);
                        Log.d("JSON", String.valueOf(h1));
                        Log.d("JSON", String.valueOf(h2));
                        selectedGate = i + 1;
                    }
                    else gateSelect(i, 0);
                }
                handler.postDelayed(this, 2000);  // Repeat every 2 seconds
            }
        };
        handler.post(runnable);
    }

    private void gateSelect(int gateNum, int num){
        gates[gateNum].setBackgroundColor(Color.parseColor(colors[num]));
        isGatePressed[gateNum] = num;
    }


    public void postOKHTTP(String status, int buttonNumber) {
        String ip = "192.168.1." + String.valueOf((buttonNumber - 1) * NUM_OF_IPs + 1);
        String url = "http://" + ip + "/post";
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "body=" + status);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    int intStatus = Integer.parseInt(status);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast((intStatus >= 0 && intStatus < 20)? "Đã thay đổi Mực chênh lệch" :
                                    "Đã thay đổi Mực nước thủ công", 5000);
                        }
                    });
                }
                catch (NumberFormatException ignored) {
                }

                Log.d("HTTP Response", Objects.requireNonNull(response.body()).string());
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void getWaterInfo(int buttonNumber) {
        String url = "http://192.168.1." + String.valueOf((buttonNumber - 1) * NUM_OF_IPs + 1) + "/getWaterInfo";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                statusText.setText("Cổng đã kết nối");
                if (response.isSuccessful()) {
                    String jsonResponse = Objects.requireNonNull(response.body()).string();
                    runOnUiThread(() -> handleWaterInfo(jsonResponse));
                }
            }
        });
    }



    public void handleWaterInfo(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            inLevel = jsonObject.getInt("inLevel");
            outLevel = jsonObject.getInt("outLevel");
            top_val = jsonObject.getInt("top_val");
            bot_val = jsonObject.getInt("bot_val");
            debugIn = jsonObject.getInt("debugIn");
            debugOut = jsonObject.getInt("debugOut");

            gateMode = jsonObject.getString("gateMode");
            gateStatus = jsonObject.getString("gateStatus");

            ///h1///
            h1 = jsonObject.getInt("h1");
            ///h2///
            h2 = jsonObject.getInt("h2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void openFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_layout, fragment);
        transaction.addToBackStack(null); // Add the fragment to the back stack
        transaction.commit();
    }

    public boolean checkWifi(String ssid) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = wifiInfo.getSSID();
        Log.d("Current SSID", currentSsid);

        currentSsid = currentSsid.replace("\"", ""); // Remove quotes


        return currentSsid.equals(ssid);
    }

    public void checkPassword(String password, int buttonNumber, PasswordCheckCallback callback) {
        String url = "http://192.168.1." + String.valueOf((buttonNumber - 1) * NUM_OF_IPs + 1) + "/checkPassword";

        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "password=" + password);

        // Build the HTTP request
        Request request = new Request.Builder().url(url).post(body).build();

        // Make the network call using OkHttp asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();

                // Pass the result back through the callback
                boolean isSuccess = responseData.equals("success");
                runOnUiThread(() -> callback.onPasswordChecked(isSuccess));
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> callback.onPasswordChecked(false));
            }
        });
    }



    //////PASSWORD POPUP//////
    public void passwordPopup(String intent, int value1, int value2) {
        ConstraintLayout constraintLayout = findViewById(R.id.popupDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.password_popup, constraintLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        TextView errorText = view.findViewById(R.id.error_desc);
        errorText.setVisibility(View.GONE);

        EditText insertText = view.findViewById(R.id.popup_insert);
        ToggleButton showPasswordButton = view.findViewById(R.id.show_password_button);
        showPasswordButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    insertText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    insertText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                insertText.setSelection(insertText.getText().length());
            }
        });

        Button popupButton1 = view.findViewById(R.id.popup_button1);
        popupButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        Button popupButton2 = view.findViewById(R.id.popup_button2);
        popupButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(insertText.getText().toString())) {
                    errorText.setText("Vui lòng nhập mật khẩu quản lý");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }
                checkPassword(insertText.getText().toString(), selectedGate, new PasswordCheckCallback() {
                    @Override
                    public void onPasswordChecked(boolean isSuccess) {
                        if (isSuccess) {
                            alertDialog.dismiss();
                            isManager[selectedGate-1] = 1;
                            if (Objects.equals(intent, "height")) heightPopup(value1, value2);
                            else if (Objects.equals(intent, "debug")) debugPopup(value1, value2);
                        } else {
                            errorText.setText("Mật khẩu đã nhập không đúng");
                            errorText.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

    public void heightPopup(int value1, int value2) {
        ConstraintLayout constraintLayout = findViewById(R.id.popupDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.height_popup, constraintLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        Spinner heightValue, height2Value;
        heightValue = view.findViewById(R.id.height_value);
        height2Value = view.findViewById(R.id.height2_value);

        TextView errorText = view.findViewById(R.id.error_desc);
        errorText.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, getResources().getStringArray(R.array.values));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);

        heightValue.setAdapter(adapter);
        height2Value.setAdapter(adapter);

        heightValue.setSelection(value1+1);
        height2Value.setSelection(value2+1);


        Button popupButton1 = view.findViewById(R.id.popup_button1);
        popupButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        Button popupButton2 = view.findViewById(R.id.popup_button2);
        popupButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tempH1 = Integer.parseInt(heightValue.getSelectedItem().toString());
                int tempH2 = Integer.parseInt(height2Value.getSelectedItem().toString());

                if (tempH1 < 0) {
                    errorText.setText("Vui lòng nhập Mực chênh lệch H1");
                    errorText.setVisibility(View.VISIBLE);
                }
                else if (tempH2 < 0){
                    errorText.setText("Vui lòng nhập Mực chênh lệch H2");
                    errorText.setVisibility(View.VISIBLE);
                }
                else {
                    h1 = tempH1;
                    h2 = tempH2;
                    postOKHTTP(String.valueOf(h1), selectedGate);
                    postOKHTTP(String.valueOf(h2+10), selectedGate);
                    // Dismiss the popup
                    alertDialog.dismiss();
                }
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

    public void debugPopup(int value1, int value2) {
        ConstraintLayout constraintLayout = findViewById(R.id.popupDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.debug_popup, constraintLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        Spinner debugInValue, debugOutValue;
        debugInValue = view.findViewById(R.id.debug_in_value);
        debugOutValue = view.findViewById(R.id.debug_out_value);

        TextView errorText = view.findViewById(R.id.error_desc);
        errorText.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, getResources().getStringArray(R.array.values));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);

        debugInValue.setAdapter(adapter);
        debugOutValue.setAdapter(adapter);

        debugInValue.setSelection(value1+1);
        debugOutValue.setSelection(value2+1);

        ToggleButton activate = view.findViewById(R.id.debug_activate);
        activate.setChecked(Objects.equals(gateMode, "DEBUG"));

        Button popupButton1 = view.findViewById(R.id.popup_button1);
        popupButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        Button popupButton2 = view.findViewById(R.id.popup_button2);
        popupButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tempDebugIn = Integer.parseInt(debugInValue.getSelectedItem().toString());
                int tempDebugOut = Integer.parseInt(debugOutValue.getSelectedItem().toString());

                if (tempDebugIn < 0) {
                    errorText.setText("Vui lòng nhập Mực nước ao");
                    errorText.setVisibility(View.VISIBLE);
                }
                else if (tempDebugOut < 0){
                    errorText.setText("Vui lòng nhập Mực nước sông");
                    errorText.setVisibility(View.VISIBLE);
                }
                else {
                    debugIn = tempDebugIn;
                    debugOut = tempDebugOut;
                    postOKHTTP(String.valueOf(debugIn+20), selectedGate);
                    postOKHTTP(String.valueOf(debugOut+30), selectedGate);
                    postOKHTTP((activate.isChecked())? "DEBUG" : "MANUAL", selectedGate);
                    // Dismiss the popup
                    alertDialog.dismiss();
                }
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }


}