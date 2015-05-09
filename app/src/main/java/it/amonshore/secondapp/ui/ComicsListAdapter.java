package it.amonshore.secondapp.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import it.amonshore.secondapp.data.Comics;

/**
 * Created by Calgia on 07/05/2015.
 *
 * L'id di ogni elemento della lista è dato da Comics.getId()
 */
public class ComicsListAdapter extends BaseAdapter {

    private final static String LOG_TAG = "CLA";

    public final static int ORDER_ASC = 0;
    public final static int ORDER_DESC = 1;
    public final static int ORDER_BY_NAME = 2;
    public final static int ORDER_BY_BEST_RELEASE = 4;

    private Context context; //TODO a che serve?
    private ArrayList<Long> sortedIds;
    private TreeMap<Long, Comics> items;
    private Comparator<Long> comparator;
    private int order;

    public ComicsListAdapter(Context context, int order) {
        this.context = context;
        this.items = new TreeMap<>();
        this.sortedIds = new ArrayList<>();
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
            Log.d(LOG_TAG, "setOrder " + order);
            this.comparator = new NameComparator();
            Collections.sort(this.sortedIds, this.comparator);
        }
    }

    /**
     *
     * @param comics
     * @return ritorna l'indice dell'elemento o -1 se l'elemento è nuovo
     */
    public int insertOrUpdate(Comics comics) {
        if (items.put(comics.getId(), comics) == null) {
            //è un nuovo elemento
            sortedIds.add(comics.getId());
            Collections.sort(this.sortedIds, this.comparator);
            return -1;
        } else {
            //è un elemento già esistente
            Collections.sort(this.sortedIds, this.comparator);
            return sortedIds.indexOf(comics.getId());
        }
    }

    /**
     *
     * @param comics
     * @return
     */
    public boolean remove(Comics comics) {
        return remove(comics.getId());
    }

    /**
     *
     * @param id
     * @return
     */
    public boolean remove(long id) {
        if (this.items.remove(id) != null) {
            this.sortedIds.remove(id);
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        this.items.clear();
    }

    public Comics[] getComics() {
        return this.items.values().toArray(new Comics[this.items.size()]);
    }

    @Override
    public boolean hasStableIds() {
        //visto che tutti gli id degli elementi non possono cambiare nel tempo
        //  ritorno true, questo fa in modo che ListView.getCheckedItemIds() ritorni
        //  gli id degli elementi checkati (altrimenti non funziona)
        return true;
    }

    @Override
    public int getCount() {
        return this.items.size();
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
        ((TextView)convertView.findViewById(android.R.id.text2)).setText("id: " + comics.getId());

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return this.sortedIds.get(position);
    }

    @Override
    public Object getItem(int position) {
        //recupero prima la chiave dell'elemento alla posizione richiesta
        return this.items.get(this.sortedIds.get(position));
    }

    private class NameComparator implements Comparator<Long> {

        public boolean desc;

        @Override
        public int compare(Long lhs, Long rhs) {
            //recupero gli elementi in modo da comparare il nome
            Comics lco = ComicsListAdapter.this.items.get(lhs);
            Comics rco = ComicsListAdapter.this.items.get(rhs);
            if (desc) {
                return rco.getName().compareToIgnoreCase(lco.getName());
            } else {
                return lco.getName().compareToIgnoreCase(rco.getName());
            }
        }
    }
}
