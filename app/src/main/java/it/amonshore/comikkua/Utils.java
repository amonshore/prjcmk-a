package it.amonshore.comikkua;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import hirondelle.date4j.DateTime;

/**
 * Created by Narsenico on 14/05/2015.
 */
public class Utils {

    private final static String LOG_TAG = "COMIKKU-A";

    private static SimpleDateFormat SDF_COMICS;
    private static SimpleDateFormat SDF_RELEASE;
    private static SimpleDateFormat SDF_RELEASE_LONG;
    private static SimpleDateFormat SDF_DB_RELASE;

    public static void init(Context context) {
        SDF_COMICS = new SimpleDateFormat(context.getString(R.string.format_comics_date), Locale.getDefault());
        SDF_RELEASE = new SimpleDateFormat(context.getString(R.string.format_release_date), Locale.getDefault());
        SDF_RELEASE_LONG = new SimpleDateFormat(context.getString(R.string.format_release_longdate), Locale.getDefault());
        SDF_DB_RELASE = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    public static String formatComicsDate(Date date) {
        return SDF_COMICS.format(date);
    }

    public static String formatReleaseDate(Date date) {
        return SDF_RELEASE.format(date);
    }

    public static String formatReleaseLongDate(Date date) {
        return SDF_RELEASE_LONG.format(date);
    }

    public static String formatDbRelease(long date) {
        return formatDbRelease(new Date(date));
    }

    public static String formatDbRelease(Date date) {
        return date == null ? null : SDF_DB_RELASE.format(date);
    }

    /**
     *
     * @param date  data nel formato yyyy-MM-dd
     * @return null se il formato non è corretto
     */
    public static Date parseDbRelease(String date) {
        if (isNullOrEmpty(date)) {
            return null;
        } else {
            try {
                return SDF_DB_RELASE.parse(date);
            } catch (ParseException pex) {
                Log.e("Utils", "Error parsing " + date, pex);
                return null;
            }
        }
    }

    /**
     *
     * @param date  data nel formato yyyy-MM-dd
     * @return conversione della data in millisecondi, oppure 0 se il formato non è corretto
     */
    public static long parseDbReleaseMilliseconds(@NonNull String date) {
        try {
            return SDF_DB_RELASE.parse(date).getTime();
        } catch (ParseException pex) {
            Log.e("Utils", "Error parsing " + date, pex);
            return 0;
        }
    }

    public static int indexOf(String[] array, String value, int def) {
        for (int ii = 0; ii < array.length; ii++) {
            if (array[ii].equals(value)) return ii;
        }
        return def;
    }

    /**
     *
     * @param str
     * @return
     */
    public static boolean isNullOrEmpty(CharSequence str) {
        return (str == null || TextUtils.getTrimmedLength(str) == 0);
    }

    /**
     *
     * @param text
     * @param def
     * @return
     */
    public static String nvl(String text, String def) {
        return isNullOrEmpty(text) ? def : text;
    }

    /**
     * Restituisce la prima stringa non vuota
     *
     * @param texts
     * @return
     */
    public static String nvl(String... texts) {
        for (String text : texts) {
            if (!isNullOrEmpty(text)) return text;
        }
        return null;
    }

    /**
     *
     * @param delimiter
     * @param excludeEmpty
     * @param texts
     * @return
     */
    public static String join(String delimiter, boolean excludeEmpty, CharSequence... texts) {
        if (texts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (CharSequence text: texts) {
            if (excludeEmpty && isNullOrEmpty(text)) continue;

            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(text);
        }
        return sb.toString();
    }

    /**
     *
     * @param separator separatore valori non sequenziali
     * @param sequenceSeparator separatore per sequenze
     * @param values    elenco valori
     * @return  stringa formattata
     */
    public static StringBuffer formatInterval(StringBuffer buffer, String separator, String sequenceSeparator, int... values) {
        if (buffer == null) buffer = new StringBuffer();
        if (values.length == 0) return buffer;

        int last = values[0];
        int count = 0;
        buffer.append(last);
        for (int ii=1; ii<values.length; ii++) {
            if (values[ii] == last+1) {
                last = values[ii];
                count++;
            } else {
                if (count > 0) {
                    buffer.append(sequenceSeparator).append(last);
                }
                last = values[ii];
                count = 0;
                buffer.append(separator).append(last);
            }
        }
        if (count > 0) {
            buffer.append(sequenceSeparator).append(last);
        }

        return buffer;
    }

    /**
     *
     * @param text testo da interpretare
     * @param separator separatore di intervalli
     * @param sequenceSeparator separatore di sequenze
     * @return elenco ordinato di interi
     */
    public static int[] parseInterval(String text, String separator, String sequenceSeparator) {
        TreeSet<Integer> list = new TreeSet<>();
        for (String token : text.split(separator)) {
            String[] range = token.split(sequenceSeparator);
            if (range.length == 1) {
                list.add(Integer.parseInt(range[0].trim()));
            } else if (range.length == 2) {
                int from = Integer.parseInt(range[0].trim());
                int to = Integer.parseInt(range[1].trim());
                do {
                    list.add(from);
                } while (++from <= to);
            }
        }
        return toIntArray(list.iterator(), new int[list.size()]);
    }

    private static int[] toIntArray(Iterator<Integer> src, int[] dst) {
        for (int ii=0; src.hasNext() ;ii++) {
            dst[ii] = src.next();
        }
        return dst;
    }

    /**
     * Log.d(LOG_TAG, msg)
     *
     * @param msg
     */
    public static void d(String msg) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, msg);
    }

    /**
     *
     * @param aClass
     * @param msg
     */
    public static void d(Class aClass, String msg) {
        if (BuildConfig.DEBUG)
            Log.d(aClass.getName(), msg);
    }

    /**
     * Log.w(LOG_TAG, msg)
     *
     * @param msg
     */
    public static void w(String msg) {
        if (BuildConfig.DEBUG)
            Log.w(LOG_TAG, msg);
    }

    /**
     *
     * @param aClass
     * @param msg
     */
    public static void w(Class aClass, String msg) {
        if (BuildConfig.DEBUG)
            Log.w(aClass.getName(), msg);
    }

    /**
     * Log.e(LOG_TAG, msg, err)
     *
     * @param msg
     * @param err
     */
    public static void e (String msg, Throwable err) {
        if (BuildConfig.DEBUG)
            Log.e(LOG_TAG, msg, err);
    }

    /**
     *
     * @param aClass
     * @param msg
     * @param err
     */
    public static void e(Class aClass, String msg, Throwable err) {
        if (BuildConfig.DEBUG)
            Log.e(aClass.getName(), msg, err);
    }

    /**
     * Calcola il primo giorno della settimana
     * @param date
     * @param weekStartOnMonday
     * @return  primo giorno della settimana
     */
    public static DateTime getStartOfWeek(DateTime date, boolean weekStartOnMonday) {
        //0..6 for Sunday..Saturday
        int day = date.getWeekDay() - 1;
        if (weekStartOnMonday) {
            if (day == 0) day = 6;
            else day--;
        }
        return date.minusDays(day);
    }

    /**
     *
     * @return true il thread corrente è quello principale
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

}
