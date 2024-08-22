package com.aasolution.dapsontien;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.EditText;
import android.widget.RelativeLayout;
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
    private TapHoldUpButton openGate, closeGate;
    private LabeledSwitch autoButton;
    private RelativeLayout insideLevel, outsideLevel, gateLevel, heightTab, buttonPage, buttonTab;
    private EditText heightValue, insideMaxValue;
    private static final int AUTO = -100, MANUAL = -101, PHONEMANUAL = -102, OPENING = -110, CLOSING = -111, STOPPED = -112;
    private int buttonNumber = -1;
    private int state = AUTO;
    private final OkHttpClient client = new OkHttpClient();
    private Toast currentToast;

    private boolean isHeightFocused = false;
    private boolean isInMaxFocused = false;

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
        openGate = findViewById(R.id.openGate);
        closeGate = findViewById(R.id.closeGate);
        autoButton = findViewById(R.id.autoButton);
        heightTab = findViewById(R.id.height_tab);
        buttonTab = findViewById(R.id.button_tab);
        heightValue = findViewById(R.id.height_value);
        insideMaxValue = findViewById(R.id.inside_max_value);
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
                showToast("Kết nối với " + ssid + " thất bại.\nError: Time Out");
                connectivityManager.unregisterNetworkCallback(networkCallbackHolder[0]);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    onBackPressed();
                }, 2000);
            }
        };

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                showToast("Đã kết nối với " + ssid);
                connectivityManager.bindProcessToNetwork(network);
                expirationHandler.removeCallbacks(expirationRunnable);
            }

            @Override
            public void onUnavailable() {
                showToast("Kết nối với " + ssid + " thất bại.\nError: Network Unavailable");
                expirationHandler.removeCallbacks(expirationRunnable);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    onBackPressed();
                }, 2000);
            }

            @Override
            public void onLost(@NonNull Network network) {
                showToast("Kết nối với " + ssid + " thất bại.\nError: Connection Lost");
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
            state = isOn ? PHONEMANUAL : AUTO;
            showToast(isOn? "Chế độ Thủ công" : "Chế độ Tự động");
            //Send to esp///
            postOKHTTP(state);
            ///Show/Hide buttons
            controlButton(state);
        });

        openGate.setOnButtonClickListener(new TapHoldUpButton.OnButtonClickListener() {
            @Override
            public void onLongHoldStart(View v) {
                showToast("Cổng đang mở");
                handleGateAction(OPENING);
            }

            @Override
            public void onLongHoldEnd(View v) {
                showToast("Đã ngưng hoạt động");
                handleGateAction(STOPPED);
            }

            @Override
            public void onClick(View v) {showToast("Vui lòng ấn giữ");}
        });

        closeGate.setOnButtonClickListener(new TapHoldUpButton.OnButtonClickListener() {
            @Override
            public void onLongHoldStart(View v) {
                showToast("Cổng đang đóng");
                handleGateAction(CLOSING);
            }

            @Override
            public void onLongHoldEnd(View v) {
                showToast("Đã ngưng hoạt động");
                handleGateAction(STOPPED);
            }

            @Override
            public void onClick(View v) {showToast("Vui lòng ấn giữ");}
        });
        heightValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                isHeightFocused = hasFocus;
                heightValue.setBackgroundResource(R.drawable.edit_focused);
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
                    insideMaxValue.clearFocus();
                    heightValue.setBackgroundResource(R.drawable.edit_unfocused);
                    insideMaxValue.setBackgroundResource(R.drawable.edit_unfocused);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return false;
            }
        });

        insideMaxValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String originalInMaxValue = insideMaxValue.getText().toString();
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    v.clearFocus();
                    insideMaxValue.setBackgroundResource(R.drawable.edit_unfocused);
                    isInMaxFocused = false;

                    String input = insideMaxValue.getText().toString();
                    int value = input.isEmpty() ? 0 : Integer.parseInt(input);

                    if (value < 0 || value > 7) {
                        // Revert to the original value
                        insideMaxValue.setText(originalInMaxValue);
                        showToast("Giá trị không hợp lệ. Vui lòng nhập giá trị từ 0 đến 7.");
                    } else {
                        // Update the original value to the new valid value
                        originalInMaxValue = String.valueOf(value);
                        insideMaxValue.setText(originalInMaxValue);
//                        showToast("Đã đổi giá trị Mực cao nhất: " + originalInMaxValue);
                        postOKHTTP(value + 10);
                    }
                    return true;
                }
                return false;
            }
        });


        heightValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String originalHeightValue = heightValue.getText().toString();
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    v.clearFocus();
                    heightValue.setBackgroundResource(R.drawable.edit_unfocused);
                    isHeightFocused = false;

                    String input = heightValue.getText().toString();
                    int value = input.isEmpty() ? 0 : Integer.parseInt(input);

                    if (value < 0 || value > 7) {
                        heightValue.setText(originalHeightValue);
                        showToast("Giá trị không hợp lệ. Vui lòng nhập giá trị từ 0 đến 7.");
                    } else {
                        originalHeightValue = String.valueOf(value);
                        heightValue.setText(originalHeightValue);
//                        showToast("Đã đổi giá trị Mực chênh lệch: " + delta);
                        postOKHTTP(value);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void handleGateAction(int status) {
        //Send to ESP///
        postOKHTTP(status);
        statusText.setText(status == OPENING ? "Đang mở" : status == CLOSING ? "Đang đóng" : "Không hoạt động");
        boolean isStopped = status == STOPPED;

        //DISABLE/ENABLE concurrent button action
        openGate.enableLongHold(isStopped);
        closeGate.enableLongHold(isStopped);
        autoButton.setEnabled(isStopped);
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
    public void postOKHTTP(int status) {
        String payload = (status == CLOSING)? "CLOSING" : (status == OPENING)? "OPENING" : (status == STOPPED)? "STOPPED" : (status == AUTO)? "AUTO" : (status == PHONEMANUAL)? "PHONEMANUAL" : String.valueOf(status);
        String ip = "192.168.1." + String.valueOf((buttonNumber - 1) * 8 + 1);
        String url = "http://" + ip + "/post";
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "body=" + payload);
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
            showToast("Permission denied by the user");
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

    int tempDelta = 0;
    int tempInMax = 0;

    //GET WATER INFO FROM ESP
    public void handleWaterInfo(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            int inLevel = jsonObject.getInt("inLevel");
            int outLevel = jsonObject.getInt("outLevel");
            int gateLevel = jsonObject.getInt("gateLevel");
            int gateMode = jsonObject.getInt("gateMode");
            int gateStatus = jsonObject.getInt("gateStatus");
            int delta = jsonObject.getInt("delta");
            if (delta != tempDelta){
                showToast("Gửi thành công giá trị Mực chênh lệch: " + delta);
                tempDelta = delta;
            }
            int inMax = jsonObject.getInt("inMax");
            if (inMax != tempInMax){
                showToast("Gửi thành công giá trị Mực cao nhất: " + inMax);
                tempInMax = inMax;
            }


            Log.d("JSON", "in: " + String.valueOf(inLevel));
            Log.d("JSON","out: " + String.valueOf(outLevel));
            Log.d("JSON","gatelevel: " + String.valueOf(gateLevel));
            Log.d("JSON","gatemode: " + getStatus(gateMode));
            Log.d("JSON","gatestatus: " + getStatus(gateStatus));
            Log.d("JSON", "delta: " + String.valueOf(delta));
            Log.d("JSON", "inMax: " + String.valueOf(inMax));

            updateWaterLevel(inLevel, outLevel, gateLevel, gateMode, gateStatus, delta, inMax);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //THREAD FUNCTION TO UPDATE WATER AND GATE STATUS
    void updateWaterLevel(int in, int out, int gate, int mode, int status, int delta, int inMax) {
        insideLevel.getLayoutParams().height = getWaterPX(in);
        outsideLevel.getLayoutParams().height = getWaterPX(out);
        gateLevel.getLayoutParams().height = getGatePX(gate);
        state = mode;

        statusText.setText(status == OPENING ? "Đang mở" : status == CLOSING ? "Đang đóng" : "Không hoạt động");
        if (gate == 1) showToast("Cổng lên cao nhất");
        else if (gate == 0) showToast("Cổng xuống thấp nhất");
        if (mode == MANUAL) autoButton.setEnabled(false);
        else{
            autoButton.setEnabled(true);
        autoButton.setOn(mode == PHONEMANUAL);
        controlButton(mode);}

        insideLevel.requestLayout();
        outsideLevel.requestLayout();
        gateLevel.requestLayout();
        if (!isHeightFocused) heightValue.setText(String.valueOf(delta));
        if (!isInMaxFocused) insideMaxValue.setText(String.valueOf(inMax));
    }

    String getStatus(int status){
        if (status == -100) return "AUTO";
        else if (status == -101) return "MANUAL";
        else if (status == -102) return "PHONEMANUAL";
        else if (status == -110) return "OPENING";
        else if (status == -111) return "CLOSING";
        else if (status == -112) return "STOPPED";
        return "";
    }

    private int getWaterPX(int level){
        Log.d("JSON", "level: " + String.valueOf(level));
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (((level + 1) * 35) * scale + 0.5f - 18);
    }

    private int getGatePX(int level){
        final float scale = getResources().getDisplayMetrics().density;
        if (level == -1) return (int) (140 * scale + 0.5f);
        else return (int) ((1-level) * 280 * scale + 0.5f);
    }

    private void showToast(String message) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        currentToast.show();
    }

    //SHOW BUTTON FOR AUTO AND MANUAL MODE
    private void controlButton(int state) {
        buttonTab.setVisibility((state == PHONEMANUAL)? View.VISIBLE : View.INVISIBLE);
        heightTab.setVisibility((state == AUTO)? View.VISIBLE : View.INVISIBLE);
    }
}


