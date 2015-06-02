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
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.data.ReleaseInfo;

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
        ItemViewHolder holder;

        if (convertView == null) {
            holder = new ItemViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_comics_item, null);
            holder.txtName = (TextView) convertView.findViewById(R.id.txt_list_comics_name);
            holder.txtPublisher = (TextView) convertView.findViewById(R.id.txt_list_comics_publisher);
            holder.txtNotes = (TextView) convertView.findViewById(R.id.txt_list_comics_notes);
            holder.txtDate = (TextView) convertView.findViewById(R.id.txt_list_comics_date);
            holder.txtNumber = (TextView) convertView.findViewById(R.id.txt_list_comics_number);
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        Comics comics = (Comics)getItem(position);
        ReleaseInfo bestReleaseInfo = mDataManager.getBestRelease(comics.getId());
        String bestReleaseNotes = null;
        if (bestReleaseInfo != null) {
            Release bestRelease = bestReleaseInfo.getRelease();
            String relDate = mContext.getString(R.string.placeholder_wishlist);
            if (bestRelease.getDate() != null) {
                relDate = mDateFormat.format(bestRelease.getDate());
            }
            holder.txtNumber.setText(Integer.toString(bestRelease.getNumber()));
            holder.txtDate.setText(relDate);

            switch (bestReleaseInfo.getGroup()) {
                case ReleaseGroupHelper.GROUP_LOST:
                case ReleaseGroupHelper.GROUP_EXPIRED:
                    holder.txtNumber.setBackgroundResource(R.color.comikku_expired_background_color);
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_expired_primary_color));
                    holder.txtDate.setBackgroundResource(R.drawable.border_expired_releasedate);
                    break;
                case ReleaseGroupHelper.GROUP_PERIOD:
                case ReleaseGroupHelper.GROUP_PERIOD_NEXT:
                case ReleaseGroupHelper.GROUP_PERIOD_OTHER:
                case ReleaseGroupHelper.GROUP_TO_PURCHASE:
                case ReleaseGroupHelper.GROUP_PURCHASED:
                    holder.txtNumber.setBackgroundResource(R.color.comikku_to_purchase_background_color);
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_to_purchase_primary_color));
                    holder.txtDate.setBackgroundResource(R.drawable.border_to_purchase_releasedate);
                    break;
                case ReleaseGroupHelper.GROUP_WISHLIST:
                    holder.txtNumber.setBackgroundResource(R.color.comikku_wishlist_background_color);
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_wishlist_primary_color));
                    holder.txtDate.setBackgroundResource(R.drawable.border_wishlist_releasedate);
                    break;
            }

            holder.txtNumber.setVisibility(View.VISIBLE);
            holder.txtDate.setVisibility(View.VISIBLE);
            bestReleaseNotes = bestRelease.getNotes();
        } else {
            holder.txtNumber.setText("");
            holder.txtDate.setText("");
            holder.txtNumber.setVisibility(View.INVISIBLE);
            holder.txtDate.setVisibility(View.INVISIBLE);
        }
        holder.txtName.setText(comics.getName());
        holder.txtPublisher.setText(comics.getPublisher());
        holder.txtNotes.setText(Utils.join(" - ", true, comics.getAuthors(),
                Utils.nvl(bestReleaseNotes, comics.getNotes())));
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
            ReleaseInfo lre = mDataManager.getBestRelease(lco.getId());
            ReleaseInfo rre = mDataManager.getBestRelease(rco.getId());
            Release lrer, rrer;

            if (lre == null) lrer = emptyRelease; else lrer = lre.getRelease();
            if (rre == null) rrer = emptyRelease; else rrer = rre.getRelease();

            if (lrer.getDate() != null && rrer.getDate() != null) {
                return lrer.getDate().compareTo(rrer.getDate());
            } else if (lrer.getDate() != null && rrer.getDate() == null) {
                return -1;
            } else if (lrer.getDate() == null && rrer.getDate() != null) {
                return 1;
            } else if (lrer.getNumber() == rrer.getNumber()) {
                return lco.getName().compareToIgnoreCase(rco.getName());
            } else {
                return lrer.getNumber() - rrer.getNumber();
            }
        }
    }

    final class ItemViewHolder {
        TextView txtName;
        TextView txtPublisher;
        TextView txtNotes;
        TextView txtNumber;
        TextView txtDate;
    }

}
