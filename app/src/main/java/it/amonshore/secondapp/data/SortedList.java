package it.amonshore.secondapp.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Narsenico on 19/05/2015.
 *
 * TODO completare
 */
public class SortedList<E> extends ArrayList<E> {

    //private ArrayList<E> mList;
    private Comparator<? super E> mComparator;

    public SortedList() {
        this(null);
    }

    public SortedList(Comparator<? super E> comparator) {
        //mList = new ArrayList<>();
    }

}
