package it.amonshore.secondapp.data;

import android.content.Context;
import android.database.Observable;
import android.os.Environment;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import it.amonshore.secondapp.Utils;

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

    //private final static String FILE_NAME = "data.json";
    private final static String FILE_NAME = "USER_backup.json";
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

    //
    private static DataManager instance;

    /**
     *
     * @param context   usare Context.getApplicationContext()
     * @return
     */
    public static DataManager getDataManager(Context context) {
        if (instance == null || instance.mContext != context) {
            Utils.d("getDataManager " + context);
            instance = new DataManager(context);
        }

        return instance;
    }

    private long mLastComicsId;
    private boolean mExternalStorage;
    private Context mContext;
    private boolean mDataLoaded;
    //
    private TreeMap<Long, Comics> mComicsCache;
    //contiene un elenco di tutti gli editori
    private HashSet<String> mPublishers;
    //contiene la best release per ogni comics
    private TreeMap<Long, ReleaseInfo> mBestReleases;
    private SimpleDateFormat mDateFormat;

    private DataManager(Context context) {
        mContext = context;
        mLastComicsId = System.currentTimeMillis();
        //controllo che la memoria esternza sia disponibile
        mExternalStorage = isExternalStorageWritable();
        Utils.d("isExternalStorageWritable " + mExternalStorage);
        //date format non localizzata
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private File getDataFile() {
        if (mExternalStorage) {
            return new File(mContext.getExternalFilesDir(null), FILE_NAME);
        } else {
            return new File(mContext.getFilesDir(), FILE_NAME);
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
        if ("F".equals(str))
            return false;
        else if ("T".equals(str))
            return true;
        else
            return obj.optBoolean(field, false);
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

    private void putPublisher(String publisher) {
        if (publisher != null && TextUtils.getTrimmedLength(publisher) > 0) {
            mPublishers.add(publisher);
        }
    }

    /**
     *
     * @return
     */
    public long getSafeNewComicsId() {
        //TODO deve ritornare un id univoco, perché verrà usato come identificativo delle View
        return ++mLastComicsId;
    }

    /**
     *
     * @return
     */
    public Set<Long> getComics() {
        return mComicsCache.keySet();
    }

    /**
     *
     * @param id
     * @return
     */
    public Comics getComics(long id) {
        return mComicsCache.get(id);
    }

    /**
     *
     * @param name
     * @return
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
        putPublisher(comics.getPublisher());
        return (mComicsCache.put(comics.getId(), comics) == null);
    }

    /**
     *
     * @param comics
     * @return  true se l'elemento è stato elmiminato, false se non esisteva
     */
    public boolean remove(Comics comics) {
        return remove(comics.getId());
    }

    /**
     *
     * @param id
     * @return  true se l'elemento è stato elmiminato, false se non esisteva
     */
    public boolean remove(long id) {
        return (mComicsCache.remove(id) != null);
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
     *
     * @return
     */
    public int readComics() {
        if (mComicsCache == null) {
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
                    parseJSON(sb.toString());
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
        return mComicsCache.size();
    }

    /**
     *
     */
    public void writeComics() {
        //TODO
    }

//    private static class ReleasesTreeMap extends TreeMap<ReleaseId, Release> {
//
//        protected long currentComicsId;
//
//    }

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
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                if (source == mObservers.get(i)) continue;
                mObservers.get(i).onChanged(cause);
            }
        }
    }
}
