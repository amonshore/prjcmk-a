package it.amonshore.comikkua.reminder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.AIncrementalStart;
import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.DBHelper;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.Release;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by narsenico on 30/01/16.
 *
 * A0033
 */
public class ReminderEventHelper extends AIncrementalStart {

    private RxBus<Integer> mEventBus;
    private DBHelper mDBHelper;

    public ReminderEventHelper(Context context) {
        mDBHelper = new DBHelper(context);
    }

    /**
     *
     * @param action    azione da eseguire, può essere anche una azione sui dati che verrà poi convertita in azione sui reminder
     */
    public void send(int action) {
        mEventBus.send(action);
    }

    @Override
    protected void safeStart() {
        if (mEventBus == null) {

            mEventBus = new RxBus<>();
            mEventBus.toObserverable()
                    .observeOn(Schedulers.io()) //gli eventi verranno consumati in un scheduler specifico per I/O
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
                    //tengo un timeout volutamente alto perché tanto verranno aggiornati alla chiamata di stop()
                    .debounce(1000, TimeUnit.MILLISECONDS)
                    .subscribe(new Subscriber<Integer>() {

                        @Override
                        public final void onCompleted() {
                            Utils.d("RX REMINDER end " + Utils.isMainThread());
                            updateReminders();
                        }

                        @Override
                        public final void onError(Throwable e) {
                            Utils.e("RX REMINDER error", e);
                        }

                        @Override
                        public final void onNext(Integer action) {
                            Utils.d("RX REMINDER " + action + " " + Utils.isMainThread());
                            if (action == DataManager.ACTION_REMINDER_CLEAR) {
                                cancelReminders();
                            } else if (action == DataManager.ACTION_REMINDER_UPDATE) {
                                updateReminders();
                            }
                        }

                    });
        }
    }

    @Override
    protected void safeStop() {
        if (mEventBus != null) {
            mEventBus.end(); //scatena onCompleted
            mEventBus = null;
        }
    }

    private void cancelReminders() {
        JobManager.instance().cancelAllForTag(ReleaseReminderJob.TAG);
    }

    private void updateReminders() {
        //eliminio i job già schedulati
        JobManager.instance().cancelAllForTag(ReleaseReminderJob.TAG);
        //
        final DataManager dataManager = DataManager.getDataManager();
        SQLiteDatabase database = null;
        Cursor curReleaseDates = null;
        long fromNow;
        //l'orario è espresso in ms, se negativo si riferisce al giorno prima,  default 8:00 AM
        long modifier = dataManager.getPreference(DataManager.KEY_PREF_REMINDER_TIME, 8 * 3_600_000);
        if (modifier < 0) {
            modifier = modifier * -1 - (24 * 3_600_000); //trasformo in positivo e tolgo un giorno
        }
        final long now = System.currentTimeMillis();
//        Utils.d(this.getClass(), "job modifier " + modifier);
        try {
            database = mDBHelper.getReadableDatabase();
            curReleaseDates = database.query(
                    DBHelper.ReleasesTable.NAME,
                    //estraggo solo la data
                    new String[] { DBHelper.ReleasesTable.COL_DATE, "COUNT(*)" },
                    //filtro sull'utente e sulla data di uscita, considero solo quelle non acquistate
                    DBHelper.ReleasesTable.COL_USER + " = '" + dataManager.getUserName() + "' and " +
                    DBHelper.ReleasesTable.COL_DATE + " >= '" + Utils.formatDbRelease(now) + "' and " +
                    "(" + DBHelper.ReleasesTable.COL_FLAGS + " & " + Release.FLAG_PURCHASED + ") <> " + Release.FLAG_PURCHASED,
                    null,
                    //raggruppo per data di uscita
                    DBHelper.ReleasesTable.COL_DATE,
                    null,
                    DBHelper.ReleasesTable.COL_DATE,
                    null);
            //per ogni data schedulo un allarme
            while (curReleaseDates.moveToNext()) {
                fromNow = Utils.parseDbReleaseMilliseconds(curReleaseDates.getString(0)) + modifier - now;
//                Utils.d(this.getClass(), "UPDREM " +  curReleaseDates.getString(0) + " -> " + fromNow);
                if (fromNow > 0) {
                    PersistableBundleCompat extras = new PersistableBundleCompat();
                    extras.putString(ReleaseReminderJob.EXTRA_DATE, curReleaseDates.getString(0));
                    extras.putInt(ReleaseReminderJob.EXTRA_COUNT, curReleaseDates.getInt(1));
                    new JobRequest.Builder(ReleaseReminderJob.TAG)
                            .setExtras(extras)
                            .setExact(fromNow)
                            .setPersisted(true) //ricarico tutto a mano al boot
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
