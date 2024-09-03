package com.aasolution.dapsontien;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aar.tapholdupbutton.TapHoldUpButton;
import com.github.angads25.toggle.widget.LabeledSwitch;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ButtonActivity extends AppCompatActivity {
    private TextView gateText, statusText;
    private TapHoldUpButton getWater, removeWater;
    private LabeledSwitch autoButton, startSwitch, autoSwitch;
    private RelativeLayout insideLevel, outsideLevel, gateLevel, bottomPage, buttonPage, insideTabText, heightTabText, height2TabText, heightAutoTabText, autoSwitchTab, buttonTab, gateButtons;
    private Spinner heightValue, insideMaxValue, height2Value, heightAutoValue;
    private int buttonNumber = -1;
    private String state = "AUTO";
    private String autoState = "OFF";
    private final OkHttpClient client = new OkHttpClient();
    private Toast currentToast;

    private boolean isHeightFocused = false;
    private boolean isHeight2Focused = false;
    private boolean isInMaxFocused = false;
    private boolean isHeightAutoFocused = false;

    ProgressDialog progressDialog;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button);
        initializeViews();
        setupToolbar();

        progressDialog = new ProgressDialog(this);

        Intent intent = getIntent();
        String buttonText = intent.getStringExtra("button_text");
        buttonNumber = intent.getIntExtra("button_number", 0);
        gateText.setText("Cổng " + buttonText);

        insideLevel.getLayoutParams().height = (int) (25 * getResources().getDisplayMetrics().density + 0.5f);
        outsideLevel.getLayoutParams().height = (int) (25 * getResources().getDisplayMetrics().density + 0.5f);

        //CONNECT ESP ACCESS POINT
        permissionRequest();
        connectToWifi("Cổng " + buttonText);

        //BUTTON UI AND FUNCTION
        controlButton(state);
        buttonSetup();

        //THREAD
        statusRequest();
    }

    //FIND VIEW BY ID
    private void initializeViews() {
        gateText = findViewById(R.id.gateText);
        statusText = findViewById(R.id.status);
        insideLevel = findViewById(R.id.inside_amount);
        outsideLevel = findViewById(R.id.outside_amount);
        gateLevel = findViewById(R.id.gate_level);
        getWater = findViewById(R.id.getWater);
        removeWater = findViewById(R.id.removeWater);
        autoButton = findViewById(R.id.autoButton);
        startSwitch = findViewById(R.id.startSwitch);
        autoSwitch = findViewById(R.id.autoSwitch);
        bottomPage = findViewById(R.id.bottom_page);
        insideTabText = findViewById(R.id.inside_tab_text);
        heightTabText = findViewById(R.id.height_tab_text);
        height2TabText = findViewById(R.id.height2_tab_text);
        autoSwitchTab = findViewById(R.id.autoSwitchTab);
        heightAutoTabText = findViewById(R.id.height_auto_tab_text);
        buttonTab = findViewById(R.id.button_tab);
        gateButtons = findViewById(R.id.gateButtons);
        heightValue = findViewById(R.id.height_value);
        height2Value = findViewById(R.id.height2_value);
        heightAutoValue = findViewById(R.id.height_auto_value);
        insideMaxValue = findViewById(R.id.inside_max_value);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.values, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        insideMaxValue.setAdapter(adapter);
        heightValue.setAdapter(adapter);
        height2Value.setAdapter(adapter);
        heightAutoValue.setAdapter(adapter);

        buttonPage = findViewById(R.id.button_page);
    }

    //TOOLBAR
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void permissionRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }


    private void connectToWifi(String ssid) {
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase("")
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        final ConnectivityManager.NetworkCallback[] networkCallbackHolder = new ConnectivityManager.NetworkCallback[1];

        Handler expirationHandler = new Handler();
        Runnable expirationRunnable = new Runnable() {
            @Override
            public void run() {
                showToast("Kết nối với " + ssid + " thất bại.\nError: Time Out", 5000);
                connectivityManager.unregisterNetworkCallback(networkCallbackHolder[0]);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    onBackPressed();
                }, 2000);
            }
        };

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                showToast("Đã kết nối với " + ssid, 5000);
                connectivityManager.bindProcessToNetwork(network);
                expirationHandler.removeCallbacks(expirationRunnable);
            }

            @Override
            public void onUnavailable() {
                showToast("Kết nối với " + ssid + " thất bại.\nError: Network Unavailable", 5000);
                expirationHandler.removeCallbacks(expirationRunnable);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    onBackPressed();
                }, 2000);
            }

            @Override
            public void onLost(@NonNull Network network) {
                showToast("Kết nối với " + ssid + " thất bại.\nError: Connection Lost", 5000);
                connectivityManager.bindProcessToNetwork(null);
                connectToWifi(ssid);
            }
        };

        networkCallbackHolder[0] = networkCallback;

        connectivityManager.requestNetwork(request, networkCallback);
        expirationHandler.postDelayed(expirationRunnable, 10000);
    }


    //Button for opening and closing
    @SuppressLint("ClickableViewAccessibility")
    private void buttonSetup() {
        autoButton.setOnToggledListener((toggleableView, isOn) -> {
            if (isOn){
                state = (startSwitch.isOn())? "PHONE_MANUAL" : "PHONE_END";
            }
            else state = "AUTO";
            showToast(isOn? "Chế độ Thủ công" : "Chế độ Tự động", 5000);
            //Send to esp///
            postOKHTTP(state);
            ///Show/Hide buttons
            controlButton(state);
        });

        startSwitch.setOnToggledListener((toggleableView, isOn) -> {
            state = isOn ? "PHONE_MANUAL" : "PHONE_END";
            showToast(isOn? "Chế độ Start" : "Chế độ End", 5000);
            //Send to esp///
            postOKHTTP(state);
            ///Show/Hide buttons
            controlButton(state);
        });

        autoSwitch.setOnToggledListener((toggleableView, isOn) -> {
            autoState = isOn ? "ON" : "OFF";
            showToast(isOn? "Bật Chế độ Tự động" : "Tắt Chế độ Tự động", 5000);
            //Send to esp///
            postOKHTTP(autoState);
            ///Show/Hide buttons
            controlButton(state);
        });

        getWater.setOnButtonClickListener(new TapHoldUpButton.OnButtonClickListener() {
            @Override
            public void onLongHoldStart(View v) {
//                showToast("Cổng đang mở", 5000);
//                handleGateAction("OPENING");
            }

            @Override
            public void onLongHoldEnd(View v) {
//                showToast("Đã ngưng hoạt động", 5000);
//                handleGateAction("STOPPED");
            }

            @Override
            public void onClick(View v) {
                handleGateAction("GET_WATER");
            }
        });

        removeWater.setOnButtonClickListener(new TapHoldUpButton.OnButtonClickListener() {
            @Override
            public void onLongHoldStart(View v) {
//                showToast("Cổng đang đóng", 5000);
//                handleGateAction("CLOSING");
            }

            @Override
            public void onLongHoldEnd(View v) {
//                showToast("Đã ngưng hoạt động", 5000);
//                handleGateAction("STOPPED");
            }

            @Override
            public void onClick(View v) {
                handleGateAction("REMOVE_WATER");
            }
        });
        heightValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                isHeightFocused = hasFocus;
                heightValue.setBackgroundResource(R.drawable.edit_focused);
            }
        });

        heightAutoValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                isHeightAutoFocused = hasFocus;
                heightAutoValue.setBackgroundResource(R.drawable.edit_focused);
            }
        });

        height2Value.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                isHeight2Focused = hasFocus;
                height2Value.setBackgroundResource(R.drawable.edit_focused);
            }
        });

        insideMaxValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                isInMaxFocused = hasFocus;
                insideMaxValue.setBackgroundResource(R.drawable.edit_focused);
            }
        });

        buttonPage.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isHeightFocused) {
                    heightValue.clearFocus();
                    heightValue.setBackgroundResource(R.drawable.edit_unfocused);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                else if (isHeight2Focused) {
                    height2Value.clearFocus();
                    height2Value.setBackgroundResource(R.drawable.edit_unfocused);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                if (isInMaxFocused) {
                    insideMaxValue.clearFocus();
                    insideMaxValue.setBackgroundResource(R.drawable.edit_unfocused);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return false;
            }
        });

        insideMaxValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String payload = String.valueOf(position);
                postOKHTTP(String.valueOf(Integer.parseInt(payload)+30));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected if needed
            }
        });

        heightValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String payload = String.valueOf(position);
                postOKHTTP(payload);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected if needed
            }
        });

        heightAutoValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String payload = String.valueOf(position);
                postOKHTTP(String.valueOf(Integer.parseInt(payload)+20));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected if needed
            }
        });

        height2Value.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String payload = String.valueOf(position);
                postOKHTTP(String.valueOf(Integer.parseInt(payload)+10));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected if needed
            }
        });
    }

    private void handleGateAction(String status) {
        //Send to ESP///
        postOKHTTP(status);
//        statusText.setText(Objects.equals(status, "OPENING") ? "Đang mở" : Objects.equals(status, "CLOSING") ? "Đang đóng" : "Không hoạt động");

        //DISABLE/ENABLE concurrent button action
//        getWater.enableLongHold(isStopped);
//        removeWater.enableLongHold(isStopped);
//        autoButton.setEnabled(isStopped);
    }

    //THREAD FOR REQUEST WATER INFO
    private void statusRequest() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                requestWaterInfo();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(runnable);
    }

    //POST TO ESP
    public void postOKHTTP(String status) {
        String ip = "192.168.1." + String.valueOf((buttonNumber - 1) * 8 + 1);
        String url = "http://" + ip + "/post";
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "body=" + status);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("HTTP Response", Objects.requireNonNull(response.body()).string());
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            showToast("Permission denied by the user", 5000);
        }
    }

    public void requestWaterInfo() {
        String url = "http://192.168.1." + String.valueOf((buttonNumber - 1) * 8 + 1) + "/getWaterInfo";
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

    int tempH1 = 0;
    int tempH1Auto = 0;
    int tempH2 = 0;
    int tempInMax = 0;

    //GET WATER INFO FROM ESP
    public void handleWaterInfo(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            int inLevel = jsonObject.getInt("inLevel");
            int outLevel = jsonObject.getInt("outLevel");
            int top_val = jsonObject.getInt("top_val");
            int bot_val = jsonObject.getInt("bot_val");

            String gateMode = jsonObject.getString("gateMode");
            String gateStatus = jsonObject.getString("gateStatus");
            String errorCode = jsonObject.getString("errorCode");
            String autoMode = jsonObject.getString("autoMode");

            ///h1///
            int h1 = jsonObject.getInt("h1");
            if (h1 != tempH1){
                showToast("Gửi thành công giá trị Mực chênh lệch: " + h1, 5000);
                tempH1 = h1;
            }
            ///h2///
            int h2 = jsonObject.getInt("h2");
            if (h2 != tempH2){
                showToast("Gửi thành công giá trị Mực chênh lệch: " + h2, 5000);
                tempH2 = h2;
            }
            ///h1_auto///
            int h1_auto = jsonObject.getInt("h1_auto");
            if (h1_auto != tempH1Auto){
                showToast("Gửi thành công giá trị Mực chênh lệch: " + h1, 5000);
                tempH1Auto = h1_auto;
            }
            ///inMax///
            int inMax = jsonObject.getInt("inMax");
            if (inMax != tempInMax){
                showToast("Gửi thành công giá trị Mực cao nhất: " + inMax, 5000);
                tempInMax = inMax;
            }

            Log.d("JSON", "in: " + String.valueOf(inLevel));
            Log.d("JSON","out: " + String.valueOf(outLevel));
            Log.d("JSON","gatemode: " + gateMode);
            Log.d("JSON","gatestatus: " + gateStatus);
            Log.d("JSON", "inMax: " + String.valueOf(inMax));

            updateWaterLevel(inLevel, outLevel, top_val, bot_val, h1, h2, h1_auto, inMax, gateMode, autoMode, gateStatus, errorCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    int prevTopVal = -1;
    int prevBotVal = -1;
    String prevErrorCode = "";

    //THREAD FUNCTION TO UPDATE WATER AND GATE STATUS
    void updateWaterLevel(int in, int out, int top_val, int bot_val, int h1, int h2, int h1_auto, int inMax, String mode, String autoMode, String status, String errorCode) {
            insideLevel.getLayoutParams().height = getWaterPX(in);
            outsideLevel.getLayoutParams().height = getWaterPX(out);

        gateLevel.getLayoutParams().height = getGatePX(top_val, bot_val);
        state = mode;
        autoState = autoMode;

        if (Objects.equals(errorCode, "H1") && !Objects.equals(errorCode, prevErrorCode)) showToast("Điều kiện lấy nước không thoả mãn", 5000);
        else if (Objects.equals(errorCode, "H2") && !Objects.equals(errorCode, prevErrorCode)) showToast("Điều kiện tháo nước không thoả mãn", 5000);

        if (Objects.equals(status, "OPENING")) statusText.setText("Đang mở");
        else if (Objects.equals(status, "CLOSING") || Objects.equals(status, "FORCE_CLOSE")) statusText.setText("Đang đóng");
        else if (Objects.equals(status, "GET_WATER")) statusText.setText("Đang lấy nước");
        else if (Objects.equals(status, "REMOVE_WATER")) statusText.setText("Đang tháo nước");
        else if (Objects.equals(status, "STOPPED")) statusText.setText("Không hoạt động");

        if (top_val == 1 && prevTopVal != top_val) showToast("Cổng lên cao nhất", 5000);
        else if (bot_val == 1 && prevBotVal != bot_val) showToast("Cổng xuống thấp nhất", 5000);
        prevTopVal = top_val;
        prevBotVal = bot_val;
        prevErrorCode = errorCode;

        controlButton(mode);

        insideLevel.requestLayout();
        outsideLevel.requestLayout();
        gateLevel.requestLayout();
        if (!isHeightFocused) heightValue.setSelection(h1);
        if (!isHeight2Focused) height2Value.setSelection(h2);
        if (!isInMaxFocused) insideMaxValue.setSelection(inMax);
        if (!isHeightAutoFocused) heightAutoValue.setSelection(h1_auto);
    }


    private int getWaterPX(int level){
        Log.d("JSON", "level: " + String.valueOf(level));
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (((level + 1) * 35) * scale + 0.5f - 18);
    }

    private int getGatePX(int top_val, int bot_val){
        final float scale = getResources().getDisplayMetrics().density;
        int height = (top_val == 1)? 0 : (bot_val == 1)? 280 : 140;
        return (int) (height * scale + 0.5f);
    }

    private void showToast(String message, int duration) {
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

    //SHOW BUTTON FOR AUTO AND MANUAL MODE
    private void controlButton(String state) {
        boolean isAuto = Objects.equals(state, "AUTO");
        boolean isEnd = Objects.equals(state, "END");

        autoButton.setEnabled(!isEnd);
        autoButton.setOn(!isAuto);
        startSwitch.setOn(!Objects.equals(state, "PHONE_END"));
        autoSwitch.setOn(Objects.equals(autoState, "ON"));

        bottomPage.setVisibility((isEnd)? View.INVISIBLE : View.VISIBLE);

        height2TabText.setVisibility((!isAuto) ? View.VISIBLE : View.INVISIBLE);
        heightTabText.setVisibility((!isAuto) ? View.VISIBLE : View.INVISIBLE);

        buttonTab.setVisibility((!isAuto) ? View.VISIBLE : View.INVISIBLE);

        insideTabText.setVisibility((isAuto)? View.VISIBLE : View.INVISIBLE);
        heightAutoTabText.setVisibility((isAuto) ? View.VISIBLE : View.INVISIBLE);
        autoSwitchTab.setVisibility((isAuto) ? View.VISIBLE : View.INVISIBLE);

        gateButtons.setVisibility((!Objects.equals(state, "PHONE_END"))? View.VISIBLE : View.INVISIBLE);
    }
}


