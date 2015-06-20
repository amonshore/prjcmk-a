package it.amonshore.comikkua.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.R;

/**
 * Created by Narsenico on 13/06/2015.
 */
public class InfoActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        //Toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //
        String[] infos = new String[] {
                String.format("Version: %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                "Author: narsenico"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, infos);
        ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(adapter);
    }
}
