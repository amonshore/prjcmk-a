package it.amonshore.secondapp.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Calgia on 07/05/2015.
 */
public class DataManager {

    private final static String LOG_TAG = "DMA";
    private final static String FILE_NAME = "data.json";
    //
    private static DataManager instance;

    /**
     *
     * @return
     */
    public static DataManager getDataManager(Context context) {
        if (instance == null || instance.mContext != context) {
            Log.d(LOG_TAG, "getDataManager " + context);
            instance = new DataManager(context);
        }

        return instance;
    }

    private long mLastId;
    private boolean mExternalStorage;
    private Context mContext;
    private TreeMap<Long, Comics> mComicsCache;

    private DataManager(Context context) {
        mContext = context;
        mLastId = System.currentTimeMillis();
        //controllo che la memoria esternza sia disponibile
        mExternalStorage = isExternalStorageWritable();
        Log.d(LOG_TAG, "isExternalStorageWritable " + mExternalStorage);
//        //TEST
//        for (int ii=1; ii<=5; ii++) {
//            list.add(new Comics(++lastId, "Item " + ii));
//        }
//        //TEST
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

    private TreeMap<Long, Comics> parseJSON(String json) {
        TreeMap<Long, Comics> map = new TreeMap<>();
        try {
            //TODO attualmente i dati sono strutturati come un array, modificarlo in modo che la struttura sia questa { lastUpdate: <timestamp>, data: <array> }
            JSONArray arr = (JSONArray) new JSONTokener(json).nextValue();
            for (int ii = 0; ii < arr.length(); ii++) {
                JSONObject obj = arr.getJSONObject(ii);
                Comics comics = json2object(obj);
                map.put(comics.getId(), comics);
            }
            //TODO parse json
        } catch (JSONException jsonex) {
            Log.e(LOG_TAG, "parseComics", jsonex);
        }
        return map;
    }

    private final static String FIELD_ID = "id";
    private final static String FIELD_NAME = "name";
    private final static String FIELD_SERIES = "series";
    private final static String FIELD_PUBLISHER = "publisher";

    private Comics json2object(JSONObject obj) throws JSONException {
        Comics comics = new Comics();
        //{"id":"94225d77-377c-11e4-8352-bb818c016cd9","name":"Saint Young Men","series":null,"publisher":"J-Pop","authors":null,"price":5.9,"periodicity":"","reserved":"F","notes":"Gesù e Buddha","releases":[{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":5,"date":null,"price":null,"reminder":null,"purchased":"T","_kk":345},{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":4,"date":null,"price":null,"reminder":null,"purchased":"T","_kk":344},{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":3,"date":null,"price":null,"reminder":null,"purchased":"T","_kk":343},{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":2,"date":null,"price":null,"reminder":null,"purchased":"T","_kk":342},{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":1,"date":null,"price":null,"reminder":null,"purchased":"T","_kk":341},{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":6,"date":"2014-12-26","price":5.9,"reminder":null,"ordererd":"F","notes":"","purchased":"T","_kk":9805,"ordered":"F"}],"bestRelease":{"comicsId":"94225d77-377c-11e4-8352-bb818c016cd9","number":6,"date":"2014-12-26","price":5.9,"reminder":null,"ordererd":"F","notes":"","purchased":"T","_kk":9805,"ordered":"F"},"lastUpdate":1425505796979}
        //nella versione ionic l'id è una stringa, devo convertirla in long
        comics.setId(tryGetId(obj));
        comics.setName(obj.getString(FIELD_NAME));
        comics.setSeries(tryGetString(obj, FIELD_SERIES));
        comics.setPublisher(tryGetString(obj, FIELD_PUBLISHER));
        //TODO altri campi
        return comics;
    }

    private long tryGetId(JSONObject obj) {
        try {
            return obj.getLong(FIELD_ID);
        } catch (JSONException jsonex) {
            return (++mLastId) * -1;
        }
    }

    private String tryGetString(JSONObject obj, String field) throws JSONException {
        return obj.isNull(field) ? null : obj.getString(field);
    }

    /**
     *
     * @return
     */
    public long getSafeNewId() {
        //TODO deve ritornare un id univoco, perché verrà usato come identificativo delle View
        return ++mLastId;
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
     * @param comics
     * @return  true se è stato aggiunto, false se ha sostituito un elemento esistente
     */
    public boolean put(Comics comics) {
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
    public int readComics() {
        if (mComicsCache == null) {
            BufferedReader br = null;
            File file = getDataFile();
            Log.d(LOG_TAG, "readComics " + file.getAbsolutePath());
            if (file.exists()) {
                try {
                    StringBuffer sb = new StringBuffer();
                    br = new BufferedReader(new FileReader(file));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }
                    mComicsCache = parseJSON(sb.toString());
                } catch (IOException ioex) {
                    Log.e(LOG_TAG, "readComics", ioex);
                } finally {
                    if (br != null) try {
                        br.close();
                    } catch (IOException ioex) {
                    }
                }
            } else {
                mComicsCache = new TreeMap<>();
            }
        }
        return mComicsCache.size();
    }

    /**
     *
     */
    public void writeComics() {
        //TODO
    }

}
