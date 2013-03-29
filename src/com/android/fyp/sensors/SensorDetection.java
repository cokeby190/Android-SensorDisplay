package com.android.fyp.sensors;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SensorDetection extends Activity implements OnClickListener, SensorEventListener, LocationListener{
	
	//UI Buttons
	private Button b_start_log, b_end_log;
	private Button b_left, b_right;
	private Button b_accel, b_decel;
	private Button b_constant, b_stop;
	private TextView tv_event;
	
	//Sensor Manager
	private SensorManager sensorMgr;
	private List<Sensor> deviceSensors;
	private Sensor mAcc, mGyro, mMagnet, mLight, mProx, mTemp;
	
	//Location Manager
	private LocationManager locationMgr; 
	
	//Power Manager - to prevent the screen sleeping and stop collecting data
	PowerManager.WakeLock wl;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensor);
		
		//initialize UI elements
		initialize();
		
		//get data from Main Activity to calibrate sensors
		getData();
		
		//set wakelock to dim, i.e. screen will be dim, and CPU will still be running and not stop
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();
        
        //----------------- LOCATION -----------------------------------------------------------------//
        
        //Location Manager
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                
        //Criteria for location provider
        Criteria crit = new Criteria(); 
        crit.setAccuracy(Criteria.ACCURACY_FINE); 
        crit.setAltitudeRequired(false); 
        crit.setBearingRequired(false); 
        crit.setPowerRequirement(Criteria.POWER_LOW); 
        String provider = locationMgr.getBestProvider(crit, true); 
        
        //if provider is null dialog to 
        if(provider != null) {
	        Location location = locationMgr.getLastKnownLocation(provider); 
	        // TODO : update LOCATION with above location
	        //requestLocationUpdates(provider, time interval, meters, LocationListener)
	        //10000 = 10 sec interval
	        locationMgr.requestLocationUpdates(provider, 0, 0, this); 
        } else {
        	String location_dis = "This application requires either GPS or Wifi to be turned on. Please enable it in the Settings button.";
			
			CreateAlertDialog dialog = new CreateAlertDialog();
        	AlertDialog alert = dialog.newdialog(this, location_dis);
        	alert.show();
        }
        
      //----------------- SENSOR -----------------------------------------------------------------//
        
        //Sensor Manager
        sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        mAcc = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //mMagnet = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        sensorMgr.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_NORMAL);
        sensorMgr.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL);
        
        
        
		
	}
	
	/**
	 * Intialise UI Elements
	 */
	private void initialize() {
		
		tv_event = (TextView) findViewById(R.id.tv_event);
		
		b_start_log = (Button) findViewById(R.id.b_start_log);
		b_end_log = (Button) findViewById(R.id.b_end_log);
		b_left = (Button) findViewById(R.id.b_left);
		b_right = (Button) findViewById(R.id.b_right);
		b_accel = (Button) findViewById(R.id.b_accel);
		b_decel = (Button) findViewById(R.id.b_decel);
		b_constant = (Button) findViewById(R.id.b_constant);
		b_stop = (Button) findViewById(R.id.b_stop);
		
		b_start_log.setOnClickListener(this);
		b_end_log.setOnClickListener(this);
		b_left.setOnClickListener(this);
    	b_right.setOnClickListener(this);
    	b_accel.setOnClickListener(this);
    	b_decel.setOnClickListener(this);
    	b_constant.setOnClickListener(this);
    	b_stop.setOnClickListener(this);
		
	}

	/**
	 * Function getting caliberation data from Main Activity
	 */
	private void getData() {
		
	}

	/**-------------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| ACTIVITY FUNCTIONS |-----------------------------------------------*
	 *//*-----------------------------------------------------------------------------------------------------------*/
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	/**------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| ONCLICK |---------------------------------------------------*
	 *//*----------------------------------------------------------------------------------------------------*/
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.b_start_log:
				break;
			case R.id.b_end_log:
				break;
			case R.id.b_left:
				break;
			case R.id.b_right:
				break;
			case R.id.b_accel:
				break;
			case R.id.b_decel:
				break;
			case R.id.b_constant:
				break;
			case R.id.b_stop:
				break;
		}
	}

	/**-----------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| SENSOR FUNCTIONS |-----------------------------------------------*
	 *//*---------------------------------------------------------------------------------------------------------*/
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		
	}

	public void onSensorChanged(SensorEvent arg0) {
		
	}

	/**-----------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| LOCATION LISTENER |-----------------------------------------------*
	 *//*---------------------------------------------------------------------------------------------------------*/	
	public void onLocationChanged(Location arg0) {
		
	}

	public void onProviderDisabled(String arg0) {
		
	}

	public void onProviderEnabled(String arg0) {
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		
	}
}
