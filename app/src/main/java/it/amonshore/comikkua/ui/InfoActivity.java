package it.amonshore.comikkua.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.reminder.ReleaseReminderJob;

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);
        adapter.add(String.format("Version: %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        adapter.add("Author: narsenico");
        adapter.add("Comics count: " + DataManager.getDataManager().getComics().size());

        if (BuildConfig.DEBUG) {
            final Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequestsForTag(ReleaseReminderJob.TAG);
            adapter.add("Reminder count: " + jobRequests.size());
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            for (JobRequest req : jobRequests) {
                Date date = new Date(System.currentTimeMillis() + req.getStartMs());
                adapter.add(String.format("id: %s when: %s", req.getJobId(), dateFormat.format(date)));
            }
        }

        final ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(adapter);
    }
}
