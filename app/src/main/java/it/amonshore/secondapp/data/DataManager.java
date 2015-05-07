package it.amonshore.secondapp.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Calgia on 07/05/2015.
 */
public class DataManager {

    public static List<Comics> getComics() {
        ArrayList<Comics> list = new ArrayList<>();
        list.add(new Comics("Item 1"));
        list.add(new Comics("Item 2"));
        list.add(new Comics("Item 3"));
        list.add(new Comics("Item 4"));
        return list;
    }

}
