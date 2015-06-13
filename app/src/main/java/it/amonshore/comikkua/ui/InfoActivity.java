package it.amonshore.comikkua.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.R;

/**
 * Created by Narsenico on 13/06/2015.
 *
 * TODO rivederla per bene
 */
public class InfoActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        String[] infos = new String[] {
                "Version name: " + BuildConfig.VERSION_NAME,
                "Version code: " + BuildConfig.VERSION_CODE
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, infos);
        setListAdapter(adapter);
    }
}
