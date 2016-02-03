package it.amonshore.comikkua.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;
import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.AIncrementalStart;
import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by narsenico on 27/01/16.
 *
 * A0058
 */
class DataEventHelper extends AIncrementalStart {

    private RxBus<DataEvent> mEventBus;
    private DBHelper mDBHelper;

    public DataEventHelper(Context context) {
        mDBHelper = new DBHelper(context);
    }

    /**
     * Invia una azione che verrà gestita insieme ad altre sucessivamente.
     *
     * @param action    azione da eseguire sui dati (ACTION_ADD, ACTION_UPD, ACTION_DEL)
     * @param comicsId  id del comics su cui operare l'azione
     * @param releaseNumber numero della release su cui operare l'azione (NO_RELEASE per nessuna)
     */
    public void send(int action, long comicsId, int releaseNumber) {
        if (mEventBus != null) {
            DataEvent event = new DataEvent();
            event.Action = action;
            event.ComicsId = comicsId;
            event.ReleaseNumber = releaseNumber;
            mEventBus.send(event);
        }
    }

    @Override
    protected void safeStart() {
        if (mEventBus == null) {

            final DataManager dataManager = DataManager.getDataManager();
//        final Queue<DataEvent> pEventQueue = new ConcurrentLinkedQueue<>();

            mEventBus = new RxBus<>();
            mEventBus.toObserverable()
                    .observeOn(Schedulers.io()) //gli eventi verranno consumati in un scheduler specifico per I/O
                            //raggruppo una serie di eventi (buffer) e li gestisco dopo che è passato un certo periodo di tempo senza altri eventi (debouce)
                            //il timeout deve essere basso per evitare che alcuni eventi non vengano gestiti
                    .publish(new Func1<Observable<DataEvent>, Observable<List<DataEvent>>>() {
                        @Override
                        public Observable<List<DataEvent>> call(Observable<DataEvent> stream) {
                            return stream.buffer(stream.debounce(200, TimeUnit.MILLISECONDS));
                        }
                    })
//                .doOnNext(new Action1<List<DataEvent>>() {
//                    @Override
//                    public void call(List<DataEvent> dataEvents) {
//                        pEventQueue.addAll(dataEvents);
//                    }
//                })
                    .subscribe(new Subscriber<List<DataEvent>>() {
                        @Override
                        public void onCompleted() {
                            Utils.d("RX DATA end " + Utils.isMainThread());
                        }

                        @Override
                        public void onError(Throwable e) {
                            Utils.e("RX REMINDER error", e);
                        }

                        @Override
                        public void onNext(List<DataEvent> dataEvents) {
                            Utils.d("RX DATA " + dataEvents.size() + " " + Utils.isMainThread());
                            if (dataEvents.size() > 0) {
                                final String userName = dataManager.getUserName();
                                SQLiteDatabase database = null;
                                try {
                                    database = mDBHelper.getWritableDatabase();
                                    for (DataEvent event : dataEvents) {
//                                    Utils.d(String.format("RX DATA act %s cid %s rid %s", event.Action, event.ComicsId, event.ReleaseNumber));
                                        switch (event.Action) {
                                            case DataManager.ACTION_CLEAR:
                                                clear(database, userName);
                                                break;
                                            case DataManager.ACTION_ADD:
                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
                                                    writeComics(database, userName, dataManager.getComics(event.ComicsId), true);
                                                } else {
                                                    writeRelease(database, userName, dataManager.getComics(event.ComicsId)
                                                            .getRelease(event.ReleaseNumber), true);
                                                }
                                                break;
                                            case DataManager.ACTION_UPD:
                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
                                                    writeComics(database, userName, dataManager.getComics(event.ComicsId), false);
                                                } else {
                                                    writeRelease(database, userName, dataManager.getComics(event.ComicsId)
                                                            .getRelease(event.ReleaseNumber), false);
                                                }
                                                break;
                                            case DataManager.ACTION_DEL:
                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
                                                    deleteComics(database, userName, event.ComicsId);
                                                } else {
                                                    deleteRelease(database, userName, event.ComicsId, event.ReleaseNumber);
                                                }
                                                break;
                                        }
                                    }
                                } catch (Exception ex) {
                                    Utils.e(this.getClass(), "Write data", ex);
                                } finally {
                                    if (database != null) {
                                        database.close();
                                    }
                                }
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

    private void writeComics(SQLiteDatabase database, String userName, Comics comics, boolean isNew) {
        if (isNew) {
            database.insert(DBHelper.ComicsTable.NAME, null,
                    DBHelper.ComicsTable.getContentValues(comics, userName));
            for (Release release : comics.getReleases()) {
                writeRelease(database, userName, release, true);
            }
        } else {
            database.replace(DBHelper.ComicsTable.NAME, null,
                    DBHelper.ComicsTable.getContentValues(comics, userName));
        }
    }

    private void deleteComics(SQLiteDatabase database, String userName, long comicsId) {
        database.delete(DBHelper.ReleasesTable.NAME,
                DBHelper.ReleasesTable.COL_COMICS_ID + " = " + comicsId + " and " +
                        DBHelper.ReleasesTable.COL_USER + " = '" + userName + "'",
                null);
        database.delete(DBHelper.ComicsTable.NAME,
                DBHelper.ComicsTable.COL_ID + " = " + comicsId + " and " +
                        DBHelper.ComicsTable.COL_USER + " = '" + userName + "'",
                null);
    }

    private void writeRelease(SQLiteDatabase database, String userName, Release release, boolean isNew) {
        if (isNew) {
            database.insert(DBHelper.ReleasesTable.NAME, null,
                    DBHelper.ReleasesTable.getContentValues(release, userName));
        } else {
            database.replace(DBHelper.ReleasesTable.NAME, null,
                    DBHelper.ReleasesTable.getContentValues(release, userName));
        }
    }

    private void deleteRelease(SQLiteDatabase database, String userName, long comicsId, int releaseNumber) {
        database.delete(DBHelper.ReleasesTable.NAME,
                DBHelper.ReleasesTable.COL_COMICS_ID + " = " + comicsId + " and " +
                        DBHelper.ReleasesTable.COL_USER + " = '" + userName + "' and " +
                        DBHelper.ReleasesTable.COL_NUMBER + " = " + releaseNumber,
                null);
    }

    private void clear(SQLiteDatabase database, String userName) {
        database.delete(DBHelper.ReleasesTable.NAME,
                DBHelper.ReleasesTable.COL_USER + " = '" + userName + "'",
                null);
        database.delete(DBHelper.ComicsTable.NAME,
                DBHelper.ComicsTable.COL_USER + " = '" + userName + "'",
                null);
    }

    private final static class DataEvent {
        public int Action;
        public long ComicsId;
        public int ReleaseNumber;
    }

}
