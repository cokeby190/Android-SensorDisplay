package com.android.fyp.sensors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;

/**
 * The Activity Displays Sensor Data to the User, including showing a dialog with additional 
 * hardware information about the sensors when clicked. 
 * @author Cheryl
 *
 */
public class SensorDisplayActivity extends Activity implements OnClickListener {
	
	private boolean face_up, face_flat;
	
	//Dialog 
	DialogAct_nonSpanned log_dialog;
	AlertDialog alert_log;

	//Sensor Manager
	private SensorManager mSensorManager;
	private SensorEventListener mSensorListener;
	private Sensor mAccelerometer, mGyroscope, mMagnet;
	private float [] aData = new float[3];
	private float [] gData = new float[3];
	private float [] mData = new float[3];
	private boolean acc = false, gyro = false, magnet = false;
	
	//UI Elements
	private Button b_caliberate, b_caliberate2, b_caliberate3, 
		b_caliberate4, b_caliberate5;
	private ImageButton b_orient_flat, b_orient_vert;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Sensor Manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        //Register Sensor Listener for all the sensors in the device. 
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
        
        b_caliberate = (Button) findViewById(R.id.b_caliberate);
        b_caliberate.setOnClickListener(this);
        b_caliberate2 = (Button) findViewById(R.id.b_caliberate2);
        b_caliberate2.setOnClickListener(this);
        b_caliberate3 = (Button) findViewById(R.id.b_caliberate3);
        b_caliberate3.setOnClickListener(this);
        b_caliberate4 = (Button) findViewById(R.id.b_caliberate4);
        b_caliberate4.setOnClickListener(this);
        b_caliberate5 = (Button) findViewById(R.id.b_caliberate5);
        b_caliberate5.setOnClickListener(this);
        
        b_orient_flat = (ImageButton) findViewById(R.id.ib_flat);
        b_orient_flat.setOnClickListener(this);
        b_orient_vert = (ImageButton) findViewById(R.id.ib_vert);
        b_orient_vert.setOnClickListener(this);
        
        //Sensor Listener Object 
        mSensorListener = new SensorEventListener() {

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}

			public void onSensorChanged(SensorEvent event) {
				
				float[] rotationMatrix;
				
				switch(event.sensor.getType()) {
					case Sensor.TYPE_ACCELEROMETER:
						aData = event.values.clone();
						
						rotationMatrix = generateRotationMatrix();
						
						if (rotationMatrix != null)
						{
							determineOrientation(rotationMatrix);
						}
						
						if(event.values[0] != 0.0 && event.values[1] != 0.0 && event.values[2] != 0.0)
							acc = true;
						break;
					case Sensor.TYPE_GYROSCOPE:
						gData = event.values.clone();
						if(event.values[0] != 0.0 && event.values[1] != 0.0 && event.values[2] != 0.0)
							gyro = true;
						break;
					case Sensor.TYPE_MAGNETIC_FIELD:
						mData = event.values.clone();
						
						rotationMatrix = generateRotationMatrix();
						
						if (rotationMatrix != null)
						{
							determineOrientation(rotationMatrix);
						}
						
						if(event.values[0] != 0.0 && event.values[1] != 0.0 && event.values[2] != 0.0)
							magnet = true;
						break;
						
				}
			}
        };
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	// register this class as a listener for the orientation and
    	// accelerometer sensors
    	mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    	mSensorManager.registerListener(mSensorListener, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
    	// unregister listener
    	super.onPause();
    	mSensorManager.unregisterListener(mSensorListener, mAccelerometer);
    	mSensorManager.unregisterListener(mSensorListener, mGyroscope);
    	mSensorManager.unregisterListener(mSensorListener, mMagnet);
    }
    
	/**
	 * OnClick Listener
	 */
	public void onClick(View v) {
		
		log_dialog = new DialogAct_nonSpanned();

		//Show the dialog when the user clicks the button
		switch(v.getId()) {

			case R.id.b_caliberate:
				
				Intent cal_data = new Intent(this, DisplaySensor.class);
				Bundle send_data = new Bundle();
				if(acc == true)
					//cal_data.putExtra("Acc", aData);
					send_data.putFloatArray("Acc", aData);
				if(gyro == true)
					//cal_data.putExtra("Gyro", gData);
					send_data.putFloatArray("Gyro", gData);
				cal_data.putExtras(send_data);
				startActivity(cal_data);
				break;

			case R.id.b_caliberate2:
				
				Intent cal_data2 = new Intent(this, SensorDetection.class);
				Bundle send_data2 = new Bundle();
				if(acc == true)
					//cal_data.putExtra("Acc", aData);
					send_data2.putFloatArray("Acc", aData);
				if(gyro == true)
					//cal_data.putExtra("Gyro", gData);
					send_data2.putFloatArray("Gyro", gData);
				cal_data2.putExtras(send_data2);
				startActivity(cal_data2);
				break;
				
			case R.id.b_caliberate3:
				
				Intent cal_data3 = new Intent(this, SensorLinear.class);
				Bundle send_data3 = new Bundle();
				if(acc == true)
					//cal_data.putExtra("Acc", aData);
					send_data3.putFloatArray("Acc", aData);
				if(gyro == true)
					//cal_data.putExtra("Gyro", gData);
					send_data3.putFloatArray("Gyro", gData);
				cal_data3.putExtras(send_data3);
				startActivity(cal_data3);
				break;
				
			case R.id.b_caliberate4:
				
				Intent cal_data4 = new Intent(this, SensorTest.class);
				Bundle send_data4 = new Bundle();
				if(acc == true)
					//cal_data.putExtra("Acc", aData);
					send_data4.putFloatArray("Acc", aData);
				if(gyro == true)
					//cal_data.putExtra("Gyro", gData);
					send_data4.putFloatArray("Gyro", gData);
				cal_data4.putExtras(send_data4);
				startActivity(cal_data4);
				break;
				
			case R.id.b_caliberate5:
				
				Intent cal_data5 = new Intent(this, SensorConstStop.class);
				Bundle send_data5 = new Bundle();
				if(acc == true)
					//cal_data.putExtra("Acc", aData);
					send_data5.putFloatArray("Acc", aData);
				if(gyro == true)
					//cal_data.putExtra("Gyro", gData);
					send_data5.putFloatArray("Gyro", gData);
				cal_data5.putExtras(send_data5);
				startActivity(cal_data5);
				break;
			
			case R.id.ib_flat:
				
				Intent cal_data6 = new Intent(this, SensorConstStop.class);
				cal_data6.putExtra("orientation", "flat");
				startActivity(cal_data6);
			
				break;
				
			case R.id.ib_vert:
	
				Intent cal_data7 = new Intent(this, SensorConstStop.class);
				cal_data7.putExtra("orientation", "vert");
				startActivity(cal_data7);
				
				break;
		}
	}
	
	//determine orientation so as to prevent Acc & Dec
	private void determineOrientation(float[] rotationMatrix) {
		
		float[] orientationValues = new float[3];
		SensorManager.getOrientation(rotationMatrix, orientationValues);
		
		double azimuth = Math.toDegrees(orientationValues[0]);
		double pitch = Math.toDegrees(orientationValues[1]);
		double roll = Math.toDegrees(orientationValues[2]);
		
		if (pitch <= 10) {
			
			if (Math.abs(roll) >= 130 && Math.abs(roll) <= 180) {
				//Log.d("Tilt", "Tilt down" + roll);
				face_up = true;
				face_flat = false;
			} else if (Math.abs(roll) <= 1) {
				face_flat = true;
				face_up = false;
				//Log.d("Face", "face_up : " + Math.abs(roll));
			} else {
				face_up = false;
				face_flat = false;
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
		
	//error orientation
	private void ErrorOrientation() {
		alert_log = log_dialog.dialog(this, "Error!", "Please choose the other orientation.");
		alert_log.show();
	}
}