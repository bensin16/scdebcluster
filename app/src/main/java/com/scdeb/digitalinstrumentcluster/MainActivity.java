/*
 * MainActivity.java
 * Created 6/24
 * Author Drew Bensinger
 */

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

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Constants
    // TODO move constants to own file
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    // Usb specific members
    /** Manager to receive connected usb devices */
    private static UsbManager mManager;
    /** Connection used to open UsbSerialPort */
    private static UsbDeviceConnection mConnection;
    /** Represents a connected USB device */
    private static UsbDevice mDevice;

    // TextViews
    //TODO **URGENT** Memory leak with text view being static
    //leave for testing
    private static TextView mDataView;

    /**
     * Register object to create callback function for when data is received
     */
    private static UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        /**
         * Log data with debug flag and 'USBIN' tag when data is received from a usb device
         * @param arg0
         */
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = "";
            data = new String(arg0);
            //data.concat("/n");
            //updateTextView(data);
            Log.d("USBIN", data);
        }
    };

    /**
     * Broadcast receiver for when an arduino devices is connected
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        /**
         * Ensure action is for usb permissions, set parameters for communication
         * @param context Context tied to the request
         * @param intent Intent containing Action that should be 'ACTION_USB_PERMISSION'
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Resolve possible null pointer exceptions, still works so far
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    mConnection = mManager.openDevice(mDevice);
                    UsbSerialDevice serialPort =
                                        UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection);
                    if (serialPort != null && serialPort.open()) {
                        // TODO maybe lower baud rate to improve accuracy of readings, might not work
                        serialPort.setBaudRate(115200);
                        serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                        serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                        serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                        serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                        serialPort.read(mCallback);
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

        // Register broadcast receiver with usb permission intent filter
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(broadcastReceiver, filter);
        setContentView(R.layout.activity_main);
    }

    // TODO move to onCreate to remove requirement of pushing button

    /**
     * Attempt to connect to an arduino over USB connection
     * @param view View tied to the button pressed
     */
    public void onClickStart(View view) {
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = mManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                mDevice = entry.getValue();

                int deviceVID = mDevice.getVendorId();
                // Compare device vendor ID to an arduino's vendor ID
                if (deviceVID == 0x2341) {
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

}