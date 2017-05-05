package de.uvwxy.footpath.gui;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.uvwxy.footpath.R;
import de.uvwxy.footpath.vista.MyActivity;

public class RoomSelectActivity extends Activity {

    private ListView mListView;
    private ArrayList<String> mList;
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_select);

        mListView = (ListView) findViewById(R.id.list);
        mList = new ArrayList<String>();

        String[] rooms = Loader.getRooms();

        for (int i = 1; i < rooms.length; i++)
            mList.add(rooms[i]);

        mAdapter = new ArrayAdapter<String>(this, R.layout.list_item_layout, mList);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                String selectedRoom = mList.get(position);
                Intent resultIntent = new Intent();
                // TODO Add extras or a data URI to this intent as appropriate.
                resultIntent.putExtra("sp", selectedRoom);
                Log.i("RoomSelect", selectedRoom);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
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
                startNumberSelectActivity();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startNumberSelectActivity(){
        Intent roomSelectIntent = new Intent(RoomSelectActivity.this, NumberSelect.class);
        startActivity(roomSelectIntent);
    }


}