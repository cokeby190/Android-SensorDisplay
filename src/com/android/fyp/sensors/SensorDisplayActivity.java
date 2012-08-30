package com.android.fyp.sensors;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        initialize();
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        //list of all sensors
        deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        
        /**
         * print out sensor types (int) in LOG
         */
        for (Sensor sensor : deviceSensors) {
            Log.v("Sensors", "" + sensor.getType());
        }
        
        mSensorListener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				
				float x,y,z;
				
				switch(event.sensor.getType()) {
					case Sensor.TYPE_ACCELEROMETER:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						tv_acc.setText("\n\nACCELEROMETER: \n\nx-axis: " + x + " (m/s^2) \ny-axis: " + y + " (m/s^2) \nz-axis: " + z + " (m/s^2) \n");
						
						break;
					case Sensor.TYPE_GYROSCOPE:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						tv_gyro.setText("\n\nGYROSCOPE: \n\nx-axis: " + x + " (rad/s) \ny-axis: " + y + " (rad/s) \nz-axis: " + z + " (rad/s) \n");
						
						break;
					case Sensor.TYPE_LIGHT:
						
						float light = event.values[0];
						
						tv_light.setText("\n\nLIGHT: \n\n" + light + " (lux) \n");
						
						break;
					case Sensor.TYPE_MAGNETIC_FIELD:
						
						x = event.values[0];
						y = event.values[1];
						z = event.values[2];
						
						tv_magnet.setText("\n\nMAGNETOMETER: \n\nx-axis: " + x + " (uT) \ny-axis: " + y + " (uT) \nz-axis: " + z + " (uT) \n");
						
						break;
					case Sensor.TYPE_PROXIMITY:
						
						float proximity = event.values[0];

						tv_prox.setText("\n\nPROXIMITY: \n\n" + proximity + " (cm) \n\n");
						
						break;
					case Sensor.TYPE_TEMPERATURE:
						
						float temp = event.values[0];

						tv_temp.setText("\n\nTEMPERATURE: \n\n" + temp + " (deg Celsius) \n\n");
						
						break;
				}
				
				//------------------------------------- UNSUPPORTED SENSORS -------------------------------------//
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
					tv_acc.setText("\n\nACCELEROMETER: \n\n" + "Not available on device" + "\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
					tv_gyro.setText("\n\nGYROSCOPE: \n\n" + "Not available on device" + "\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
					tv_magnet.setText("\n\nMAGNETOMETER: \n\n" + "Not available on device" + "\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
					tv_light.setText("\n\nLIGHT: \n\n" + "Not available on device" + "\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) == null) {
					tv_prox.setText("\n\nPROXIMITY: \n\n" + "Not available on device" + "\n");
				}
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE) == null) {
					tv_temp.setText("\n\nTEMPERATURE: \n\n" + "Not available on device" + "\n");
				}
			
			//------------------------------------- UNSUPPORTED SENSORS -------------------------------------//
			
			}
        	
        };
    }
    
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
    	
    	b_acc.setOnClickListener(this);
    	b_gyro.setOnClickListener(this);
    	b_magnet.setOnClickListener(this);
    	b_light.setOnClickListener(this);
    	b_prox.setOnClickListener(this);
    	b_temp.setOnClickListener(this);
    	
    }
    
    @Override
	protected void onResume() { 
		super.onResume();
		
		for (Sensor sensor : deviceSensors) {
			
			mSensorManager.registerListener(mSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		sensor_no.setText("\n\nNumber of sensors detected: " + deviceSensors.size());
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		for (Sensor sensor : deviceSensors) {
			mSensorManager.unregisterListener(mSensorListener, sensor);
		}
	}

	@Override
	public void onClick(View v) {
		
		DialogAct show_dialog = new DialogAct();
		AlertDialog alert;
		
		switch(v.getId()) {
			case R.id.accelerometer_button:
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

					mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
					
					Spanned accelerometer = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + mAcc.getName() + "</small><br/><br/>"
												+ "<big>Type: </big><br/>" + "<small>" + "Accelerometer" + "</small><br/><br/>"
												+ "<big>Vendor: </big><br/>" + "<small>" + mAcc.getVendor() + "</small><br/><br/>"
												+ "<big>Version: </big><br/>" + "<small>" + mAcc.getVersion() + "</small><br/><br/>"
												+ "<big>Power: </big><br/>" + "<small>" + mAcc.getPower() + " mA</small><br/>");
					
					alert = show_dialog.dialog(this, "Accelerometer Information", accelerometer);
				} else {
					alert = show_dialog.dialog(this, "Accelerometer Information", Html.fromHtml("Sensor Unsupported by Device"));
				}

				alert.show();
				
				break;
			case R.id.gyroscope_button:							
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
					
					mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
					
					Spanned gyroscope = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + mGyro.getName()+ "</small><br/><br/>"
												+ "<big>Type: </big><br/>" + "<small>" + "Gyroscope" + "</small><br/><br/>"
												+ "<big>Vendor: </big><br/>" + "<small>" + mGyro.getVendor() + "</small><br/><br/>"
												+ "<big>Version: </big><br/>" + "<small>" + mGyro.getVersion() + "</small><br/><br/>"
												+ "<big>Power: </big><br/>" + "<small>" + mGyro.getPower() + " mA</small><br/>");
					
					alert = show_dialog.dialog(this, "Gyroscope Information", gyroscope);
				} else {
					alert = show_dialog.dialog(this, "Gyroscope Information", Html.fromHtml("Sensor Unsupported by Device"));
				}	

				alert.show();
				
				break;
			case R.id.light_button:
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
					
					mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
					
					Spanned light = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + mLight.getName()+ "</small><br/><br/>"
												+ "<big>Type: </big><br/>" + "<small>" + "Light" + "</small><br/><br/>"
												+ "<big>Vendor: </big><br/>" + "<small>" + mLight.getVendor() + "</small><br/><br/>"
												+ "<big>Version: </big><br/>" + "<small>" + mLight.getVersion() + "</small><br/><br/>"
												+ "<big>Power: </big><br/>" + "<small>" + mLight.getPower() + " mA</small><br/>");
					
					alert = show_dialog.dialog(this, "Light Information", light);
				} else {
					alert = show_dialog.dialog(this, "Light Information", Html.fromHtml("Sensor Unsupported by Device"));
				}
				
				alert.show();
				
				break;
			case R.id.magnetometer_button:
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
					
					mMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
					
					Spanned magnetometer = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + mMagnet.getName()+ "</small><br/><br/>"
												+ "<big>Type: </big><br/>" + "<small>" + "Magnetometer" + "</small><br/><br/>"
												+ "<big>Vendor: </big><br/>" + "<small>" + mMagnet.getVendor() + "</small><br/><br/>"
												+ "<big>Version: </big><br/>" + "<small>" + mMagnet.getVersion() + "</small><br/><br/>"
												+ "<big>Power: </big><br/>" + "<small>" + mMagnet.getPower() + " mA</small><br/>");
					
					alert = show_dialog.dialog(this, "Magnetometer Information", magnetometer);
				} else {
					alert = show_dialog.dialog(this, "Proximity Information", Html.fromHtml("Sensor Unsupported by Device"));
				}
				
				alert.show();
				break;
			case R.id.proximity_button:
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
					
					mProx = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
					
					Spanned proximity = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + mProx.getName()+ "</small><br/><br/>"
												+ "<big>Type: </big><br/>" + "<small>" + "Proximity" + "</small><br/><br/>"
												+ "<big>Vendor: </big><br/>" + "<small>" + mProx.getVendor() + "</small><br/><br/>"
												+ "<big>Version: </big><br/>" + "<small>" + mProx.getVersion() + "</small><br/><br/>"
												+ "<big>Power: </big><br/>" + "<small>" + mProx.getPower() + " mA</small><br/>");
					
					alert = show_dialog.dialog(this, "Proximity Information", proximity);
				} else {
					alert = show_dialog.dialog(this, "Proximity Information", Html.fromHtml("Sensor Unsupported by Device"));
				}
				
				alert.show();
				break;
			case R.id.temp_button:
				
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE) != null) {
					
					mTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
					
					Spanned temp = Html.fromHtml("<big>Name: </big><br/>" + "<small>" + mTemp.getName()+ "</small><br/><br/>"
												+ "<big>Type: </big><br/>" + "<small>" + "Temperature" + "</small><br/><br/>"
												+ "<big>Vendor: </big><br/>" + "<small>" + mTemp.getVendor() + "</small><br/><br/>"
												+ "<big>Version: </big><br/>" + "<small>" + mTemp.getVersion() + "</small><br/><br/>"
												+ "<big>Power: </big><br/>" + "<small>" + mTemp.getPower() + " mA</small><br/>");
					
					alert = show_dialog.dialog(this, "Temperature Information", temp);
				} else {
					alert = show_dialog.dialog(this, "Temperature Information", Html.fromHtml("Sensor Unsupported by Device"));
				}
				
				alert.show();
				
				break;
		
		}
	}
	
}