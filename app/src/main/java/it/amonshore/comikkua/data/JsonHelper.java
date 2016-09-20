package it.amonshore.comikkua.data;

import android.text.TextUtils;
import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by narsenico on 12/08/16.
 */
public class JsonHelper {

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
    private final static String FIELD_CATEGORIES = "categories";

    private long mLastComicsId;
    private final SimpleDateFormat mDateFormat;

    public JsonHelper() {
        mLastComicsId = System.currentTimeMillis();
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    /**
     *
     * @param obj
     * @return
     * @throws JSONException
     */
    public Comics json2comics(JSONObject obj) throws JSONException {
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
        JSONArray arrReleases = obj.optJSONArray(FIELD_RELEASES);
        if (arrReleases != null) {
            for (int ii = 0; ii < arrReleases.length(); ii++) {
                comics.putRelease(json2release(comics.createRelease(false), arrReleases.getJSONObject(ii)));
            }
        }

        return comics;
    }

    private Release json2release(Release release, JSONObject obj) throws JSONException {
        release.setNumber(obj.getInt(FIELD_NUMBER));
        release.setDate(tryGetDate(obj, FIELD_DATE));
        release.setPrice(tryGetDouble(obj, FIELD_PRICE));
//        release.setReminder(tryGetBoolean(obj, FIELD_REMINDER));
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

    /**
     *
     * @param comics    comics
     * @param includeReleases   se true verranno incluse le uscite (proprietà "releases")
     * @return  una istanza di JSONObject
     * @throws JSONException
     * @throws IOException
     */
    public JSONObject comics2json(Comics comics, boolean includeReleases) throws JSONException, IOException {
        final StringWriter stringWriter = new StringWriter();
        final JsonWriter writer = new JsonWriter(stringWriter);
        try {
            writeJson(writer, comics, includeReleases);
            return (JSONObject) new JSONTokener(stringWriter.toString()).nextValue();
        } finally {
            try { writer.close(); } catch (IOException ioex) { }
        }
    }

    /**
     *
     * @param comicsIterable    lista di comics
     * @param includeReleases   se true verranno incluse le uscite (proprietà "releases")
     * @return  una istanza di JSONArray
     * @throws JSONException
     * @throws IOException
     */
    public JSONArray comics2json(Iterable<Comics> comicsIterable, boolean includeReleases) throws JSONException, IOException {
        final StringWriter stringWriter = new StringWriter();
        final JsonWriter writer = new JsonWriter(stringWriter);
        try {
            writeJson(writer, comicsIterable, includeReleases);
            return (JSONArray) new JSONTokener(stringWriter.toString()).nextValue();
        } finally {
            try { writer.close(); } catch (IOException ioex) { }
        }
    }

    /**
     *
     * @param release   release
     * @return  una istanza di JSONObject
     * @throws JSONException
     * @throws IOException
     */
    public JSONObject release2json(Release release) throws JSONException, IOException {
        final StringWriter stringWriter = new StringWriter();
        final JsonWriter writer = new JsonWriter(stringWriter);
        try {
            writeJson(writer, release);
            return (JSONObject) new JSONTokener(stringWriter.toString()).nextValue();
        } finally {
            try { writer.close(); } catch (IOException ioex) { }
        }
    }

    /**
     *
     * @param releaseIterable   una lista di release
     * @return  una istanza di JSONArray
     * @throws JSONException
     * @throws IOException
     */
    public JSONArray releases2json(Iterable<Release> releaseIterable) throws JSONException, IOException {
        final StringWriter stringWriter = new StringWriter();
        final JsonWriter writer = new JsonWriter(stringWriter);
        try {
            writeJson(writer, releaseIterable);
            return (JSONArray) new JSONTokener(stringWriter.toString()).nextValue();
        } finally {
            try { writer.close(); } catch (IOException ioex) { }
        }
    }

    /**
     *
     * @param writer    writer
     * @param comicsIterable    lista di comics
     * @param includeReleases   se true verranno incluse le uscite (proprietà "releases")
     * @throws IOException
     */
    public void writeJson(JsonWriter writer, Iterable<Comics> comicsIterable, boolean includeReleases) throws IOException {
        writer.beginArray();
        for (Comics comics : comicsIterable) {
            writeJson(writer, comics, includeReleases);
        }
        writer.endArray();
    }

    /**
     *
     * @param writer    writer
     * @param comics    istanza di Comcis da convertire in JSON
     * @param includeReleases   se true verranno incluse le uscite (proprietà "releases")
     * @throws IOException
     */
    public void writeJson(JsonWriter writer, Comics comics, boolean includeReleases) throws IOException {
        writer.beginObject();
        writer.name(FIELD_ID).value(comics.getId());
        writer.name(FIELD_NAME).value(comics.getName());
        writer.name(FIELD_SERIES).value(comics.getSeries());
        writer.name(FIELD_PUBLISHER).value(comics.getPublisher());
        writer.name(FIELD_AUTHORS).value(comics.getAuthors());
        writer.name(FIELD_PRICE).value(comics.getPrice());
        writer.name(FIELD_PERIODICITY).value(comics.getPeriodicity());
        writer.name(FIELD_RESERVED).value(comics.isReserved() ? TRUE : FALSE);
        writer.name(FIELD_NOTES).value(comics.getNotes());
        if (includeReleases) {
            writer.name(FIELD_RELEASES);
            writer.beginArray();
            for (Release release : comics.getReleases()) {
                writeJson(writer, release);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    public void writeJson(JsonWriter writer, Iterable<Release> releaseIterable) throws IOException {
        writer.beginArray();
        for (Release release : releaseIterable) {
            writeJson(writer, release);
        }
        writer.endArray();
    }

    public void writeJson(JsonWriter writer, Release release) throws IOException {
        writer.beginObject();
        writer.name(FIELD_ID).value(release.getComicsId());
        writer.name(FIELD_NUMBER).value(release.getNumber());
        writer.name(FIELD_DATE);
        if (release.getDate() == null) {
            writer.nullValue();
        } else {
            writer.value(mDateFormat.format(release.getDate()));
        }
        writer.name(FIELD_PRICE).value(release.getPrice());
//        writer.name(FIELD_REMINDER).value(release.isReminder() ? TRUE : FALSE);
        writer.name(FIELD_ORDERED).value(release.isOrdered() ? TRUE : FALSE);
        writer.name(FIELD_PURCHASED).value(release.isPurchased() ? TRUE : FALSE);
        writer.name(FIELD_NOTES).value(release.getNotes());
        writer.endObject();
    }

}