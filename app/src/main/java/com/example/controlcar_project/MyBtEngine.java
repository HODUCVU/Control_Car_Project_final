package com.example.controlcar_project;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MyBtEngine {

    private final MainActivity context;
    static final int BT_STATE_NONE = 0;
    static final int BT_STATE_CONNECTING = 1;
    static final int BT_STATE_CONNECTED = 2;
    //uid for all 3rd party devices (not android)
    private static final UUID UUID_BT_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //set MAC Address of device you want to connect in BT_DEVICE_MAC
    private static final String BT_DEVICE_MAC = "0C:B8:15:F3:94:7A";//put your device MAC !!!!

    private BtWaitConnThread mWaitConnThread = null;
    private BtWorkThread mWorkThread = null;
    private final BluetoothAdapter mAdapter;
    private int mState;

    MyBtEngine(MainActivity context) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = BT_STATE_NONE;
        this.context = context;
    }

    synchronized void start() {
        if (mAdapter == null) {
            return;
        }
        BluetoothDevice device;
        try {
            device = mAdapter.getRemoteDevice(BT_DEVICE_MAC);
        } catch (Exception e) {
            return;
        }

        if (mWaitConnThread != null) {
            mWaitConnThread.cancel();
            mWaitConnThread = null;
        }
        if (mWorkThread != null) {
            mWorkThread.cancel();
            mWorkThread = null;
        }

        mWaitConnThread = new BtWaitConnThread(device);
        mWaitConnThread.start();
        setState(BT_STATE_CONNECTING);
    }

    public synchronized void stop() {
        if (mWaitConnThread != null) {
            mWaitConnThread.cancel();
            mWaitConnThread = null;
        }
        if (mWorkThread != null) {
            mWorkThread.cancel();
            mWorkThread = null;
        }
        setState(BT_STATE_NONE);
    }

    private synchronized void setState(int state) {
        Log.e("MyBtEngine", "setState() " + mState + " -> " + state);
        mState = state;
    }

    synchronized int getState() {
        return mState;
    }

    public boolean writeBt(byte[] out) {
        BtWorkThread r; // temp obj , just to keep write function not to destroyed if mWorkThread finish
        synchronized (this) {
            if ((mWorkThread == null) || (mState != BT_STATE_CONNECTED)) {
                return false;
            }
            r = mWorkThread;
        }
        r.write(out);
        return true;
    }

    private synchronized void startWorker(BluetoothSocket socket, BluetoothDevice device) {
        if (mWaitConnThread != null) {
            mWaitConnThread.cancel();
            mWaitConnThread = null;
        }
        if (mWorkThread != null) {
            mWorkThread.cancel();
            mWorkThread = null;
        }
        mWorkThread = new BtWorkThread(socket);
        mWorkThread.start();
        setState(BT_STATE_CONNECTED);
    }

    private class BtWaitConnThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private BtWaitConnThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                tmp = device.createRfcommSocketToServiceRecord(UUID_BT_DEVICE);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mAdapter.cancelDiscovery();
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) { }
                setState(BT_STATE_NONE);
                return;
            }
            synchronized (MyBtEngine.this) {
                mWaitConnThread = null;//set itself to null because thread exit soon
            }
            startWorker(mmSocket, mmDevice);
        }
        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class BtWorkThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private BtWorkThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn  = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream  = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            while (true) {
                try {
                    int charFromEsp = mmInStream.read();//TODO: add code here for handling input from ESP
                } catch (IOException e) {
                    MyBtEngine.this.start();//restart from beginning
                    return; // exit worker thread
                }
            }
        }

        public boolean write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) { return false; }
            return true;
        }
        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
