package com.fea.bletest;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.UUID;


public class MainActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    String SERVICE_UUID ="0000ffe0-0000-1000-8000-00805f9b34fc";
    String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fc";
    String client_characteristic_configuration ="00002902-0000-1000-8000-00805f9b34fb";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermisions();
        initializeBluetooth();
        startScan();

        // To send data
//        byte data[]={0x53,0x00,0x00,0x1};
//        sendData(data);
    }
    void initPermisions()
    {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
    }
    private void initializeBluetooth() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanCallback = new ScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (result == null || result.getDevice().getName() == null) {
                } else {
                    if (result.getDevice().getName().equals("ESP32"))
                        connectToDevice(result.getDevice());
                }

            }
        };
    }

    // connect to the BLE device
    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }

    @SuppressLint("MissingPermission")
    private void sendData(byte[] data) {
        mCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    // start scanning for BLE devices
    @SuppressLint("MissingPermission")
    private void startScan() {
        mBluetoothLeScanner.startScan(mScanCallback);
    }

    // stop scanning for BLE devices
    @SuppressLint("MissingPermission")
    private void stopScan() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                Log.d("ReceivedData", Arrays.toString(data));
                // Do something with the data
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] data = characteristic.getValue();
            Log.d("ReceivedData", Arrays.toString(data));
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                stopScan();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
                mCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                if (service != null) {
                    if (mCharacteristic != null) {
                        gatt.setCharacteristicNotification(mCharacteristic, true);
                        BluetoothGattDescriptor desc =mCharacteristic.getDescriptor(UUID.fromString(client_characteristic_configuration));
                        byte[] enablenotification = {0x01, 0x00};
                        desc.setValue(enablenotification);
                        gatt.writeDescriptor(desc);
                    }
                }
            }
        }
    };
}
