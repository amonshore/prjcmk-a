package it.amonshore.secondapp.ui.comics;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.ui.AFragment;
import it.amonshore.secondapp.ui.release.ReleaseEditorActivity;
import it.amonshore.secondapp.ui.release.ReleaseListFragment;

/**
 * Created by Narsenico on 20/05/2015.
 */
public class ComicsDetailActivity extends ActionBarActivity {

    public final static String EXTRA_COMICS_ID = "comicsId";

    private Comics mComics;
    private DataManager mDataManager;
    private ReleaseListFragment mReleaseListFragment;
    private TextView mTxtName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager(getApplicationContext());
        //leggo i parametri
        Intent intent = getIntent();
        //presumo che l'id sia valido
        mComics = mDataManager.getComics(intent.getLongExtra(EXTRA_COMICS_ID, 0));
        //
        mTxtName = ((TextView)findViewById(R.id.txt_detail_comics_name));
        updateHeader();
        //
        ((FloatingActionButton)findViewById(R.id.fab_comics_edit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComicsEditor(mComics);
            }
        });
        //listener fab
        ((FloatingActionButton)findViewById(R.id.fab_release_add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseEditor(mComics, -1);
            }
        });
        //
        mReleaseListFragment = ((ReleaseListFragment)getSupportFragmentManager().findFragmentById(R.id.frg_release_list));
        mReleaseListFragment.setComics(mComics, ReleaseGroupHelper.MODE_COMICS);
        mReleaseListFragment.needDataRefresh(AFragment.CAUSE_LOADING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ComicsEditorActivity.EDIT_COMICS_REQUEST) {
                updateHeader();
            } else if (requestCode == ReleaseEditorActivity.EDIT_RELEASE_REQUEST) {
                mReleaseListFragment.needDataRefresh(AFragment.CAUSE_DATA_CHANGED);
            }
        }
    }

    private void updateHeader() {
        mTxtName.setText(mComics.getName() + " " + mComics.getPublisher());
    }

    private void showComicsEditor(Comics comics) {
        Intent intent = new Intent(this, ComicsEditorActivity.class);
        intent.putExtra(ComicsEditorActivity.EXTRA_COMICS_ID, comics.getId());
        startActivityForResult(intent, ComicsEditorActivity.EDIT_COMICS_REQUEST);
    }

    private void showReleaseEditor(Comics comics, int number) {
        Intent intent = new Intent(this, ReleaseEditorActivity.class);
        intent.putExtra(ReleaseEditorActivity.EXTRA_COMICS_ID, mComics.getId());
        intent.putExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, number);
        startActivityForResult(intent, ReleaseEditorActivity.EDIT_RELEASE_REQUEST);
    }

//    final class ReleasesAdapter extends BaseAdapter implements StickyListHeadersAdapter {
//
//        private ReleaseInfo[] mReleaseInfos;
//        private LayoutInflater mInflater;
//        private SimpleDateFormat mDateFormat;
//
//        public ReleasesAdapter(Context context, Comics comics) {
//            mInflater = LayoutInflater.from(context);
//            mDateFormat = new SimpleDateFormat("c dd MMM", Locale.getDefault());
//            //creo gli elementi per la lista
//            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
//            boolean groupByMonth = sharedPref.getBoolean(SettingsActivity.KEY_PREF_GROUP_BY_MONTH, false);
//            boolean weekStartOnMonday = sharedPref.getBoolean(SettingsActivity.KEY_PREF_WEEK_START_ON_MONDAY, false);
//            ReleaseGroupHelper helper = new ReleaseGroupHelper(ReleaseGroupHelper.MODE_COMICS, groupByMonth, weekStartOnMonday);
//            helper.addReleases(comics.getReleases());
//            mReleaseInfos = helper.getReleaseInfos();
//        }
//
//        @Override
//        public int getCount() {
//            return mReleaseInfos.length;
//        }
//
//        @Override
//        public Object getItem(int position) {
//            return mReleaseInfos[position];
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return position;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            ViewHolder holder;
//
//            if (convertView == null) {
//                holder = new ViewHolder();
//                convertView = mInflater.inflate(R.layout.list_release_item, parent, false);
//                holder.txtName = (TextView) convertView.findViewById(R.id.txt_list_release_name);
//                holder.txtInfo = (TextView) convertView.findViewById(R.id.txt_list_release_info);
//                convertView.setTag(holder);
//            } else {
//                holder = (ViewHolder) convertView.getTag();
//            }
//
//            Release release = mReleaseInfos[position].getRelease();
//            String relDate = "";
//            if (release.getDate() != null) {
//                relDate = mDateFormat.format(release.getDate());
//            }
//            holder.txtName.setText(Integer.toString(mReleaseInfos[position].getRelease().getNumber()));
//            holder.txtInfo.setText(String.format("#%s - %s - p %s", release.getNumber(), relDate, release.isPurchased()));
//
//            return convertView;
//        }
//
//        @Override
//        public View getHeaderView(int position, View convertView, ViewGroup parent) {
//            HeaderViewHolder holder;
//            if (convertView == null) {
//                Utils.d("getHeaderView " + position);
//                holder = new HeaderViewHolder();
//                convertView = mInflater.inflate(R.layout.list_release_header, parent, false);
//                holder.text = (TextView) convertView.findViewById(R.id.txt_list_release_header);
//                convertView.setTag(holder);
//            } else {
//                holder = (HeaderViewHolder) convertView.getTag();
//            }
//            //set header text as first char in name
//            String headerText = "Group " + mReleaseInfos[position].getGroup();
//            holder.text.setText(headerText);
//            return convertView;
//        }
//
//        @Override
//        public long getHeaderId(int position) {
//            //return the first character of the country as ID because this is what headers are based upon
//            return mReleaseInfos[position].getGroup();
//        }
//
//        class HeaderViewHolder {
//            TextView text;
//        }
//
//        class ViewHolder {
//            TextView txtName;
//            TextView txtInfo;
//        }
//
//    }
}
