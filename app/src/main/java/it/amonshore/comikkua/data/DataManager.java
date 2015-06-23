package it.amonshore.comikkua.data;

import android.content.Context;
import android.database.Observable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.Utils;

/**
 * Created by Calgia on 07/05/2015.
 */
public class DataManager extends Observable<ComicsObserver> {

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

    private final static String FILE_NAME = "data.json";
    //private final static String FILE_NAME = "USER_backup.json";
    private final static String FILE_BACKUP = "data.bck";
    private final static String FIELD_ID = "id";
    private final static String FIELD_NAME = "name";
    private final static String FIELD_SERIES = "series";
    private final static String FIELD_PUBLISHER = "publisher";
    private final static String FIELD_AUTHORS = "authors";
    private final static String FIELD_PRICE = "price";
    private final static String FIELD_PERIODICITY = "periodicity";
    private final static String FIELD_RESERVED = "reserved";
    private final static String FIELD_NOTES = "notes";
    private final static String FIELD_RELEASES = "releases";
    private final static String FIELD_NUMBER = "number";
    private final static String FIELD_DATE = "date";
    private final static String FIELD_REMINDER = "reminder";
    private final static String FIELD_PURCHASED = "purchased";
    private final static String FIELD_ORDERED = "ordered";

    private final static String TRUE = "T";
    private final static String FALSE = "F";

    //
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

    private String mUserName;
    private long mLastComicsId;
    private boolean mExternalStorage;
    private Context mContext;
    private boolean mDataLoaded;
    //
    private TreeMap<Long, Comics> mComicsCache;
    //
    private UndoHelper<Comics> mUndoComics;
    private UndoHelper<Release> mUndoRelease;
    //contiene un elenco di tutti gli editori
    private HashSet<String> mPublishers;
    //contiene la best release per ogni comics
    private TreeMap<Long, ReleaseInfo> mBestReleases;
    private SimpleDateFormat mDateFormat;
    //
    private AsyncWriteHandler mWriteHandler;

    private DataManager(Context context, String userName) {
        mContext = context;
        mUserName = userName;
        mLastComicsId = System.currentTimeMillis();
        //controllo che la memoria esternza sia disponibile
        mExternalStorage = isExternalStorageWritable();
        Utils.d("isExternalStorageWritable " + mExternalStorage);
        //date format non localizzata
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        //
        mUndoComics = new UndoHelper<>();
        mUndoRelease = new UndoHelper<>();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getDataFile() {
        String fileName = Utils.nvl(mUserName, "") + FILE_NAME;
        if (mExternalStorage) {
            return new File(mContext.getExternalFilesDir(null), fileName);
        } else {
            return new File(mContext.getFilesDir(), fileName);
        }
    }

    private File getBackupDataFile() {
        String fileName = Utils.nvl(mUserName, "") + FILE_BACKUP;
        if (mExternalStorage) {
            return new File(mContext.getExternalFilesDir(null), fileName);
        } else {
            return new File(mContext.getFilesDir(), fileName);
        }
    }

    private void parseJSON(String json) {
        try {
            //TODO attualmente i dati sono strutturati come un array, modificarlo in modo che la struttura sia questa { lastUpdate: <timestamp>, data: <array> }
            JSONArray arr = (JSONArray) new JSONTokener(json).nextValue();
            for (int ii = 0; ii < arr.length(); ii++) {
                JSONObject obj = arr.getJSONObject(ii);
                Comics comics = json2comics(obj);
                mComicsCache.put(comics.getId(), comics);
                putPublisher(comics.getPublisher());
                updateBestRelease(comics.getId());
            }
            //TODO parse json
        } catch (JSONException jsonex) {
            Utils.e("parseComics", jsonex);
        }
    }

    private Comics json2comics(JSONObject obj) throws JSONException {
        Comics comics = new Comics();
        //nella versione ionic l'id è una stringa, devo convertirla in long
        comics.setId(tryGetId(obj));
        comics.setName(obj.getString(FIELD_NAME));
        comics.setSeries(tryGetString(obj, FIELD_SERIES));
        comics.setPublisher(tryGetString(obj, FIELD_PUBLISHER));
        comics.setAuthors(tryGetString(obj, FIELD_AUTHORS));
        comics.setPrice(tryGetDouble(obj, FIELD_PRICE));
        comics.setPeriodicity(tryGetString(obj, FIELD_PERIODICITY));
        comics.setReserved(tryGetBoolean(obj, FIELD_RESERVED));
        comics.setNotes(tryGetString(obj, FIELD_NOTES));
        //prelevo le release
        JSONArray arrReleases = obj.getJSONArray(FIELD_RELEASES);
        for (int ii = 0; ii < arrReleases.length(); ii++) {
            comics.putRelease(json2release(comics.createRelease(false), arrReleases.getJSONObject(ii)));
        }

        return comics;
    }

    private Release json2release(Release release, JSONObject obj) throws JSONException {
        release.setNumber(obj.getInt(FIELD_NUMBER));
        release.setDate(tryGetDate(obj, FIELD_DATE));
        release.setPrice(tryGetDouble(obj, FIELD_PRICE));
        release.setReminder(tryGetBoolean(obj, FIELD_REMINDER));
        release.setOrdered(tryGetBoolean(obj, FIELD_ORDERED));
        release.setPurchased(tryGetBoolean(obj, FIELD_PURCHASED));
        release.setNotes(tryGetString(obj, FIELD_NOTES));
        return release;
    }

    private long tryGetId(JSONObject obj) {
        try {
            return obj.getLong(FIELD_ID);
        } catch (JSONException jsonex) {
            return (++mLastComicsId) * -1;
        }
    }

    private String tryGetString(JSONObject obj, String field) throws JSONException {
        return obj.isNull(field) ? null : obj.getString(field);
    }

    private double tryGetDouble(JSONObject obj, String field) throws JSONException {
        return obj.isNull(field) ? 0.0d : obj.getDouble(field);
    }

    private boolean tryGetBoolean(JSONObject obj, String field) throws JSONException {
        String str = obj.optString(field);
        return !FALSE.equals(str) && (TRUE.equals(str) || obj.optBoolean(field, false));
    }

    private Date tryGetDate(JSONObject obj, String field) throws JSONException {
        if (obj.isNull(field)) {
            return null;
        } else {
            String str = obj.getString(field);
            if (TextUtils.isEmpty(str)) {
                return null;
            } else {
                try {
                    return mDateFormat.parse(str);
                } catch (ParseException pex) {
                    //Utils.e("DataManager.tryGateDate " + str, pex);
                    return null;
                }
            }
        }
    }

    private final Object mSyncObj = new Object();

    private void writeJson(JsonWriter writer, Comics comics) throws IOException {
        writer.beginObject();
        writer.name(FIELD_ID).value(comics.getName());
        writer.name(FIELD_NAME).value(comics.getName());
        writer.name(FIELD_SERIES).value(comics.getSeries());
        writer.name(FIELD_PUBLISHER).value(comics.getPublisher());
        writer.name(FIELD_AUTHORS).value(comics.getAuthors());
        writer.name(FIELD_PRICE).value(comics.getPrice());
        writer.name(FIELD_PERIODICITY).value(comics.getPeriodicity());
        writer.name(FIELD_RESERVED).value(comics.isReserved() ? TRUE : FALSE);
        writer.name(FIELD_NOTES).value(comics.getNotes());
        writer.name(FIELD_RELEASES);
        writer.beginArray();
        for (Release release : comics.getReleases()) {
            writeJson(writer, release);
        }
        writer.endArray();
        writer.endObject();
    }

    private void writeJson(JsonWriter writer, Release release) throws IOException {
        writer.beginObject();
        writer.name(FIELD_NUMBER).value(release.getNumber());
        writer.name(FIELD_DATE);
        if (release.getDate() == null) {
            writer.nullValue();
        } else {
            writer.value(mDateFormat.format(release.getDate()));
        }
        writer.name(FIELD_PRICE).value(release.getPrice());
        writer.name(FIELD_REMINDER).value(release.isReminder() ? TRUE : FALSE);
        writer.name(FIELD_ORDERED).value(release.isOrdered() ? TRUE : FALSE);
        writer.name(FIELD_PURCHASED).value(release.isPurchased() ? TRUE : FALSE);
        writer.name(FIELD_NOTES).value(release.getNotes());
        writer.endObject();
    }

    private void save() {
        if (!isDataLoaded())
            return;

        synchronized (mSyncObj) {
            FileOutputStream fos = null;
            try {
                File file = getDataFile();
                Utils.d(this.getClass(), "start writing...");
                fos = new FileOutputStream(file);
                JsonWriter writer = new JsonWriter(new OutputStreamWriter(fos, "UTF-8"));
                writer.setIndent("  ");
                writer.beginArray();
                for (Long comicsId : mComicsCache.keySet()) {
                    writeJson(writer, mComicsCache.get(comicsId));
                }
                writer.endArray();
                writer.close();
                fos = null;
                Utils.d(this.getClass(), "... end writing");
            } catch (IOException ioex) {
                Utils.e("save data", ioex);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioex) {
                        Utils.e(this.getClass(), "save", ioex);
                    }
                }
            }
        }
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
     * @param id
     * @return  ritorna l'istanza di Comics con l'id specificato o null se non viene trovata
     */
    public Comics getComics(long id) {
        return mComicsCache.get(id);
    }

    /**
     *
     * @param name
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
     * @param comics
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
     * @param id
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
     * @param comicsId
     * @param number
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
     * @return
     */
    public String[] getPublishers() {
        return mPublishers.toArray(new String[mPublishers.size()]);
    }

    /**
     *
     * @param id
     * @return
     */
    public ReleaseInfo updateBestRelease(long id) {
        Comics comics = getComics(id);
        ReleaseInfo ri = ComicsBestReleaseHelper.getComicsBestRelease(comics);
        mBestReleases.put(comics.getId(), ri);
        return ri;
    }

    /**
     *
     * @param id
     * @return
     */
    public ReleaseInfo getBestRelease(long id) {
        return mBestReleases.get(id);
    }

    /**
     * Legge i dati
     *
     * @return  numero di comics letti
     */
    public int readComics() {
        if (mComicsCache == null || !mDataLoaded) {
            synchronized (mSyncObj) {
                BufferedReader br = null;
                File file = getDataFile();
                mComicsCache = new TreeMap<>();
                mPublishers = new HashSet<>();
                mBestReleases = new TreeMap<>();
                Utils.d("readComics " + file.getAbsolutePath());
                if (file.exists()) {
                    try {
                        StringBuffer sb = new StringBuffer();
                        br = new BufferedReader(new FileReader(file));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                        }
                        if (sb.length() > 0) {
                            parseJSON(sb.toString());
                        } else {
                            Utils.w(FILE_NAME + " is empty");
                        }
                    } catch (IOException ioex) {
                        Utils.e("readComics", ioex);
                    } finally {
                        if (br != null) try {
                            br.close();
                        } catch (IOException ioex) {
                        }
                    }
                }
                mDataLoaded = true;
            }
        }
        return mComicsCache.size();
    }

    /**
     * @return  true se il backup è statao create con successo
     */
    public boolean createBackup() {
        try {
            Utils.d(this.getClass(), "create backup");
            Utils.copyFile(getDataFile(), getBackupDataFile());
            return true;
        } catch (IOException ioex) {
            Utils.e(this.getClass(), "createBackup", ioex);
        }
        return false;
    }

    /**
     *
     * @return
     */
    public boolean restoreBackup() {
        try {
            Utils.d(this.getClass(), "restore backup");
            Utils.copyFile(getBackupDataFile(), getDataFile());
            mDataLoaded = false;
            return true;
        } catch (IOException ioex) {
            Utils.e(this.getClass(), "restoreBackup", ioex);
        }
        return false;
    }

    /**
     *
     * @return
     */
    public long getLastModifiedBackupFile() {
        return getBackupDataFile().lastModified();
    }

    /**
     *
     * @return
     */
    public Uri getDataFileUri() {
        return Uri.fromFile(getDataFile());
    }

    /**
     * Salva i dati
     */
    public void writeComics() {
        writeComics(false);
    }

    /**
     * Salva i dati
     *
     * @param flush se true i dati verranno salvati subito
     */
    public void writeComics(boolean flush) {
        if (mWriteHandler != null) {
            mWriteHandler.appendRequest(flush);
        }
    }

    /**
     *
     * @return
     */
    public boolean isDataLoaded() {
        return mDataLoaded;
    }

    /**
     *
     * @param cause
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
     * @param cause
     * @param source
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
     * @return
     */
    public UndoHelper<Comics> getUndoComics() {
        return mUndoComics;
    }

    /**
     *
     * @return
     */
    public UndoHelper<Release> getUndoRelease() {
        return mUndoRelease;
    }

    private void dispose() {
        unregisterAll();
        stopWriteHandler();
    }

    private class AsyncWriteHandler {

        private Semaphore mMainLoopHandler;
        private Semaphore mNoLongerHandler;
        private long mTimeout = 1000;
        private boolean mCancel;
        private boolean mFlush;
        private boolean mHasPendingRequest;

        public boolean hasPendingRequest() {
            return mHasPendingRequest;
        }

        public void appendRequest() {
            appendRequest(false);
        }

        public void appendRequest(boolean flush) {
            Utils.d(this.getClass(), "appendRequest " + flush);
            mFlush = flush;
            mMainLoopHandler.release();
            mNoLongerHandler.release();
        }

        public void cancel() {
            Utils.d(this.getClass(), "cancel");
            mCancel = true ;
            appendRequest();
        }

        public void start() {
            mCancel = false;
            mMainLoopHandler = new Semaphore(0);
            mNoLongerHandler = new Semaphore(0);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!mCancel) {
                            //
                            mMainLoopHandler.drainPermits();
                            //attendo un tempo indefinito
                            mMainLoopHandler.acquire();
//                            Utils.d(this.getClass(), "*** aquired");
                            //finchè ci sono richieste ciclo
                            mHasPendingRequest = true;
                            while (!mFlush && !mCancel && mNoLongerHandler.tryAcquire(mTimeout, TimeUnit.MILLISECONDS)) {
                            }
                            mFlush = false;
                            Utils.d(this.getClass(), "*** saving");
                            //quando scade salvo
                            DataManager.this.save();
                            mHasPendingRequest = false;
                        }
                    } catch (InterruptedException iex) {
                        Utils.e("async save main loop", iex);
                    }
                }
            }).start();
        }
    }

}
