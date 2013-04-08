package com.android.fyp.sensors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries.GraphViewStyle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SensorLinear extends Activity implements OnClickListener, SensorEventListener, LocationListener{
	
	private GraphView graphView;
	private GraphViewSeries gyro_x, gyro_y, gyro_z, gyro_angle;
	private LinearLayout layout;
	
	private boolean tilt_up, tilt_down;
	private boolean speed_accuracy = false;
	private boolean entered;
	
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
	//threshold for noise
	private float noise_thres = (float) 0.09;
	private float gyro_thres = (float) 0.01;
	//threshold for forward, backward acceleration (acc_x)
//	private float fwd_thres = (float) 0.8;
	//higher = less sensitive
	//lower = more sensitive
	private float fwd_thres = (float) 0.8;
	private float back_thres = (float) 0.8;
	
	//Gyroscope
	//to store the integrated angle
	private double angle_x, angle_y, angle_z, prev_x, prev_y, prev_z;
	//to convert radians to degree
	private final float rad2deg = (float) (180.0f / Math.PI);
	// Create a constant to convert nanoseconds to seconds.
	private static final float NS2S = 1.0f / 1000000000.0f;
	private float dT;
	private float timestamp;
	//peak detection
	double max = -10000000;
	double min = 10000000;
	//minimal vertical distance between two peaks
	double dist_peaks = 0.05;
	boolean g_stationary;
	
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
	private boolean stop = true;
	
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
        
        mAcc = sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyro = sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnet = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        sensorMgr.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_NORMAL);
        sensorMgr.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL);
        sensorMgr.registerListener(this, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
        //graph();
        
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
		
		float[] rotationMatrix;
		
		switch(event.sensor.getType()) {
		
			case Sensor.TYPE_LINEAR_ACCELERATION:
				
				aData = event.values.clone();
				
				rotationMatrix = generateRotationMatrix();
			
				if (rotationMatrix != null)
				{
					determineOrientation(rotationMatrix);
				}
				
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
				
				//get forward acceleration values - Y AXIS
				double fwd_acc = aData[1];
				
				//--------------------------------- STOP / CONSTANT STATE ----------------------------------------//
				if((aData[1] > 0 && aData[1] <= aData_calib[1]+noise_thres) || (aData[1] >1 && aData[1] >= aData_calib[1]-noise_thres) ) {
					//if(prev_state != State.STOP)
					//if(gps_speed == 0.0 || g_stationary == true) {
					//if(g_stationary == true) {
					if(speed_accuracy) {
						if(gps_speed == 0.0) {
							if(EventState.checkTransit(State.STOP)) {
								stop = true;
								EventState.setCurrent(State.STOP, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString());
								event_string += "\nSTOPPPPPP" + ", curr_state : " + EventState.getState().toString() + "\n";
							}
						}
						else if(gps_speed >= 2.0) {
							if(EventState.checkTransit(State.CONST)) {
								stop = false;
								EventState.setCurrent(State.CONST, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString());
								event_string += "\nCONSTANT SPEED" + ", curr_state : " + EventState.getState().toString() + "\n";
							}
						}
					} else {
						if(g_stationary == true) {
							if(EventState.checkTransit(State.STOP)) {
								stop = true;
								EventState.setCurrent(State.STOP, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString());
								event_string += "\nSTOPPPPPP" + ", curr_state : " + EventState.getState().toString() + "\n";
							}
						}
					}
				}
				
				if(fwd_acc >= back_thres) {
//					if(g_stationary == false) {

//					if(!tilt_up && !tilt_down) {
//						Log.d("ACC", "TILT UP : " + tilt_up + " tilt_down : " + tilt_down);
						if(EventState.checkTransit(State.DEC)) {
							stop = false;
							EventState.setCurrent(State.DEC, System.currentTimeMillis());
							tv_event.setText(EventState.getState().toString());
							event_string += "\nDECELERATE" + ", curr_state : " + EventState.getState().toString() + "\n";
						}
//					}
				}
				else if(fwd_acc <= (fwd_thres*-1)) {
//					if(g_stationary == false) {

//					if(!tilt_up && !tilt_down) {
//						Log.d("ACC", "TILT UP : " + tilt_up + " tilt_down : " + tilt_down);
						if(EventState.checkTransit(State.ACC)) {
							stop = false;
							EventState.setCurrent(State.ACC, System.currentTimeMillis());
							tv_event.setText(EventState.getState().toString());
							event_string += "\nACCELERATE" + ", curr_state : " + EventState.getState().toString() + "\n";
						}
//					}
				}
				
				break;
				
			case Sensor.TYPE_GYROSCOPE:

				gData = event.values.clone();
				
//				//append data to graph
//				gyro_x.appendData(new GraphViewData(System.currentTimeMillis(), gData[0]), true);
//				gyro_y.appendData(new GraphViewData(System.currentTimeMillis(), gData[1]), true);
//				gyro_z.appendData(new GraphViewData(System.currentTimeMillis(), gData[2]), true);
//				gyro_angle.appendData(new GraphViewData(System.currentTimeMillis(), angle_z), true);
				
				//--------------------------------- GYRO STOP / CONSTANT STATE ----------------------------------------//
				if((gData[1] > 0 && gData[1] <= cal_gyro[1]+gyro_thres) || (gData[1] >0 && gData[1] >= cal_gyro[1]-gyro_thres) ) {
					g_stationary = true;
				}
				
				if(timestamp != 0) {
					dT = (event.timestamp - timestamp) * NS2S;							
				} 
				timestamp = event.timestamp;
				
				if(gData[2] > max)
					max = gData[2];
				else if(gData[2] < min)
					min = gData[2];
				
				if(gData[2] < max - dist_peaks) {
					integrateGyro(event.timestamp, timestamp, "z", gData[2], dT);
					if((angle_z - prev_z) > 1.0) {
						if(EventState.checkDir(State.LEFT)) {
							EventState.setDir(State.LEFT, System.currentTimeMillis());
							tv_event.setText(EventState.getDir().toString());
							event_string += "\nLEFT" + ", curr_state : " + EventState.getDir().toString() + "\n";
						}
					}
					else if((angle_z - prev_z) < -1.0) {
						if(EventState.checkDir(State.RIGHT)) {
							EventState.setDir(State.RIGHT, System.currentTimeMillis());
							tv_event.setText(EventState.getDir().toString());
							event_string += "\nRIGHT" + ", curr_state : " + EventState.getDir().toString() + "\n";
						}
					}
					
					max = gData[2];
					prev_z = angle_z;
				} else {
					EventState.setDir_str(State.STRAIGHT);
					if(stop) {
						tv_event.setText(EventState.getState().toString());
						//event_string += "\nSTOPPPPPP" + ", curr_state : " + EventState.getState().toString() + "\n";
					}
				}
				
				break;
				
			case Sensor.TYPE_MAGNETIC_FIELD:
				
				mData = event.values.clone();
				rotationMatrix = generateRotationMatrix();
				
				if (rotationMatrix != null)
				{
					determineOrientation(rotationMatrix);
				}
				break;
				
		}
		
		tv_show_events.setText(event_string);
//		tv_event.setText(EventState.getState().toString());
//		event_string += "State : " + EventState.getState().toString();
//		tv_show_events.setText(event_string);
		
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
		
	}
	
	/**-----------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| LOCATION LISTENER |-----------------------------------------------*
	 *//*---------------------------------------------------------------------------------------------------------*/	
	public void onLocationChanged(Location location) {
		updateLocation(location);
		
		if(location.getAccuracy() == Criteria.ACCURACY_FINE)
			speed_accuracy = true;
		else
			speed_accuracy = false;
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
	
	//integrate Gyroscope data to get angle change
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
	
	private void graph() {
		/**------------------------------------------------------------------------------------------------------**
 		 * 	---------------------------------------| GYROSCOPE GRAPH |-------------------------------------------*
 		 *//*----------------------------------------------------------------------------------------------------*/																										 	
 		//Gyroscope graph
 		gyro_x = new GraphViewSeries("gyro_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3),new GraphViewData[] {});
 		gyro_y = new GraphViewSeries("gyro_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3),new GraphViewData[] {});
 		gyro_z = new GraphViewSeries("gyro_z", null ,new GraphViewData[] {});
 		gyro_angle = new GraphViewSeries("gyro_angle", new GraphViewStyle(Color.rgb(200, 50, 00), 3) ,new GraphViewData[] {});
 		
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Gyroscope Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(gyro_x); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.gyro_graph_x);
 		layout.addView(graphView);
 		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Gyroscope Data") {
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX)
					return formatter.format(value); // convert unix time to
													// human time
				else
					return super.formatLabel(value, isValueX); // let the
																// y-value be
																// normal-formatted
			}
		};

		graphView.addSeries(gyro_y); // data
		graphView.setScrollable(true);
		graphView.setViewPort(1, 80000);
		// graphView.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.gyro_graph_y);
		layout.addView(graphView);
		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Gyroscope Data") {
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX)
					return formatter.format(value); // convert unix time to
													// human time
				else
					return super.formatLabel(value, isValueX); // let the
																// y-value be
																// normal-formatted
			}
		};

		graphView.addSeries(gyro_z); // data
		graphView.setScrollable(true);
		graphView.setViewPort(1, 80000);
		// graphView.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.gyro_graph_z);
		layout.addView(graphView);
		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Gyroscope Data") {
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX)
					return formatter.format(value); // convert unix time to
													// human time
				else
					return super.formatLabel(value, isValueX); // let the
																// y-value be
																// normal-formatted
			}
		};

		graphView.addSeries(gyro_angle); // data
		graphView.setScrollable(true);
		graphView.setViewPort(1, 80000);
		// graphView.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.gyro_graph_angle);
		layout.addView(graphView);
	}
	
	//determine orientation so as to prevent Acc & Dec
	private void determineOrientation(float[] rotationMatrix) {
		
		float[] orientationValues = new float[3];
		SensorManager.getOrientation(rotationMatrix, orientationValues);
		
		double azimuth = Math.toDegrees(orientationValues[0]);
		double pitch = Math.toDegrees(orientationValues[1]);
		double roll = Math.toDegrees(orientationValues[2]);
		
		if (pitch <= 10) {
			
			if (roll >= -50 && roll <= -10) {
				Log.d("Tilt", "Tilt down" + roll);
				tilt_down = true;
				tilt_up = false;
			} else if (roll >= 10 && roll <= 50) {
				Log.d("Tilt", "Tilt up" + roll);
				tilt_up = true;
				tilt_down = false;
			} else {
				tilt_up = false;
				tilt_down = false;
			}
		}
	}

	//get rotation matrix to earth coordinates
	private float[] generateRotationMatrix() {
		float[] rotationMatrix = null;
		if (aData != null && mData != null) {
			
			rotationMatrix = new float[16];
			
			boolean rotationMatrixGenerated;
			rotationMatrixGenerated = SensorManager.getRotationMatrix(
					rotationMatrix, null, aData, mData);
			
			if (!rotationMatrixGenerated) {
				Log.d("Rotation Matrix", "Failed to generate Rotation Matrix");
				rotationMatrix = null;
			}
		}
		return rotationMatrix;
	}
}
