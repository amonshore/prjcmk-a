package it.amonshore.secondapp;

import android.text.format.Time;
import android.util.Log;

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
     * Log.d(LOG_TAG, msg)
     *
     * @param msg
     */
    public static void d(String msg) {
        Log.d(LOG_TAG, msg);
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

}
