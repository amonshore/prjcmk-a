package it.amonshore.comikkua.reminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import junit.framework.Assert;

import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.DBHelper;
import it.amonshore.comikkua.data.DataManager;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by narsenico on 30/01/16.
 *
 * A0033
 */
public class ReminderEventHelper {

    private RxBus<Integer> mEventBus;
    private Subscription mSubscription;
    private DBHelper mDBHelper;

    public ReminderEventHelper(Context context) {
        mDBHelper = new DBHelper(context);
    }

    /**
     * L'azione ACTION_REMINDER_BOOT viene eseguita subito.
     *
     * @param action    azione da eseguire, può essere anche una azione sui dati che verrà poi convertita in azione sui reminder
     */
    public void send(int action) {
        if (action == DataManager.ACTION_REMINDER_BOOT) {
            updateReminders();
        } else {
            mEventBus.send(action);
        }
    }

    /**
     * Solo l'ultimo evento verrà gestito.
     *
     * @param timeout millisecondi che devono trascorrere senza l'invio di nessun evento prima che venga gestito l'ultimo
     */
    public void start(long timeout) {
        Assert.assertNull(mEventBus);

        mEventBus = new RxBus<>();
        mSubscription = mEventBus.toObserverable()
                .map(new Func1<Integer, Integer>() {
                    public Integer call(Integer action) {
                        switch (action) {
                            case DataManager.ACTION_REMINDER_CLEAR:
                            case DataManager.ACTION_REMINDER_UPDATE:
                                return action;
                            case DataManager.ACTION_CLEAR:
                                return DataManager.ACTION_REMINDER_CLEAR;
                            default:
                                return DataManager.ACTION_REMINDER_UPDATE;
                        }
                    }
                })
                .debounce(timeout, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer action) {
                        Utils.d(this.getClass(), "RX REMINDER " + action);
                        if (action == DataManager.ACTION_REMINDER_CLEAR) {
                            cancelReminders();
                        } else if (action == DataManager.ACTION_REMINDER_UPDATE) {
                            updateReminders();
                        }
                    }
                });
    }

    /**
     *
     */
    public void stop() {
        if (mEventBus != null) {
            if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                Utils.d("RX REMINDER unsubscribe");
                mSubscription.unsubscribe();
            }
            mEventBus = null;
        }
    }

    private void cancelReminders() {
        JobManager.instance().cancelAllForTag(ReleaseReminderJob.TAG);
    }

    private void updateReminders() {
        //TODO A0033 ricordarsi di registrare un listener sul boot di sistema per eseguire questo metodo
        //eliminio i job già schedulati
        JobManager.instance().cancelAllForTag(ReleaseReminderJob.TAG);
        //
        final DataManager dataManager = DataManager.getDataManager();
        SQLiteDatabase database = null;
        Cursor curReleaseDates = null;
        //TODO A0033 parametrizzare il modificatore dell'ora di schedulazione facendo scegliere all'utente l'ora della schedulazione e flag stesso giorno o giorno prima
        long fromNow;
        //default 8:00 AM
        final long modifier = dataManager.getPreference(DataManager.KEY_PREF_REMINDER_TIME, 8 * 3_600_000) -
                (dataManager.getPreference(DataManager.KEY_PREF_REMINDER_DAY_BEFORE, false) ? 24 * 3_600_000 : 0);
        final long now = System.currentTimeMillis();
        Utils.d(this.getClass(), "job modifier " + modifier);
        try {
            database = mDBHelper.getReadableDatabase();
            curReleaseDates = database.query(
                    DBHelper.ReleasesTable.NAME,
                    //estraggo solo la data
                    new String[]{DBHelper.ReleasesTable.COL_DATE, "COUNT(*)"},
                    //filtro sull'utente e sulla data di uscita
                    //TODO considerare solo quelle non acquistate
                    DBHelper.ReleasesTable.COL_USER + " = '" + dataManager.getUserName() + "' and " +
                            DBHelper.ReleasesTable.COL_DATE + " >= '" + Utils.formatDbRelease(now) + "'",
                    null,
                    //raggruppo per data di uscita
                    DBHelper.ReleasesTable.COL_DATE,
                    null,
                    DBHelper.ReleasesTable.COL_DATE,
                    null);
            //per ogni data schedulo un allarme
            while (curReleaseDates.moveToNext()) {
                fromNow = Utils.parseDbRelease(curReleaseDates.getString(0)).getTime() + modifier - now;
//                Utils.d(this.getClass(), "UPDREM " +  curReleaseDates.getString(0) + " -> " + fromNow);
                if (fromNow > 0) {
                    PersistableBundleCompat extras = new PersistableBundleCompat();
                    extras.putString(ReleaseReminderJob.EXTRA_DATE, curReleaseDates.getString(0));
                    extras.putInt(ReleaseReminderJob.EXTRA_COUNT, curReleaseDates.getInt(1));
                    new JobRequest.Builder(ReleaseReminderJob.TAG)
                            .setExtras(extras)
                            .setExact(fromNow)
//                            .setPersisted(true) ricarico tutto a mano al boot
                            .build()
                            .schedule();
                }
            }
        } catch (Exception ex) {
            Utils.e(this.getClass(), "Update reminder", ex);
        } finally {
            if (curReleaseDates != null) {
                curReleaseDates.close();
            }
            if (database != null) {
                database.close();
            }
        }
    }

}
