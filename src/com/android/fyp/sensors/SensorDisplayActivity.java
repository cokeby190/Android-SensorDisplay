package com.android.fyp.sensors;

import android.app.Activity;
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
	private float [] aData = new float[3];
	private float [] gData = new float[3];
	private boolean acc = false, gyro = false;
	
	//UI Elements
	private Button b_caliberate, b_caliberate2, b_caliberate3, 
		b_caliberate4, b_caliberate5;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Sensor Manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        //Register Sensor Listener for all the sensors in the device. 
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        
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
        
        //Sensor Listener Object 
        mSensorListener = new SensorEventListener() {

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}

			public void onSensorChanged(SensorEvent event) {
				
				switch(event.sensor.getType()) {
					case Sensor.TYPE_ACCELEROMETER:
						aData = event.values.clone();
						if(event.values[0] != 0.0 && event.values[1] != 0.0 && event.values[2] != 0.0)
							acc = true;
						break;
					case Sensor.TYPE_GYROSCOPE:
						gData = event.values.clone();
						if(event.values[0] != 0.0 && event.values[1] != 0.0 && event.values[2] != 0.0)
							gyro = true;
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
    }

    @Override
    protected void onPause() {
    	// unregister listener
    	super.onPause();
    	mSensorManager.unregisterListener(mSensorListener, mAccelerometer);
    	mSensorManager.unregisterListener(mSensorListener, mGyroscope);
    }
    
	/**
	 * OnClick Listener
	 */
	public void onClick(View v) {

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
		}
	}
}