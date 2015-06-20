package it.amonshore.comikkua.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.DataManager;

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
            //
            findPreference("pref_backup_now").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (DataManager.getDataManager().createBackup()) {
                        Toast.makeText(getActivity(), R.string.toast_backup_created, Toast.LENGTH_SHORT).show();
                        updateBackupFileInfo();
                    } else {
                        Toast.makeText(getActivity(), R.string.toast_backup_problem, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            //
            updateBackupFileInfo();
            findPreference("pref_restore_now").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO non ci devono essere richieste di salvataggio pendenti
                    if (DataManager.getDataManager().restoreBackup()) {
                        Toast.makeText(getActivity(), R.string.toast_backup_restored, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), R.string.toast_restore_problem, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }

        private void updateBackupFileInfo() {
            long lastModified = DataManager.getDataManager().getLastModifiedBackupFile();
            if (lastModified == 0) {
                Preference pref = findPreference("pref_restore_now");
                pref.setSummary("");
                pref.setEnabled(false);
            } else {
                Preference pref = findPreference("pref_restore_now");
                pref.setSummary(getString(R.string.last_restored, new Date(lastModified)));
                pref.setEnabled(true);
            }
        }
//
//        @Override
//        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
//            Utils.d(this.getClass(), "onPreferenceTreeClick " + preference.getKey());
//            return super.onPreferenceTreeClick(preferenceScreen, preference);
//        }
    }

}
