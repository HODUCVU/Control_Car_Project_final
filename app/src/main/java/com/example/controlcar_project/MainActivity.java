package com.example.controlcar_project;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //Connect to ESP32 with bluetooth
    TextView txtStatus, txtPaired;
    Button btnOn, btnOff, btnDiscoverable, btnPaired;
    BluetoothAdapter mBluetoothAdapter;

    //Control car
    Button btnTurnLeft, btnTurnRight, btnUp, btnDown;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    private final int STATUS_CHECK_INTERVAL = 500;
    private MyBtEngine mBtEngine;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // ẩn thanh title
        setContentView(R.layout.activity_main);
        init();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            txtStatus.setText("Bluetooth is not available");
        } else {
            txtStatus.setText("Bluetooth is available");
        }

        if (mBluetoothAdapter.isEnabled()) {
            txtStatus.setText("Status: On");
        } else {
            txtStatus.setText("Status: Off");
        }

        btnOn.setOnClickListener(view -> {
            if (!mBluetoothAdapter.isEnabled()) {
                showToast("Turning ON Bluetooth...");

                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},PackageManager.PERMISSION_GRANTED);
                    return;
                }
                startActivityForResult(i, REQUEST_ENABLE_BT);
            }else {
                showToast("Bluetooth is already ON");
            }
        });

        btnDiscoverable.setOnClickListener(view -> {
            if(!mBluetoothAdapter.isDiscovering()) {
                showToast("Make your Device Discoverable");
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                if (ActivityCompat.checkSelfPermission(this, Arrays.toString(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE})) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_ADVERTISE},PackageManager.PERMISSION_GRANTED);
                    return;
                }
                startActivityForResult(i, REQUEST_DISCOVER_BT);
            }
        });
        btnOff.setOnClickListener(view -> {
            if(mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
                showToast("Turning Bluetooth OFF");
                txtStatus.setText("Bluetooth is OFF");
            } else {
                txtStatus.setText("Bluetooth is already OFF");
            }
        });
        btnPaired.setOnClickListener(view ->{
            if(mBluetoothAdapter.isEnabled()) {
                txtPaired.setText("");
                Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device: devices) {
                    //get Name devices and MAC Address devices
                    txtPaired.append("\nDevice: " + device.getName() + "," + device);
                }
            }
            else {
                showToast("Turn ON Bluetooth to Paired Devices");
                txtPaired.setText("Paired Devices");
            }

        });
        mBtEngine = new MyBtEngine(MainActivity.this);
        handlerStatusCheck.postDelayed(new Runnable() {
            @Override
            public void run() {
                onBtStatusCheckTimer();
                handlerStatusCheck.postDelayed(this, STATUS_CHECK_INTERVAL);
            }
        }, STATUS_CHECK_INTERVAL);

        handleTouchOnControl();
    }
    private final Handler handlerStatusCheck = new Handler();
    private void onBtStatusCheckTimer(){
        if(mBtEngine.getState() == MyBtEngine.BT_STATE_NONE) {
            mBtEngine.start();
        }


    }
    boolean writeBt(byte[] buffer){ return mBtEngine.writeBt(buffer); } // Send data with this function
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    txtStatus.setText("Bluetooth is ON");
                } else {
                    txtStatus.setText("Bluetooth is OFF");
                }
                break;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }
    private void showToast(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
    private void init() {
        txtStatus = findViewById(R.id.txtStatus);
        txtPaired = findViewById(R.id.txtPaire);
        btnOn = findViewById(R.id.btnTurnOn);
        btnOff = findViewById(R.id.btnTurnOff);
        btnDiscoverable = findViewById(R.id.btnDícoverable);
        btnPaired = findViewById(R.id.btnPaire);

        btnTurnLeft = findViewById(R.id.btnTurnLeft);
        btnTurnRight = findViewById(R.id.btnTurnRight);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
    }

    private void handleTouchOnControl() {
        btnTurnLeft.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            byte[] buffer;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //Xử lý sự kiện khi button được nhấn xuống
                    buffer = new byte[]{'L', '1'};
                    writeBt(buffer);
                    break;
                case MotionEvent.ACTION_UP:
                    //Xử lý sự kiện khi button được thả ra sau khi ấn giữ
                    buffer = new byte[]{'L', '0'};
                    writeBt(buffer);
                    break;
            }
            return false;
        });

        btnTurnRight.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            byte[] buffer;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //Xử lý sự kiện khi button được nhấn xuống
                    buffer = new byte[]{'R', '1'};
                    writeBt(buffer);
                    break;
                case MotionEvent.ACTION_UP:
                    //Xử lý sự kiện khi button được thả ra sau khi ấn giữ
                    buffer = new byte[]{'R', '0'};
                    writeBt(buffer);
                    break;
            }
            return false;
        });

        btnUp.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            byte[] buffer;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //Xử lý sự kiện khi button được nhấn xuống
                    buffer = new byte[]{'U', '1'};
                    writeBt(buffer);
                    break;
                case MotionEvent.ACTION_UP:
                    //Xử lý sự kiện khi button được thả ra sau khi ấn giữ
                    buffer = new byte[]{'U', '0'};
                    writeBt(buffer);
                    break;
            }
            return false;
        });
        btnDown.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            byte[] buffer;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //Xử lý sự kiện khi button được nhấn xuống
                    buffer = new byte[]{'D', '1'};
                    writeBt(buffer);
                    break;
                case MotionEvent.ACTION_UP:
                    //Xử lý sự kiện khi button được thả ra sau khi ấn giữ
                    buffer = new byte[]{'D', '0'};
                    writeBt(buffer);
                    break;
            }
            return false;
        });
    }
}