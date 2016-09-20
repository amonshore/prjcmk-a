package it.amonshore.comikkua.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.Observable;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.evernote.android.job.JobManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hirondelle.date4j.DateTime;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.reminder.ReleaseReminderJobCreator;
import it.amonshore.comikkua.reminder.ReminderEventHelper;

/**
 * Created by Narsenico on 07/05/2015.
 */
public class DataManager extends Observable<ComicsObserver> {

    private final static int ACTION_DATA = 1;
    private final static int ACTION_REMINDER = 1 << 10;

    public final static int ACTION_ADD = ACTION_DATA | 1 << 1;
    public final static int ACTION_UPD = ACTION_DATA | 1 << 2;
    public final static int ACTION_DEL = ACTION_DATA | 1 << 3;
    public final static int ACTION_CLEAR = ACTION_DATA | 1 << 4;
    public final static int ACTION_REMINDER_CLEAR = ACTION_REMINDER | 1 << 11;
    public final static int ACTION_REMINDER_UPDATE = ACTION_REMINDER | 1 << 12;

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
    public static final int CAUSE_CREATED = 1 << 12; // 4096
    public static final int CAUSE_SYNC = 1 << 13;
    public static final int CAUSE_SYNC_READY = CAUSE_SYNC | 1 << 14;
    public static final int CAUSE_SYNC_STARTED = CAUSE_SYNC | 1 << 15;
    public static final int CAUSE_SYNC_REFUSED = CAUSE_SYNC | 1 << 16;
    public static final int CAUSE_SYNC_STOPPED = CAUSE_SYNC | 1 << 17;
    public static final int CAUSE_SYNC_ERROR = CAUSE_SYNC | 1 << 18;

    public static final long NO_COMICS = -1;
    public static final int NO_RELEASE = -1;

    public static final String KEY_PREF_GROUP_BY_MONTH = "pref_group_by_month";
    public static final String KEY_PREF_WEEK_START_ON_MONDAY = "pref_week_start_on_monday";
    private static final String KEY_PREF_LAST_PURCHASED = "pref_last_purchased";
    public static final String KEY_PREF_AUTOFILL_RELEASE = "pref_autofill_release";
    private static final String KEY_PREF_REMINDER = "pref_reminder";
    public static final String KEY_PREF_REMINDER_TIME = "pref_reminder_time";

    private static final String SYNC_DEBUG_HOST = "192.168.0.4:3000";
    private static final String SYNC_PROD_HOST = "212.94.138.208:3000";

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
//    private AsyncWriteHandler mWriteHandler;
    //A0038
    private long mLastReadDate;
    //A0049
    private DBHelper mDBHelper;
    //
    private SharedPreferences mPreferences;
    //
    private final Object mSyncObj = new Object();
    //A0058
    private DataEventHelper mDataEventHelper;
    //A0033
    private ReminderEventHelper mReminderEventHelper;
    private boolean mIsReminderEnabled = false;
    //A0068
    private SyncEventHelper mSyncEventHelper;
    private boolean mIsSyncEnabled = false;

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
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        mIsReminderEnabled = getPreference(KEY_PREF_REMINDER, false);
        //
        mDBHelper = new DBHelper(context);
        //creo i gestori eventi sui dati e sui reminder
        mDataEventHelper = new DataEventHelper(mContext);
        mReminderEventHelper = new ReminderEventHelper(mContext);
        //A0068 gestore eventi per la sincronizzazione remota dei dati
        mSyncEventHelper = new SyncEventHelper(mContext);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (KEY_PREF_LAST_PURCHASED.equals(key)) {
                        updateBestRelease();
                    } else if (KEY_PREF_REMINDER.equals(key)) {
                        if (sharedPreferences.getBoolean(key, false)) {
                            mIsReminderEnabled = true;
                            mReminderEventHelper.send(ACTION_REMINDER_UPDATE);
                        } else {
                            mIsReminderEnabled = false;
                            mReminderEventHelper.send(ACTION_REMINDER_CLEAR);
                        }
                    } else if (mIsReminderEnabled) {
                        if (KEY_PREF_REMINDER_TIME.equals(key)) {
                            mReminderEventHelper.send(ACTION_REMINDER_UPDATE);
                        }
                    }
                }
            };

    private void putPublisher(String publisher) {
        if (publisher != null && TextUtils.getTrimmedLength(publisher) > 0) {
            mPublishers.add(publisher);
        }
    }

    public SharedPreferences getPreferenceManager() {
        return mPreferences;
    }

    public boolean getPreference(String key, boolean def) {
        return mPreferences.getBoolean(key, def);
    }

    public int getPreference(String key, int def) {
        return mPreferences.getInt(key, def);
    }

    public long getPreference(String key, long def) {
        return mPreferences.getLong(key, def);
    }

    public String getPreference(String key, String def) {
        return mPreferences.getString(key, def);
    }

    public String getUserName() {
        return mUserName;
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
     * @return la lista di comics non modificabile
     */
    public Iterable<Comics> getRawComics() {
        return Collections.unmodifiableCollection(mComicsCache.values());
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
    private void updateBestRelease() {
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
        boolean showLastPurchased = mPreferences.getBoolean(KEY_PREF_LAST_PURCHASED, false);
        ReleaseInfo ri = ComicsBestReleaseHelper.getComicsBestRelease(comics, showLastPurchased);
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
     * @return  this
     */
    public DataManager readComics() {
//        if (mComicsCache == null || !mDataLoaded) {
          if (!mDataLoaded) {
            SQLiteDatabase database = null;
            Cursor curComics = null, curReleases = null;
            try {
                database = mDBHelper.getReadableDatabase();
                mComicsCache.clear();
                mPublishers.clear();
                mBestReleases.clear();
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
                    comics.setReserved(DBHelper.TRUE.equals(curComics.getString(DBHelper.ComicsTable.IDX_RESERVED)));
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
                        release.setDate(Utils.parseDbRelease(curReleases.getString(DBHelper.ReleasesTable.IDX_DATE)));
                        release.setPrice(curReleases.getDouble(DBHelper.ReleasesTable.IDX_PRICE));
                        release.setFlags(curReleases.getInt(DBHelper.ReleasesTable.IDX_FLAGS));
                        release.setNotes(curReleases.getString(DBHelper.ReleasesTable.IDX_NOTES));
                        comics.putRelease(release);
                    }
                    curReleases.close();
                    curReleases = null;
                    //
//                    Utils.d(this.getClass(), "A0049 read " + comics.getName() + " -> " + comics.getId());
                    mComicsCache.put(comics.getId(), comics);
                    putPublisher(comics.getPublisher());
                    updateBestRelease(comics.getId());
                }
            } catch (Exception ex) {
                Utils.e(this.getClass(), "Read data", ex);
            } finally {
                if (curComics != null) {
                    curComics.close();
                }
                if (curReleases != null) {
                    curReleases.close();
                }
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
        return this;
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
     * Rimuove i file delle immagini il cui comics è stato rimosso.
     *
     * @param removeAll true rimuove l'immagine senza controllare se il comics esiste
     * @return this
     */
    public DataManager removeDirtyImages(boolean removeAll) {
        final Pattern pattern = Pattern.compile(Comics.IMAGE_PREFIX + "(-?\\d+)\\.jpg");
        final File folder = FileHelper.getExternalFolder(mContext);
        final String[] fileNames = folder.list();
        for (String fileName : fileNames) {
            final Matcher matcher = pattern.matcher(fileName);
            if (matcher.find() && (removeAll || getComics(Long.parseLong(matcher.group(1))) == null)) {
                final File file = new File(folder, fileName);
                if (!file.delete()) {
                    Utils.w(this.getClass(), "Cannot delete " + file.getPath());
                }
            }
        }

        return this;
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
            removeDirtyImages(true); //A0055
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
     * @param action    azione da eseguire sui dati (ACTION_ADD, ACTION_UPD, ACTION_DEL, etc)
     * @param comicsId  id del comics su cui operare l'azione
     * @param releaseNumber numero della release su cui operare l'azione (NO_RELEASE per nessuna)
     * @return  this
     */
    public DataManager updateData(int action, long comicsId, int releaseNumber) {
        //se è un evento di tipo data lo invio al gestore degli eventi
        if ((action & ACTION_DATA) == ACTION_DATA) {
            mDataEventHelper.send(action, comicsId, releaseNumber);
            //A0068 se abilitato invio l'evento anche al gestore della sincronizzazione remota
            if (mIsSyncEnabled) {
                mSyncEventHelper.send(action, comicsId, releaseNumber);
            }
        }
        //invio all'osservatore un nuovo evento da gestire indipendentemente dal tipo
        if (mIsReminderEnabled) {
            mReminderEventHelper.send(action);
        }

        return this;
    }

    /**
     * Attiva la sincronizzazione remota.
     * Se la presentazione va a buon fine verrà inviata la notifica CAUSE_SYNC_READY,
     * seguita da CAUSE_SYNC_START a sincronizzazione attiva.
     * In caso contrario CAUSE_SYNC_REFUSED.
     *
     * @param syncId    codice di sincronizzazione
     * @return this
     */
    public DataManager initRemoteSync(String syncId) {
        if (mIsSyncEnabled) {
            mSyncEventHelper.stop();
        }
        mIsSyncEnabled = false; //riporto a false, sarà messo a true solo a sync avviata con successo
        mSyncEventHelper.applySyncId(getPreference("pref_sync_debugurl", false) ? SYNC_DEBUG_HOST : SYNC_PROD_HOST,
                syncId, new SyncEventHelper.SyncListener() {
            @Override
            public void onResponse(int response) {
                if (response == SyncEventHelper.SYNC_READY) {
                    notifyChanged(CAUSE_SYNC_READY);
                    mIsSyncEnabled = true;
                    mSyncEventHelper.start();
                } else if (response == SyncEventHelper.SYNC_STARTED) {
                    notifyChanged(CAUSE_SYNC_STARTED);
                } else if (response == SyncEventHelper.SYNC_REFUSED) {
                    mIsSyncEnabled = false;
                    // segnalo che il codice di sincronizzazione è stato rifiutato
                    notifyChanged(CAUSE_SYNC_REFUSED);
                } else if (response == SyncEventHelper.SYNC_SENT) {
                    // TODO dati inviati
                } else if (response == SyncEventHelper.SYNC_EXPIRED) {
                    mIsSyncEnabled = false;
                    mSyncEventHelper.stop();
                    // segnalo che la sincronizzazione non è più attiva
                        notifyChanged(CAUSE_SYNC_STOPPED);
                } else {
                    mIsSyncEnabled = false;
                    mSyncEventHelper.stop();
                    // segnalo che la sincronizzazione non è più attiva a causa di un errore
                    notifyChanged(CAUSE_SYNC_ERROR);
                }
            }

            @Override
            public void onDataReceived(Object data) {
                // TODO ho ricevuto nuovi dati... che me ne faccio?
            }
        });

        return this;
    }

    /**
     *
     * @return this
     */
    public DataManager initReminderEngine() {
        //A0033 inizializzo gestore notifiche
        JobManager
                .create(mContext)
                .addJobCreator(new ReleaseReminderJobCreator());

        return this;
    }

    /**
     * @return  this
     */
    public DataManager startWriteHandler() {
        //può capitare che una istanza della MainActivity venga creata quando ne esiste già una
        //quindi gli helper tengono conto di questo fatto e attivano i processi interni una sola
        //volta
        mDataEventHelper.start();
        mReminderEventHelper.start();

        return this;
    }

    /**
     * @return  this
     */
    public DataManager stopWriteHandler() {
        //perché vengano fermati i processi interni deve essere chiamato lo stop tante volte
        //quante volte è stato chiamato start
        mDataEventHelper.stop();
        mReminderEventHelper.stop();
        //A0068
        mSyncEventHelper.stop();

        return this;
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
        mPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        unregisterAll();
        stopWriteHandler();
    }

}
