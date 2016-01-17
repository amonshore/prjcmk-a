package it.amonshore.comikkua.reminder;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.evernote.android.job.Job;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.ui.MainActivity;

/**
 * Created by narsenico on 15/01/16.
 */
public class ReleaseReminderJob extends Job {

    public static final String TAG = "release_job";
    public static final String EXTRA_COUNT = "extra_count";
    public static final String EXTRA_DATE = "extra_date";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        //
        int count = params.getExtras().getInt(EXTRA_COUNT, 0);
        String date = params.getExtras().getString(EXTRA_DATE, null);
        String today = Utils.formatDbRelease(System.currentTimeMillis());
        String text;

        if (today.equals(date)) {
            text = count == 1 ?
                    "1 uscita prevista per oggi" :
                    "%1$s uscite previste per oggi";
        } else {
            text = count == 1 ?
                    "1 uscita prevista per %2$s" :
                    "%1$s uscite previste per %2$s";
        }

        //preparo l'intent per l'apertura della MainActivity
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(getContext())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Nuove uscite")
                .setContentText(String.format(text, count, date))
                .setAutoCancel(true)
                .setNumber(count)
                .setContentIntent(PendingIntent.getActivity(getContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat.from(getContext()).notify(1, notifyBuilder.build());
        return Result.SUCCESS;
    }
}
