package com.android.fyp.sensors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
import android.util.Log;
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
	private TextView tv_event, tv_show_events, tv_gps;
	
	//Dialog 
	DialogAct_nonSpanned log_dialog;
	AlertDialog alert_log;
	
	//Save to External Memory
	private static final String sensorlogpath = "sensor_data";
	private SaveExt save_ext;
	private String data_save = "";
	private String data_log = "";
	private String curr_time;
	private String log_time;
	private boolean start_log = false;
	private boolean end_log = false;
	
	//Sensor Manager
	private SensorManager sensorMgr;
	private List<Sensor> deviceSensors;
	private Sensor mAcc, mGyro, mMagnet, mLight, mProx, mTemp;
	private float[] aData = new float[3];
	private float[] gData = new float[3];
	private float[] mData = new float[3];
	private float[] aData_calib = new float[3];
	
	//Accelerometer
	private int count = 0;
	private float noise_thres = (float) 0.09;
	
	//Gyroscope
	//to store the integrated angle
	private double angle_x, angle_y, angle_z, prev_x, prev_y, prev_z;
	//to convert radians to degree
	private final float rad2deg = (float) (180.0f / Math.PI);
	
	//Location Manager
	private LocationManager locationMgr; 
	private Location lastknown;
	//to store the gps speed
	private float gps_speed;
	
	//Power Manager - to prevent the screen sleeping and stop collecting data
	private PowerManager pm;
	private PowerManager.WakeLock wl;
	
	//Caliberated values;
	private float[] cal_acc = new float[3];
	private float[] cal_gyro = new float[3];
	
	//State Transition
	//private State prev_state, curr_state;
	private String event_string = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensor);
		
		//initialize UI elements
		initialize();
		
		//get data from Main Activity to calibrate sensors
		getData();
		
		//save data to SD card
        save_ext = new SaveExt(this, sensorlogpath);
        save_ext.setState();
		
		//set wakelock to dim, i.e. screen will be dim, and CPU will still be running and not stop
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        
        //----------------- LOCATION -----------------------------------------------------------------//
        
        //Location Manager
        locationMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
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
		tv_show_events = (TextView) findViewById(R.id.tv_show_events);
		tv_gps = (TextView) findViewById(R.id.tv_gps);
		
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
		
		Bundle getdata = getIntent().getExtras();
    	if(getdata.getFloatArray("Acc") != null) {
    		cal_acc = getdata.getFloatArray("Acc");
    		Log.d("ACC_X", cal_acc[0] + "");
    		Log.d("ACC_Y", cal_acc[1] + "");
    		Log.d("ACC_Z", cal_acc[2] + "");
    	}
    	if(getdata.getFloatArray("Gyro") != null) {
    		cal_gyro = getdata.getFloatArray("Gyro");
    	}
		
	}

	/**-------------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| ACTIVITY FUNCTIONS |-----------------------------------------------*
	 *//*-----------------------------------------------------------------------------------------------------------*/
	@Override
    protected void onResume() {
    	super.onResume();
    	
    	// register listener for sensors
    	sensorMgr.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_NORMAL);
    	sensorMgr.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL);
    	
    	//Criteria for location provider
        Criteria crit = new Criteria(); 
        crit.setAccuracy(Criteria.ACCURACY_FINE); 
        crit.setAltitudeRequired(false); 
        crit.setBearingRequired(false); 
        crit.setPowerRequirement(Criteria.POWER_LOW); 
        String provider = locationMgr.getBestProvider(crit, true); 
        
        //if provider is null dialog to 
        if(provider != null) {
	        lastknown = locationMgr.getLastKnownLocation(provider);
	        if(lastknown != null)
	        	updateLocation(lastknown);
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
    	
    	//Get wakelock to prevent stop recording of sensor data
    	wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
    	wl.acquire();
    }

    @Override
    protected void onPause() {
    	// unregister listener
    	super.onPause();
    	
    	//release wakelock
    	wl.release();
    	
    	sensorMgr.unregisterListener(this, mAcc);
    	sensorMgr.unregisterListener(this, mGyro);

    	//stop listening for location
    	locationMgr.removeUpdates(this);
    }
    
	/**------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| ONCLICK |---------------------------------------------------*
	 *//*----------------------------------------------------------------------------------------------------*/
	public void onClick(View view) {
		
		log_dialog = new DialogAct_nonSpanned();
		SimpleDateFormat sdf_grdtruth = new SimpleDateFormat("dd-MM-yy HH-mm");
		
		switch(view.getId()) {
		case R.id.b_start_log:
			//Log.d("LOG", "START LOG : " + start_log + "END_LOG : " + end_log);
			if(start_log == false && end_log == false) {
				start_log = true;
				end_log = true;
				alert_log = log_dialog.dialog(this, "Alert", "Log has Started.");
				
				//save timestamp on start log
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH-mm");
				curr_time = sdf.format(new Date());
			}
			else
				alert_log = log_dialog.dialog(this, "Error!", "Current Log has not ended, cannot start a new Log.");
				
			alert_log.show();
			
			break;
		case R.id.b_end_log:
			//Log.d("LOG", "START LOG : " + start_log + "END_LOG : " + end_log);
			if(start_log == true && end_log == true) {
				end_log = false;
				start_log = false;
				alert_log = log_dialog.dialog(this, "Alert", "Log has Ended.");
				
				//KILL THE APP once logging stops
				this.finish();
			}
			else
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot end Log.");
			
			alert_log.show();
			
			break;
			
		case R.id.b_left:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "LEFT" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time , data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}
			
			break;
		case R.id.b_right:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "RIGHT" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time , data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}
			
			break;
		case R.id.b_straight:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "STRAIGHT" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time, data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}

			break;
		case R.id.b_accel:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "ACCELERATION" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time, data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}

			break;
		case R.id.b_decel:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "DECELERATION" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time, data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}

			break;
		case R.id.b_constant:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "CONSTANT" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time, data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}

			break;
		case R.id.b_stop:
			if(start_log == true && end_log == true) {
				data_log += time_stamp("time") + "\t" + "STOP" + "\n";
				//save timestamp on start log
				log_time = sdf_grdtruth.format(new Date());
				save_ext.writeExt(curr_time, data_log, "GroundTruth");
				data_log = "";
			}
			else {
				alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
				alert_log.show();
			}

			break;
		}
	}

	/**-----------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| SENSOR FUNCTIONS |-----------------------------------------------*
	 *//*---------------------------------------------------------------------------------------------------------*/
	public void onSensorChanged(SensorEvent event) {
		
		switch(event.sensor.getType()) {
		
			case Sensor.TYPE_ACCELEROMETER:
				
				aData = event.values.clone();
				
				if(cal_acc != null) {
					aData[0] = aData[0] - cal_acc[0];
					aData[1] = aData[1] - cal_acc[1];
					aData[2] = aData[2] - cal_acc[2];
					
					if(count == 0) {
						aData_calib[0] = aData[0];
						aData_calib[1] = aData[1];
						aData_calib[2] = aData[2];
					}
						
					count++;
					
				}else {
					aData_calib = event.values.clone();
				}
				
				//get forward acceleration values
				double fwd_acc = aData[0];
				
				//--------------------------------- STOP / CONSTANT STATE ----------------------------------------//
				if((aData[0] > 0 && aData[0] <= aData_calib[0]+noise_thres) || (aData[0] >0 && aData[0] >= aData_calib[0]-noise_thres) ) {
					//if(prev_state != State.STOP)
					if(gps_speed == 0.0)
						EventState.setCurrent(State.STOP);
					else if(gps_speed >= 2.0)
						EventState.setCurrent(State.CONST);
				}
				
				break;
				
			case Sensor.TYPE_GYROSCOPE:

				gData = event.values.clone();
				
				break;
		}
		
		tv_event.setText(EventState.getState().toString());
//		event_string += "State : " + EventState.getState().toString();
//		tv_show_events.setText(event_string);
		
	}
	
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		
	}
	
	/**-----------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| LOCATION LISTENER |-----------------------------------------------*
	 *//*---------------------------------------------------------------------------------------------------------*/	
	public void onLocationChanged(Location location) {
		updateLocation(location);
	}

	public void onProviderDisabled(String arg0) {
		
	}

	public void onProviderEnabled(String arg0) {
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		
	}
	
	private void updateLocation(Location location) {
		
		if(start_log == true && end_log == true) {
			// save to SD
			data_save += time_stamp("time") + "\t" + "GPS" + "\t" + "latitude,"
					+ location.getLatitude() + "\t" + "longitude,"
					+ location.getLongitude() + "\t" + "Speed,"
					+ location.getSpeed() + "\n";
			save_ext.writeExt(curr_time, data_save, "GPS");

			data_save = "";
		}
		
		//get gps speed in kmph instead of mps
		gps_speed = (float) (location.getSpeed()*3.6);
		tv_gps.setText("\nGPS Speed: " + gps_speed + "\n\n");
	}
	
	/**--------------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| OTHER FUNCTIONS |---------------------------------------------------*
	 *//*------------------------------------------------------------------------------------------------------------*/
	
	/**
	 * Convert time in Millis to dateformat specified by SimpleDateFormat (date)
	 * @param option	to specify to return the date or the time
	 * @return			String of converted timestamp from Millis (date)
	 */
	protected String time_stamp(String option) {
		// Create a DateFormatter object for displaying date information.
		
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
		
		if(option == "date")
			formatter = new SimpleDateFormat("dd-MM-yy");
		else if(option == "time")
			formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");

        // Get date and time information in milliseconds
        long now = System.currentTimeMillis();

        // Create a calendar object that will convert the date and time value
        // in milliseconds to date. We use the setTimeInMillis() method of the
        // Calendar object.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        
        return formatter.format(calendar.getTime());
	}
	
	private void integrateGyro(long event_timestamp, float timestamp, String axes, float value, float dT) {
    	
    	if(axes.equals("x"))
    		prev_x = angle_x;
    	if(axes.equals("y"))
    		prev_y = angle_y;
    	if(axes.equals("z"))
    		prev_z = angle_z;
		
		// This timestep's delta rotation to be multiplied by the current rotation
		// after computing it from the gyro sample data.
		if(axes.equals("x"))
			angle_x += (value*dT) * rad2deg;
		if(axes.equals("y"))
			angle_y += (value*dT) * rad2deg;
		if(axes.equals("z"))
			angle_z += (value*dT) * rad2deg;
		
		Log.d("integrate", "angle : " + angle_z);
    }
}
