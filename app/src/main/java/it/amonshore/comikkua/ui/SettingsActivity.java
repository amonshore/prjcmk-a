package it.amonshore.comikkua.ui;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

//    private static final String BACKUP_FILE_NAME = "comikku_data.bck";
//    private static final String OLD_FILE_NAME = "data.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //
            addPreferencesFromResource(R.xml.preferences);
            //
            final DataManager dataManager = DataManager.getDataManager();
//            //A0051 final File bckFile = FileHelper.getExternalFile(getActivity(), BACKUP_FILE_NAME);
//            final File bckFile = FileHelper.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME);
//            final File oldFile = FileHelper.getExternalFile(getActivity(), OLD_FILE_NAME);
//            //se non è permessa la scrittura sulla memoria esterna disabilito la gestione del backup locale
//            if (FileHelper.isExternalStorageWritable()) {
//                updateBackupFileInfo(bckFile);
//                //
//                findPreference("pref_backup_now").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                    @Override
//                    public boolean onPreferenceClick(Preference preference) {
//                        //TODO A0021 attendere che non ci siano richieste di salvataggio pendenti
//                        //A0049
//                        final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
//                                getString(R.string.dialog_backup_wait_title),
//                                getString(R.string.dialog_backup_wait_message),
//                                true, false);
//                        new AsyncTask<Void, Void, Boolean>(){
//                            @Override
//                            protected Boolean doInBackground(Void... params) {
//                                return dataManager.backupToFile(bckFile);
//                            }
//
//                            @Override
//                            protected void onPostExecute(Boolean result) {
//                                progressDialog.dismiss();
//                                if (result) {
//                                    Toast.makeText(getActivity(), R.string.toast_backup_created, Toast.LENGTH_SHORT).show();
//                                    updateBackupFileInfo(bckFile);
//                                } else {
//                                    Toast.makeText(getActivity(), R.string.toast_backup_problem, Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        }.execute();
//                        return true;
//                    }
//                });
//                //
//                findPreference("pref_restore_now").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                    @Override
//                    public boolean onPreferenceClick(Preference preference) {
//                        //A0049
//                        if (dataManager.restoreFromFile(bckFile)) {
//                            Toast.makeText(getActivity(), R.string.toast_backup_restored, Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getActivity(), R.string.toast_restore_problem, Toast.LENGTH_SHORT).show();
//                        }
//                        return true;
//                    }
//                });
//            } else {
//                findPreference("pref_backup_now").setEnabled(false);
//                findPreference("pref_restore_now").setEnabled(false);
//            }
//            //A0049
//            if (oldFile.exists()) {
//                findPreference("pref_restore_old").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                    @Override
//                    public boolean onPreferenceClick(Preference preference) {
//                        //A0049
//                        if (dataManager.restoreFromFile(oldFile)) {
//                            Toast.makeText(getActivity(), R.string.toast_backup_restored, Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getActivity(), R.string.toast_backup_problem, Toast.LENGTH_SHORT).show();
//                        }
//                        return true;
//                    }
//                });
//            } else {
//                findPreference("pref_restore_old").setEnabled(false);
//            }
//            //
//            findPreference("pref_clear_data").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    //A0049
//                    dataManager.clearData();
//                    Toast.makeText(getActivity(), R.string.toast_data_cleared, Toast.LENGTH_SHORT).show();
//                    return true;
//                }
//            });
        }

//        private void updateBackupFileInfo(File file) {
//            long lastModified = file.lastModified();
//            if (lastModified == 0) {
//                Preference pref = findPreference("pref_restore_now");
//                pref.setSummary("");
//                pref.setEnabled(false);
//            } else {
//                Preference pref = findPreference("pref_restore_now");
//                pref.setSummary(getString(R.string.last_restored, new Date(lastModified)));
//                pref.setEnabled(true);
//            }
//        }

    }

}
