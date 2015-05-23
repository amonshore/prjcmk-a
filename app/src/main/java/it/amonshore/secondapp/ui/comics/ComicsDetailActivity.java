package it.amonshore.secondapp.ui.comics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.data.ReleaseInfo;
import it.amonshore.secondapp.ui.SettingsActivity;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Narsenico on 20/05/2015.
 */
public class ComicsDetailActivity extends ActionBarActivity {

    public final static String EXTRA_ENTRY = "entry";

    private Comics mComics;
    private DataManager mDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_detail);
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager(getApplicationContext());
        //leggo i parametri
        Intent intent = getIntent();
        mComics = (Comics)intent.getSerializableExtra(EXTRA_ENTRY);
        //
        ((TextView)findViewById(R.id.txt_detail_comics_name)).setText(mComics.getName());
        //
        StickyListHeadersListView stickyList = (StickyListHeadersListView) findViewById(R.id.lst_detail_comics);
        stickyList.setAdapter(new ReleasesAdapter(this, mComics));
    }

    final class ReleasesAdapter extends BaseAdapter implements StickyListHeadersAdapter {

        private ReleaseInfo[] mReleaseInfos;
        private LayoutInflater mInflater;
        private SimpleDateFormat mDateFormat;

        public ReleasesAdapter(Context context, Comics comics) {
            mInflater = LayoutInflater.from(context);
            mDateFormat = new SimpleDateFormat("c dd MMM", Locale.getDefault());
            //creo gli elementi per la lista
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean groupByMonth = sharedPref.getBoolean(SettingsActivity.KEY_PREF_GROUP_BY_MONTH, false);
            boolean weekStartOnMonday = sharedPref.getBoolean(SettingsActivity.KEY_PREF_WEEK_START_ON_MONDAY, false);
            ReleaseGroupHelper helper = new ReleaseGroupHelper(ReleaseGroupHelper.MODE_COMICS, groupByMonth, weekStartOnMonday);
            helper.addReleases(comics.getReleases());
            mReleaseInfos = helper.getReleaseInfos();
        }

        @Override
        public int getCount() {
            return mReleaseInfos.length;
        }

        @Override
        public Object getItem(int position) {
            return mReleaseInfos[position];
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
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Release release = mReleaseInfos[position].getRelease();
            String relDate = "";
            if (release.getDate() != null) {
                relDate = mDateFormat.format(release.getDate());
            }
            holder.txtName.setText(Integer.toString(mReleaseInfos[position].getRelease().getNumber()));
            holder.txtInfo.setText(String.format("#%s - %s - p %s", release.getNumber(), relDate, release.isPurchased()));

            return convertView;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            if (convertView == null) {
                Utils.d("getHeaderView " + position);
                holder = new HeaderViewHolder();
                convertView = mInflater.inflate(R.layout.list_release_header, parent, false);
                holder.text = (TextView) convertView.findViewById(R.id.txt_list_release_header);
                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }
            //set header text as first char in name
            String headerText = "Group " + mReleaseInfos[position].getGroup();
            holder.text.setText(headerText);
            return convertView;
        }

        @Override
        public long getHeaderId(int position) {
            //return the first character of the country as ID because this is what headers are based upon
            return mReleaseInfos[position].getGroup();
        }

        class HeaderViewHolder {
            TextView text;
        }

        class ViewHolder {
            TextView txtName;
            TextView txtInfo;
        }

    }
}
