package it.amonshore.secondapp.data;

/**
 * Created by Narsenico on 14/05/2015.
 */
public class Utils {

    public static int indexOf(String[] array, String value, int def) {
        for (int ii = 0; ii < array.length; ii++) {
            if (array[ii].equals(value)) return ii;
        }
        return def;
    }

}
