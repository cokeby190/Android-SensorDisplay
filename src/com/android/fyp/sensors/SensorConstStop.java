package com.android.fyp.sensors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SensorConstStop extends Activity implements OnClickListener, SensorEventListener, LocationListener{
	
	private GraphView graphView;
	private GraphViewSeries gyro_x, gyro_y, gyro_z, gyro_angle;
	private LinearLayout layout;
	
	//ORIENTATION
	private boolean flat_flag = false;
	private boolean vert_flag = false;

	//STATE LIST
	private List<State> q_state = new ArrayList<State>();
	private List<Long> q_time = new ArrayList<Long>();
	private List<State> q_risk = new ArrayList<State>();
	
	//UI Buttons
	private Button b_start_log, b_end_log;
	private Button b_left, b_right;
	private Button b_accel, b_decel;
	private Button b_constant, b_stop;
	private TextView tv_event, tv_show_events, tv_gps, tv_acc;
	private ImageView iv_warn;
	
	//Dialog 
	DialogAct_nonSpanned log_dialog;
	AlertDialog alert_log;
	
	//Save to External Memory
	private static final String sensorlogpath = "sensor_data";
	private SaveExt save_ext;
	private String data_save = "";
	private String data_log = "";
	private String drive_log = "";
	private String curr_time;
	private boolean start_log = false;
	private boolean end_log = false;
	
	//Sensor Manager
	private SensorManager sensorMgr;
	private Sensor mAcc, mGyro, mMagnet;
	private float[] aData = new float[3];
	private float[] gData = new float[3];
	private float[] mData = new float[3];
	
	//Accelerometer
	//private int count = 0;
	//threshold for noise
	//private float noise_thres = (float) 0.09;
	//private float gyro_thres = (float) 0.01;
	//relative acceleration
	double rel_acc = 0;
	private boolean acc_check = false, dec_check = false;
	double max_acc = -10000000;
	double min_acc = 10000000;
	int count = 0;
	private double[] window = new double[10];
	
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
	double max_gyro = -10000000;
	double min_gyro = 10000000;
	private double[] window_gyro = new double[10];
	int count2 = 0;
	
	//THRESHOLDS
	//5000 = 5secs
	private final int constant_delay = 5000;
	//threshold for forward, backward acceleration (acc_x)
	//higher = less sensitive
	//lower = more sensitive
	private final double fwd_thres = 0.5;
	private final double back_thres = 0.4;
	//accelerationfast
	private final double acc_aggr = 10;
	//accelerationturnfast
	private final double acc_turn_aggr = 5;
	
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
	
	//print event
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
		
		tv_acc = (TextView) findViewById(R.id.accelerometer_text);
		iv_warn = (ImageView) findViewById(R.id.iv_warning);
		
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
		//TODO :
		
//		Bundle getdata = getIntent().getExtras();
//    	if(getdata.getFloatArray("Acc") != null) {
//    		cal_acc = getdata.getFloatArray("Acc");
//    		Log.d("ACC_X", cal_acc[0] + "");
//    		Log.d("ACC_Y", cal_acc[1] + "");
//    		Log.d("ACC_Z", cal_acc[2] + "");
//    	}
//    	if(getdata.getFloatArray("Gyro") != null) {
//    		cal_gyro = getdata.getFloatArray("Gyro");
//    	}
		
		Intent intent = getIntent();
		String orientation = intent.getStringExtra("orientation");
		if(orientation.equals("flat"))
			flat_flag = true;
		else if(orientation.equals("vert"))
			vert_flag = true;
		
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
				tag_save_log("LEFT", "GroundTruth");
			}
			else 
				cannotTag();
			
			break;
		case R.id.b_right:
			if(start_log == true && end_log == true) {
				tag_save_log("RIGHT", "GroundTruth");
			}
			else 
				cannotTag();
			
			break;
		case R.id.b_straight:
			if(start_log == true && end_log == true) {
				tag_save_log("STRAIGHT", "GroundTruth");
			}
			else
				cannotTag();

			break;
		case R.id.b_accel:
			if(start_log == true && end_log == true) {
				tag_save_log("ACCELERATION", "GroundTruth");
			}
			else
				cannotTag();

			break;
		case R.id.b_decel:
			if(start_log == true && end_log == true) {
				tag_save_log("DECELERATION", "GroundTruth");
			}
			else
				cannotTag();

			break;
		case R.id.b_constant:
			if(start_log == true && end_log == true) {
				tag_save_log("CONSTANT", "GroundTruth");	
			}
			else
				cannotTag();

			break;
		case R.id.b_stop:
			if(start_log == true && end_log == true) {
				tag_save_log("STOP", "GroundTruth");
			}
			else
				cannotTag();

			break;
		}
	}

	/**-----------------------------------------------------------------------------------------------------------**
	 * 	---------------------------------------| SENSOR FUNCTIONS |-----------------------------------------------*
	 *//*---------------------------------------------------------------------------------------------------------*/
	public void onSensorChanged(SensorEvent event) {
		
		long diff_const;
		String aggressive = "";
		long acc_window;
		
		switch(event.sensor.getType()) {
		
			case Sensor.TYPE_ACCELEROMETER:
				
				aData = event.values.clone();
				
				acc_window = System.currentTimeMillis();
				double fwd_acc = 0;
				float gravity = 0;
				
				//change depending on orientation
				if(flat_flag == true) {
					//get forward acceleration values - Y AXIS
					fwd_acc = aData[1];
					gravity = sensorMgr.GRAVITY_EARTH;
				}
				else if(vert_flag == true) {
					fwd_acc = aData[2];
					gravity = (float) (sensorMgr.GRAVITY_EARTH + 0.4);
				}

				double check_acceleration = Math.sqrt(aData[1]*aData[1] + aData[2]*aData[2]);
				
				tv_acc.setText("\nACCELEROMETER: \n\nx-axis: " + aData[0] + " (m/s^2) \ny-axis: " + aData[1] + " (m/s^2) \nz-axis: " + aData[2] + " (m/s^2) \n" +
						"resultant :" + check_acceleration +"\n\n");
				
				//relative acceleration
				rel_acc = Math.abs(fwd_acc - sensorMgr.GRAVITY_EARTH);

				//time duration for the event
				diff_const = System.currentTimeMillis() - EventState.getStartTs();
				
			/**-----------------------------------------------------------------------------------------------------------**
			 * 	---------------------------------------| STOP/CONSTANT/ACCEL/DECEL FUNCTIONS |-----------------------------*
			 *//*---------------------------------------------------------------------------------------------------------*/
				if(check_acceleration <= gravity) {
					//IF GPS SPEED = 0 >> STOP ----------------------------------------------------------------------------------------//
					if(gps_speed == 0.0) {
						if(EventState.checkTransit(State.STOP)) {
							processStateTime(System.currentTimeMillis() - EventState.getStartTs());
							processStateList(State.STOP, "STOP");
							EventState.setCurrent(State.STOP, System.currentTimeMillis());
							tv_event.setText(EventState.getState().toString());
						}
					} //ELSE IF GPS SPEED > 2 >> CONSTANT - (2mps = 7.2kmph) ---------------------------------------------------------//
					else if(gps_speed >= 2.0 && diff_const > 5000) {
						if(EventState.checkTransit(State.CONST)) {
							processStateTime(System.currentTimeMillis() - EventState.getStartTs());
							processStateList(State.CONST, "CONSTANT SPEED");
							EventState.setCurrent(State.CONST, System.currentTimeMillis());
							tv_event.setText(EventState.getState().toString());
						}
					}
				}
				else if(check_acceleration > gravity) {
					//FLAT ORIENTATION
					if(flat_flag == true) {
						//DECELERATION (FLAT)----------------------------------------------------------------------------------------//
						if(fwd_acc <= (back_thres*-1)) {
							if(EventState.checkTransit(State.DEC)) {
								processStateTime(System.currentTimeMillis() - EventState.getStartTs());
								processStateList(State.DEC, "DECELERATE");
								EventState.setCurrent(State.DEC, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString() + " " + rel_acc);
								
								dec_check = true;
								//acc_aggressive(rel_acc); 
							}
						} //ACCELERATION (FLAT) ----------------------------------------------------------------------------------------//
						else if(fwd_acc >= fwd_thres) {
							if(EventState.checkTransit(State.ACC)) {
								processStateTime(System.currentTimeMillis() - EventState.getStartTs());
								processStateList(State.ACC, "ACCELERATE");
								EventState.setCurrent(State.ACC, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString()  + " " + rel_acc);
								
								acc_check = true;
								acc_aggressive(rel_acc);
							}
						}
					} //VERTICAL ORIENTATION ----------------------------------------------------------------------------------------//
					else if(vert_flag == true) {
						//DECELERATION (VERT)----------------------------------------------------------------------------------------//
						if(fwd_acc >= fwd_thres) {
							if(EventState.checkTransit(State.DEC)) {
								processStateTime(System.currentTimeMillis() - EventState.getStartTs());
								processStateList(State.DEC, "DECELERATE");
								EventState.setCurrent(State.DEC, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString() + " " + rel_acc);

								dec_check = true;
								//acc_aggressive(rel_acc);
							}
						} 	//ACCELERATION (VERT)----------------------------------------------------------------------------------------// 
						else if(fwd_acc <= (back_thres*-1)) {
							if(EventState.checkTransit(State.ACC)) {
								processStateTime(System.currentTimeMillis() - EventState.getStartTs());
								processStateList(State.ACC, "ACCELERATE");
								EventState.setCurrent(State.ACC, System.currentTimeMillis());
								tv_event.setText(EventState.getState().toString() + " " + rel_acc);
								
								acc_check = true;
								acc_aggressive(rel_acc);
							}
						}
					}

					if(count < 10)
						window[count] = fwd_acc;
					
					count++; 
					
//					Toast.makeText(this, "count: " + count, Toast.LENGTH_SHORT).show();
					
					if(count == 10) {
						for(int i=0; i<10; i++) {
							if(window[i] > max_acc)
								max_acc = window[i];
							else if(window[i] < min_acc)
								min_acc = window[i];
								
//							Log.d("rapid", "test : " + (max_acc - min_acc));
//							Toast.makeText(this, "test : " + (max_acc - min_acc), Toast.LENGTH_SHORT).show();
							
							if((max_acc - min_acc) != 20000000 && (max_acc - min_acc) >= 3.0) {
//								Log.d("rapid", "test : " + (max_acc - min_acc));
//								Toast.makeText(this, "entered : " + (max_acc - min_acc), Toast.LENGTH_SHORT).show();
//								aggr_save_log("RAPID ACCELERATION/DECELERATION");
								
								processRiskyStateList(State.RAPID_ACC, "RAPID ACCELERATION/DECELERATION");
								
//								new show_warning().execute();
							}
						}
						count = 0;
					}
					max_acc = -10000000;
					min_acc = 10000000;
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
							if(EventState.getState() == State.ACC && rel_acc >= acc_turn_aggr) {
								//aggr_save_log("RISKY LEFT TURN");
								
								processRiskyStateList(State.R_LEFT, "RISKY LEFT TURN");
								
//								new show_warning().execute();
							}
							
							processStateTime(System.currentTimeMillis() - EventState.getStartTs());
							processStateList(State.LEFT, "LEFT");
							EventState.setDir(State.LEFT, System.currentTimeMillis());
							tv_event.setText(EventState.getDir().toString());
						}
					}
					else if((angle_z - prev_z) < -1.0) {
						if(EventState.checkDir(State.RIGHT)) {
							
							if(EventState.getState() == State.ACC && rel_acc >= acc_turn_aggr) {
								//aggr_save_log("RISKY RIGHT TURN");
								
								processRiskyStateList(State.R_RIGHT, "RISKY RIGHT TURN");
								
//								new show_warning().execute();
							}
							
							processStateTime(System.currentTimeMillis() - EventState.getStartTs());
							processStateList(State.RIGHT, "RIGHT");
							EventState.setDir(State.RIGHT, System.currentTimeMillis());
							tv_event.setText(EventState.getDir().toString());
						}
					}
					
					max = gData[2];
					prev_z = angle_z;
				} else {
					EventState.setDir_str(State.STRAIGHT);
				}
				
				if(count2 < 10)
					window_gyro[count2] = gData[2];
				
				count2++; 
				
//				Toast.makeText(this, "count: " + count2, Toast.LENGTH_SHORT).show();
				
				if(count2 == 10) {
					for(int i=0; i<10; i++) {
						if(window_gyro[i] > max_gyro)
							max_gyro = window_gyro[i];
						else if(window_gyro[i] < min_gyro)
							min_gyro = window_gyro[i];
							
//						Log.d("rapid", "test : " + (max_gyro - min_gyro));
//						Toast.makeText(this, "test : " + (max_gyro - min_gyro) + " , max : " + max_gyro + " ,min : " + min_gyro, Toast.LENGTH_SHORT).show();
						
						if(max_gyro!= -10000000 && min_gyro != 10000000 && 
								Math.abs(max_gyro - min_gyro) >= 3.0) {
//							Log.d("rapid", "test : " + (max_gyro - min_gyro));
//							Toast.makeText(this, "entered : " + (max_gyro - min_gyro), Toast.LENGTH_SHORT).show();
							//aggr_save_log("SWERVING");
							
							processRiskyStateList(State.SWERVE, "SWERVING");
							
//							new show_warning().execute();
						}
					}
					count2 = 0;
				}
				max_gyro = -10000000;
				min_gyro = 10000000;
				
				break;
				
			case Sensor.TYPE_MAGNETIC_FIELD:
				
				mData = event.values.clone();

				break;
				
		}
		
		tv_show_events.setText(event_string);
		
		acc_check = false;
		dec_check = false;
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
	
	//add state to state list
	private void processStateList(State state, String msg) {
		//IF STATELIST IS EMPTY -------------------------------------------------------------------------------------------------//
		if(q_state.isEmpty()) {
			q_state.add(state);
			event_string += "\n" + msg + ", curr_state : " + state.toString() + "\n";
			
			//log the event to the file
			if(start_log == true && end_log == true) {
				//save to SD
				data_save += time_stamp("time") + "\t" + msg;
			}
		} //IF STATELIST IS NOT EMPTY AND LAST ELEMENT IN LIST != CURRENT STATE (PREVENT DUPLICATES) ----------------------------//
		else if(!q_state.isEmpty() && q_state.get(q_state.size()-1) != state) {
			q_state.add(state);
			event_string += "\n" + msg + ", curr_state : " + state.toString() + "\n";
			
			//log the event to the file
			if(start_log == true && end_log == true) {
				//save to SD
				data_save += time_stamp("time") + "\t" + msg;
			}
		}
	}
	
	//add state to risky list
	private void processRiskyStateList(State state, String msg) {
		//IF STATELIST IS EMPTY -------------------------------------------------------------------------------------------------//
		if(q_risk.isEmpty()) {
			q_risk.add(state);
//			event_string += "\n" + msg + ", curr_state : " + state.toString() + "\n";
			
			aggr_save_log(msg);
			
		} //IF STATELIST IS NOT EMPTY AND LAST ELEMENT IN LIST != CURRENT STATE (PREVENT DUPLICATES) ----------------------------//
		else if(!q_risk.isEmpty() && q_risk.get(q_risk.size()-1) != state) {
			q_risk.add(state);
//			event_string += "\n" + msg + ", curr_state : " + state.toString() + "\n";
			
			aggr_save_log(msg);
		}
	}
	
	//ADD TIME FOR THE STATE
	private void processStateTime(long time) {
		
		//converting millisec to sec
		double convert = time/1000.0;
		String aggressive = "";
		
		//IF TIME LIST IS EMPTY, AND STATELIST IS 1------------------------------------------------------------------------------//
		if(q_time.isEmpty() && q_state.size() == 1) {
			q_time.add(time);
			if(convert< 0.5) {
				aggressive = " AGGRESSIVE";
				aggr_save_log(q_state.get(0) + "\t" + convert);
				
//				new show_warning().execute();
			}

			event_string += "\nTime : " + convert + aggressive + "\n";
			
			//log the event to the file
			if(start_log == true && end_log == true) {
				//save to SD
				data_save += "\t" + convert + "\n";
				save_ext.writeExt(curr_time , data_save, "Eventlog");
				
				data_save = "";
			}
		} //IF TIME LIST IS NOT EMPTY, AND LIST SIZE IS 1 LESSER THAN STATELIST-------------------------------------------------//
		else if(!q_time.isEmpty() && q_time.size() == q_state.size()-1) {
			q_time.add(time);
			if(convert< 0.5) {
				aggressive = " AGGRESSIVE";
				aggr_save_log(q_state.get(q_state.size()-1) + "\t" + convert);
				
//				new show_warning().execute();
			}
			
			event_string += "\nTime : " + convert + aggressive + "\n";
			
			//log the event to the file
			if(start_log == true && end_log == true) {
				//save to SD
				data_save += "\t" + convert + "\n";
				save_ext.writeExt(curr_time , data_save, "Eventlog");
				
				data_save = "";
			}
		}
	}

	//IF ACCELERATION IS AGGRESSIVE
	private void acc_aggressive(double rel_acc) {
		if(rel_acc >= acc_aggr) {
			aggr_save_log(EventState.getState().toString());
			
//			new show_warning().execute();
		}
	}
	
	//saving aggressive events
	private void aggr_save_log(String msg) {
		//log the event to the file
		if(start_log == true && end_log == true) {
			//save to SD
			drive_log += time_stamp("time") + "\t" + msg + "\n";
			save_ext.writeExt(curr_time , drive_log, "DrivingLog");
			
			drive_log = "";
		}
		
		new show_warning().execute();
	}
	
	//tag save log
	private void tag_save_log(String msg, String path) {
		data_log += time_stamp("time") + "\t" + msg + "\n";
		//save timestamp on start log
		save_ext.writeExt(curr_time , data_log, path);
		data_log = "";
	}
	
	//error when cannot tag
	private void cannotTag() {
		alert_log = log_dialog.dialog(this, "Error!", "Log has not started, cannot TAG.");
		alert_log.show();
	}
	
	//show warning logo
	private class show_warning extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			
			runOnUiThread(new Runnable() {
		        
		        public void run() {
		        	iv_warn.setVisibility(View.VISIBLE);
		        }
		      });
			
			try {
				Thread.sleep(2000);
			} catch(Exception e){};
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			iv_warn.setVisibility(View.INVISIBLE);
		}
	}
}
