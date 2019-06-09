package projects.rashwan.shrouk.espbluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    /**  Buttons Names and Values to Send */
    String Button1Name = "Test 1";
    String Button2Name = "Test 2", Button2Value = "";
    String Button3Name = "Test 3", Button3Value = "";
    String Button4Name = "Test 4", Button4Value = "";
    String EspMAC = "30:AE:A4:15:C1:2E";
    int DefaultColor = Color.LTGRAY;
    int ChangedColor = Color.rgb(0,255,0);


    /* Layout Views */
    int lastPressed = 0;
    Button button1,button2,button3,button4, button;
    EditText sendBluetooth;
    ListView mDevicesListView;

    /* Bluetooth Values */
    String readMessage;
    BluetoothAdapter mBTAdapter;
    Handler mHandler;
    ConnectedThread mConnectedThread;
    BluetoothSocket mBTSocket = null;
    Set<BluetoothDevice> mPairedDevices;
    ArrayAdapter<String> mBTArrayAdapter;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initialization */
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        mBTArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        sendBluetooth = findViewById(R.id.editText);

        mDevicesListView = findViewById(R.id.devicesListView);
        assert mDevicesListView != null;
        mDevicesListView.setAdapter(mBTArrayAdapter);
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        button = findViewById(R.id.pairedDevices);
            // ------------- //
        button1 = findViewById(R.id.button);
        button1.setText(Button1Name);
        button1.setBackgroundColor(DefaultColor);
            // ------------- //
        button2 = findViewById(R.id.button2);
        button2.setBackgroundColor(DefaultColor);
        button2.setText(Button2Name);
            // ------------- //
        button3 = findViewById(R.id.button3);
        button3.setBackgroundColor(DefaultColor);
        button3.setText(Button3Name);
            // ------------- //
        button4 = findViewById(R.id.button4);
        button4.setBackgroundColor(DefaultColor);
        button4.setText(Button4Name);


        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){

                    try {
                        String Message = (new String((byte[])msg.obj, StandardCharsets.UTF_8));
                        readMessage = readMessage + "" + Message;
                        if (readMessage.contains("OK"))
                        {
                            if(lastPressed == 1)
                            {
                                button1.setBackgroundColor(ChangedColor);
                            }
                            else if(lastPressed == 2)
                            {
                                button2.setBackgroundColor(ChangedColor);
                            }
                            else if(lastPressed == 3)
                            {
                                button3.setBackgroundColor(ChangedColor);
                            }
                            else if(lastPressed == 4)
                            {
                                button4.setBackgroundColor(ChangedColor);
                            }
                            readMessage = "";
                            lastPressed = 0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();

                    else {

                        Toast.makeText(getApplicationContext(),"Not Connected ",Toast.LENGTH_SHORT).show();
                        //connectESP32();
                    }
                }
            }
        };
        if (mBTAdapter == null) {
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            //Log.d("Start", "Connect to ESP32");
            //connectESP32();

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices();
                }
            });

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button1.setBackgroundColor(DefaultColor);
                    button2.setBackgroundColor(DefaultColor);
                    button3.setBackgroundColor(DefaultColor);
                    button4.setBackgroundColor(DefaultColor);
                    lastPressed = 1;
                    String toSend = sendBluetooth.getText().toString();
                    mConnectedThread.write(toSend);
                }
            });

            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button1.setBackgroundColor(DefaultColor);
                    button2.setBackgroundColor(DefaultColor);
                    button3.setBackgroundColor(DefaultColor);
                    button4.setBackgroundColor(DefaultColor);
                    lastPressed = 2;
                    mConnectedThread.write(Button2Value);
                }
            });

            button3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button1.setBackgroundColor(DefaultColor);
                    button2.setBackgroundColor(DefaultColor);
                    button3.setBackgroundColor(DefaultColor);
                    button4.setBackgroundColor(DefaultColor);
                    lastPressed = 3;
                    mConnectedThread.write(Button3Value);
                }
            });

            button4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button1.setBackgroundColor(DefaultColor);
                    button2.setBackgroundColor(DefaultColor);
                    button3.setBackgroundColor(DefaultColor);
                    button4.setBackgroundColor(DefaultColor);
                    lastPressed = 4;
                    mConnectedThread.write(Button4Value);
                }
            });

        }
    }

    /**
     *  Connect Bluetooth to Esp32s
     */
    public void connectESP32() {

        if (!mBTAdapter.isEnabled()) {
            Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            public void run() {
                boolean fail = false;
                BluetoothDevice device = mBTAdapter.getRemoteDevice(EspMAC);
                String name = device.getName();
                Log.d("Bluetooth", name);

                try {
                    mBTSocket = createBluetoothSocket(device);

                } catch (IOException e) {
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                try {
                    mBTSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if (!fail) {
                    mConnectedThread = new ConnectedThread(mBTSocket);
                    mConnectedThread.start();
                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget();
                }
            }
        }.start();
    }

    private void listPairedDevices(){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);
            Toast.makeText(getBaseContext(), "Connecting to " + name +" mac address " + address, Toast.LENGTH_SHORT).show();

            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
                    String name = device.getName();
                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };


    /**
     * @param device : create a bluetooth sockect with Esp32 Device.
     * @return BluetoothSocket : the created socket.
     * @throws IOException : when you can't access Bluetooth socket
     */
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    /**
     * Class extend thread to get input stream from bluetooth in background.
     */
    class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;
            int bytes;
            while (true) {
               // if (!mBTSocket.isConnected())
                //{
                    //connectESP32();
                //}
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
