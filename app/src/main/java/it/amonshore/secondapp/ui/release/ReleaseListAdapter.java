package it.amonshore.secondapp.ui.release;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.data.ReleaseInfo;
import it.amonshore.secondapp.Utils;
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
    private SimpleDateFormat mDateFormat;
    private boolean mGroupByMonth;

    /**
     *
     * @param context
     */
    public ReleaseListAdapter(Context context) {
        mContext = context;
        mDataManager = DataManager.getDataManager(context);
        mInflater = LayoutInflater.from(context);
        mDateFormat = new SimpleDateFormat("c dd MMM", Locale.getDefault());
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
        Comics comics = mDataManager.getComics(ri.getRelease().getComicsId());
        if (comics.removeRelease(ri.getRelease().getNumber())) {
            mDataManager.updateBestRelease(comics.getId());
            mReleaseInfos.remove(position);
            return true;
        } else {
            return false;
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
            for (long comicsId : mDataManager.getComics()) {
                helper.addReleases(mDataManager.getComics(comicsId).getReleases());
            }
        } else {
            helper.addReleases(comics.getReleases());
        }
        mReleaseInfos = new ArrayList<>(Arrays.asList(helper.getReleaseInfos()));
        return mReleaseInfos.size();
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
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_release_item, parent, false);
            holder.txtName = (TextView) convertView.findViewById(R.id.txt_list_release_name);
            holder.txtInfo = (TextView) convertView.findViewById(R.id.txt_list_release_info);
            holder.txtNumber = (TextView) convertView.findViewById(R.id.txt_list_release_number);
            holder.txtDate = (TextView) convertView.findViewById(R.id.txt_list_release_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Release release = mReleaseInfos.get(position).getRelease();
        Comics comics = mDataManager.getComics(release.getComicsId());
        String relDate = "";
        if (release.getDate() != null) {
            relDate = mDateFormat.format(release.getDate());
        }
        holder.txtName.setText(comics.getName());
        holder.txtNumber.setText(Integer.toString(release.getNumber()));
        holder.txtDate.setText(relDate);
        holder.txtInfo.setText(String.format("%s - p %s", release.getNotes(), release.isPurchased()));

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

    final class ViewHolder {
        TextView txtName;
        TextView txtInfo;
        TextView txtNumber;
        TextView txtDate;
    }

}
