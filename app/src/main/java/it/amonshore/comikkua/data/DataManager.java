package it.amonshore.comikkua.data;

import android.content.Context;
import android.database.Cursor;
import android.database.Observable;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import hirondelle.date4j.DateTime;
import it.amonshore.comikkua.Utils;

/**
 * Created by Narsenico on 07/05/2015.
 */
public class DataManager extends Observable<ComicsObserver> {

    public final static int ACTION_ADD = 1;
    public final static int ACTION_UPD = 2;
    public final static int ACTION_DEL = 3;
    public final static int ACTION_CLEAR = 4;

    public static final int CAUSE_EMPTY = 0;
    public static final int CAUSE_SAFE = 1;
    public static final int CAUSE_LOADING = 1 << 1;
    public static final int CAUSE_SETTINGS_CHANGED = 1 << 2;
    public static final int CAUSE_ROTATION = 1 << 3;
    public static final int CAUSE_PAGE_CHANGED = 1 << 4;
    public static final int CAUSE_COMICS_ADDED = 1 << 5;
    public static final int CAUSE_COMICS_CHANGED = 1 << 6;
    public static final int CAUSE_COMICS_REMOVED = 1 << 7;
    public static final int CAUSE_RELEASE_ADDED = 1 << 8;
    public static final int CAUSE_RELEASE_CHANGED = 1 << 9;
    public static final int CAUSE_RELEASE_REMOVED = 1 << 10;
    public static final int CAUSE_RELEASES_MODE_CHANGED = 1 << 11;
    public static final int CAUSE_CREATED = 1 << 12;

    public static final long NO_COMICS = -1;
    public static final int NO_RELEASE = -1;

    private static DataManager instance;

    /**
     *
     * @param context   usare Context.getApplicationContext()
     * @param userName nome utente
     * @return  istanza di DataManager
     */
    public static DataManager init(Context context, String userName) {
        if (instance != null && instance.mContext != context) {
            Utils.d(DataManager.class, "dispose DM");
            instance.dispose();
            instance = null;
        }

        if (instance == null) {
            Utils.d(DataManager.class, "init DM");
            instance = new DataManager(context, userName);
        }

        return instance;
    }

    public static DataManager getDataManager() {
        return instance;
    }

    private final String mUserName;
    private long mLastComicsId;
    private final Context mContext;
    private boolean mDataLoaded;
    //
    private TreeMap<Long, Comics> mComicsCache;
    //
    private final UndoHelper<Comics> mUndoComics;
    private final UndoHelper<Release> mUndoRelease;
    //contiene un elenco di tutti gli editori
    private HashSet<String> mPublishers;
    //contiene la best release per ogni comics
    private TreeMap<Long, ReleaseInfo> mBestReleases;
    //
    private AsyncWriteHandler mWriteHandler;
    //A0038
    private long mLastReadDate;
    //A0049
    private DBHelper mDBHelper;
    //
    private final Object mSyncObj = new Object();

    private DataManager(Context context, String userName) {
        mContext = context;
        mUserName = userName;
        //
        mComicsCache = new TreeMap<>();
        mPublishers = new HashSet<>();
        mBestReleases = new TreeMap<>();
        mUndoComics = new UndoHelper<>();
        mUndoRelease = new UndoHelper<>();
        //
        mDBHelper = new DBHelper(context);
    }

    private void putPublisher(String publisher) {
        if (publisher != null && TextUtils.getTrimmedLength(publisher) > 0) {
            mPublishers.add(publisher);
        }
    }

    /**
     *
     * @return  ritorna un nuovo id univoco
     */
    public long getSafeNewComicsId() {
        //TODO deve ritornare un id univoco, perché verrà usato come identificativo delle View
        return ++mLastComicsId;
    }

    /**
     *
     * @return  ritorna tutte gli id dei comics
     */
    public Set<Long> getComics() {
        return mComicsCache.keySet();
    }

    /**
     *
     * @param id    id del comics
     * @return  ritorna l'istanza di Comics con l'id specificato o null se non viene trovata
     */
    public Comics getComics(long id) {
        return mComicsCache.get(id);
    }

    /**
     *
     * @param name  nome del comics da ricercare
     * @return  ritorna l'istanza di Comics con il nome specificato o null se non viene trovata
     */
    public Comics getComicsByName(String name) {
        if (name == null) return null;

        for (Comics comics : mComicsCache.values()) {
            if (comics.getName().trim().equalsIgnoreCase(name.trim())) {
                return comics;
            }
        }
        return null;
    }

    /**
     *
     * @param comics    istanza da aggiungere
     * @return  true se è stato aggiunto, false se ha sostituito un elemento esistente
     */
    public boolean put(Comics comics) {
        synchronized (mSyncObj) {
            putPublisher(comics.getPublisher());
            return (mComicsCache.put(comics.getId(), comics) == null);
        }
    }

    /**
     * L'istanza rimossa puù essere recuperata chiamando getLastRemovedComics()
     *
     * @param id    id del comics da rimuovere
     * @return  true se l'elemento è stato elmiminato, false se non esisteva
     */
    public boolean remove(long id) {
        synchronized (mSyncObj) {
            Comics comics = mComicsCache.remove(id);
            if (comics != null) {
                mUndoComics.push(comics);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * L'istanza rimossa puù essere recuperata chiamando getLastRemovedReleases()
     *
     * @param comicsId  id del comics per cui rimuovere la release
     * @param number    numero della release da rimuovere
     * @return  true se viene rimossao o false altrimenti
     */
    public boolean removeRelease(long comicsId, int number) {
        Release release = getComics(comicsId).removeRelease(number);
        if (release != null) {
            mUndoRelease.push(release);
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @return  l'elenco dei publisher presenti nei comics
     */
    public String[] getPublishers() {
        return mPublishers.toArray(new String[mPublishers.size()]);
    }

    /**
     *
     */
    public void updateBestRelease() {
        synchronized (mSyncObj) {
            for (long id : getComics()) {
                updateBestRelease(id);
            }
        }
    }

    /**
     *
     * @param id    id del comics
     * @return  info sulla best release
     */
    public ReleaseInfo updateBestRelease(long id) {
        Comics comics = getComics(id);
        ReleaseInfo ri = ComicsBestReleaseHelper.getComicsBestRelease(mContext, comics);
        mBestReleases.put(comics.getId(), ri);
        return ri;
    }

    /**
     *
     *
     * @param id    id del comics
     * @return  la best release relativa al comics
     */
    public ReleaseInfo getBestRelease(long id) {
        return mBestReleases.get(id);
    }

    /**
     * Leggi i dati
     *
     * @return  numero di comics letti
     */
    public int readComics() {
        if (mComicsCache == null || !mDataLoaded) {
            SQLiteDatabase database = null;
            try {
                database = mDBHelper.getReadableDatabase();
                Cursor curComics = null, curReleases = null;
                mComicsCache.clear();
                mPublishers.clear();
                mBestReleases.clear();
                try {
                    //estraggo tutti i comics dell'utente
                    curComics = database.query(DBHelper.ComicsTable.NAME,
                            //tutte le colonne
                            DBHelper.ComicsTable.COLUMNS,
                            //filtro sull'utente
                            DBHelper.ComicsTable.COL_USER + " = '" + mUserName + "'",
                            null,
                            null,
                            null,
                            //order by
                            DBHelper.ComicsTable.COL_ID,
                            null);
                    //scorro il cursore dei comics
                    while (curComics.moveToNext()) {
                        Comics comics = new Comics(curComics.getLong(DBHelper.ComicsTable.IDX_ID));
                        comics.setName(curComics.getString(DBHelper.ComicsTable.IDX_NAME));
                        comics.setSeries(curComics.getString(DBHelper.ComicsTable.IDX_SERIES));
                        comics.setPublisher(curComics.getString(DBHelper.ComicsTable.IDX_PUBLISHER));
                        comics.setAuthors(curComics.getString(DBHelper.ComicsTable.IDX_AUTHORS));
                        comics.setPrice(curComics.getDouble(DBHelper.ComicsTable.IDX_PRICE));
                        comics.setPeriodicity(curComics.getString(DBHelper.ComicsTable.IDX_PERIODICITY));
                        comics.setReserved(curComics.getString(DBHelper.ComicsTable.IDX_RESERVED) == DBHelper.TRUE);
                        comics.setNotes(curComics.getString(DBHelper.ComicsTable.IDX_NOTES));
                        comics.setImage(curComics.getString(DBHelper.ComicsTable.IDX_IMAGE));
                        //
                        mLastComicsId = Math.max(mLastComicsId, comics.getId());
                        //estraggo tutte le release per il comics
                        curReleases = database.query(DBHelper.ReleasesTable.NAME,
                                //tutte le colonne
                                DBHelper.ReleasesTable.COLUMNS,
                                //filtro sull'utente
                                DBHelper.ReleasesTable.COL_USER + " = '" + mUserName + "' and " +
                                    DBHelper.ReleasesTable.COL_COMICS_ID + " = " + comics.getId(),
                                null,
                                null,
                                null,
                                //order by
                                DBHelper.ReleasesTable.COL_NUMBER,
                                null);
                        while (curReleases.moveToNext()) {
                            Release release = new Release(comics.getId());
                            release.setNumber(curReleases.getInt(DBHelper.ReleasesTable.IDX_NUMBER));
                            release.setDate(Utils.parseDbRelease(curReleases.getString(DBHelper.ReleasesTable.IDX_DATA)));
                            release.setPrice(curReleases.getDouble(DBHelper.ReleasesTable.IDX_PRICE));
                            release.setFlags(curReleases.getInt(DBHelper.ReleasesTable.IDX_FLAGS));
                            release.setNotes(curReleases.getString(DBHelper.ReleasesTable.IDX_NOTES));
                            comics.putRelease(release);
                        }
                        curReleases.close();
                        curReleases = null;
                        //
                        Utils.d(this.getClass(), "A0049 read " + comics.getName() + " -> " + comics.getId());
                        mComicsCache.put(comics.getId(), comics);
                        putPublisher(comics.getPublisher());
                        updateBestRelease(comics.getId());
                    }
                } finally {
                    if (curComics != null) {
                        curComics.close();
                    }
                    if (curReleases != null) {
                        curReleases.close();
                    }
                }
            } catch (Exception ex) {
                Utils.e(this.getClass(), "Read data", ex);
            } finally {
                if (database != null) {
                    database.close();
                }
            }
            mDataLoaded = true;
            final TimeZone timeZone = TimeZone.getDefault();
            mLastReadDate = DateTime.today(timeZone).getStartOfDay().getMilliseconds(timeZone);
        } else {
            //Utils.d("readComics: already loaded");
            //A0038 rispetto all'ultima volta che ho letto i dati, è cambiato giorno?
            //  se si è meglio aggiornare le best release
            final TimeZone timeZone = TimeZone.getDefault();
            final long today = DateTime.today(timeZone).getStartOfDay().getMilliseconds(timeZone);
            if (today != mLastReadDate) {
                //Utils.d("readComics: best release update needed!");
                updateBestRelease();
                mLastReadDate = today;
            }
        }
        return mComicsCache.size();
    }

    /**
     *
     * @param file nome del file dove verrà salvato il backup dei dati
     * @return  true se non ci sono stati problemi durante il salvataggio
     */
    public boolean backupToFile(File file) {
        FileHelper fileHelper = new FileHelper();
        try {
            fileHelper.exportComics(file, mComicsCache.values().toArray(new Comics[mComicsCache.size()]));
            return true;
        } catch (IOException ioex) {
            Utils.e(this.getClass(), "Error during data backup", ioex);
            return false;
        }
    }

    /**
     *
     * @param file nome del file contenente il backup
     * @return  true se non ci sono stati problemi durante il restore
     */
    public boolean restoreFromFile(File file) {
        synchronized (mSyncObj) {
            FileHelper fileHelper = new FileHelper();
            try {
                Comics[] comics = fileHelper.importComics(file);
                mComicsCache.clear();
                mPublishers.clear();
                mBestReleases.clear();
                updateData(ACTION_CLEAR, NO_COMICS, NO_RELEASE);
                for (Comics cc : comics) {
                    put(cc);
                    mLastComicsId = Math.max(mLastComicsId, cc.getId());
                    updateBestRelease(cc.getId());
                    updateData(ACTION_ADD, cc.getId(), NO_RELEASE);
                }
                return true;
            } catch (IOException ioex) {
                Utils.e(this.getClass(), "Error during data restore", ioex);
                return false;
            }
        }
    }

    /**
     *
     */
    public void clearData() {
        synchronized (mSyncObj) {
            mComicsCache.clear();
            mPublishers.clear();
            mBestReleases.clear();
            updateData(ACTION_CLEAR, NO_COMICS, NO_RELEASE);
        }
    }

    /**
     *
     * @param cause la causa del cambiamento (vedi DataManager.CAUSE_xxx)
     */
    public void notifyChanged(int cause) {
        Utils.d(this.getClass(), "notifyChanged " + cause);

        //see DataSetObservable.notifyChanged()
        synchronized(mObservers) {
            // since onChanged() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged(cause);
            }
        }
    }

    /**
     *
     * @param cause la causa del cambiamento (vedi DataManager.CAUSE_xxx)
     * @param source    per questo osservatore non verrà notificato il cambiamento
     */
    public void notifyChangedButMe(int cause, ComicsObserver source) {
        Utils.d(this.getClass(), "notifyChangedButMe " + cause);

        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                if (source == mObservers.get(i)) continue;
                mObservers.get(i).onChanged(cause);
            }
        }
    }

    /**
     *
     * @param action    azione da eseguire sui dati (ACTION_ADD, ACTION_UPD, ACTION_DEL)
     * @param comicsId  id del comics su cui operare l'azione
     * @param releaseNumber numero della release su cui operare l'azione (NO_RELEASE per nessuna)
     */
    public void updateData(int action, long comicsId, int releaseNumber) {
        //Utils.d(this.getClass(), String.format("A0049 act %s, cid %s, rel %s", action, comicsId, releaseNumber));
        mWriteHandler.appendRequest(new AsyncActionRequest(action, comicsId, releaseNumber));
    }

    /**
     *
     */
    public void startWriteHandler() {
        mWriteHandler = new AsyncWriteHandler();
        mWriteHandler.start();
    }

    /**
     *
     */
    public void stopWriteHandler() {
        mWriteHandler.cancel();
        mWriteHandler = null;
    }

    /**
     *
     * @return  ritrona l'helper per l'undo dei comics eliminati
     */
    public UndoHelper<Comics> getUndoComics() {
        return mUndoComics;
    }

    /**
     *
     * @return  ritrona l'helper per l'undo delle release eliminate
     */
    public UndoHelper<Release> getUndoRelease() {
        return mUndoRelease;
    }

    private void dispose() {
        unregisterAll();
        stopWriteHandler();
    }

    private static final class AsyncActionRequest {
        public int Action;
        public long ComicsId;
        public int ReleaseNumber;

        public AsyncActionRequest(int action, long comicsId, int releaseNumber) {
            this.Action = action;
            this.ComicsId = comicsId;
            this.ReleaseNumber = releaseNumber;
        }
    }

    private class AsyncWriteHandler {

        private Semaphore mMainLoopHandler;
        private boolean mCancel;
        //A0049
        private Queue<AsyncActionRequest> mQueue;

        public AsyncWriteHandler() {
            mQueue = new ConcurrentLinkedQueue<>();
        }

        public void appendRequest(AsyncActionRequest request) {
            mQueue.add(request);
            mMainLoopHandler.release();
        }

        public void cancel() {
//            Utils.d(this.getClass(), "cancel");
            mCancel = true ;
            mMainLoopHandler.release();
        }

        public void start() {
            mCancel = false;
            mMainLoopHandler = new Semaphore(0);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!mCancel) {
                            //
                            mMainLoopHandler.drainPermits();
                            //attendo un tempo indefinito
                            mMainLoopHandler.acquire();
                            Utils.d(this.getClass(), "*** saving");
                            SQLiteDatabase database = null;
                            try {
                                database = DataManager.this.mDBHelper.getWritableDatabase();
                                AsyncActionRequest request;
                                while ((request = mQueue.poll()) != null) {
                                    if (request.Action == ACTION_CLEAR) {
                                        clear(database);
                                    } else if (request.ReleaseNumber == DataManager.NO_RELEASE) {  //comics
                                        if (request.Action == DataManager.ACTION_ADD) {
                                            writeComics(database, DataManager.this.getComics(request.ComicsId), true);
                                        } else if (request.Action == DataManager.ACTION_UPD) {
                                            writeComics(database, DataManager.this.getComics(request.ComicsId), false);
                                        } else if (request.Action == DataManager.ACTION_DEL) {
                                            deleteComics(database, request.ComicsId);
                                        }
                                    } else {    //release
                                        if (request.Action == DataManager.ACTION_ADD) {
                                            writeRelease(database, DataManager.this.getComics(request.ComicsId)
                                                    .getRelease(request.ReleaseNumber), true);
                                        } else if (request.Action == DataManager.ACTION_UPD) {
                                            writeRelease(database, DataManager.this.getComics(request.ComicsId)
                                                    .getRelease(request.ReleaseNumber), false);
                                        } else if (request.Action == DataManager.ACTION_DEL) {
                                            deleteRelease(database, request.ComicsId, request.ReleaseNumber);
                                        }
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
                    } catch (InterruptedException iex) {
                        Utils.e("async save main loop", iex);
                    }
                }
            }).start();
        }

        private void writeComics(SQLiteDatabase database, Comics comics, boolean isNew) {
            if (isNew) {
                long res = database.insert(DBHelper.ComicsTable.NAME, null,
                        DBHelper.ComicsTable.getContentValues(comics, DataManager.this.mUserName));
//                Utils.d(this.getClass(), "A0049 write new comics " + comics.getName() + " -> " + (res >= 0));
                for (Release release : comics.getReleases()) {
                    writeRelease(database, release, true);
                }
            } else {
                long res = database.replace(DBHelper.ComicsTable.NAME, null,
                        DBHelper.ComicsTable.getContentValues(comics, DataManager.this.mUserName));
//                Utils.d(this.getClass(), "A0049 write comics " + comics.getName() + " -> " + (res >= 0));
            }
        }

        private void deleteComics(SQLiteDatabase database, long comicsId) {
            int res1 = database.delete(DBHelper.ReleasesTable.NAME,
                    DBHelper.ReleasesTable.COL_COMICS_ID + " = " + comicsId + " and " +
                            DBHelper.ReleasesTable.COL_USER + " = '" + DataManager.this.mUserName + "'",
                    null);
            int res2 = database.delete(DBHelper.ComicsTable.NAME,
                    DBHelper.ComicsTable.COL_ID + " = " + comicsId + " and " +
                        DBHelper.ComicsTable.COL_USER + " = '" + DataManager.this.mUserName + "'",
                    null);
//            Utils.d(this.getClass(), "A0049 delete comics " + comicsId + " -> " + res2 + "(" + res1 + ")");
        }

        private void writeRelease(SQLiteDatabase database, Release release, boolean isNew) {
            if (isNew) {
                long res = database.insert(DBHelper.ReleasesTable.NAME, null,
                        DBHelper.ReleasesTable.getContentValues(release, DataManager.this.mUserName));
//                Utils.d(this.getClass(), "A0049 write new release " + release.getNumber() + " -> " + (res >= 0));
            } else {
                long res = database.replace(DBHelper.ReleasesTable.NAME, null,
                        DBHelper.ReleasesTable.getContentValues(release, DataManager.this.mUserName));
//                Utils.d(this.getClass(), "A0049 write comics " + release.getNumber() + " -> " + (res >= 0));
            }
        }

        private void deleteRelease(SQLiteDatabase database, long comicsId, int releaseNumber) {
            int res = database.delete(DBHelper.ReleasesTable.NAME,
                    DBHelper.ReleasesTable.COL_COMICS_ID + " = " + comicsId + " and " +
                            DBHelper.ReleasesTable.COL_USER + " = '" + DataManager.this.mUserName + "' and " +
                            DBHelper.ReleasesTable.COL_NUMBER + " = " + releaseNumber,
                    null);
//            Utils.d(this.getClass(), "A0049 delete release " + releaseNumber + " -> " + res);
        }

        private void clear(SQLiteDatabase database) {
            int res1 = database.delete(DBHelper.ReleasesTable.NAME,
                    DBHelper.ReleasesTable.COL_USER + " = '" + DataManager.this.mUserName + "'",
                    null);
            int res2 = database.delete(DBHelper.ComicsTable.NAME,
                    DBHelper.ComicsTable.COL_USER + " = '" + DataManager.this.mUserName + "'",
                    null);
//            Utils.d(this.getClass(), "A0049 clear -> " + res2 + "(" + res1 + ")");
        }

    }

}
