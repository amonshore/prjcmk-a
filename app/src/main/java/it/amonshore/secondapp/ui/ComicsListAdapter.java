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

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;

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

    private Context context;
    private DataManager dataManager;
    private ArrayList<Long> sortedIds;
    private Comparator<Long> comparator;
    private int order;

    public ComicsListAdapter(Context context, int order) {
        this.context = context;
        this.dataManager = DataManager.getDataManager(context);
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
            if ((order & ORDER_BY_NAME) == ORDER_BY_NAME) {
                this.comparator = new NameComparator((order & ORDER_DESC) == ORDER_DESC);
            } else {
                this.comparator = new ReleaseComparator((order & ORDER_DESC) == ORDER_DESC);
            }
            Collections.sort(this.sortedIds, this.comparator);
        }
    }

    /**
     *
     * @param comics
     * @return ritorna la posizione dell'elemento
     */
    public int insertOrUpdate(Comics comics) {
        if (this.dataManager.put(comics)) {
            //è un nuovo elemento
            sortedIds.add(comics.getId());
            Collections.sort(this.sortedIds, this.comparator);
            return this.sortedIds.indexOf(comics.getId());
        } else {
            //è un elemento già esistente
            Collections.sort(this.sortedIds, this.comparator);
            return sortedIds.indexOf(comics.getId());
        }
    }

    /**
     *
     * @return
     */
    public Comics createNewComics() {
        return new Comics(this.dataManager.getSafeNewId());
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
        if (this.dataManager.remove(id)) {
            this.sortedIds.remove(id);
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @return
     */
    public int refresh() {
        this.dataManager.readComics();
        this.sortedIds.clear();
        this.sortedIds.addAll(this.dataManager.getComics());
        Collections.sort(this.sortedIds, this.comparator);
        return this.sortedIds.size();
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
        return this.sortedIds.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_comics_item, null);
            //convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_activated_2, null);
        }

        Comics comics = (Comics)getItem(position);
        //Log.d(LOG_TAG, "getView @" + position + " id " + comics.getId() + " " + comics.getName());
        //((TextView)convertView.findViewById(android.R.id.text1)).setText(comics.getName());
        //((TextView)convertView.findViewById(android.R.id.text2)).setText(comics.getPublisher());
        ((TextView)convertView.findViewById(R.id.txt_list_comics_name)).setText(comics.getName());
        ((TextView)convertView.findViewById(R.id.txt_list_comics_publisher)).setText(comics.getPublisher());

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return this.sortedIds.get(position);
    }

    @Override
    public Object getItem(int position) {
        //recupero prima la chiave dell'elemento alla posizione richiesta
        return this.dataManager.getComics(this.sortedIds.get(position));
    }

    private class NameComparator implements Comparator<Long> {

        private boolean desc;
        public NameComparator(boolean desc) {
            this.desc = desc;
        }

        @Override
        public int compare(Long lhs, Long rhs) {
            //recupero gli elementi in modo da comparare il nome
            Comics lco = ComicsListAdapter.this.dataManager.getComics(lhs);
            Comics rco = ComicsListAdapter.this.dataManager.getComics(rhs);
            if (desc) {
                return rco.getName().compareToIgnoreCase(lco.getName());
            } else {
                return lco.getName().compareToIgnoreCase(rco.getName());
            }
        }
    }

    private class ReleaseComparator implements Comparator<Long> {

        private boolean desc;
        public ReleaseComparator(boolean desc) {
            this.desc = desc;
        }

        @Override
        public int compare(Long lhs, Long rhs) {
            //recupero gli elementi in modo da comparare il nome
            Comics lco = ComicsListAdapter.this.dataManager.getComics(lhs);
            Comics rco = ComicsListAdapter.this.dataManager.getComics(rhs);
            //TODO ordinare per release
            if (!desc) {
                return rco.getName().compareToIgnoreCase(lco.getName());
            } else {
                return lco.getName().compareToIgnoreCase(rco.getName());
            }
        }
    }
}
