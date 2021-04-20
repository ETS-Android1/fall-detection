package things.android.essential;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private TextView labelBluetoothStatus;

    private Button enableBluetoothButton;
    private Button disableBluetoothButton;
    private Button listPairedDevicesButton;
    private Button discoverNewDevicesButton;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> bluetoothDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;

    private TextView status;
    ConstraintLayout setting;
    RelativeLayout waiting;
    Context context;


    private Handler mHandler; // Main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BLUETOOTH_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS}, PackageManager.PERMISSION_GRANTED);

        labelBluetoothStatus = findViewById(R.id.bluetoothStatus);

        enableBluetoothButton = findViewById(R.id.scan);
        disableBluetoothButton = findViewById(R.id.off);
        listPairedDevicesButton = findViewById(R.id.pairedBtn);
        discoverNewDevicesButton = findViewById(R.id.discover);


        setting = (ConstraintLayout) findViewById(R.id.setting);
        waiting = findViewById(R.id.waiting);
        status = findViewById(R.id.status);

        context = this;

        mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mDevicesListView = (ListView) findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);


        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {


                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        labelBluetoothStatus.setText("Connected to Device: " + (String) (msg.obj));
                    } else
                        labelBluetoothStatus.setText("Connection Failed");
                }
                if (msg.what == MESSAGE_READ) {
                    labelBluetoothStatus.setText("Sending SOS: " + (String) (msg.obj));
                }

            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            labelBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            disableBluetoothButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOff(v);
                }
            });

            listPairedDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listPairedDevices(v);
                }
            });

            discoverNewDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover(v);
                }
            });
        }
    }

    private void bluetoothOn(View view) {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            labelBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();

        } else {
            labelBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to

        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                labelBluetoothStatus.setText("Enabled");
            } else
                labelBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view) {
        bluetoothAdapter.disable(); // turn off
        labelBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view) {
        // Check if the device is already discovering
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                bluetoothAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view) {
        bluetoothDevices = bluetoothAdapter.getBondedDevices();
        if (bluetoothAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : bluetoothDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("SetTextI18n")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            //setting.setVisibility(View.GONE);
            //waiting.setVisibility(View.VISIBLE);

            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            labelBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

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
                    if (!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();


                    }
                }
            }.start();

        }

    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BLUETOOTH_MODULE_UUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignore) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    // Read from the InputStream
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    if (bytes != 0) {
                        //SystemClock.sleep(200); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        //bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                        Toast.makeText(getBaseContext(), "Sending SOS", Toast.LENGTH_LONG).show();
                        mHandler.obtainMessage(MESSAGE_READ, 1, -1, "0745664689")
                                .sendToTarget();
                        //status.setText("Sending SOS");
                        //SmsManager sms = SmsManager.getDefault();
                        //sms.sendTextMessage("+0745664689",null,"SOS 2.523678 23.5532678",null,null);
                        //LocationManager location = LocationManager.GPS_PROVIDER


                        // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);

            } catch (IOException ignore) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignore) {
            }
        }
    }
}
