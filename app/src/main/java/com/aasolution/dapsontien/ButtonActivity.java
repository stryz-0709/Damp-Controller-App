package com.aasolution.dapsontien;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
    private EditText heightValue;
    private static final int AUTO = -100, MANUAL = -101, OPENING = -110, CLOSING = -111, STOPPED = -112;
    private int buttonNumber = -1;
    private int state = AUTO;
    private final OkHttpClient client = new OkHttpClient();

    private boolean isHeightFocused = false;

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

        insideLevel.getLayoutParams().height = (int) (40 * getResources().getDisplayMetrics().density + 0.5f);
        outsideLevel.getLayoutParams().height = (int) (40 * getResources().getDisplayMetrics().density + 0.5f);

        //CONNECT ESP ACCESS POINT
        permissionRequest();
        wifiConnect("Cổng " + buttonText);

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

    private void wifiConnect(String ssid) {
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder().setSsid(ssid).setWpa2Passphrase("").build();
        NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).setNetworkSpecifier(specifier).build();
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.i("WiFiConnection", "Connected to " + ssid);
                connectivityManager.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                showToast("WiFi connection to " + ssid + " could not be established. Please check WiFi settings.");
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.w("WiFiConnection", "Lost connection to " + ssid);
                connectivityManager.bindProcessToNetwork(null);
            }
        });
    }


    //Button for opening and closing
    @SuppressLint("ClickableViewAccessibility")
    private void buttonSetup() {
        autoButton.setOnToggledListener((toggleableView, isOn) -> {
            state = isOn ? MANUAL : AUTO;
            //Send to esp///
            postOKHTTP(state);
            ///Show/Hide buttons
            controlButton(state);
        });

        openGate.setOnButtonClickListener(new TapHoldUpButton.OnButtonClickListener() {
            @Override
            public void onLongHoldStart(View v) {
                handleGateAction(OPENING);
            }

            @Override
            public void onLongHoldEnd(View v) {
                handleGateAction(STOPPED);
            }

            @Override
            public void onClick(View v) {showToast("Vui lòng ấn giữ");}
        });

        closeGate.setOnButtonClickListener(new TapHoldUpButton.OnButtonClickListener() {
            @Override
            public void onLongHoldStart(View v) {
                handleGateAction(CLOSING);
            }

            @Override
            public void onLongHoldEnd(View v) {
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
                return false;
            }
        });

        heightValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    v.clearFocus();
                    heightValue.setBackgroundResource(R.drawable.edit_unfocused);
                    isHeightFocused = false;
                    String payload = heightValue.getText().toString().equals("")? "0" : heightValue.getText().toString();
                    heightValue.setText(String.valueOf(Integer.parseInt(payload)));
                    postOKHTTP(Integer.parseInt(payload));
                    return true;
                }
                return false;
            }
        });
    }

    private void handleGateAction(int status) {
        //Send to ESP///
        postOKHTTP(status);
        statusText.setText(status == OPENING ? "CỔNG ĐANG MỞ" : status == CLOSING ? "CỔNG ĐANG ĐÓNG" : "CỔNG KHÔNG HOẠT ĐỘNG");
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
        String payload = (status == CLOSING)? "CLOSING" : (status == OPENING)? "OPENING" : (status == STOPPED)? "STOPPED" : (status == AUTO)? "AUTO" : (status == MANUAL)? "MANUAL" : String.valueOf(status);
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


    //GET WATER INFO FROM ESP
    public void handleWaterInfo(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            int inLevel = jsonObject.getInt("inLevel");
            int outLevel = jsonObject.getInt("outLevel");
//            int gateLevel = jsonObject.getInt("gateLevel");
            int gateMode = jsonObject.getInt("gateMode");
            int gateStatus = jsonObject.getInt("gateStatus");
            int delta = jsonObject.getInt("delta");

            Log.d("JSON", "in: " + String.valueOf(inLevel));
            Log.d("JSON","out: " + String.valueOf(outLevel));
//            Log.d("JSON","gatelevel: " + String.valueOf(gateLevel));
            Log.d("JSON","gatemode: " + getStatus(gateMode));
            Log.d("JSON","gatestatus: " + getStatus(gateStatus));
            Log.d("JSON", "delta: " + String.valueOf(delta));


//            int convertedGateLevel = gateLevel;

//            updateWaterLevel(inLevel, outLevel, convertedGateLevel, gateMode, gateStatus, delta);
            updateWaterLevel(inLevel, outLevel, gateMode, gateStatus, delta);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //THREAD FUNCTION TO UPDATE WATER AND GATE STATUS
    void updateWaterLevel(int in, int out, int mode, int status, int delta) {
        insideLevel.getLayoutParams().height = getWaterPX(in);
        outsideLevel.getLayoutParams().height = getWaterPX(out);
//        gateLevel.getLayoutParams().height = getGatePX(gate);
        state = mode;

        statusText.setText(status == OPENING ? "CỔNG ĐANG MỞ" : status == CLOSING ? "CỔNG ĐANG ĐÓNG" : "CỔNG KHÔNG HOẠT ĐỘNG");
        autoButton.setOn(mode == MANUAL);
        controlButton(mode);

        insideLevel.requestLayout();
        outsideLevel.requestLayout();
        gateLevel.requestLayout();
        if (!isHeightFocused) heightValue.setText(String.valueOf(delta));
    }

    String getStatus(int status){
        if (status == -100) return "AUTO";
        else if (status == -101) return "MANUAL";
        else if (status == -110) return "OPENING";
        else if (status == -111) return "CLOSING";
        else if (status == -112) return "STOPPED";
        return "";
    }

    private int getWaterPX(int level){
        final float scale = getResources().getDisplayMetrics().density;
        return (int) ((level + 1) * 40 * scale + 0.5f);
    }

    private int getGatePX(int level){
        final float scale = getResources().getDisplayMetrics().density;
        return (int) ((level - 1) * 36 * scale + 0.5f);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    //SHOW BUTTON FOR AUTO AND MANUAL MODE
    private void controlButton(int state) {
        buttonTab.setVisibility((state == MANUAL)? View.VISIBLE : View.INVISIBLE);
        heightTab.setVisibility((state == AUTO)? View.VISIBLE : View.INVISIBLE);
    }
}


