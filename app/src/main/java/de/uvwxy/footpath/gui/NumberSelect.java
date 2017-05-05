package de.uvwxy.footpath.gui;

/**
 * Created by Erii on 3/24/2017.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import de.uvwxy.footpath.R;

/**
 * Created by Erii on 3/24/2017.
 */

public class NumberSelect extends Activity{
    private String startingPosition = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.numeric_keyboard);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.switch_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_switch:
                startRoomSelectActivity();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startRoomSelectActivity(){
        Intent intent = new Intent(this, RoomSelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);
        finish();
    }


    public void one(View view){
        startingPosition+="1";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void two(View view){
        startingPosition+="2";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void three(View view){
        startingPosition+="3";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void four(View view){
        startingPosition+="4";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void five(View view){
        startingPosition+="5";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void six(View view){
        startingPosition+="6";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void seven(View view){
        startingPosition+="7";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void eight(View view){
        startingPosition+="8";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void nine(View view){
        startingPosition+="9";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void clear(View view){
        startingPosition ="";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void clear(){
        startingPosition ="";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void delete(View view){
        if (startingPosition != null && startingPosition.length() > 0 ) {
            startingPosition = startingPosition.substring(0, startingPosition.length()-1);
        }
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void finish(View view){
        if(numberAllowed()) {
            Intent resultIntent = new Intent();
            // TODO Add extras or a data URI to this intent as appropriate.
            resultIntent.putExtra("sp", startingPosition);
            Log.i("NumberSelect", startingPosition);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    public void zero(View view){
        startingPosition+="0";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public void dot(View view){
        startingPosition+=".";
        TextView mNumber = (TextView) findViewById(R.id.number);
        mNumber.setText(startingPosition);
    }

    public String getStartingPosition(){
        return startingPosition;
    }

    public boolean numberAllowed(){
        if(Arrays.asList(Loader.getRooms()).contains(startingPosition))
            return true;
        else
        {
            Toast.makeText(NumberSelect.this, "The room number: "+ startingPosition +" doesn't exist. " +
                    "Please reenter another room number", Toast.LENGTH_LONG).show();
            clear();
            return false;
        }
    }


}

