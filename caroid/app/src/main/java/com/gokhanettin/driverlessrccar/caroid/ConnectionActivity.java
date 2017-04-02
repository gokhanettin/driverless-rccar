package com.gokhanettin.driverlessrccar.caroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.Set;

public class ConnectionActivity extends AppCompatActivity {
    public static final String TAG = "ConnectionActivity";

    public static final String EXTRA_BT_ADDRESS = "extra_bt_address";
    public static final String EXTRA_IP = "extra_ip";
    public static final String EXTRA_PORT = "extra_port";

    private static final int REQUEST_ENABLE_BT = 0;
    private String mBluetoothAddress = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            Toast.makeText(ConnectionActivity.this,
                    "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            populatePairedDeviceList();
        }

        Button buttonConnect = (Button) findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(mConnectClickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "BT enabled, populating device list");
                populatePairedDeviceList();
            } else {
                Log.d(TAG, "User rejected to enable BT, finishing");
                finish();
            }
        }
    }

    private void populatePairedDeviceList() {
        final ListView listViewPairedDevices =
                (ListView) findViewById(R.id.list_view_paired_devices);
        final TextView textViewBluetoothAddress =
                (TextView) findViewById((R.id.text_view_bluetooth_addr));
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            // There are paired devices.
            for (BluetoothDevice device : pairedDevices) {
                list.add(device.getName() + "\n" + device.getAddress());
            }

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, list);

            // Default device
            listViewPairedDevices.setAdapter(adapter);
            listViewPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String info = ((TextView) view).getText().toString();
                    mBluetoothAddress = info.substring(info.length() - 17);
                    textViewBluetoothAddress.setText(mBluetoothAddress);
                }
            });
        } else {
            Toast.makeText(getApplicationContext(),
                    "No Paired Bluetooth Device Found.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private final View.OnClickListener mConnectClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EditText editTextIp = (EditText) findViewById(R.id.edit_text_ip);
            EditText editTextPort = (EditText) findViewById(R.id.edit_text_port);

            String ip = editTextIp.getText().toString();
            int port = -1;
            try {
                port = Integer.parseInt(editTextPort.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid port no", e);
            }

            if (mBluetoothAddress != null && !ip.isEmpty() && port != -1) {
                Intent data = new Intent();
                data.putExtra(EXTRA_BT_ADDRESS, mBluetoothAddress);
                data.putExtra(EXTRA_IP, ip);
                data.putExtra(EXTRA_PORT, port);
                setResult(RESULT_OK, data);
            } else {
                Toast.makeText(getApplicationContext(), "Incomplete information",
                        Toast.LENGTH_LONG).show();
            }
            finish();
        }
    };
}
