package it.amonshore.comikkua.ui.options;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.FileHelper;

public class DataOptionsActivity extends ActionBarActivity {

    private static final String BACKUP_FILE_NAME = "comikku_data.bck";
    private static final String OLD_FILE_NAME = "data.json";

    private static final int ID_BACKAUP_NOW = 0;
    private static final int ID_RESTORE_NOW = 1;
    private static final int ID_RESTORE_OLD = 2;
    private static final int ID_CLEAR_DATA = 3;
    private static final int ID_CLEAR_PREFERENCES = 4;

    private DataOptions[] mDataOptionses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_options);
        //
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDataOptionses = new DataOptions[] {
                new DataOptions(ID_BACKAUP_NOW, getString(R.string.opt_backup_now_title),
                        R.string.opt_backup_now_confirm_title, R.string.opt_backup_now_confirm_message, R.string.opt_backup_now_confirm_ok,
                        R.string.opt_backup_now_wait),
                new DataOptions(ID_RESTORE_NOW, getString(R.string.opt_restore_now_title),
                        R.string.opt_restore_now_confirm_title, R.string.opt_restore_now_confirm_message, R.string.opt_restore_now_confirm_ok,
                        R.string.opt_restore_now_wait),
                new DataOptions(ID_RESTORE_OLD, getString(R.string.opt_restore_old_title),
                        R.string.opt_restore_old_confirm_title, R.string.opt_restore_old_confirm_message, R.string.opt_restore_old_confirm_ok,
                        R.string.opt_restore_old_wait),
                new DataOptions(ID_CLEAR_DATA, getString(R.string.opt_clear_data_title),
                        R.string.opt_clear_data_confirm_title, R.string.opt_clear_data_confirm_message, R.string.opt_clear_data_confirm_ok,
                        R.string.opt_clear_data_wait)
        };

        //TODO: aggiungere ripristino opzioni di defualt

        //
        final DataManager dataManager = DataManager.getDataManager();
        final File bckFile = FileHelper.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME);
        final File oldFile = FileHelper.getExternalFile(this, OLD_FILE_NAME);
        //se non è permessa la scrittura sulla memoria esterna disabilito la gestione del backup locale
        if (FileHelper.isExternalStorageWritable()) {
            updateBackupFileInfo(bckFile);
        } else {
            mDataOptionses[ID_BACKAUP_NOW].Enabled = false;
            mDataOptionses[ID_RESTORE_NOW].Enabled = false;
        }
        if (!oldFile.exists()) {
            mDataOptionses[ID_RESTORE_OLD].Enabled = false;
        }

        final ListView listView = (ListView)findViewById(R.id.list);
        final DataOptionsAdapter adapter = new DataOptionsAdapter(this, mDataOptionses);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DataOptions dataOptions = (DataOptions) listView.getItemAtPosition(position);
                if (dataOptions.Enabled) {
                    //conferma azione
                    final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (which == DialogInterface.BUTTON_POSITIVE) {

                                //TODO A0021 attendere che non ci siano richieste di salvataggio pendenti
                                //A0049
                                final ProgressDialog progressDialog = ProgressDialog.show(DataOptionsActivity.this,
                                        getString(R.string.dialog_backup_wait_title),
                                        getString(dataOptions.WaitMessageId),
                                        true, false);
                                new AsyncTask<Void, Void, Boolean>() {
                                    @Override
                                    protected Boolean doInBackground(Void... params) {
                                        if (dataOptions.Id == ID_BACKAUP_NOW) {
                                            return dataManager.backupToFile(bckFile);
                                        } else if (dataOptions.Id == ID_RESTORE_NOW) {
                                            return dataManager.restoreFromFile(bckFile);
                                        } else if (dataOptions.Id == ID_RESTORE_OLD) {
                                            return dataManager.restoreFromFile(oldFile);
                                        } else if (dataOptions.Id == ID_CLEAR_DATA) {
                                            dataManager.clearData();
                                            return true;
                                        } else {
                                            return false;
                                        }
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean result) {
                                        progressDialog.dismiss();

                                        if (dataOptions.Id == ID_BACKAUP_NOW) {
                                            if (result) {
                                                Toast.makeText(DataOptionsActivity.this, R.string.opt_backup_now_done, Toast.LENGTH_SHORT).show();
                                                updateBackupFileInfo(bckFile);
                                                adapter.notifyDataSetChanged();
                                            } else {
                                                Toast.makeText(DataOptionsActivity.this, R.string.opt_backup_now_fail, Toast.LENGTH_SHORT).show();
                                            }
                                        } else if (dataOptions.Id == ID_RESTORE_NOW) {
                                            if (result) {
                                                Toast.makeText(DataOptionsActivity.this, R.string.opt_restore_now_done, Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(DataOptionsActivity.this, R.string.opt_restore_now_fail, Toast.LENGTH_SHORT).show();
                                            }
                                        } else if (dataOptions.Id == ID_RESTORE_OLD) {
                                            if (result) {
                                                Toast.makeText(DataOptionsActivity.this, R.string.opt_restore_now_done, Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(DataOptionsActivity.this, R.string.opt_backup_now_fail, Toast.LENGTH_SHORT).show();
                                            }
                                        } else if (dataOptions.Id == ID_CLEAR_DATA) {
                                            dataManager.clearData();
                                            Toast.makeText(DataOptionsActivity.this, R.string.opt_clear_data_done, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }.execute();
                            }
                        }
                    };
                    final AlertDialog.Builder builder = new AlertDialog.Builder(DataOptionsActivity.this)
                            .setTitle(dataOptions.ConfirmTitleId)
                            .setMessage(dataOptions.ConfirmMessageId)
                            .setPositiveButton(dataOptions.ConfirmPositiveId, onClickListener)
                            .setNegativeButton(R.string.cancel, onClickListener);
                    builder.show();
                }
            }
        });
        listView.setAdapter(adapter);
    }

    private void updateBackupFileInfo(File file) {
        long lastModified = file.lastModified();
        DataOptions opt = mDataOptionses[ID_RESTORE_NOW];
        if (lastModified == 0) {
            opt.Summary = null;
            opt.Enabled = false;
        } else {
            opt.Summary =  getString(R.string.last_restored, new Date(lastModified));
            opt.Enabled = true;
        }
    }

    private final static class DataOptions {
        public long Id;
        public String Title;
        public String Summary;
        public int ConfirmTitleId;
        public int ConfirmMessageId;
        public int ConfirmPositiveId;
        public int WaitMessageId;
        public boolean Enabled;
        public DataOptions(long id, String title, int confirmTitleId,
                           int confirmMessageId, int confirmPositiveId,
                           int waitMessageId) {
            this.Id = id;
            this.Title = title;
            this.Summary = null;
            this.ConfirmTitleId = confirmTitleId;
            this.ConfirmMessageId = confirmMessageId;
            this.ConfirmPositiveId = confirmPositiveId;
            this.WaitMessageId = waitMessageId;
            this.Enabled = true;
        }
    }

    private final static class DataOptionsAdapter extends BaseAdapter {

        private Context mContext;
        private DataOptions[] mDataOptions;

        public DataOptionsAdapter(Context context, DataOptions[] dataOptions) {
            mContext = context;
            mDataOptions = dataOptions;
        }

        @Override
        public int getCount() {
            return mDataOptions.length;
        }

        @Override
        public Object getItem(int position) {
            return mDataOptions[position];
        }

        @Override
        public long getItemId(int position) {
            return ((DataOptions)getItem(position)).Id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DataOptions dataOptions = (DataOptions)getItem(position);

            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_data_options_item, parent, false);
            TextView txtTitle = ((TextView)convertView.findViewById(R.id.title));
            TextView txtSummary = ((TextView)convertView.findViewById(R.id.summary));

            txtTitle.setText(dataOptions.Title);
            txtSummary.setText(dataOptions.Summary);

            if (!dataOptions.Enabled) {
                txtTitle.setTextColor(convertView.getResources().getColor(R.color.comikku_disalbled_text));
                txtSummary.setTextColor(convertView.getResources().getColor(R.color.comikku_disalbled_text));
            }

            return convertView;
        }
    }

}
