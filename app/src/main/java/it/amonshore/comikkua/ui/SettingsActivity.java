package it.amonshore.comikkua.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import it.amonshore.comikkua.R;

public class SettingsActivity extends ActionBarActivity {

    public static final String KEY_PREF_GROUP_BY_MONTH = "pref_group_by_month";
    public static final String KEY_PREF_WEEK_START_ON_MONDAY = "pref_week_start_on_monday";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        //meglio usare un fragment, così l'activity può essere una ActionBarActivity
//        getFragmentManager().beginTransaction()
//                .replace(R.id.content_view, new SettingsFragment())
//                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //
            addPreferencesFromResource(R.xml.preferences);
        }
    }

}
