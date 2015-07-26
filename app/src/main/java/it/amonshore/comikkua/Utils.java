package it.amonshore.comikkua;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import hirondelle.date4j.DateTime;

/**
 * Created by Narsenico on 14/05/2015.
 */
public class Utils {

    public final static String LOG_TAG = "COMIKKU-A";

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
    public final static String join(String delimiter, boolean excludeEmpty, CharSequence... texts) {
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
     * Log.d(LOG_TAG, msg)
     *
     * @param msg
     */
    public static void d(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /**
     *
     * @param aClass
     * @param msg
     */
    public static void d(Class aClass, String msg) {
        Log.d(aClass.getName(), msg);
    }

    /**
     * Log.w(LOG_TAG, msg)
     *
     * @param msg
     */
    public static void w(String msg) {
        Log.w(LOG_TAG, msg);
    }

    /**
     * Log.e(LOG_TAG, msg, err)
     *
     * @param msg
     * @param err
     */
    public static void e (String msg, Throwable err) {
        Log.e(LOG_TAG, msg, err);
    }

    /**
     *
     * @param aClass
     * @param msg
     * @param err
     */
    public static void e(Class aClass, String msg, Throwable err) {
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
     * Calcola l'ultimo giorno della settimana
     * @param date
     * @param weekStartOnMonday
     * @return ultimo giorno della settimana
     */
    public static DateTime getEndOfWeek(DateTime date, boolean weekStartOnMonday) {
        return getStartOfWeek(date, weekStartOnMonday).plusDays(6);
    }

    /**
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyFile(File source, File dest) throws IOException {
        if (source.exists()) {
            try (FileOutputStream fw = new FileOutputStream(dest)) {
                try (FileInputStream fr = new FileInputStream(source)) {
                    byte[] buff = new byte[1024];
                    int nn;
                    while ((nn = fr.read(buff, 0, buff.length)) > 0) {
                        fw.write(buff, 0, nn);
                    }
                }
            }
        }
    }

}
