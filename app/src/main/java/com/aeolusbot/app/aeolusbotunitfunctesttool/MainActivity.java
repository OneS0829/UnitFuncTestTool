package com.aeolusbot.app.aeolusbotunitfunctesttool;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView unitFuncTestListView;
    ArrayList<String> unitFuncTestArrayList;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unitFuncTestListView = (ListView)findViewById(R.id.unitFuncListView);
        unitFuncTestArrayList = new ArrayList<String>();

        onUnitFuncTestItemAdd();

        ArrayAdapter unitFuncTestArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,unitFuncTestArrayList);
        unitFuncTestListView.setAdapter(unitFuncTestArrayAdapter);
        unitFuncTestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: "+String.valueOf(position));
                Intent intent = new Intent();
                if(position == 0)
                {
                    intent.setClass(MainActivity.this, MapTouchActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
    public void onUnitFuncTestItemAdd()
    {
        unitFuncTestArrayList.add("Map Touch"); //0
    }
}
