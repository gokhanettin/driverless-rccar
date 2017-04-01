package com.gokhanettin.driverlessrccar.caroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonArduinoOnly = (Button) findViewById(R.id.button_monitor_only);
        final Button buttonCameraOnly = (Button) findViewById(R.id.button_camera_only);
        final Button buttonDataAcquisition = (Button) findViewById(R.id.button_data_acquisition);
        final Button buttonAutonomousDrive = (Button) findViewById(R.id.button_autonomous_drive);

        buttonArduinoOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,
                        ArduinoActivity.class);
                Log.d(TAG, "Starting ArduinoActivity");
                startActivity(intent);
            }
        });

        buttonCameraOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,
                        CameraActivity.class);
                Log.d(TAG, "Starting CameraActivity");
                startActivity(intent);
            }
        });

        buttonDataAcquisition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,
                        AcquisitionActivity.class);
                Log.d(TAG, "Starting AcquisitionActivity");
                startActivity(intent);
            }
        });

        buttonAutonomousDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });
    }
}
