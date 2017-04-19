package de.uvwxy.footpath.core;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import de.uvwxy.footpath.ToolBox;

/**
 * This class is fed with data from the Accelerometer and Compass sensors. If a step is detected on the acc
 * data it calls the trigger function on its interface StepTrigger, with the given direction.
 *
 * Usage:
 * Create an object: stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);
 * @author Paul Smith
 *
 */
public class StepDetection {
	public final long INTERVAL_MS = 1000/30;

	// Hold an interface to notify the outside world of detected steps
	private StepTrigger st;
	// Context needed to get access to sensor service
	private Context context;

	private static SensorManager sm;					// Holds references to the SensorManager
	List<Sensor> lSensor;								// List of all sensors

//	private double compassValue = -1.0;					// Last compass value

	private static final int vhSize = 6;
	private double[] values_history = new double[vhSize];
	private int vhPointer = 0;

	private double a;
	private double peak;
	private int step_timeout_ms;
	private long last_step_ts = 0;
//	private double old_z = 0.0;

	// last acc is low pass filtered
	private double[] lastAcc = new double[] {0.0, 0.0, 0.0};
	// last comp is untouched
	private double[] lastComp = new double[] {0.0, 0.0, 0.0};

	private int round = 0;

	private Sensor accelerometer;
	private Sensor magnetometer;
	private Float azimut;
	/**
	 * Handles sensor events. Updates the sensor
	 */
	public SensorEventListener mySensorEventListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		float[] mGravity;
		float[] mGeomagnetic;

		@Override
		public void onSensorChanged(SensorEvent event) {
			if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
				mGravity = event.values;
				st.dataHookAcc(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]);
				lastAcc[0] = ToolBox.lowpassFilter(lastAcc[0], event.values[0], a);
				lastAcc[1] = ToolBox.lowpassFilter(lastAcc[1], event.values[1], a);
				lastAcc[2] = ToolBox.lowpassFilter(lastAcc[2], event.values[2], a);
			}

			if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
				mGeomagnetic = event.values;
			}
			if(mGravity != null && mGeomagnetic != null){
				float R[] = new float[9];
				float I[] = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
				if(success){
					float orientation[] = new float[3];
					SensorManager.getOrientation(R, orientation);
					azimut = orientation[0];
					float azimutD = (float)Math.toDegrees(azimut);
					if(azimutD < 0.0f){
						azimutD += 360.0f;
					}
					st.dataHookComp(System.currentTimeMillis(), azimutD, orientation[1], orientation[2]);
					lastComp[0] = azimutD;
					lastComp[1] = orientation[1];
					lastComp[2] = orientation[2];
					//Log.i("onSensorChanged: ", Float.toString(azimutD));
				}
			}
		}
	};

	public double getA() {
		return a;
	}

	public double getPeak() {
		return peak;
	}

	public int getStep_timeout_ms() {
		return step_timeout_ms;
	}

	public void setA(double a) {
		this.a = a;
	}

	public void setPeak(double peak) {
		this.peak = peak;
	}

	public void setStep_timeout_ms(int stepTimeoutMs) {
		step_timeout_ms = stepTimeoutMs;
	}

	public StepDetection(Context context, StepTrigger st, double a, double peak, int step_timeout_ms){
		this.context = context;
		this.st = st;
		this.a = a;
		this.peak = peak;
		this.step_timeout_ms = step_timeout_ms;
		Log.i("StepDetection", "yep");
	}

	/**
	 * Enable step detection
	 */
	public void load(){
		// Sensors
		sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		//lSensor = sm.getSensorList(Sensor.TYPE_ALL);

//		for (int i = 0; i < lSensor.size(); i++) {
//			// Register only compass and accelerometer
//			if (lSensor.get(i).getType() == Sensor.TYPE_ACCELEROMETER
//					|| lSensor.get(i).getType() == Sensor.TYPE_ORIENTATION) {
// 				sm.registerListener(mySensorEventListener, lSensor.get(i), SensorManager.SENSOR_DELAY_FASTEST);
//			}
//		}

		//updates
		accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		sm.registerListener(mySensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		sm.registerListener(mySensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
		// Register timer
		timer = new Timer("UpdateData", false);
		TimerTask task = new TimerTask(){

			@Override
			public void run() {
				updateData();
			}
		};
		timer.schedule(task, 0, INTERVAL_MS);
	}

	/**
	 * Disable step detection
	 */
	public void unload(){
		Log.i("unload", "yep");
		timer.cancel();
		timer.purge();
		timer = null;
		sm.unregisterListener(mySensorEventListener);
	}

	/**
	 * This is called every INTERVAL_MS ms from the TimerTask.
	 */
	private void updateData(){
		// Get current time for time stamps
		long now_ms = System.currentTimeMillis();

		// Create local value for compass and old_z, such that it is consistent during logs
		// (It might change in between, which is circumvented by this)

		// array.clone() does not work here!!
		// this does not work as well!!
//		double[] oldAcc = {lastAcc[0],lastAcc[1],lastAcc[2]};
//		double[] oldComp = {lastComp[0],lastComp[1],lastComp[2]};

		double[] oldAcc = new double[3];
		System.arraycopy(lastAcc, 0, oldAcc, 0, 3);
		double[] oldComp = new double[3];
		System.arraycopy(lastComp, 0, oldComp, 0, 3);
		double lCompass = oldComp[0];
		double lOld_z = oldAcc[2];
		st.timedDataHook(now_ms, oldAcc, oldComp);

		addData(lOld_z);

		// Check if a step is detected upon data
		if((now_ms - last_step_ts) > step_timeout_ms && checkForStep(peak)){
			// Set latest detected step to "now"
			last_step_ts = now_ms;
			// Call algorithm for navigation/updating position
			st.trigger(now_ms, lCompass);
			Log.i("FOOTPATH", "Detected step  in  round = " + round  + " @ "+ now_ms);

		}
		round++;
	}

	private void addData(double value){
		values_history[vhPointer % vhSize] = value;
		vhPointer++;
		vhPointer = vhPointer % vhSize;
	}

	private boolean checkForStep(double peakSize) {
		// Add value to values_history

		int lookahead = 5;
		double diff = peakSize;

		for( int t = 1; t <= lookahead; t++){
			if((values_history[(vhPointer - 1 - t + vhSize + vhSize) % vhSize] -
					values_history[(vhPointer - 1 + vhSize) % vhSize]
					> diff)){
				Log.i("FOOTPATH", "Detected step with t = " + t + ", diff = " + diff + " < " + (values_history[(vhPointer - 1 - t + vhSize + vhSize) % vhSize] -
						values_history[(vhPointer - 1 + vhSize) % vhSize]));
				return true;
			}
		}
		return false;
	}

	Timer timer;
}