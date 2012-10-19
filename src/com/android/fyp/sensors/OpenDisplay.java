package com.android.fyp.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * The primary class for this application
 * @author Stephan Williams
 *
 */
public class OpenDisplay extends Activity implements SensorEventListener {

//      These are the values used for calibration.
	private float dx = 0;
	private float dy = 0;
	private float dz = 0;

//      Keeps track of the recording start times so
//      the origin of the graph can be kept at t=0
	private long timeStart = 0;

//      holds the last sensor event, used for calibration
	SensorEvent lastEvent;

////      This is my subclass of CountDownTimer, which adds some convenience
////      methods for checking if the timer is finished
//	MyCountDownTimer timer = new MyCountDownTimer(0, 0);

	private float highAccel = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	//requestWindowFeature(Window.FEATURE_NO_TITLE);
	setContentView(R.layout.graph);

//      Sets up this class (which implements SensorEventListener) to recieve
//      sensor events, specifically from the accelerometer.
	SensorManager manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	Sensor accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);

//      Sets up the actions for the "Start Recording" button
//	((Button)findViewById(R.id.startTimer)).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
////                              When clicked, if the timer is not running (meaning the app is not recording),
////                              clear the graph, change the button text to "Stop Recording", and reset the 
////                              timer and highest acceleration values.
//				if (!timer.isRunning()) {
//					((GraphView)findViewById(R.id.graph)).clearPoints();
//					((Button)findViewById(R.id.startTimer)).setText(R.string.stopTimer);
//					timeStart = 0;
//					highAccel = 0;
//					timer = new MyCountDownTimer(Long.MAX_VALUE, 100);
//					timer.start();
//				} else {
//					((Button)findViewById(R.id.startTimer)).setText(R.string.startTimer);
//					timer.cancelTimer();
//				}
//			}
//		});
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onStop() {
		super.onStop();
//		((Button)findViewById(R.id.startTimer)).setText(R.string.startTimer);
//		timer.cancelTimer();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		lastEvent = event;
//		TextView xAccel = ((TextView)findViewById(R.id.x_accel));
//		TextView yAccel = ((TextView)findViewById(R.id.y_accel));
//		TextView zAccel = ((TextView)findViewById(R.id.z_accel));
//		TextView tAccel = ((TextView)findViewById(R.id.all_accel));
//		TextView hAccel = ((TextView)findViewById(R.id.high_accel));

//		xAccel.setText("X: " + (event.values[0] - dx));
//		yAccel.setText("Y: " + (event.values[1] - dy));
//		zAccel.setText("Z: " + (event.values[2] - dz));

		float totalAccel = FloatMath.sqrt((event.values[0] - dx) * (event.values[0] - dx) +
						  (event.values[1] - dy) * (event.values[1] - dy) +
						  (event.values[2] - dz) * (event.values[2] - dz)) - SensorManager.GRAVITY_EARTH;

//		tAccel.setText("T: " + totalAccel);
//		if (timer.isRunning()) {
//                      sets the start time of recording
			if (timeStart == 0) timeStart = event.timestamp;
			if (totalAccel > highAccel) highAccel = totalAccel;
//			hAccel.setText("H: " + highAccel);
//                      add the point to the graph, converting the nanoseconds from the timer to seconds
			((GraphView)findViewById(R.id.graph)).addPoint((float)((event.timestamp - timeStart) / 1.0E9), totalAccel);
//		} else if (totalAccel > 10) {
////                      for the "punch detector" feature, records high accelerations
////                      if not already recording.
//			((GraphView)findViewById(R.id.graph)).clearPoints();
//			timeStart = 0;
//			highAccel = 0;
//			timer = new MyCountDownTimer(1000, 100);
//			timer.start();
//		}
	}

//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.menu, menu);
//		return true;
//	}

//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
////              gets which menu item was pressed to calibrate,
////              uses the last recorded accelerometer value to zero
////              the display value
//		case (R.id.calX):
//			dx = lastEvent.values[0];
//			break;
//		case (R.id.calY):
//			dy = lastEvent.values[1];
//			break;
//		case (R.id.calZ):
//			dz = lastEvent.values[2];
//			break;
////              reset and view switching buttons
//		case (R.id.reset):
//			dx = dy = dz = 0;
//			break;
//		case (R.id.switchView):
//			((ViewFlipper)findViewById(R.id.details)).showNext();
//			break;
//                default:
//			break;
//		}
//		return super.onOptionsItemSelected(item);
//	}
}