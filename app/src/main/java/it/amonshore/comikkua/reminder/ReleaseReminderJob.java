package it.amonshore.comikkua.reminder;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

import com.evernote.android.job.Job;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;
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
        final Context context = getContext();
        final int count = params.getExtras().getInt(EXTRA_COUNT, 0);
        final String date = params.getExtras().getString(EXTRA_DATE, null);
        final TimeZone timeZone = TimeZone.getDefault();
        final DateTime dtDate = new DateTime(date);
        final DateTime dtToday = DateTime.today(timeZone);
        final DateTime dtTomorrow = dtToday.plusDays(1);
        final String title;
        final String text;

        if (count == 1) {
            title = context.getString(R.string.notification_release_title_single);
        } else {
            title = context.getString(R.string.notification_release_title_more, count);
        }

        if (dtDate.isSameDayAs(dtToday)) {
            text = context.getString(R.string.notification_release_today);
        } else if (dtDate.isSameDayAs(dtTomorrow)) {
            text = context.getString(R.string.notification_release_tomorrow);
        } else {
            text = context.getString(R.string.notification_release_next_days, dtDate.getMilliseconds(timeZone));
        }

        //preparo l'intent per l'apertura della MainActivity
        final Intent intent = new Intent(context, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
//                .setNumber(count)
                .setContentIntent(resultPendingIntent);

        NotificationManagerCompat.from(context).notify(1, notifyBuilder.build());
        return Result.SUCCESS;
    }
}
