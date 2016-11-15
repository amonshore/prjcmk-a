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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
//        writer.name(FIELD_REMINDER).value(release.isReminder() ? TRUE : FALSE);
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
     * @param context
     * @return
     */
    public static File getExternalFolder(Context context) {
        if (isExternalStorageWritable()) {
            return context.getExternalFilesDir(null);
        } else {
            return context.getFilesDir();
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

    /**
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    public static boolean copyFile(File source, File dest) throws IOException {
        boolean res = false;
        if (source.exists()) {
            FileChannel fcSource = null;
            FileChannel fcDestination = null;
            try {
                fcSource = new FileInputStream(source).getChannel();
                fcDestination = new FileOutputStream(dest).getChannel();
//                if (fcDestination != null && fcSource != null) {
                    fcDestination.transferFrom(fcSource, 0, fcSource.size());
                    res = true;
//                }
            } finally {
                if (fcSource != null) {
                    fcSource.close();
                }
                if (fcDestination != null) {
                    fcDestination.close();
                }
            }
        }
        return res;
    }

//    /**
//     * @param uri The Uri to check.
//     * @return Whether the Uri authority is ExternalStorageProvider.
//     * @author paulburke
//     */
//    public static boolean isExternalStorageDocument(Uri uri) {
//        return "com.android.externalstorage.documents".equals(uri.getAuthority());
//    }
//
//    /**
//     * @param uri The Uri to check.
//     * @return Whether the Uri authority is DownloadsProvider.
//     * @author paulburke
//     */
//    public static boolean isDownloadsDocument(Uri uri) {
//        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
//    }
//
//    /**
//     * @param uri The Uri to check.
//     * @return Whether the Uri authority is MediaProvider.
//     * @author paulburke
//     */
//    public static boolean isMediaDocument(Uri uri) {
//        return "com.android.providers.media.documents".equals(uri.getAuthority());
//    }
//
//    /**
//     * Get the value of the data column for this Uri. This is useful for
//     * MediaStore Uris, and other file-based ContentProviders.
//     *
//     * @param context The context.
//     * @param uri The Uri to query.
//     * @param selection (Optional) Filter used in the query.
//     * @param selectionArgs (Optional) Selection arguments used in the query.
//     * @return The value of the _data column, which is typically a file path.
//     * @author paulburke
//     */
//    public static String getDataColumn(Context context, Uri uri, String selection,
//                                       String[] selectionArgs) {
//
//        Cursor cursor = null;
//        final String column = "_data";
//        final String[] projection = {
//                column
//        };
//
//        try {
//            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
//                    null);
//            if (cursor != null && cursor.moveToFirst()) {
//                final int column_index = cursor.getColumnIndexOrThrow(column);
//                return cursor.getString(column_index);
//            }
//        } finally {
//            if (cursor != null)
//                cursor.close();
//        }
//        return null;
//    }
//
//    public static String getPath(final Context context, final Uri uri) {
//
//        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
//
//        // DocumentProvider
//        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
//            if (isExternalStorageDocument(uri)) {
//                final String docId = DocumentsContract.getDocumentId(uri);
//                final String[] split = docId.split(":");
//                final String type = split[0];
//
//                if ("primary".equalsIgnoreCase(type)) {
//                    return Environment.getExternalStorageDirectory() + "/" + split[1];
//                }
//
//                // TODO handle non-primary volumes
//            }
//            // DownloadsProvider
//            else if (isDownloadsDocument(uri)) {
//
//                final String id = DocumentsContract.getDocumentId(uri);
//                final Uri contentUri = ContentUris.withAppendedId(
//                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
//
//                return getDataColumn(context, contentUri, null, null);
//            }
//            // MediaProvider
//            else if (isMediaDocument(uri)) {
//                final String docId = DocumentsContract.getDocumentId(uri);
//                final String[] split = docId.split(":");
//                final String type = split[0];
//
//                Uri contentUri = null;
//                if ("image".equals(type)) {
//                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//                } else if ("video".equals(type)) {
//                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//                } else if ("audio".equals(type)) {
//                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//                }
//
//                final String selection = "_id=?";
//                final String[] selectionArgs = new String[] {
//                        split[1]
//                };
//
//                return getDataColumn(context, contentUri, selection, selectionArgs);
//            }
//        }
//        // MediaStore (and general)
//        else if ("content".equalsIgnoreCase(uri.getScheme())) {
//            return getDataColumn(context, uri, null, null);
//        }
//        // File
//        else if ("file".equalsIgnoreCase(uri.getScheme())) {
//            return uri.getPath();
//        }
//
//        return null;
//    }
//
//    /**
//     * Convert Uri into File, if possible.
//     *
//     * @return file A local file that the Uri was pointing to, or null if the
//     *         Uri is unsupported.
//     * @see #getPath(Context, Uri)
//     * @author paulburke
//     */
//    public static File getFile(Context context, Uri uri) {
//        if (uri != null) {
//            String path = getPath(context, uri);
//            if (path != null) {
//                return new File(path);
//            }
//        }
//        return null;
//    }
//
//    public static String getRealPathFromURI(Context context, Uri contentUri) {
//        Cursor cursor = null;
//        try {
//            String[] proj = { MediaStore.Images.Media.DATA };
//            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
//            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            cursor.moveToFirst();
//            return cursor.getString(column_index);
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//    }

    public static String getExtension(File file) {
        String ext = null;
        String s = file.getName();
        int ii = s.lastIndexOf('.');
        if (ii > 0 && ii < s.length() - 1) {
            ext = s.substring(ii + 1).toLowerCase();
        }
        return ext;
    }

    public static byte[] readAllBytes(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            int nn;
            byte[] buff = new byte[4096];

            while ((nn = fis.read(buff, 0, buff.length)) > 0) {
                baos.write(buff, 0, nn);
            }
            return baos.toByteArray();
        } finally {
            if (baos != null) {
                baos.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }

    public static byte[] createChecksum(File file) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis =  new FileInputStream(file);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(File file) throws IOException, NoSuchAlgorithmException {
        byte[] b = createChecksum(file);
        String result = "";

        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

}
