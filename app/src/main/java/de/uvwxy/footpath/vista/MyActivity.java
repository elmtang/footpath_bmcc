package de.uvwxy.footpath.vista;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

import de.uvwxy.footpath.R;


public class MyActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    private BluetoothGattCharacteristic enableCh;
    private BluetoothGattCharacteristic rawCh;
    private BluetoothGattCharacteristic filteredCh;
    private BluetoothGattCharacteristic clampCh;

    private ArrayList<BluetoothGatt> btgatts = new ArrayList<BluetoothGatt>();
    private ArrayList<BluetoothDevice> btdevices = new ArrayList<BluetoothDevice>();

    private BluetoothGattCharacteristic startCh;
    private BluetoothGattCharacteristic effectCh;
    private BluetoothGattCharacteristic timerCh;

    Queue<BluetoothGattCharacteristic> qe=new LinkedList<BluetoothGattCharacteristic>();
    String TAG = "report: ";

    File file;
    FileOutputStream fo;

    int smallest = 400;
    int largest = 400;

    public MyActivity(){

    }

    public void processCharacteristic(BluetoothGattCharacteristic c) {
        if(qe.isEmpty()) {
            qe.add(c);
            btgatts.get(0).writeCharacteristic(c);
        } else {
            qe.add(c);
        }
    }

    public void processQ() {
        Iterator it = qe.iterator();

        if(it.hasNext()) {
            BluetoothGattCharacteristic c = (BluetoothGattCharacteristic) it.next();
            btgatts.get(0).writeCharacteristic(c);
        }
    }


    //TextView sensorView  = (TextView) findViewById(R.id.sensor_reading);
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothGatt gatt = device.connectGatt(MyActivity.this, false, mGattCallback);
                        btdevices.add(device);
                        btgatts.add(gatt);
                        Toast.makeText(MyActivity.this, "device: " + device.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };



    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("gattCallback", "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i("gattCallback", "Attempting to start service discovery:" +
                        gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("gattCallback", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("gattCallback", "servicesDiscovered");

                BluetoothGattService svc = gatt.getService(VistaGattAttributes.UUID_SENSOR_SERVICE);
                if(svc == null)
                {
                    Log.d("onSD", "Sensor Service not found!");
                    return;
                }
                filteredCh = svc.getCharacteristic(VistaGattAttributes.UUID_FILTERED_SENSOR_READING_CHARACTERISTIC);

                if(filteredCh == null){
                    Log.d("onSD", "filtered reading characteristic not found!");
                    return;
                }

                gatt.readCharacteristic(filteredCh);

                //Enable Notification for filteredCh
                gatt.setCharacteristicNotification(filteredCh, true);
                UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                BluetoothGattDescriptor descriptor = filteredCh.getDescriptor(uuid);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);


                //Register for "Mode" (I am not sure what exactly this Mode does yet")
                //svc = gatt.getService(VistaGattAttributes.UUID_VISTA_SERVICE);
                //BluetoothGattCharacteristic mode = svc.getCharacteristic(VistaGattAttributes.UUID_MODE_CHARACTERISTIC);
                //mode.setValue(2,BluetoothGattCharacteristic.FORMAT_UINT8,0);
                //gatt.writeCharacteristic(mode);

                Log.w(TAG, "wroteCharacteristc");

            } else {
                Log.w("gattCallback", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("gattCallback", "onCharacteristicRead");

                int char_int_value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                Log.i("onCharacteristicRead", "Value: " + Integer.toString(char_int_value));
                }
            }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //Log.w("gattCallback", "onCharacteristicChanged");
            int char_int_value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);

                String reading = Integer.toString(char_int_value);
                Log.i("onCharacteristicChanged", "Value: " + reading);

                byte[] readingArray = reading.getBytes();
                byte[] spaceArray = "\n".getBytes();
                try {
                    fo.write(readingArray);
                    fo.write(spaceArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onCharacteristicWrite");
                qe.remove(characteristic);
                processQ();
            } else {
                Log.w(TAG, "onCharacteristicWrite: ERROR");
            }
        }
    };

    /**called on scan button press
    public void scanDevices(View view) {
        final Button b = (Button) view;
        b.setText("Scanning!");

        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                Button button = (Button) findViewById(R.id.btnVista);
                button.setText(R.string.vista);
            }
        }, SCAN_PERIOD);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }*/


   /* public void startVibration(View view){
        Button start = (Button) view;
        start.setText("Starting");

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Button button = (Button) findViewById(R.id.start_vibration);
                button.setText("Start Vibration");
                BluetoothGattService svc = btgatts.get(0).getService(VistaGattAttributes.UUID_VIBRATE_SERVICE);

                effectCh = svc.getCharacteristic(VistaGattAttributes.UUID_EFFECT_CHARACTERISTIC);
                effectCh.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);

                timerCh = svc.getCharacteristic(VistaGattAttributes.UUID_TIMER_CHARACTERISTIC);
                timerCh.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT16, 0);

                startCh = svc.getCharacteristic(VistaGattAttributes.UUID_START_CHARACTERISTIC);

                byte[] bbuf = new byte[1];
                bbuf[0] = (byte)(1);
                startCh.setValue(bbuf);

                //register for the mode
                svc = btgatts.get(0).getService(VistaGattAttributes.UUID_VISTA_SERVICE);
                BluetoothGattCharacteristic mode = svc.getCharacteristic(VistaGattAttributes.UUID_MODE_CHARACTERISTIC);
                mode.setValue(2,BluetoothGattCharacteristic.FORMAT_UINT8,0);

                processCharacteristic(mode);
                processCharacteristic(effectCh);
                processCharacteristic(timerCh);

                processCharacteristic(startCh);
            }
        }, 1000);
    }*/


 /*   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_my);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            //Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();



        //File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/sensorReading.txt/");
        //File myFile = new File(folder.getAbsolutePath(), "sensorReading.txt");
       // System.err.println(myFile);

       //String filepath = "/storage/emulated/0/Documents/sensorReading.txt/sensorReading.txt";

       /* String name = "John Doe";

        int age = 44;
        double temp = 26.9;
        FileWriter fw;
        try {
            fw = new FileWriter(new File("sensorReading.txt"));
            fw.write(String.format("My name is %s.",name));
            //fw.write(System.lineSeparator()); //new line
            fw.write(String.format("I am %d years old.",age));
            //fw.write(System.lineSeparator()); //new line
            fw.write(String.format("Today's temperature is %.2f.",temp));
            //fw.write(System.lineSeparator()); //new line
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("Done");*/

        //this has a potential
 /*           try {
                String content = "JavaCodeGeeks is the best!";
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filepath));
                bufferedWriter.write(content, 0, 10);
                bufferedWriter.close();
                System.out.println("The file was successfully updated");
            } catch (IOException e) {
                e.printStackTrace();
            }*/



        /*File scoreFile = new File("C:\\sensorReading.txt");
        if(!scoreFile.exists()) {
            try {
                scoreFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        try {
            FileOutputStream oFile = new FileOutputStream(scoreFile, false);
            // Write new score.
            //byte[] contentBytes = (String.valueOf(prevScore[0]+" "+prevScore[1])).getBytes();
            oFile.write("Bye".getBytes());
            oFile.flush();
            oFile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/


//this one doesn't give you an error but nothing gets written on the file


/*
        try {
            file = new File(Environment.getExternalStorageDirectory(), "srHand30cm.csv");
            fo = new FileOutputStream(file, true);
            String mycontent = "T3: 30 cm \n";
            byte[] bytesArray = mycontent.getBytes();
            fo.write("\n".getBytes());
            fo.write(bytesArray);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void writeFile2() throws IOException {
        FileWriter fw = new FileWriter("sensorReading1.txt", true);
        fw.write("something");
        fw.close();
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    public void footPath(){
        mHandler = new Handler();
        //figure out how to use Context Class, because that seems like an issue.
        //when a class A calls another function...right now I get a null from context part
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        ContextWrapper cw = new ContextWrapper(this);
        if(cw == null)
            Log.d("cw", "null");
        if(cw.getBaseContext() == null)
            Log.i("cw.getBaseContext()", "null");

        if(cw.getPackageManager() == null)
            Log.i("cw.getPackageManger()", "null");
        else
            Log.i("cw.getPackageManger()", "Not null");

        if (!cw.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) cw.getSystemService(ContextWrapper.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            //Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();

    }


}
