package it.amonshore.secondapp.ui.comics;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.poliveira.apps.parallaxlistview.ParallaxListView;
import com.poliveira.apps.parallaxlistview.ParallaxScrollView;

import java.util.Arrays;
import java.util.Comparator;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.ui.AFragment;
import it.amonshore.secondapp.ui.release.ReleaseListAdapter;
import it.amonshore.secondapp.ui.release.ReleaseListFragment;
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
        //((TextView)findViewById(R.id.txt_detail_comics_name)).setText(mComics.getName());

        ParallaxScrollView parallaxScrollView = (ParallaxScrollView)findViewById(R.id.parallax);
        parallaxScrollView.setParallaxView(LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, parallaxScrollView, false));

        ((TextView)findViewById(android.R.id.text1)).setText(mComics.getName());

        //
        StickyListHeadersListView stickyList = (StickyListHeadersListView) findViewById(R.id.lst_detail_comics);
        stickyList.setAdapter(new ComicsAdapter(this, mComics));
    }

    class ComicsAdapter extends BaseAdapter implements StickyListHeadersAdapter {

        private Release[] mReleases;
        private LayoutInflater mInflater;

        public ComicsAdapter(Context context, Comics comics) {
            mInflater = LayoutInflater.from(context);
            mReleases = comics.getReleases();
            Arrays.sort(mReleases, new Comparator<Release>() {
                @Override
                public int compare(Release lhs, Release rhs) {
                    return (lhs.getNumber() % 2) - (rhs.getNumber() % 2);
                }
            });
        }

        @Override
        public int getCount() {
            return mReleases.length;
        }

        @Override
        public Object getItem(int position) {
            return mReleases[position];
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

            holder.txtName.setText(Integer.toString(mReleases[position].getNumber()));

            return convertView;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            if (convertView == null) {
                holder = new HeaderViewHolder();
                convertView = mInflater.inflate(R.layout.list_release_header, parent, false);
                holder.text = (TextView) convertView.findViewById(R.id.txt_list_release_header);
                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }
            //set header text as first char in name
            String headerText = "" + mReleases[position].getNumber() % 2;
            holder.text.setText(headerText);
            return convertView;
        }

        @Override
        public long getHeaderId(int position) {
            //return the first character of the country as ID because this is what headers are based upon
            return mReleases[position].getNumber() % 2;
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
