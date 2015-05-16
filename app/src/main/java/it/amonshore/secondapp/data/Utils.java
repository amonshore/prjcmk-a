package it.amonshore.secondapp.data;

import android.util.Log;

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

}
