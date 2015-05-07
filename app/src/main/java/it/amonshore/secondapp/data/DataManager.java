package it.amonshore.secondapp.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Calgia on 07/05/2015.
 */
public class DataManager {

    public static List<Comics> getComics() {
        ArrayList<Comics> list = new ArrayList<>();
        for (int ii=1; ii<=100; ii++) {
            list.add(new Comics("Item " + ii));
        }
        return list;
    }

}
