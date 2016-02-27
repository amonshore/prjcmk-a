package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.data.ReleaseGroupHelper;
import it.amonshore.comikkua.data.ReleaseInfo;

/**
 * Created by Calgia on 07/05/2015.
 *
 * L'id di ogni elemento della lista è dato da Comics.getId()
 */
class ComicsListAdapter extends BaseAdapter implements SectionIndexer {

    public final static int ORDER_BY_NAME = 2;
    public final static int ORDER_BY_BEST_RELEASE = 4;

    private final Context mContext;
    private final DataManager mDataManager;
    private final ArrayList<Long> mSortedIds;
    private Comparator<Long> mComparator;
    private int mOrder;

    private HashMap<String, Integer> mMapFastScrollSections;
    private String[] mFastScrollSections;

    public ComicsListAdapter(Context context, int order) {
        mContext = context;
        mDataManager = DataManager.getDataManager();
        mSortedIds = new ArrayList<>();
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
            //imposto il mComparator in base all'ordine
            if (order == ORDER_BY_NAME) {
                mComparator = new NameComparator();
            } else {
                mComparator = new ReleaseComparator();
            }
            Collections.sort(mSortedIds, mComparator);
            prepareFastScrollSections();
        }
    }

//    /**
//     *
//     * @param id
//     * @return
//     */
//    public boolean remove(long id) {
//        // TODO: non mi piace per niente, non dovrebbe essere fatto qua la rimozione dal DataManager
//        if (mDataManager.remove(id)) {
//            mSortedIds.remove(id);
//            prepareFastScrollSections();
//            return true;
//        } else {
//            return false;
//        }
//    }

    /**
     *
     * @return
     */
    public int refresh() {
        mSortedIds.clear();
        //A0061 mSortedIds.addAll(mDataManager.getComics());
        mSortedIds.addAll(mDataManager.getFilteredComics());
        Collections.sort(mSortedIds, mComparator);
        prepareFastScrollSections();
        return mSortedIds.size();
    }

    private void prepareFastScrollSections() {
        if (mOrder == ORDER_BY_NAME) {
            mMapFastScrollSections = new LinkedHashMap<>();
            for (int ii = 0; ii < mSortedIds.size(); ii++) {
                Comics comics = mDataManager.getComics(mSortedIds.get(ii));
                String ch = comics.getName().substring(0, 1).toUpperCase();
                if (!mMapFastScrollSections.containsKey(ch))
                    mMapFastScrollSections.put(ch, ii);
            }
            mFastScrollSections = mMapFastScrollSections.keySet().toArray(new String[mMapFastScrollSections.size()]);
        } else {
            mMapFastScrollSections = null;
            mFastScrollSections = null;
        }
    }

    @Override
    public Object[] getSections() {
        return mFastScrollSections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
//        Utils.d(this.getClass(), "getPositionForSection " + sectionIndex);
        return mMapFastScrollSections.get(mFastScrollSections[sectionIndex]);
    }

    @Override
    public int getSectionForPosition(int position) {
//        Utils.d(this.getClass(), "getSectionForPosition " + position);
        return 0;
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
            //indicare il parent altrimenti NON vengono considerate le dimensioni dell'item
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_comics_item, parent, false);
            holder.leftView = convertView.findViewById(R.id.lay_number);
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
                relDate = Utils.formatComicsDate(bestRelease.getDate());
            }
            holder.txtNumber.setText(Integer.toString(bestRelease.getNumber()));
            holder.txtDate.setText(relDate);

            switch (bestReleaseInfo.getGroup()) {
                case ReleaseGroupHelper.GROUP_LOST:
                case ReleaseGroupHelper.GROUP_EXPIRED:
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_expired_color_primary));
                    holder.leftView.setBackgroundResource(R.drawable.border_comics_expired);
                    break;
                case ReleaseGroupHelper.GROUP_TO_PURCHASE:
                    if (bestReleaseInfo.isReleasedToday()) {
                        holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_today_color_primary));
                        holder.leftView.setBackgroundResource(R.drawable.border_comics_today);
                        break;
                    }
                case ReleaseGroupHelper.GROUP_PERIOD:
                case ReleaseGroupHelper.GROUP_PERIOD_NEXT:
                case ReleaseGroupHelper.GROUP_PERIOD_OTHER:
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_to_purchase_color_primary));
                    holder.leftView.setBackgroundResource(R.drawable.border_comics_to_purchase);
                    break;
                case ReleaseGroupHelper.GROUP_WISHLIST:
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_wishlist_color_primary));
                    holder.leftView.setBackgroundResource(R.drawable.border_comics_wishlist);
                    break;
                case ReleaseGroupHelper.GROUP_PURCHASED:    //A0046
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_purchased_color_primary));
                    holder.leftView.setBackgroundResource(R.drawable.border_comics_purchased);
                    break;
            }
            bestReleaseNotes = bestRelease.getNotes();
        } else {
            holder.txtNumber.setText("");
            holder.txtDate.setText("");
            holder.leftView.setBackgroundResource(R.drawable.border_comics_empty);
        }
        holder.txtName.setText(comics.getName());
        holder.txtPublisher.setText(comics.getPublisher());
        holder.txtNotes.setText(Utils.join(" - ", true, comics.getAuthors(),
                Utils.nvl(bestReleaseNotes, comics.getNotes())));
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        //A0048
        if (position >= mSortedIds.size())
            return position;
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

            //A0032
            int res;
            if (lrer.getDate() != null && rrer.getDate() != null) {
                res = lrer.getDate().compareTo(rrer.getDate());
            } else if (lrer.getDate() != null) {
                res = -1;
            } else if (rrer.getDate() != null) {
                res = 1;
            } else if (lrer.getNumber() == rrer.getNumber()) {
                res = lco.getName().compareToIgnoreCase(rco.getName());
            } else {
                res = lrer.getNumber() - rrer.getNumber();
            }

            if (res == 0 ) {
                res = lco.getName().compareToIgnoreCase(rco.getName());
            }

            return res;
        }
    }

    final class ItemViewHolder {
        View leftView;
        TextView txtName;
        TextView txtPublisher;
        TextView txtNotes;
        TextView txtNumber;
        TextView txtDate;
    }

}
