package de.uvwxy.footpath.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.xmlpull.v1.XmlPullParserException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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
import android.widget.ToggleButton;

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
	
	// GUI
	private static final int ROOM_SELECT_ACTIVITY_RESULT_CODE = 0;
	private boolean startPressed = false;
	private TextView startTextView;

	//Vista
	public BluetoothManager mBluetoothManager;
	public BluetoothAdapter mBluetoothAdapter;
	final int REQUEST_BLUETOOTH_ENABLE = 0;
	final int REQUEST_COARSE_PERMISSIONS = 0;
	final int SCAN_PERIOD = 10000;
	final int READ_PERIOD = 100;
	boolean isBluetoothSupported = false;
	boolean isBluetoothEnabled = false;
	boolean isCoarseLocationEnabled = false;
	boolean version21 = false;
	boolean version23 = false;
	public Handler mHandler;
	public boolean mScanning;

	public BluetoothLeScanner mBluetoothLeScanner;
	public ScanCallback mScanCallBack;

	public BluetoothAdapter.LeScanCallback mLowEnergyScanCallBack;

	public BluetoothGattCallback mBluetoothGattCallback;
	public ArrayList<BluetoothDevice> mDevices;
	public ArrayList<BluetoothGatt> mGatts;

	public BluetoothGattService sensorService;
	public UUID sensorServiceUUID;
	public BluetoothGattCharacteristic filteredSensorCharacteristic;
	public UUID filteredSensorCharacteristicUUID;

	public Runnable runnable;

	// Navigator needs static access to graph
	public static Graph getGraph(){
		return g;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectroom);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectroom);

		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		mHandler = new Handler();
		mDevices = new ArrayList<BluetoothDevice>();
		mGatts = new ArrayList<BluetoothGatt>();

		version21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
		version23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

		isBluetoothSupported = checkIfBluetoothIsSupported();
		isBluetoothEnabled = checkIfBluetoothIsEnabled();
		enableBluetooth();

		if (version23) {
			getLocationPermission();
		}

		if (version21) {
			initBluetoothLECallback21();
		}
		else {
			initBluetoothLECallback18();
		}

		initBluetoothGatt();

		sensorServiceUUID = UUID.fromString("4505A8D9-4D7F-4A83-9F01-2E1C8FE0DF03");
		filteredSensorCharacteristicUUID = UUID.fromString("1E2E7F91-BA4D-4FF9-9914-FF0624386E68");
		runnable = null;

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
	}

	public void selectStart(View view){
		startPressed = true;
		Intent numbersIntent = new Intent(Loader.this, RoomSelectActivity.class);
		startActivityForResult(numbersIntent, ROOM_SELECT_ACTIVITY_RESULT_CODE);
	}

	public void selectDestination(View view){
		startPressed = false;
		Intent numbersIntent = new Intent(Loader.this, RoomSelectActivity.class);
		startActivityForResult(numbersIntent, ROOM_SELECT_ACTIVITY_RESULT_CODE);
	}

	// This method is called when the second activity finishes
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.i("onActivityResult", "here");
		Log.i("requestCode = ", Integer.toString(requestCode));
		Log.i("resultCode = ", Integer.toString(resultCode));
		// check that it is the SecondActivity with an OK result
		if (requestCode == ROOM_SELECT_ACTIVITY_RESULT_CODE) {
			if (resultCode == RESULT_OK) {

				// get String data from Intent
				String returnedString = data.getStringExtra("sp");

				if (startPressed == true) {
					// set text view with string
					startingRoom = returnedString;
					startTextView = (TextView) findViewById(R.id.button_select_start);
					startTextView.setText(returnedString);
					startTextView.setTextSize(48);
					Toast.makeText(Loader.this, "Your starting room number is "
							+ returnedString, Toast.LENGTH_LONG).show();
				} else {
					destinationRoom = returnedString;
					TextView destinationTextView = (TextView) findViewById(R.id.button_select_destination);
					destinationTextView.setText(returnedString);
					destinationTextView.setTextSize(48);
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
		if(startingRoom != null
				&& destinationRoom != null && !startingRoom.equals(destinationRoom)) {
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
			if(startingRoom == null || destinationRoom == null)
				Toast.makeText(Loader.this, "Please make sure to select your starting position and destination before starting your trip.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(Loader.this, "Your starting position cannot be the same as your destination. \nPlease reenter.", Toast.LENGTH_SHORT).show();

		}
	}

	public void scanLowEnergyDevice(View view) {
        if (!mScanning) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;

                    if (version21) { stopScan21(); }
                    else { stopScan18(); }

                    ((ToggleButton) findViewById(R.id.toggleScan)).setChecked(false);
                }

            }, SCAN_PERIOD);

            mScanning = true;
            if (version21) { startScan21(); }
            else { startScan18(); }
            ((ToggleButton) findViewById(R.id.toggleScan)).setChecked(true);

        } else {

            mScanning = false;
            if (version21) { stopScan21(); }
            else { stopScan18(); }
            ((ToggleButton) findViewById(R.id.toggleScan)).setChecked(false);

        }
    }

    public void startScan18() {
        mBluetoothAdapter.startLeScan(mLowEnergyScanCallBack);
    }

    public void stopScan18() {
        mBluetoothAdapter.stopLeScan(mLowEnergyScanCallBack);
    }

    @TargetApi(21)
    public void startScan21() {
        mBluetoothLeScanner.startScan(mScanCallBack);
    }

    @TargetApi(21)
    public void stopScan21() {
        mBluetoothLeScanner.stopScan(mScanCallBack);
    }



    @TargetApi(23)
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_PERMISSIONS);
        } else {
			/*
            ((TextView) findViewById(R.id.requestcoarse)).setText("Coarse Location Permission Initially Granted!");
            isCoarseLocationEnabled = true;
            */
        }
    }

    private void initBluetoothLECallback18() {
        mLowEnergyScanCallBack = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                boolean notNull = device.getName() != null;
                if (notNull) {
                    boolean vista = device.getName().equals("Vista Demo");
                    boolean empty = mDevices.isEmpty() && mGatts.isEmpty();

                    if (vista && empty) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDevices.add(device);
                                mGatts.add(device.connectGatt(new Loader(), false, mBluetoothGattCallback));
								/*
                                ((TextView) findViewById(R.id.devicefound)).setText(
                                        "Device Found - " + device.getName());
                                */
                            }
                        });
                    }
                }
            }
        };

    }

    @TargetApi(21)
    private void initBluetoothLECallback21() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                super.onScanResult(callbackType, result);

                boolean notNull = result.getDevice().getName() != null;
                if (notNull) {
                    boolean vista = result.getDevice().getName().equals("Vista Demo");
                    boolean empty = mDevices.isEmpty() && mGatts.isEmpty();

                    if (vista && empty) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDevices.add(result.getDevice());
                                mGatts.add(
                                        result.getDevice().connectGatt(
                                                new Loader(), false, mBluetoothGattCallback));
								/*
                                ((TextView) findViewById(R.id.devicefound)).setText(
                                        "Device Found - " + result.getDevice().getName());
                                */
                            }
                        });
                    }
                }
            }


            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
    }

    public void initBluetoothGatt() {
        mBluetoothGattCallback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
								/*
                                ((TextView) findViewById(R.id.connectgattserver)).setText(
                                        "Connected to Gatt Server");*/
                                gatt.discoverServices();
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
								/*
                                ((TextView) findViewById(R.id.connectgattserver)).setText(
                                        "Disconnected from Gatt Server");
                                */
                            }
                        }
                    });

            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                super.onServicesDiscovered(gatt, status);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
							/*
                            ((TextView) findViewById(R.id.services)).setText(
                                    "Services Discovered");
							*/
                            sensorService = gatt.getService(sensorServiceUUID);
                            filteredSensorCharacteristic =
                                    sensorService.getCharacteristic(filteredSensorCharacteristicUUID);
                            runnable = new Runnable() {
                                @Override
                                public void run() {
                                    gatt.readCharacteristic(filteredSensorCharacteristic);
                                    readBLECharacteristic();
                                }
                            };
                            readBLECharacteristic();

                        } else {
							/*
                            ((TextView) findViewById(R.id.services)).setText(
                                    "No Services Discovered");
                            */
                        }
                    }
                });

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             final BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //((TextView) findViewById(R.id.reading)).setText("Read Sensor Characteristic");
                        int data = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                        ((TextView) findViewById(R.id.data)).setText(String.valueOf(data));
                    }
                });



            }
        };
    }

    private boolean checkIfBluetoothIsSupported() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //((TextView) findViewById(R.id.blesupport)).setText("Bluetooth Low Energy Supported");
            return true;
        } else {
            //((TextView) findViewById(R.id.blesupport)).setText("Bluetooth Low Energy Not Supported");
            return false;
        }
    }


    private boolean checkIfBluetoothIsEnabled() {
        if (this.mBluetoothAdapter == null) {
            //((TextView) findViewById(R.id.blebfenable)).setText("Bluetooth Adapter was initially null");
            return false;
        } else if (!this.mBluetoothAdapter.isEnabled()) {
            //((TextView) findViewById(R.id.blebfenable)).setText("Bluetooth was initially disabled");
            return false;
        } else {
            //((TextView) findViewById(R.id.blebfenable)).setText("Bluetooth was initially enabled");
            //((TextView) findViewById(R.id.bleafenable)).setText("Bluetooth was initially enabled");
            return true;
        }
    }

    private void enableBluetooth() {
        if (!isBluetoothEnabled) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_BLUETOOTH_ENABLE);
        }
    }
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode == RESULT_OK) {
                ((TextView) findViewById(R.id.bleafenable)).setText("Bluetooth is now enabled");
                isBluetoothEnabled = true;

            } else if (resultCode == RESULT_CANCELED) {
                ((TextView) findViewById(R.id.bleafenable)).setText("Bluetooth is still disabled");
                isBluetoothEnabled = false;
            }
        }

        ((Button) findViewById(R.id.toggleScan)).setEnabled(
                    isBluetoothSupported && isBluetoothEnabled && isCoarseLocationEnabled);

    }
*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
/*
        if (requestCode == REQUEST_COARSE_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ((TextView) findViewById(R.id.requestcoarse)).setText("Coarse Location Permission Granted!");
                isCoarseLocationEnabled = true;
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                ((TextView) findViewById(R.id.requestcoarse)).setText("Coarse Location Permission Denied!");
                isCoarseLocationEnabled = false;
            }
        }

        ((Button) findViewById(R.id.toggleScan)).setEnabled(
                isBluetoothSupported && isBluetoothEnabled && isCoarseLocationEnabled);
*/
    }


    private void readBLECharacteristic() {
        mHandler.postDelayed(runnable, READ_PERIOD);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        while (mGatts.size() > 0) {
            mGatts.remove( mGatts.size() - 1 ).close();
        }
        mGatts.clear();
        mDevices.clear();
        if (runnable != null) {
            mHandler.removeCallbacks(runnable);
        }
    }

}