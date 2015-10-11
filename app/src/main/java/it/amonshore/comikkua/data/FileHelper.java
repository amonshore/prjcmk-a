package it.amonshore.comikkua.data;

import android.content.Context;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.amonshore.comikkua.Utils;

/**
 * Created by Narsenico on 08/10/2015.
 */
public class FileHelper {

    public final static String FILE_NAME = "data.json";
    public final static String FILE_BACKUP = "data.bck";

    private final static String TRUE = "T";
    private final static String FALSE = "F";

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

    private long mLastComicsId;
    private final SimpleDateFormat mDateFormat;

    public FileHelper() {
        mLastComicsId = System.currentTimeMillis();
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    /**
     * Legge il contenuto del file json e crea un elenco di Comics.
     *
     * @param file  file di testo da cui leggere i dati
     * @return elenco di Comics
     */
    public Comics[] importComics(File file) throws IOException {
        Utils.d("import comics from " + file.getAbsolutePath());
        List<Comics> lstComics = new ArrayList<>();
        BufferedReader br = null;
        if (file.exists()) {
            try {
                StringBuilder sb = new StringBuilder();
                br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                if (sb.length() > 0) {
                    try {
                        JSONArray arr = (JSONArray) new JSONTokener(sb.toString()).nextValue();
                        for (int ii = 0; ii < arr.length(); ii++) {
                            lstComics.add(json2comics(arr.getJSONObject(ii)));
                        }
                    } catch (JSONException jsonex) {
                        Utils.e("parseComics", jsonex);
                    }
                } else {
                    Utils.w(file.getName() + " is empty");
                }
            } finally {
                if (br != null) try {
                    br.close();
                } catch (IOException ioex) {
                    //
                }
            }
        }
        return lstComics.toArray(new Comics[lstComics.size()]);
    }

    /**
     *
     * @param file  file in cui verranno esportati i dati in formato json
     * @param comics    elennco di comics da esportare
     */
    public void exportComics(File file, Comics[] comics) throws IOException {
        Utils.d("save comics to " + file.getName());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(fos, "UTF-8"));
            writer.setIndent("  ");
            writer.beginArray();
            for (Comics cc : comics) {
                writeJson(writer, cc);
            }
            writer.endArray();
            writer.close();
            fos = null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioex) {
                    //
                }
            }
        }
    }

    private Comics json2comics(JSONObject obj) throws JSONException {
        //nella versione ionic l'id è una stringa, devo convertirla in long
        Comics comics = new Comics(tryGetId(obj));
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

    /**
     * Restituisce una istanza di file il cui percorso è dato dall'external storage
     * se accessibile in scrittura, altrimenti dalla cartella dell'app interna.
     *
     * @param context contesto
     * @param fileName  nome del file (senza il percorso)
     * @return  una istanza di File
     */
    public static File getExternalFile(Context context, String fileName) {
        if (isExternalStorageWritable()) {
            return new File(context.getExternalFilesDir(null), fileName);
        } else {
            return new File(context.getFilesDir(), fileName);
        }
    }

    /**
     *
     * @param folderType    il tipo della cartella esterna in cui risiede il file (vedi Enviroment.DIRECTORY_xxx)
     * @param fileName  nome del file
     * @return  una istanza di File
     */
    public static File getExternalFile(String folderType, String fileName) {
        return new File(Environment.getExternalStoragePublicDirectory(folderType), fileName);
    }

    /**
     *
     * @return true se l'external storage è accessibile in scrittura
     */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

}
