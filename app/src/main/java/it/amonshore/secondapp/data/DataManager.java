package it.amonshore.secondapp.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Calgia on 07/05/2015.
 */
public class DataManager {

    private static ArrayList<Comics> list = new ArrayList<>();
    static {
        long id = System.currentTimeMillis();
        for (int ii=1; ii<=5; ii++) {
            list.add(new Comics(++id, "Item " + ii));
        }
    }

    public static List<Comics> readComics() {
        return list;
    }

    public static void writeComics(Comics... comics) {
        list.clear();
        for (Comics co : comics)
            list.add(co);
    }

}
