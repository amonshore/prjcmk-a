package it.amonshore.comikkua.ui.release;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.MultiReleaseInfo;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.data.ReleaseGroupHelper;
import it.amonshore.comikkua.data.ReleaseInfo;
import it.amonshore.comikkua.Utils;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by Calgia on 15/05/2015.
 *
 * Per gestire le sezioni visibili in ogni visibilità vedere putReleaseInSection
 */
public class ReleaseListAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private Context mContext;
    private DataManager mDataManager;
    private List<ReleaseInfo> mReleaseInfos;
    private LayoutInflater mInflater;
    private boolean mGroupByMonth;
    private View.OnClickListener mOnNumberViewClickListener;
    private boolean mUseSingleComicsLayout;

    /**
     *
     * @param context
     */
    public ReleaseListAdapter(Context context) {
        mContext = context;
        mDataManager = DataManager.getDataManager();
        mInflater = LayoutInflater.from(context);
   }

    /**
     *
     * @param comics
     * @return
     */
    public Release createNewRelease(Comics comics) {
        return comics.createRelease(true);
    }

    /**
     *
     * @param position
     * @return
     */
    public boolean remove(int position) {
        ReleaseInfo ri = (ReleaseInfo)getItem(position);
        long comicsId = ri.getRelease().getComicsId();
        //A0041 se multi rimuovo tutte le release
        if (ri instanceof MultiReleaseInfo) {
            for (Release rel : ((MultiReleaseInfo)ri)) {
                mDataManager.removeRelease(comicsId, rel.getNumber());
            }
            mDataManager.updateBestRelease(comicsId);
            mReleaseInfos.remove(position);
            return true;
        } else {
            if (mDataManager.removeRelease(comicsId, ri.getRelease().getNumber())) {
                mDataManager.updateBestRelease(comicsId);
                mReleaseInfos.remove(position);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     *
     * @param comics se specificato aggiorna i dati con i soli
     * @param mode
     * @param groupByMonth
     * @param weekStartOnMonday
     * @return
     */
    public int refresh(Comics comics, int mode, boolean groupByMonth, boolean weekStartOnMonday) {
        mGroupByMonth = groupByMonth;
        //creo gli elementi per la lista
        ReleaseGroupHelper helper = new ReleaseGroupHelper(mode, groupByMonth, weekStartOnMonday);
        if (comics == null) {
            mUseSingleComicsLayout = false;
            for (long comicsId : mDataManager.getComics()) {
                //A0041 raggruppo tutte le wishlist in un'unica release
                helper.addReleases(true, mDataManager.getComics(comicsId).getReleases());
            }
        } else {
            mUseSingleComicsLayout = true;
            helper.addReleases(comics.getReleases());
        }
        mReleaseInfos = new ArrayList<>(Arrays.asList(helper.getReleaseInfos()));
        return mReleaseInfos.size();
    }

    public View.OnClickListener getOnNumberViewClickListener() {
        return mOnNumberViewClickListener;
    }

    /**
     *
     * @param onNumberViewClickListener
     */
    public void setOnNumberViewClickListener(View.OnClickListener onNumberViewClickListener) {
        this.mOnNumberViewClickListener = onNumberViewClickListener;
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
        return mReleaseInfos == null ? 0 : mReleaseInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mReleaseInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder holder;

        if (convertView == null) {
            holder = new ItemViewHolder();
            if (mUseSingleComicsLayout) {
                convertView = mInflater.inflate(R.layout.list_single_comics_release_item, parent, false);
            } else {
                convertView = mInflater.inflate(R.layout.list_release_item, parent, false);
                holder.txtName = (TextView) convertView.findViewById(R.id.txt_list_release_name);
            }
            holder.txtNotes = (TextView) convertView.findViewById(R.id.txt_list_release_notes);
            holder.txtDate = (TextView) convertView.findViewById(R.id.txt_list_release_date);
            holder.txtNumber = (TextView) convertView.findViewById(R.id.txt_list_release_number);
            //imposto il listener sul click
            holder.txtNumber.setOnClickListener(mOnNumberViewClickListener);
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        ReleaseInfo ri = mReleaseInfos.get(position);
        Release release = ri.getRelease();
        Comics comics = mDataManager.getComics(release.getComicsId());
        String relDate = "";
        if (release.getDate() != null) {
            if (mUseSingleComicsLayout) {
                relDate = Utils.formatReleaseLongDate(release.getDate());
            } else {
                relDate = Utils.formatReleaseDate(release.getDate());
            }
        }
        if (!mUseSingleComicsLayout) {
            holder.txtName.setText(comics.getName());
        }
        holder.txtNumber.setText(Integer.toString(release.getNumber()));
        holder.txtDate.setText(relDate);

        //A0041 se MultiReleaseInfo in note riporto tutti i numeri delle release
        if (ri instanceof MultiReleaseInfo) {
            MultiReleaseInfo mri = (MultiReleaseInfo)ri;
            if (mri.size() > 1) {
                holder.txtNotes.setText(mContext.getString(R.string.also_releases, mri.getFormattedReleaseNumbers()));
            } else {
                holder.txtNotes.setText(Utils.nvl(release.getNotes(), comics.getNotes(), ""));
            }
        } else if (mUseSingleComicsLayout) {
            holder.txtNotes.setText(Utils.nvl(release.getNotes(), ""));
        } else {
            holder.txtNotes.setText(Utils.nvl(release.getNotes(), comics.getNotes(), ""));
        }

        //se la release è stata prenotata inserisco una icona
        if (release.isOrdered()) {
            holder.txtNotes.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_archive_16, 0);
        } else {
            holder.txtNotes.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        }

        //imposto background e colore del testo in base allo stato della release
        //me ne frego del gruppo se è purchased, questo perché al click sul numero aggiorno solo questa vista, e non tutta la lista (vedi fragment)
        //  di conseguenza posso trovarmi release purchased in qualsiasi gruppo
        if (release.isPurchased()) {
            holder.txtNumber.setBackgroundResource(R.drawable.background_oval_purchased);
            holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_purchased_color_text_light));
        } else {
            switch (ri.getGroup()) {
                case ReleaseGroupHelper.GROUP_LOST:
                case ReleaseGroupHelper.GROUP_EXPIRED:
                    holder.txtNumber.setBackgroundResource(R.drawable.background_oval_expired);
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_expired_color_text_light));
                    break;
                case ReleaseGroupHelper.GROUP_PERIOD:
                case ReleaseGroupHelper.GROUP_TO_PURCHASE:
                    if (ri.isReleasedToday()) {
                        holder.txtNumber.setBackgroundResource(R.drawable.background_oval_today);
                        holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_today_color_text_light));
                        break;
                    } else if (ri.getRelease().isWishlist()) { //A0037
                        holder.txtNumber.setBackgroundResource(R.drawable.background_oval_wishlist);
                        holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_wishlist_color_text_light));
                        break;
                    } else if (ri.isExpired()) {
                        holder.txtNumber.setBackgroundResource(R.drawable.background_oval_expired);
                        holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_expired_color_text_light));
                        break;
                    }
                case ReleaseGroupHelper.GROUP_PERIOD_NEXT:
                case ReleaseGroupHelper.GROUP_PERIOD_OTHER:
                case ReleaseGroupHelper.GROUP_PURCHASED:
                    holder.txtNumber.setBackgroundResource(R.drawable.background_oval_to_purchase);
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_to_purchase_color_text_light));
                    break;
                case ReleaseGroupHelper.GROUP_WISHLIST:
                    holder.txtNumber.setBackgroundResource(R.drawable.background_oval_wishlist);
                    holder.txtNumber.setTextColor(mContext.getResources().getColor(R.color.comikku_wishlist_color_text_light));
                    break;
            }
        }

        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            //Utils.d("getHeaderView " + position);
            holder = new HeaderViewHolder();
            convertView = mInflater.inflate(R.layout.list_release_header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.txt_list_release_header);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        holder.text.setText(getGroupTitle(mReleaseInfos.get(position).getGroup()));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        //return the first character of the country as ID because this is what headers are based upon
        return mReleaseInfos.get(position).getGroup();
    }

    public CharSequence getGroupTitle(int group) {
        switch (group) {
            case ReleaseGroupHelper.GROUP_PERIOD:
                return mContext.getString(mGroupByMonth ? R.string.title_release_group_current_month :
                        R.string.title_release_group_current_week);
            case ReleaseGroupHelper.GROUP_PERIOD_NEXT:
                return mContext.getString(mGroupByMonth ? R.string.title_release_group_next_month :
                        R.string.title_release_group_next_week);
            case ReleaseGroupHelper.GROUP_PERIOD_OTHER:
                return mContext.getString(R.string.title_release_group_future);
            case ReleaseGroupHelper.GROUP_EXPIRED:
                return mContext.getString(R.string.title_release_group_expired);
            case ReleaseGroupHelper.GROUP_LOST:
                return mContext.getString(R.string.title_release_group_lost);
            case ReleaseGroupHelper.GROUP_WISHLIST:
                return mContext.getString(R.string.title_release_group_wishlist);
            case ReleaseGroupHelper.GROUP_PURCHASED:
                return mContext.getString(R.string.title_release_group_purchased);
            case ReleaseGroupHelper.GROUP_TO_PURCHASE:
                return mContext.getString(R.string.title_release_group_to_purchase);
            default:
                return mContext.getString(R.string.title_release_group_unknown, group);
        }
    }

    final class HeaderViewHolder {
        TextView text;
    }

    final class ItemViewHolder {
        TextView txtName;
        TextView txtNotes;
        TextView txtNumber;
        TextView txtDate;
    }

}
