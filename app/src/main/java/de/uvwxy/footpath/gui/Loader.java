package de.uvwxy.footpath.gui;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath.Rev;
import de.uvwxy.footpath.graph.Graph;
import de.uvwxy.footpath.graph.GraphNode;
import de.uvwxy.footpath.graph.LatLonPos;
import de.uvwxy.footpath.vista.MyActivity;

/**
 * 
 * @author Paul Smith
 *
 */
public class Loader extends Activity {

	public static final String LOADER_SETTINGS = "FootPathSettings";
	
	// GRAPH
	private static Graph g;					// Holds the data structure
	private String startingRoom;
	private String destinationRoom;

	private static String[] rooms = null;			// Array of all room names added to drop down lists
	private LocationManager locationManager;
	
	// GUI
	private static final int NUMBER_SELECT_ACTIVITY_RESULT_CODE = 0;
	private boolean startPressed = false;
	private TextView startTextView;

	// Navigator needs static access to graph
	public static Graph getGraph(){
		return g;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectroom);

		// Create new graph
		g = new Graph();
		// And add layer(s) of ways
		try {
			g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.fiterman));
			g.mergeNodes();
			rooms = g.getRoomList();
			Log.i("onCreate","Graph created!");
		} catch (NotFoundException e) {
			longToast("Error: resource not found:\n\n" + e);
		} catch (XmlPullParserException e) {
			longToast("Error: xml error:\n\n" + e);
		} catch (IOException e) {
			longToast("Error: io error:\n\n" + e);
		}
		this.setTitle("Footpath");
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	}

	public void selectStart(View view){
		startPressed = true;
		Intent numbersIntent = new Intent(Loader.this, NumberSelect.class);
		startActivityForResult(numbersIntent, NUMBER_SELECT_ACTIVITY_RESULT_CODE);
	}

	public void selectDestination(View view){
		startPressed = false;
		Intent numbersIntent = new Intent(Loader.this, NumberSelect.class);
		startActivityForResult(numbersIntent, NUMBER_SELECT_ACTIVITY_RESULT_CODE);
	}

	// This method is called when the second activity finishes
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// check that it is the SecondActivity with an OK result
		if (requestCode == NUMBER_SELECT_ACTIVITY_RESULT_CODE) {
			if (resultCode == RESULT_OK) {

				// get String data from Intent
				String returnedString = data.getStringExtra("sp");

				if (startPressed == true) {
					// set text view with string
					startingRoom = returnedString;
					startTextView = (TextView) findViewById(R.id.button_select_start);
					startTextView.setText(returnedString);
					startTextView.setTextSize(60);
					startTextView.setBackgroundColor(Color.parseColor("#3D5AFE"));
					Toast.makeText(Loader.this, "Your starting room number is "
							+ returnedString, Toast.LENGTH_LONG).show();
				} else {
					destinationRoom = returnedString;
					TextView destinationTextView = (TextView) findViewById(R.id.button_select_destination);
					destinationTextView.setText(returnedString);
					destinationTextView.setTextSize(60);
					destinationTextView.setBackgroundColor(Color.parseColor("#F44336"));
					Toast.makeText(Loader.this, "Your destination room number is "
							+ returnedString, Toast.LENGTH_LONG).show();
				}

			}
		}
	}

	public static String[] getRooms(){
		return rooms;
	}

	public String getStartingRoom(){
		return startingRoom;
	}

	public String getDestinationRoom(){
		return destinationRoom;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	
	@Override
	public void onPause() {
		super.onPause();
	}


	private void longToast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
	}

	public void startNavigation(View view){
		if(!startingRoom.equals(destinationRoom)) {
			Toast.makeText(Loader.this, "Starting Navigation", Toast.LENGTH_LONG).show();
			Log.i("FOOTPATH", "Starting navigation intent");
			// Create intent for navigation
			Intent intentNavigator = new Intent(Loader.this, Navigator.class);

			intentNavigator.putExtra("from", startingRoom);
			intentNavigator.putExtra("to", destinationRoom);
			// Source: http://www.pedometersaustralia.com/g/13868/measure-step-length-.html
			intentNavigator.putExtra("stepLength", Float.parseFloat(String.valueOf(170 * 0.415f)));
			// Start intent for navigation
			startActivityForResult(intentNavigator, 1);
		}else{
			Toast.makeText(Loader.this, "Your starting position cannot be the same as your destination. \nPlease reenter.", Toast.LENGTH_LONG).show();
		}
	}

}