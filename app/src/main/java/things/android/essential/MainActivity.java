package things.android.essential;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends Activity implements SerialListener {

    // Create an executor that executes tasks in a background thread.
    ScheduledExecutorService backgroundExecutor = Executors.newSingleThreadScheduledExecutor();

    private TextView labelBluetoothStatus;
    private TextView status;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> blueToothArrayAdapter;

    ConstraintLayout setting;
    RelativeLayout waiting;
    Context context;
    String connectedDevice;


    private Handler mHandler; // Handler that will receive callback notifications
    private SerialSocket socket = null;

    private final static int REQUEST_ENABLE_BLUETOOTH = 1;
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS}, PackageManager.PERMISSION_GRANTED);

        labelBluetoothStatus = findViewById(R.id.bluetoothStatus);

        Button enableBluetoothButton = findViewById(R.id.scan);
        Button disableBluetoothButton = findViewById(R.id.off);
        Button listPairedDevicesButton = findViewById(R.id.pairedBtn);
        Button discoverNewDevicesButton = findViewById(R.id.discover);


        setting = (ConstraintLayout) findViewById(R.id.setting);
        waiting = findViewById(R.id.waiting);
        status = findViewById(R.id.status);

        context = this;

        blueToothArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ListView mDevicesListView = findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(blueToothArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(deviceClickListener);


        mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        labelBluetoothStatus.setText("Connected to Device: " + msg.obj);
                    } else if (msg.arg1 == 2) {
                        labelBluetoothStatus.setText("Connection Failed" + msg.obj);
                    } else {
                        labelBluetoothStatus.setText("" + msg.obj);
                    }
                }
                if (msg.what == MESSAGE_READ) {
                    labelBluetoothStatus.setText("Sending SOS: " + msg.obj);
                }
            }
        };

        if (blueToothArrayAdapter == null) {
            // Device does not support Bluetooth
            labelBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                }
            });

            disableBluetoothButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOff();
                }
            });
            listPairedDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listPairedDevices();
                }
            });
            discoverNewDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover();
                }
            });
        }
    }

    private void bluetoothOn() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            labelBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();

        } else {
            labelBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    private void bluetoothOff() {
        bluetoothAdapter.disable();
        labelBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover() {
        // Check if the device is already discovering
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                blueToothArrayAdapter.clear(); // clear items
                bluetoothAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void listPairedDevices() {
        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        blueToothArrayAdapter.clear(); // clear items
        if (bluetoothAdapter.isEnabled()) {
            for (BluetoothDevice device : bluetoothDevices)
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    blueToothArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                blueToothArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                blueToothArrayAdapter.notifyDataSetChanged();
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);

        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                labelBluetoothStatus.setText("Enabled");
            } else
                labelBluetoothStatus.setText("Disabled");
        }
    }


    private final AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
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
            connectedDevice = info.substring(0, info.length() - 17);

            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                    try {
                        socket = new SerialSocket(getBaseContext().getApplicationContext(), device);
                        socket.connect((SerialListener) context);
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }

    };

    @Override
    public void onSerialConnect() throws IOException {
        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, connectedDevice)
                .sendToTarget();
        byte[] data = ("Greetings from Android" + "\r\n").getBytes();
        if (socket != null) {
            socket.write(data);
        }

    }

    @Override
    public void onSerialConnectError(Exception e) {
        mHandler.obtainMessage(CONNECTING_STATUS, 2, -1, e.getMessage())
                .sendToTarget();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        mHandler.obtainMessage(CONNECTING_STATUS, 3, -1, ", Connection lost")
                .sendToTarget();
    }

    private void receive(byte[] data) {
        mHandler.obtainMessage(MESSAGE_READ, 1, -1, "0745664689")
                .sendToTarget();
    }


}
