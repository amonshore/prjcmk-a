package it.amonshore.secondapp.ui.comics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.Release;

/**
 * Created by Calgia on 07/05/2015.
 *
 * L'id di ogni elemento della lista è dato da Comics.getId()
 */
public class ComicsListAdapter extends BaseAdapter {

    public final static int ORDER_BY_NAME = 2;
    public final static int ORDER_BY_BEST_RELEASE = 4;

    private Context mContext;
    private DataManager mDataManager;
    private ArrayList<Long> mSortedIds;
    private Comparator<Long> mComparator;
    private int mOrder;
    private SimpleDateFormat mDateFormat;

    public ComicsListAdapter(Context context, int order) {
        mContext = context;
        mDataManager = DataManager.getDataManager(context);
        mSortedIds = new ArrayList<>();
        mDateFormat = new SimpleDateFormat("c dd MMM", Locale.getDefault());
        setOrder(order);
    }

    /**
     *
     * @return  ritorna l'ordinmaneto usato
     */
    public int getOrder() {
        return mOrder;
    }

    /**
     *
     * @param order
     */
    public void setOrder(int order) {
        if (order != mOrder) {
            mOrder = order;
            //TODO impostare il mComparator in base all'ordine
            if (order == ORDER_BY_NAME) {
                mComparator = new NameComparator();
            } else {
                mComparator = new ReleaseComparator();
            }
            Collections.sort(mSortedIds, mComparator);
        }
    }

    /**
     *
     * @param comics
     * @return ritorna la posizione dell'elemento
     */
    public int insertOrUpdate(Comics comics) {
        if (mDataManager.put(comics)) {
            //è un nuovo elemento
            mSortedIds.add(comics.getId());
            Collections.sort(mSortedIds, mComparator);
            return mSortedIds.indexOf(comics.getId());
        } else {
            //è un elemento già esistente
            Collections.sort(mSortedIds, mComparator);
            return mSortedIds.indexOf(comics.getId());
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
        if (mDataManager.remove(id)) {
            mSortedIds.remove(id);
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
        mSortedIds.clear();
        mSortedIds.addAll(mDataManager.getComics());
        Collections.sort(mSortedIds, mComparator);
        return mSortedIds.size();
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
        return mSortedIds.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //TODO usare un view holder
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_comics_item, null);
            //convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_activated_2, null);
        }

        Comics comics = (Comics)getItem(position);
        Release bestRelease = mDataManager.getBestRelease(comics.getId());
        if (bestRelease != null) {
            String relDate = "";
            if (bestRelease.getDate() != null) {
                relDate = mDateFormat.format(bestRelease.getDate());
            }
            ((TextView) convertView.findViewById(R.id.txt_list_comics_best_release))
                    .setText(String.format("#%s - %s - p %s", bestRelease.getNumber(), relDate, bestRelease.isPurchased()));
        } else {
            ((TextView) convertView.findViewById(R.id.txt_list_comics_best_release)).setText("");
        }
        ((TextView)convertView.findViewById(R.id.txt_list_comics_name)).setText(comics.getName());
        ((TextView)convertView.findViewById(R.id.txt_list_comics_publisher)).setText(comics.getPublisher());

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return mSortedIds.get(position);
    }

    @Override
    public Object getItem(int position) {
        //recupero prima la chiave dell'elemento alla posizione richiesta
        return mDataManager.getComics(mSortedIds.get(position));
    }

    private class NameComparator implements Comparator<Long> {

        @Override
        public int compare(Long lhs, Long rhs) {
            //recupero gli elementi in modo da comparare il nome
            Comics lco = ComicsListAdapter.this.mDataManager.getComics(lhs);
            Comics rco = ComicsListAdapter.this.mDataManager.getComics(rhs);
            return lco.getName().compareToIgnoreCase(rco.getName());
        }
    }

    private final static Release emptyRelease = new Release(0) {
        @Override
        public int getNumber() {
            return Integer.MAX_VALUE;
        }
    };

    private class ReleaseComparator implements Comparator<Long> {

        @Override
        public int compare(Long lhs, Long rhs) {
            Comics lco = ComicsListAdapter.this.mDataManager.getComics(lhs);
            Comics rco = ComicsListAdapter.this.mDataManager.getComics(rhs);
            Release lre = mDataManager.getBestRelease(lco.getId());
            Release rre = mDataManager.getBestRelease(rco.getId());

            if (lre == null) lre = emptyRelease;
            if (rre == null) rre = emptyRelease;

            if (lre.getDate() != null && rre.getDate() != null) {
                return lre.getDate().compareTo(rre.getDate());
            } else if (lre.getDate() != null && rre.getDate() == null) {
                return -1;
            } else if (lre.getDate() == null && rre.getDate() != null) {
                return 1;
            } else if (lre.getNumber() == rre.getNumber()) {
                return lco.getName().compareToIgnoreCase(rco.getName());
            } else {
                return lre.getNumber() - rre.getNumber();
            }
        }
    }
}
