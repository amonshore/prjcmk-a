package it.amonshore.comikkua.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.FileHelper;

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
            final DataManager dataManager = DataManager.getDataManager();
            final File file = FileHelper.getExternalFile(getActivity(), "comikku_data.bck");
            findPreference("pref_backup_now").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO A0021 attendere che non ci siano richieste di salvataggio pendenti
                    //A0049
                    if (dataManager.backupToFile(file)) {
                        Toast.makeText(getActivity(), R.string.toast_backup_created, Toast.LENGTH_SHORT).show();
                        updateBackupFileInfo(file);
                    } else {
                        Toast.makeText(getActivity(), R.string.toast_backup_problem, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            //
            updateBackupFileInfo(file);
            findPreference("pref_restore_now").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //A0049
                    if (dataManager.restoreFromFile(file)) {
                        Toast.makeText(getActivity(), R.string.toast_backup_restored, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), R.string.toast_restore_problem, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            //
            findPreference("pref_load_file").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //A0049
                    File file = FileHelper.getExternalFile(getActivity(), "data.json");
                    if (dataManager.restoreFromFile(file)) {
                        Toast.makeText(getActivity(), "imp data.json ok", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "imp data.json ERR", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            //
            findPreference("pref_clear_data").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO A0049 pulire il database e la cache interna
                    //dataManager.updateData(DataManager.ACTION_CLEAR, DataManager.NO_COMICS, DataManager.NO_RELEASE);
                    //Toast.makeText(getActivity(), "data cleared", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        private void updateBackupFileInfo(File file) {
            long lastModified = file.lastModified();
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
