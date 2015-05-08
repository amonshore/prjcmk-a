package it.amonshore.secondapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import it.amonshore.secondapp.data.Comics;

/**
 * Created by Calgia on 07/05/2015.
 */
public class ComicsListAdapter extends BaseAdapter {

    //TODO deve implementare la logica di ordinamento dei dati
    //  getItem e getItemId devono ritornare il valore di un elenco ordinato

    public final static int ORDER_ASC = 0;
    public final static int ORDER_DESC = 1;
    public final static int ORDER_BY_NAME = 2;
    public final static int ORDER_BY_BEST_RELEASE = 4;

    private Context context; //TODO a che serve?
    private ArrayList<Comics> items;
    private Comparator<Comics> comparator;
    private int order;

    public ComicsListAdapter(Context context, int order) {
        this.context = context;
        this.items = new ArrayList<>();
        setOrder(order);
    }

    /**
     *
     * @return
     */
    public int getOrder() {
        return order;
    }

    /**
     *
     * @param order
     */
    public void setOrder(int order) {
        if (order != this.order) {
            this.order = order;
            //TODO impostare il comparator in base all'ordine
        }
    }

    /**
     *
     * @param comics
     * @return ritorna l'indice dell'elemento
     */
    public int insertOrUpdate(Comics comics) {
        //TODO
        int index = Collections.binarySearch(items, comics, comparator);
        return -1;
    }

    public void clear() {
        items.clear();
    }

    public Comics[] getComics() {
        return items.toArray(new Comics[items.size()]);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            //convertView = LayoutInflater.from(context).inflate(R.layout.list_comics_item, null);
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_activated_2, null);
        }

        Comics comics = (Comics)getItem(position);
        //TextView txtComicsName = (TextView)convertView.findViewById(R.id.txt_comics_name);
        //txtComicsName.setText(comics.getName());

        ((TextView)convertView.findViewById(android.R.id.text1)).setText(comics.getName());
        ((TextView)convertView.findViewById(android.R.id.text2)).setText("notes");

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }
}
