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
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
	private List<Sensor> deviceSensors;
	private Sensor mAcc, mGyro, mMagnet, mLight, mProx, mTemp;
	
	//UI Elements
	private TextView sensor_no;
	private TextView tv_acc, tv_gyro, tv_magnet, tv_light, tv_prox, tv_temp;
	private Button b_acc, b_gyro, b_magnet, b_light, b_prox, b_temp;
	private Button b_display;
	
	//Dialog 
	DialogAct show_dialog;
	AlertDialog alert;
	
	//Save to External Mem
	SaveExt save_ext;
	String data_save = "";
	
	//Power Manager - to prevent the screen sleeping and stop collecting data
	PowerManager.WakeLock wl;
	
	//Location Manager - to detect location changes
	LocationManager locationManager;
	
	//Graph
	private GraphView graphView;
	private GraphViewSeries acc_x, acc_y, acc_z;
	private GraphViewSeries gyro_x, gyro_y, gyro_z;
	private GraphViewSeries mag_x, mag_y, mag_z;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Intent i = new Intent(this, AppPref_Act.class);
        //startActivity(i);
        
        //initialise UI elements
        initialize();
        
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
        for (Sensor sensor : deviceSensors) {
            Log.v("Sensors", "" + sensor.getType());
        }
        
        //Register Sensor Listener for all the sensors in the device. 
        for (Sensor sensor : deviceSensors) {
			
			mSensorManager.registerListener(mSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		sensor_no.setText("\n\nNumber of sensors detected: " + deviceSensors.size());

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
						
						tv_acc.setText("\nACCELEROMETER: \n\nx-axis: " + x + " (m/s^2) \ny-axis: " + y + " (m/s^2) \nz-axis: " + z + " (m/s^2) \n\n");
						
						//save to SD
						data_save += time_stamp("time") + "\t" + "Accelerometer" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\n";
						save_ext.writeExt(time_stamp("date") , data_save, "accelerometer");
						
						data_save = "";

						//append data to graph
						acc_x.appendData(new GraphViewData(System.currentTimeMillis(), x), true);
//						acc_y.appendData(new GraphViewData(System.currentTimeMillis(), y), true);
//						acc_z.appendData(new GraphViewData(System.currentTimeMillis(), z), true);
						
						break;
					case Sensor.TYPE_GYROSCOPE:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						tv_gyro.setText("\nGYROSCOPE: \n\nx-axis: " + x + " (rad/s) \ny-axis: " + y + " (rad/s) \nz-axis: " + z + " (rad/s) \n\n");
						
						//save to SD
						data_save += time_stamp("time") + "\t" + "Gyroscope" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\n";
						save_ext.writeExt(time_stamp("date") , data_save, "gyroscope");
						
						data_save = "";
						
						//append data to graph
						gyro_x.appendData(new GraphViewData(System.currentTimeMillis(), x), true);
//						gyro_y.appendData(new GraphViewData(System.currentTimeMillis(), y), true);
//						gyro_z.appendData(new GraphViewData(System.currentTimeMillis(), z), true);
						
						break;
					case Sensor.TYPE_LIGHT:
						
						float light = event.values[0];
						
						tv_light.setText("\nLIGHT: \n\n" + light + " (lux) \n\n");
						
						break;
					case Sensor.TYPE_MAGNETIC_FIELD:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						tv_magnet.setText("\nMAGNETOMETER: \n\nx-axis: " + x + " (uT) \ny-axis: " + y + " (uT) \nz-axis: " + z + " (uT) \n\n");
						
						//save to SD
						data_save += time_stamp("time") + "\t" + "Magnetometer" + "\t" + "x," + x + "\t" + "y," + y + "\t" + "z," + z + "\n";
						save_ext.writeExt(time_stamp("date") , data_save, "magnetometer");
						
						data_save = "";
						
						//append data to graph
						mag_x.appendData(new GraphViewData(System.currentTimeMillis(), x), true);
						
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
        
        //Accelerometer graph
 		acc_x = new GraphViewSeries("acc_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3), new GraphViewData[] {});
 		acc_y = new GraphViewSeries("acc_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3), new GraphViewData[] {});
 		acc_z = new GraphViewSeries("acc_z", null, new GraphViewData[] {});
 		
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Accelerometer Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(acc_x); // data
 		//graphView.addSeries(acc_y);
 		//graphView.addSeries(acc_z);
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		LinearLayout layout = (LinearLayout) findViewById(R.id.acc_graph);
 		layout.addView(graphView);
 		
 		//Gyroscope graph
 		gyro_x = new GraphViewSeries("gyro_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3),new GraphViewData[] {});
 		gyro_y = new GraphViewSeries("gyro_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3),new GraphViewData[] {});
 		gyro_z = new GraphViewSeries("gyro_z", null ,new GraphViewData[] {});
 		
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Gyroscope Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(gyro_x); // data
 		//graphView.addSeries(gyro_y);
 		//graphView.addSeries(gyro_z);
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.gyro_graph);
 		layout.addView(graphView);
			
 		//Magnetometer graph
 		mag_x = new GraphViewSeries("mag_x", new GraphViewStyle(Color.rgb(200, 50, 00), 3),new GraphViewData[] {});
 		mag_y = new GraphViewSeries("mag_y", new GraphViewStyle(Color.rgb(90, 250, 00), 3),new GraphViewData[] {});
 		mag_z = new GraphViewSeries("mag_z", null ,new GraphViewData[] {});
 		
 		// LineGraphView( context, heading)
 		graphView = new LineGraphView(this, "Magnetometer Data") {
 			SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
 			@Override
 			protected String formatLabel(double value, boolean isValueX) {
 				if (isValueX)
 					return formatter.format(value); 	// convert unix time to human time
 				else 
					return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
 			}
 		};

 		graphView.addSeries(mag_x); // data
 		//graphView.addSeries(gyro_y);
 		//graphView.addSeries(gyro_z);
 		graphView.setScrollable(true);
 		graphView.setViewPort(1, 80000);
		//graphView.setScalable(true);

 		layout = (LinearLayout) findViewById(R.id.mag_graph);
 		layout.addView(graphView);
			
 		
	}

	/**
     * Initialise UI elements
     */
    private void initialize() {
    	
    	sensor_no = (TextView) findViewById(R.id.sensor_no);
    	
    	tv_acc = (TextView) findViewById(R.id.accelerometer_text);
    	tv_gyro = (TextView) findViewById(R.id.gyroscope_text);
    	tv_magnet = (TextView) findViewById(R.id.magnetometer_text);
    	tv_light = (TextView) findViewById(R.id.light_text);
    	tv_prox = (TextView) findViewById(R.id.proximity_text);
    	tv_temp = (TextView) findViewById(R.id.temp_text);
    	
    	b_acc = (Button) findViewById(R.id.accelerometer_button);
    	b_gyro = (Button) findViewById(R.id.gyroscope_button);
    	b_magnet = (Button) findViewById(R.id.magnetometer_button);
    	b_light = (Button) findViewById(R.id.light_button);
    	b_prox = (Button) findViewById(R.id.proximity_button);
    	b_temp = (Button) findViewById(R.id.temp_button);
    	
    	b_display = (Button) findViewById(R.id.b_display);
    	
    	b_display.setOnClickListener(this);
    	
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
	}

	/**
	 * OnClick Listener
	 */
	public void onClick(View v) {
		
		show_dialog = new DialogAct();
		
		//Show the dialog when the user clicks the button
		switch(v.getId()) {
			case R.id.b_display:
				//Intent display = new Intent(SensorDisplayActivity.this, DisplaySensor.class);
				Intent display = new Intent(SensorDisplayActivity.this, displayGraph.class);
				display.putExtra("type", "line");
				startActivity(display);
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
	 * @return	String of converted timestamp from Millis (date)
	 */
	protected String time_stamp(String option) {
		// Create a DateFormatter object for displaying date information.
		
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy hh:mm:ss.SSS");
		
		if(option == "date")
			formatter = new SimpleDateFormat("dd-MM-yy");
		else if(option == "time")
			formatter = new SimpleDateFormat("hh:mm:ss.SSS");

        // Get date and time information in milliseconds
        long now = System.currentTimeMillis();

        // Create a calendar object that will convert the date and time value
        // in milliseconds to date. We use the setTimeInMillis() method of the
        // Calendar object.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
         
        System.out.println(formatter.format(calendar.getTime()));
        
        return formatter.format(calendar.getTime());
	}
	
//	/**
//	 * Convert time in Millis to dateformat specified by SimpleDateFormat (time)
//	 * @return	String of converted timestamp from Millis (time)
//	 */
//	protected String time_stamp_time() {
//		// Create a DateFormatter object for displaying date information.
//        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss.SSS");
//
//        // Get date and time information in milliseconds
//        long now = System.currentTimeMillis();
//
//        // Create a calendar object that will convert the date and time value
//        // in milliseconds to date. We use the setTimeInMillis() method of the
//        // Calendar object.
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(now);
//         
//        System.out.println(formatter.format(calendar.getTime()));
//        
//        return formatter.format(calendar.getTime());
//	}
}