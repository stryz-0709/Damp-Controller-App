package com.aasolution.dapsontien;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aar.tapholdupbutton.TapHoldUpButton;
import com.github.angads25.toggle.widget.LabeledSwitch;

import org.w3c.dom.Text;

import java.util.Objects;

import okhttp3.OkHttpClient;


public class ButtonFragment extends Fragment {
    View mView;
    MainActivity mainActivity;
    private TextView gateText, statusText;
    private RelativeLayout getWater, removeWater, bug;
    private RelativeLayout insideLevel, outsideLevel, gateLevel, editTab;
    private TextView heightValue, height2Value;
    private Handler handler, wifiHandler;
    private Runnable runnable, wifiRunnable;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_button, container, false);
        mainActivity = (MainActivity) getActivity();

        initializeViews();
        setupToolbar();
        buttonSetup();

        refreshStatus();

        return mView;

    }

    private void initializeViews() {
        gateText = mView.findViewById(R.id.gateText);
        gateText.setText("Cổng " + String.valueOf(mainActivity.selectedGate));

        statusText = mView.findViewById(R.id.status);

        insideLevel = mView.findViewById(R.id.inside_amount);
        outsideLevel = mView.findViewById(R.id.outside_amount);
        gateLevel = mView.findViewById(R.id.gate_level);

        getWater = mView.findViewById(R.id.getWater);
        removeWater = mView.findViewById(R.id.removeWater);

        heightValue = mView.findViewById(R.id.height_value);
        height2Value = mView.findViewById(R.id.height2_value);

        bug = mView.findViewById(R.id.bug);

        editTab = mView.findViewById(R.id.edit_tab);

        insideLevel.getLayoutParams().height = (int) (25 * getResources().getDisplayMetrics().density + 0.5f);
        outsideLevel.getLayoutParams().height = (int) (25 * getResources().getDisplayMetrics().density + 0.5f);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buttonSetup() {
        bug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainActivity.isManager[mainActivity.selectedGate-1] == 0) mainActivity.passwordPopup("debug", mainActivity.debugIn, mainActivity.debugOut);
                else mainActivity.debugPopup(mainActivity.debugIn, mainActivity.debugOut);
            }
        });


        editTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity.isManager[mainActivity.selectedGate-1] == 0) mainActivity.passwordPopup("height", mainActivity.h1, mainActivity.h2);
                else mainActivity.heightPopup(mainActivity.h1, mainActivity.h2);
            }
        });

        getWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWater.setBackground(ContextCompat.getDrawable(
                        requireContext(),
                         R.drawable.button_enabled));
                handleGateAction("LAY_NUOC");
            }
        });

        removeWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeWater.setBackground(ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.button_enabled));
                handleGateAction("THAO_NUOC");
            }
        });

    }

    private void setupToolbar() {
        Toolbar toolbar = mView.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");

        if (getActivity() != null) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        }

        // Ensure the support action bar is not null
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayShowHomeEnabled(true);
        }

        // Handle the navigation click listener (back button on toolbar)
        toolbar.setNavigationOnClickListener(v -> {
            // Check if there are any fragments in the back stack
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();  // Pop the current fragment
            } else {
                requireActivity().onBackPressed();  // Go back in the activity's back stack
            }
        });
    }

    private void refreshStatus() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateWaterLevel(mainActivity.inLevel, mainActivity.outLevel, mainActivity.top_val, mainActivity.bot_val,
                        mainActivity.h1, mainActivity.h2, mainActivity.gateMode, mainActivity.gateStatus);
                handler.postDelayed(this, 2000);  // Repeat every 2 seconds
            }
        };
        handler.post(runnable);

        wifiHandler = new Handler();
        wifiRunnable = new Runnable() {
            @Override
            public void run() {
                if (!Objects.equals(mainActivity.getWifi(), "Cổng " + mainActivity.selectedGate) ||
                        mainActivity.h1 < 0 || mainActivity.h2 < 0){
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();  // Pop the current fragment
                    } else {
                        requireActivity().onBackPressed();  // Go back in the activity's back stack
                    }
                }
                wifiHandler.postDelayed(this, 2000);  // Repeat every 2 seconds
            }
        };
        wifiHandler.post(wifiRunnable);
    }

    private void handleGateAction(String status) {
        //Send to ESP///
        mainActivity.postOKHTTP(status, mainActivity.selectedGate);
    }

    int prevTopVal = -1;
    int prevBotVal = -1;
    String prevErrorCode = "";

    void updateWaterLevel(int in, int out, int top_val, int bot_val, int h1, int h2, String mode, String status) {
        insideLevel.getLayoutParams().height = getWaterPX(in);
        outsideLevel.getLayoutParams().height = getWaterPX(out);

        gateLevel.getLayoutParams().height = getGatePX(top_val, bot_val);

        if (Objects.equals(status, "DONG_CONG")) statusText.setText("Đang đóng cổng");
        else if (Objects.equals(status, "LAY_NUOC")) statusText.setText("Đang lấy nước");
        else if (Objects.equals(status, "THAO_NUOC")) statusText.setText("Đang tháo nước");
        else if (Objects.equals(status, "SWITCH_TREN")) statusText.setText("Đang chạm switch trên");
        else if (Objects.equals(status, "SWITCH_DUOI")) statusText.setText("Đang chạm switch dưới");
        else if (Objects.equals(status, "STANDBY")) statusText.setText("Không hoạt động");

        if (top_val == 1 && prevTopVal != top_val) mainActivity.showToast("Cổng lên cao nhất", 5000);
        else if (bot_val == 1 && prevBotVal != bot_val) mainActivity.showToast("Cổng xuống thấp nhất", 5000);
        prevTopVal = top_val;
        prevBotVal = bot_val;

        heightValue.setText(String.valueOf(h1));
        height2Value.setText(String.valueOf(h2));

        bug.setBackground(ContextCompat.getDrawable(
                requireContext(),
                Objects.equals(mainActivity.gateMode, "DEBUG") ? R.drawable.green_box : R.drawable.gray_box
        ));

        getWater.setBackground(ContextCompat.getDrawable(
                requireContext(),
                Objects.equals(mainActivity.gateStatus, "LAY_NUOC") ? R.drawable.button_enabled : R.drawable.button
        ));

        removeWater.setBackground(ContextCompat.getDrawable(
                requireContext(),
                Objects.equals(mainActivity.gateStatus, "THAO_NUOC") ? R.drawable.button_enabled : R.drawable.button
        ));

        insideLevel.requestLayout();
        outsideLevel.requestLayout();
        gateLevel.requestLayout();
    }

    private int getWaterPX(int level) {
        Log.d("JSON", "level: " + level);
        float baseHeight = getResources().getDimension(com.intuit.sdp.R.dimen._20sdp);
        float offset = getResources().getDimension(com.intuit.sdp.R.dimen._10sdp);
        return (int) ((level + 1) * baseHeight - offset);
    }

    private int getGatePX(int top_val, int bot_val) {
        float height;
        if (top_val == 1) {
            height = 0;
        } else if (bot_val == 1) {
            height = getResources().getDimension(com.intuit.sdp.R.dimen._160sdp);
        } else {
            height = getResources().getDimension(com.intuit.sdp.R.dimen._80sdp);
        }
        return (int) height;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (wifiHandler != null && wifiRunnable != null) {
            wifiHandler.removeCallbacks(wifiRunnable);
        }
    }

}