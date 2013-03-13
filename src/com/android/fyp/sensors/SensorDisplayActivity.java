package com.android.fyp.sensors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;

import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Activity Displays Sensor Data to the User, including showing a dialog with additional 
 * hardware information about the sensors when clicked. 
 * @author Cheryl
 *
 */
public class SensorDisplayActivity extends Activity implements OnClickListener {
	
	//Sensor Manager
	private SensorManager mSensorManager;
	private SensorEventListener mSensorListener;
	private Sensor mAccelerometer, mGyroscope;
	
	//UI Elements
	private Button b_caliberate;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        b_caliberate = (Button) findViewById(R.id.b_caliberate);
        
        //Sensor Manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        //Register Sensor Listener for all the sensors in the device. 
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        
        //Sensor Listener Object 
        mSensorListener = new SensorEventListener() {

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}

			public void onSensorChanged(SensorEvent event) {
				
			}
        };
    }
    
	/**
	 * OnClick Listener
	 */
	public void onClick(View v) {

		//Show the dialog when the user clicks the button
		switch(v.getId()) {

			case R.id.b_caliberate:
				
				
				
				break;
		
		}
	}
}