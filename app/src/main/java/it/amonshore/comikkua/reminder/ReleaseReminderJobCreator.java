package it.amonshore.comikkua.reminder;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by narsenico on 15/01/16.
 */
public class ReleaseReminderJobCreator implements JobCreator {

    @Override
    public Job create(String tag) {
        switch (tag) {
            case ReleaseReminderJob.TAG:
                return new ReleaseReminderJob();
            default:
                return null;
        }
    }
}
