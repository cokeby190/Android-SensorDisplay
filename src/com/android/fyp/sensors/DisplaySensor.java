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
public class DisplaySensor extends Activity implements OnClickListener {
	
	//Sensor Manager
	private SensorManager mSensorManager;
	private SensorEventListener mSensorListener;
	private List<Sensor> deviceSensors;
	private Sensor mAcc, mGyro, mMagnet, mLight, mProx, mTemp;
	
	//Caliberated values;
	private float[] cal_acc = new float[3];
	private float[] cal_gyro = new float[3];
	
	//UI Elements
	private TextView sensor_no;
	private TextView tv_acc, tv_gyro, tv_magnet, tv_light, tv_prox, tv_temp, tv_orientation;
	private Button b_start_log, b_end_log;
	private boolean start_log = false;
	private boolean end_log = false;
	private Button b_acc, b_gyro, b_magnet, b_light, b_prox, b_temp;
	private Button b_start_left, b_end_left, b_start_right, b_end_right;
	private Button b_display;
	
	//Sensors
	float[] aData = new float[3];
	float[] mData = new float[3];
	float[] gData = new float[3];
	
	float[] Rmat = new float[9];
	float[] OrientValues = new float[3];
	
	//Gyropscope
	// Create a constant to convert nanoseconds to seconds.
	private static final float NS2S = 1.0f / 1000000000.0f;
	private final float[] deltaRotationVector = new float[4];
	private float timestamp;
	public static final float EPSILON = 0.000000001f;
	private double angle_x, angle_y, angle_z;
	private final float rad2deg = (float) (180.0f / Math.PI);
	private int count = 0;
	private gyro_data[] window = new gyro_data[10];
	
	//Dialog 
	DialogAct show_dialog;
	AlertDialog alert;
	DialogAct_nonSpanned log_dialog;
	AlertDialog alert_log;
	
	//Save to External Mem
	SaveExt save_ext;
	String data_save = "";
	String data_log = "";
	String curr_time;
	String log_time;
	
	//Power Manager - to prevent the screen sleeping and stop collecting data
	PowerManager.WakeLock wl;
	
	//Location Manager - to detect location changes
	LocationManager locationManager;
	Location last_known;
	boolean GPSenabled;
	boolean NETenabled;
	
	//Graph
	private GraphView graphView;
	private GraphViewSeries acc_x, acc_y, acc_z;
	private GraphViewSeries gyro_x, gyro_y, gyro_z, gyro_angle;
	private GraphViewSeries mag_x, mag_y, mag_z;
	private GraphViewSeries orient_x, orient_y, orient_z;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);
        
        //Intent i = new Intent(this, AppPref_Act.class);
        //startActivity(i);
        
        //initialise UI elements
        initialize();
        
        getData();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
       
        //set wakelock to dim, i.e. screen will be dim, and CPU will still be running and not stop
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();

        //save data to SD card
        save_ext = new SaveExt(this);
        save_ext.setState();
        
        //Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        //Sensor Manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        //list of all sensors
        deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        
        //print out sensor types (int) in LOG
//        for (Sensor sensor : deviceSensors) {
//            Log.v("Sensors", "" + sensor.getType());
//        }
//        
        //Register Sensor Listener for all the sensors in the device. 
        for (Sensor sensor : deviceSensors) {
			//mSensorManager.registerListener(mSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        	mSensorManager.registerListener(mSensorListener, sensor, 40000);
		}
		
		sensor_no.setText("\n\nNumber of sensors detected: " + deviceSensors.size());
		
		// location
		GPSenabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		NETenabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		// Check if enabled and if not send user to the GSP settings
		// Better solution would be to display a dialog and suggesting to
		// go to the settings
		if (!GPSenabled && !NETenabled) {

//			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//			startActivity(intent);

			String location_dis = "This application requires either GPS or Wifi to be turned on. Please enable it in the Settings button.";
			
			CreateAlertDialog dialog = new CreateAlertDialog();
        	AlertDialog alert = dialog.newdialog(this, location_dis);
        	alert.show();
        	
		} else {
			// Register the listener with the Location Manager to receive
			// location updates
			//(String provider, long minTime, float minDistance, LocationListener listener)
			//locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			
			if(!GPSenabled && NETenabled) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
				Toast.makeText(getApplicationContext(), "GPS no NET yes", Toast.LENGTH_LONG).show();
			}
			
			else if (GPSenabled && !NETenabled) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			Toast.makeText(getApplicationContext(), "GPS yes NET no", Toast.LENGTH_LONG).show();
		}
			else {
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				String bestProvider = locationManager.getBestProvider(criteria, false);
				last_known = locationManager.getLastKnownLocation(bestProvider);
				locationManager.requestLocationUpdates(bestProvider, 0, 0, locationListener);
				
				Toast.makeText(getApplicationContext(), "GPS yes NET yes", Toast.LENGTH_LONG).show();
			}		
		}
		
        //Sensor Listener Object 
        mSensorListener = new SensorEventListener() {

			public void onAccuracyChanged(Sensor arg0, int arg1) {
				
			}

			/**
			 * Function called whenever there are changes in sensor data
			 */
			public void onSensorChanged(SensorEvent event) {
				
				float x,y,z;
				
				/**
				 * Switchcase to *do something* whenever the sensor of that type changes.
				 */
				switch(event.sensor.getType()) {
					case Sensor.TYPE_ACCELEROMETER:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						aData = event.values.clone();
						
						tv_acc.setText("\nACCELEROMETER: \n\nx-axis: " + x + " (m/s^2) \ny-axis: " + y + " (m/s^2) \nz-axis: " + z + " (m/s^2) \n\n");
						
						//Log.d("LOG", start_log + " " + end_log);
						if(start_log == true && end_log == true) {
							Log.d("LOG_ACC", start_log + " " + end_log);
							//save to SD
							data_save += time_stamp("time") + "\t" + "Accelerometer" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\n";
							//save_ext.writeExt(time_stamp("date") , data_save, "accelerometer");
							//save_ext.writeExt(curr_time , data_save, "accelerometer");
							save_ext.writeExt(curr_time , data_save, "acc");
							
							data_save = "";
						}

						//append data to graph
						acc_x.appendData(new GraphViewData(System.currentTimeMillis(), x), true);
						acc_y.appendData(new GraphViewData(System.currentTimeMillis(), y), true);
						acc_z.appendData(new GraphViewData(System.currentTimeMillis(), z), true);

						break;
					case Sensor.TYPE_GYROSCOPE:
						
						final float currentRotVector[] =  { 1, 0, 0, 0 };
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						gData = event.values.clone();
						
						if(count < 10)
							window[count] = new gyro_data(x,y,z);
						
						count++; 

						double max = -10000000;
						double dist = 8.0;
						long max_time = 0;
						
						if(count == 10) {
							for(int i=0; i<10; i++) {
								if(window[i].z > max)
									max = window[i].z;
								if(window[i].z < max - dist) {
									Log.d("PEAK", window[i].z + "");
									//Toast.makeText(getApplicationContext(), "PEAK", Toast.LENGTH_SHORT).show();
									max = window[i].z;
									max_time = System.currentTimeMillis();
								}
							}
							count = 0;
						}							
						
						// Axis of the rotation sample, not normalized yet.
//						float axisX = event.values[0];
//						float axisY = event.values[1];
//						float axisZ = event.values[2];
//						
//						double RotAngle = 0.00;
						
						// This timestep's delta rotation to be multiplied by the current rotation
						// after computing it from the gyro sample data.
						if (timestamp != 0) {
							
							final float dT = (event.timestamp - timestamp) * NS2S;
							
							angle_x += (x*dT) * rad2deg;
							angle_y += (y*dT) * rad2deg;
							angle_z += (z*dT) * rad2deg;
							
//							// Calculate the angular speed of the sample
//						    float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);
//
//						    // Normalize the rotation vector if it's big enough to get the axis
//						    // (that is, EPSILON should represent your maximum allowable margin of error)
//						    if (omegaMagnitude > EPSILON) {
//						    	axisX /= omegaMagnitude;
//						    	axisY /= omegaMagnitude;
//						    	axisZ /= omegaMagnitude;
//						    }
//						    
//						    // Integrate around this axis with the angular speed by the timestep
//						    // in order to get a delta rotation from this sample over the timestep
//						    // We will convert this axis-angle representation of the delta rotation
//						    // into a quaternion before turning it into the rotation matrix.
//						    float thetaOverTwo = omegaMagnitude * dT / 2.0f;
//						    float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
//						    float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
//						    deltaRotationVector[0] = sinThetaOverTwo * axisX;
//						    deltaRotationVector[1] = sinThetaOverTwo * axisY;
//						    deltaRotationVector[2] = sinThetaOverTwo * axisZ;
//						    deltaRotationVector[3] = cosThetaOverTwo;
//						    
//							currentRotVector[0] = deltaRotationVector[0]
//									* currentRotVector[0] - deltaRotationVector[1]
//									* currentRotVector[1] - deltaRotationVector[2]
//									* currentRotVector[2] - deltaRotationVector[3]
//									* currentRotVector[3];
//	
//							currentRotVector[1] = deltaRotationVector[0]
//									* currentRotVector[1] + deltaRotationVector[1]
//									* currentRotVector[0] + deltaRotationVector[2]
//									* currentRotVector[3] - deltaRotationVector[3]
//									* currentRotVector[2];
//	
//							currentRotVector[2] = deltaRotationVector[0]
//									* currentRotVector[2] - deltaRotationVector[1]
//									* currentRotVector[3] + deltaRotationVector[2]
//									* currentRotVector[0] + deltaRotationVector[3]
//									* currentRotVector[1];
//	
//							currentRotVector[3] = deltaRotationVector[0]
//									* currentRotVector[3] + deltaRotationVector[1]
//									* currentRotVector[2] - deltaRotationVector[2]
//									* currentRotVector[1] + deltaRotationVector[3]
//									* currentRotVector[0];
//							final float rad2deg = (float) (180.0f / Math.PI);
//							RotAngle = currentRotVector[0] * rad2deg;
//							axisX = currentRotVector[1];
//							axisY = currentRotVector[2];
//							axisZ = currentRotVector[3];
	
							// Log.d("GYROSCOPE_INITIAL", "X: " + x + //
							// " Y: " + y + //
							// " Z: " + z );
							// Log.d("GYROSCOPE", "axisX: " + axisX + //
							// " axisY: " + axisY + //
							// " axisZ: " + axisZ + //
							// " RotAngle: " + RotAngle);
						}
    
						timestamp = event.timestamp;
//						float[] deltaRotationMatrix = new float[9];
//						SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
						// User code should concatenate the delta rotation we computed with the current rotation
						// in order to get the updated rotation.
						// rotationCurrent = rotationCurrent * deltaRotationMatrix;
//						x = (x * deltaRotationMatrix[0]) + (x * deltaRotationMatrix[1]) + (x * deltaRotationMatrix[2]);
//						y = (y * deltaRotationMatrix[3]) + (y * deltaRotationMatrix[4]) + (y * deltaRotationMatrix[5]);
//						z = (z * deltaRotationMatrix[6]) + (z * deltaRotationMatrix[7]) + (z * deltaRotationMatrix[8]);

						//tv_gyro.setText("\nGYROSCOPE: \n\nx-axis: " + x + " (rad/s) \ny-axis: " + y + " (rad/s) \nz-axis: " + z + " (rad/s) \n\n" + "RotAngle : " + angle + "\n\n");
						tv_gyro.setText("\nGYROSCOPE: \n\nx-axis: " + angle_x + " (rad/s) \ny-axis: " + angle_y + " (rad/s) \nz-axis: " + angle_z + " (rad/s) \n\n");
						
						if(start_log == true && end_log == true) {
							//save to SD
							//data_save += time_stamp("time") + "\t" + "Gyroscope" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\t" + "angle," + angle + "\n";
							data_save += time_stamp("time") + "\t" + "Gyroscope" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\n";
							//save_ext.writeExt(time_stamp("date") , data_save, "gyroscope");
							//save_ext.writeExt(curr_time , data_save, "gyroscope");
							save_ext.writeExt(curr_time , data_save, "gyro");
							
							data_save = "";
						}
						
						//append data to graph
						gyro_x.appendData(new GraphViewData(System.currentTimeMillis(), x), true);
						gyro_y.appendData(new GraphViewData(System.currentTimeMillis(), y), true);
						gyro_z.appendData(new GraphViewData(System.currentTimeMillis(), z), true);
						//gyro_angle.appendData(new GraphViewData(System.currentTimeMillis(), angle), true);
						//gyro_line.appendData(new GraphViewData(max_time, max), true);
						
						break;
					case Sensor.TYPE_LIGHT:
						
						float light = event.values[0];
						
						tv_light.setText("\nLIGHT: \n\n" + light + " (lux) \n\n");
						
						break;
					case Sensor.TYPE_MAGNETIC_FIELD:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						mData = event.values.clone();
						
						tv_magnet.setText("\nMAGNETOMETER: \n\nx-axis: " + x + " (uT) \ny-axis: " + y + " (uT) \nz-axis: " + z + " (uT) \n\n");
						
						if(start_log == true && end_log == true) {
							//save to SD
							data_save += time_stamp("time") + "\t" + "Magnetometer" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\n";
							//save_ext.writeExt(time_stamp("date") , data_save, "magnetometer");
							//save_ext.writeExt(curr_time , data_save, "magnetometer");
							save_ext.writeExt(curr_time , data_save, "magnet");
							
							data_save = "";
						}
						
						//append data to graph
						mag_x.appendData(new GraphViewData(System.currentTimeMillis(), x), true);
						mag_y.appendData(new GraphViewData(System.currentTimeMillis(), y), true);
						mag_z.appendData(new GraphViewData(System.currentTimeMillis(), z), true);

						break;
					case Sensor.TYPE_PROXIMITY:
						
						float proximity = event.values[0];

						tv_prox.setText("\nPROXIMITY: \n\n" + proximity + " (cm) \n\n");
						
						break;
					case Sensor.TYPE_AMBIENT_TEMPERATURE:
						
						float temp = event.values[0];

						tv_temp.setText("\nTEMPERATURE: \n\n" + temp + " (deg Celsius) \n\n");
						
						break;
				}
				
				if(aData!=null && mData!=null) {
		        	//SensorManager.getRotationMatrix(Rmat, Imat, aData, mData);
		        	SensorManager.getRotationMatrix(Rmat, null, aData, mData);
		        	SensorManager.getOrientation(Rmat, OrientValues);
//		        	
//		        	Log.d("acc_x", aData[0] + "");
//		        	Log.d("acc_y", aData[0] + "");
//		        	Log.d("acc_z", aData[0] + "");
//		        	
//		        	Log.d("mag_x", mData[0] + "");
//		        	Log.d("mag_y", mData[0] + "");
//		        	Log.d("mag_z", mData[0] + "");
//		        	
//		        	Log.d("ORIENTATION", OrientValues[0] + "");
		        	
		        	tv_orientation.setText("\nOrientation: \n\nx-axis: " + OrientValues[0] + " (uT) \ny-axis: " + OrientValues[1] + " (uT) \nz-axis: " + OrientValues[2] + " (uT) \n\n");
		        	
		        	orient_x.appendData(new GraphViewData(System.currentTimeMillis(), OrientValues[0]), true);
					orient_y.appendData(new GraphViewData(System.currentTimeMillis(), OrientValues[1]), true);
					orient_z.appendData(new GraphViewData(System.currentTimeMillis(), OrientValues[2]), true);
		        }
				
				//------------------------------------- UNSUPPORTED SENSORS -------------------------------------//
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
					tv_acc.setText("\nACCELEROMETER: \n\n" + "Not available on device" + "\n\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
					tv_gyro.setText("\nGYROSCOPE: \n\n" + "Not available on device" + "\n\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
					tv_magnet.setText("\nMAGNETOMETER: \n\n" + "Not available on device" + "\n\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
					tv_light.setText("\nLIGHT: \n\n" + "Not available on device" + "\n\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) == null) {
					tv_prox.setText("\nPROXIMITY: \n\n" + "Not available on device" + "\n\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
					tv_temp.setText("\nTEMPERATURE: \n\n" + "Not available on device" + "\n\n");
				}
			
			//------------------------------------- UNSUPPORTED SENSORS -------------------------------------//
			}
        	
        };
        
//        Log.d("ACCELEROMETER", aData + "");
//        Log.d("Magnetometer", mData + "");
//        
//        if(aData!=null && mData!=null) {
//        	//SensorManager.getRotationMatrix(Rmat, Imat, aData, mData);
//        	SensorManager.getRotationMatrix(Rmat, null, aData, mData);
//        	SensorManager.getOrientation(Rmat, OrientValues);
//        	
//        	Log.d("acc_x", aData[0] + "");
//        	Log.d("acc_y", aData[0] + "");
//        	Log.d("acc_z", aData[0] + "");
//        	
//        	Log.d("mag_x", mData[0] + "");
//        	Log.d("mag_y", mData[0] + "");
//        	Log.d("mag_z", mData[0] + "");
//        	
//        	Log.d("ORIENTATION", OrientValues[0] + "");
//        	
////        	orient_x.appendData(new GraphViewData(System.currentTimeMillis(), OrientValues[0]), true);
////			orient_y.appendData(new GraphViewData(System.currentTimeMillis(), OrientValues[1]), true);
////			orient_z.appendData(new GraphViewData(System.currentTimeMillis(), OrientValues[2]), true);
//        }
        
        
        //------------------------------------- INITIALISE GRAPH -------------------------------------//
        
        /**------------------------------------------------------------------------------------------------------**
 		 * 	-------------------------------------| ACCELEROMETER GRAPH|------------------------------------------*
 		 *//*----------------------------------------------------------------------------------------------------*/		
        //Accelerometer graph
 		acc_x = new GraphViewSeries("acc_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3), new GraphViewData[] {});
 		
 		/**
 		 * Graph
 		 */
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Accelerometer Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(acc_x); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		LinearLayout layout = (LinearLayout) findViewById(R.id.acc_graph_x);
 		layout.addView(graphView);
 		
 		acc_y = new GraphViewSeries("acc_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3), new GraphViewData[] {});
 		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Accelerometer Data") {
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
 		
 		graphView.addSeries(acc_y); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.acc_graph_y);
 		layout.addView(graphView);
 		
 		acc_z = new GraphViewSeries("acc_z", null, new GraphViewData[] {});
 		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Accelerometer Data") {
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
 		
 		graphView.addSeries(acc_z); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.acc_graph_z);
 		layout.addView(graphView);
 		
 		
 		/**------------------------------------------------------------------------------------------------------**
 		 * 	-------------------------------------| ORIENTATION GRAPH|------------------------------------------*
 		 *//*----------------------------------------------------------------------------------------------------*/		
        //Accelerometer graph
 		orient_x = new GraphViewSeries("orient_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3), new GraphViewData[] {});
 		
 		/**
 		 * Graph
 		 */
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Orientation Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(orient_x); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.orient_graph_x);
 		layout.addView(graphView);
 		
 		orient_y = new GraphViewSeries("orient_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3), new GraphViewData[] {});
 		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Orientation Data") {
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
 		
 		graphView.addSeries(orient_y); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.orient_graph_y);
 		layout.addView(graphView);
 		
 		orient_z = new GraphViewSeries("orient_z", null, new GraphViewData[] {});
 		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Orientation Data") {
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
 		
 		graphView.addSeries(orient_z); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.orient_graph_z);
 		layout.addView(graphView);
 		
 		
 		
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
		
//		// LineGraphView( context, heading)
//		graphView = new LineGraphView(this, "Gyroscope Data") {
//			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
//
//			@Override
//			protected String formatLabel(double value, boolean isValueX) {
//				if (isValueX)
//					return formatter.format(value); // convert unix time to
//													// human time
//				else
//					return super.formatLabel(value, isValueX); // let the
//																// y-value be
//																// normal-formatted
//			}
//		};
//
//		graphView.addSeries(gyro_angle); // data
//		graphView.setScrollable(true);
//		graphView.setViewPort(1, 80000);
//		// graphView.setScalable(true);
//
//		layout = (LinearLayout) findViewById(R.id.gyro_graph_angle);
//		layout.addView(graphView);
			
 		/**------------------------------------------------------------------------------------------------------**
 		 * 	-------------------------------------| MAGNETOMETER GRAPH |-------------------------------------------*
 		 *//*----------------------------------------------------------------------------------------------------*/	
 		
 		//Magnetometer graph
 		mag_x = new GraphViewSeries("mag_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3),new GraphViewData[] {});
 		mag_y = new GraphViewSeries("mag_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3),new GraphViewData[] {});
 		mag_z = new GraphViewSeries("mag_z", null ,new GraphViewData[] {});
 		
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Magnetometer Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(mag_x); // data
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.mag_graph_x);
 		layout.addView(graphView);
 		
		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Magnetometer Data") {
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

		graphView.addSeries(mag_y); // data
		graphView.setScrollable(true);
		graphView.setViewPort(1, 80000);
		// graphView.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.mag_graph_y);
		layout.addView(graphView);

		// LineGraphView( context, heading)
		graphView = new LineGraphView(this, "Magnetometer Data") {
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

		graphView.addSeries(mag_z); // data
		graphView.setScrollable(true);
		graphView.setViewPort(1, 80000);
		// graphView.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.mag_graph_z);
		layout.addView(graphView);
 		
 		//------------------------------------- INITIALISE GRAPH -------------------------------------//
	}
    
    private void getData() {
    	
    	Bundle getdata = getIntent().getExtras();
    	if(getdata.getFloatArray("Acc") != null) {
    		cal_acc = getdata.getFloatArray("Acc");
    	}
    	if(getdata.getFloatArray("Gyro") != null) {
    		cal_gyro = getdata.getFloatArray("Gyro");
    	}
    	
    }
    
    //-------------------------------------------------------------------------------------------//
    //									 | LOCATION LISTENER |									 //	
    //-------------------------------------------------------------------------------------------//
    /**
	 * Define a listener that responds to location updates
	 */
	LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			
			//updating location
			last_known = location;
			
			if(start_log == true && end_log == true) {
				// save to SD
				data_save += time_stamp("time") + "\t" + "GPS" + "\t" + "latitude,"
						+ location.getLatitude() + "\t" + "longitude,"
						+ location.getLongitude() + "\t" + "Speed,"
						+ location.getSpeed() + "\n";
				//save_ext.writeExt(time_stamp("date"), data_save, "GPS");
				save_ext.writeExt(curr_time, data_save, "GPS");
	
				data_save = "";
			}

			Toast.makeText(getApplicationContext(), "LOCATION INFORMATION : " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}
	};

	/**
     * Initialise UI elements
     */
    private void initialize() {
    	
    	b_start_log = (Button) findViewById(R.id.b_start_log);
    	b_end_log = (Button) findViewById(R.id.b_end_log);
    	
    	sensor_no = (TextView) findViewById(R.id.sensor_no);
    	
    	tv_acc = (TextView) findViewById(R.id.accelerometer_text);
    	tv_gyro = (TextView) findViewById(R.id.gyroscope_text);
    	tv_magnet = (TextView) findViewById(R.id.magnetometer_text);
    	tv_light = (TextView) findViewById(R.id.light_text);
    	tv_prox = (TextView) findViewById(R.id.proximity_text);
    	tv_temp = (TextView) findViewById(R.id.temp_text);
    	
    	tv_orientation = (TextView) findViewById(R.id.orientation_text);
    	
    	b_acc = (Button) findViewById(R.id.accelerometer_button);
    	b_gyro = (Button) findViewById(R.id.gyroscope_button);
    	b_magnet = (Button) findViewById(R.id.magnetometer_button);
    	b_light = (Button) findViewById(R.id.light_button);
    	b_prox = (Button) findViewById(R.id.proximity_button);
    	b_temp = (Button) findViewById(R.id.temp_button);
    	
    	b_start_left = (Button) findViewById(R.id.b_start_left);
    	b_end_left = (Button) findViewById(R.id.b_end_left);
    	b_start_right = (Button) findViewById(R.id.b_start_right);
    	b_end_right = (Button) findViewById(R.id.b_end_right);
    	
    	//b_display = (Button) findViewById(R.id.b_display);
    	
    	//b_display.setOnClickListener(this);
    	
    	b_start_log.setOnClickListener(this);
    	b_end_log.setOnClickListener(this);
		
    	b_acc.setOnClickListener(this);
    	b_gyro.setOnClickListener(this);
    	b_magnet.setOnClickListener(this);
    	b_light.setOnClickListener(this);
    	b_prox.setOnClickListener(this);
    	b_temp.setOnClickListener(this);
    }
    
    /**
     * Activity OnResume
     */
    @Override
	protected void onResume() { 
		super.onResume();
		
		//register listener again onResume
		for (Sensor sensor : deviceSensors) {
			
			mSensorManager.registerListener(mSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		sensor_no.setText("\n\nNumber of sensors detected: " + deviceSensors.size());
		
		//wakelock
		wl.acquire(); 
		
		//get location
		//locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		
		if (!GPSenabled && !NETenabled) {

//			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//			startActivity(intent);

			String location_dis = "This application requires either GPS or Wifi to be turned on. Please enable it in the Settings button.";
			
			CreateAlertDialog dialog = new CreateAlertDialog();
        	AlertDialog alert = dialog.newdialog(this, location_dis);
        	alert.show();
        	
		} else {
			// Register the listener with the Location Manager to receive
			// location updates
			//(String provider, long minTime, float minDistance, LocationListener listener)
			//locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			
			if(!GPSenabled && NETenabled) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
				Toast.makeText(getApplicationContext(), "GPS no NET yes", Toast.LENGTH_LONG).show();
			}
			
			else if (GPSenabled && !NETenabled) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			Toast.makeText(getApplicationContext(), "GPS yes NET no", Toast.LENGTH_LONG).show();
		}
			else {
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				String bestProvider = locationManager.getBestProvider(criteria, false);
				last_known = locationManager.getLastKnownLocation(bestProvider);
				locationManager.requestLocationUpdates(bestProvider, 0, 0, locationListener);
				
				Toast.makeText(getApplicationContext(), "GPS yes NET yes", Toast.LENGTH_LONG).show();
			}		
		}
	}

    /**
     * Activity OnPause
     */
	@Override
	protected void onPause() {
		super.onPause();
		
		//unregister listener again since User is not using them
		for (Sensor sensor : deviceSensors) {
			mSensorManager.unregisterListener(mSensorListener, sensor);
		}
		
		//release wakelock
		wl.release();
		
		//stop listening for location
		locationManager.removeUpdates(locationListener);
	}

	/**
	 * Activity OnStop
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		//stop listening for location
		locationManager.removeUpdates(locationListener);
	}

	/**
	 * OnClick Listener
	 */
	public void onClick(View v) {
		
		show_dialog = new DialogAct();
		log_dialog = new DialogAct_nonSpanned();

		//Show the dialog when the user clicks the button
		switch(v.getId()) {
//			case R.id.b_display:
//				//Intent display = new Intent(SensorDisplayActivity.this, DisplaySensor.class);
//				Intent display = new Intent(SensorDisplayActivity.this, displayGraph.class);
//				display.putExtra("type", "line");
//				startActivity(display);
//				break;
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
				
			case R.id.b_start_left:
				data_log += time_stamp("time") + "\t" + "current time" + "\t" + "\n";
				//save timestamp on start log
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH-mm");
				log_time = sdf.format(new Date());
				save_ext.writeExt(log_time , data_log, "log_time");
				break;
			case R.id.b_end_left:
				break;
			case R.id.accelerometer_button:
				show_data(mAcc, Sensor.TYPE_ACCELEROMETER, "Accelerometer");
				break;
			case R.id.gyroscope_button:							
				show_data(mGyro, Sensor.TYPE_GYROSCOPE, "Gyroscope");
				break;
			case R.id.light_button:
				show_data(mLight, Sensor.TYPE_LIGHT, "Light");
				break;
			case R.id.magnetometer_button:
				show_data(mMagnet, Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer");
				break;
			case R.id.proximity_button:
				show_data(mProx, Sensor.TYPE_PROXIMITY, "Proximity");
				break;
			case R.id.temp_button:
				show_data(mTemp, Sensor.TYPE_AMBIENT_TEMPERATURE, "Temperature");
				break;
		
		}
	}
	
	/**
	 * Function to display sensor information on Android UI in the form of a dialog
	 * @param sensor		Sensor datatype to initialise particular sensor
	 * @param sensor_type	type of sensor, TYPE.Sensor_xx
	 * @param type_str		String to print out TYPE of sensor
	 */
	private void show_data(Sensor sensor, int sensor_type, String type_str) {
		if (mSensorManager.getDefaultSensor(sensor_type) != null) {
			
			sensor = mSensorManager.getDefaultSensor(sensor_type);
			
			Spanned sensor_data = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + sensor.getName()+ "</small><br/><br/>"
										+ "<big>Type: </big><br/>" + "<small>" + type_str + "</small><br/><br/>"
										+ "<big>Vendor: </big><br/>" + "<small>" + sensor.getVendor() + "</small><br/><br/>"
										+ "<big>Version: </big><br/>" + "<small>" + sensor.getVersion() + "</small><br/><br/>"
										+ "<big>Power: </big><br/>" + "<small>" + sensor.getPower() + " mA</small><br/>");
			
			alert = show_dialog.dialog(this, type_str + " Information", sensor_data);
		} else {
			alert = show_dialog.dialog(this, type_str + " Information", Html.fromHtml("Sensor Unsupported by Device"));
		}
		
		alert.show();
	}	

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
         
        //System.out.println(formatter.format(calendar.getTime()));
        
        return formatter.format(calendar.getTime());
	}
	
//	private float [] genRotMatrix() {
//		float[] rotMatrix = null;
//		
//		if(accValues != null && magValues != null) {
//			rotMatrix = new float[16];
//			boolean rotMatGen;
//			rotMatGen = SensorManager.getRotationMatrix(rotMatrix,null, accValues, magValues);
//			
//			if(!rotMatGen) {
//				Log.d("ERROR", "Rotation Matrix Generation Failed");
//				rotMatrix = null;
//			}
//		}
//		return rotMatrix;
//	}
}