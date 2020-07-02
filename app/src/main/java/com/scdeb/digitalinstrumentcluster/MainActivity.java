package com.scdeb.digitalinstrumentcluster;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    // Usb specific members
    private static UsbManager mManager;
    private static UsbDeviceConnection mConnection;
    private static UsbDevice mDevice;
    private static UsbSerialDevice mSerialPort;

    // TextViews
    //TODO Memory leak with text view being static
    //leave for testing
    private static TextView mDataView;

    private static UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = "boobs";
            data = new String(arg0);
            //data.concat("/n");
            //updateTextView(data);
            Log.d("USBIN", data);
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    mConnection = mManager.openDevice(mDevice);
                    mSerialPort = UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection);
                    if (mSerialPort != null && mSerialPort.open()) {
                        mSerialPort.setBaudRate(115200);
                        mSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                        mSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                        mSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
                        mSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                        mSerialPort.read(mCallback);
                    } else {
                        Log.d("SERIAL", "PORT NOT OPEN/PORT NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERMISSION NOT GRANTED");
                }
            }
        }
    };

    /**
    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }
     **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataView = (TextView) findViewById(R.id.textView);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(broadcastReceiver, filter);
        setContentView(R.layout.activity_main);
    }

    public void onClickStart(View view) {
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = mManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                mDevice = entry.getValue();

                int deviceVID = mDevice.getVendorId();
                if (deviceVID == 0x2341) { //arduino vendor id
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0,
                            new Intent(ACTION_USB_PERMISSION), 0);
                    mManager.requestPermission(mDevice, pi);
                    keep = false;
                } else {
                    mConnection = null;
                    mDevice = null;
                }

                if (!keep) {
                    break;
                }
            }
        }
    }

    public static void updateTextView(String updated) {
        mDataView.setText(updated);
    }

}