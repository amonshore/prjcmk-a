package it.amonshore.secondapp.ui.release;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.applidium.headerlistview.HeaderListView;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseId;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.ui.OnChangePageListener;
import it.amonshore.secondapp.ui.SettingsActivity;

/**
 * Created by Calgia on 15/05/2015.
 */
public class ReleaseListFragment extends Fragment implements OnChangePageListener {

    public final static String ARG_MODE = "arg_mode";
    public final static String ARG_COMICS_ID = "arg_comics_id";

    private AbsListView mListView;
    private ReleaseListAdapter mAdapter;
    private ActionMode mActionMode;
    private DataManager mDataManager;
    //TODO deve essere passato in qualche modo, può essere null
    private Comics mComics;
    private boolean mGroupByMonth;
    private boolean mWeekStartOnMonday;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //deve essere chiamato in onCreate
        setHasOptionsMenu(true);
        mDataManager = DataManager.getDataManager(getActivity().getApplicationContext());
        //recupero i parametri
        Bundle args = getArguments();
        long comicsId = args.getLong(ARG_COMICS_ID);
        int mode = args.getInt(ARG_MODE, ReleaseListAdapter.MODE_SHOPPING);
        if (comicsId != 0) {
            mComics = mDataManager.getComics(comicsId);
        }
        //
        mAdapter = new ReleaseListAdapter(getActivity().getApplicationContext(), mode);
        //leggo i dati in modalità asincrona
        refreshData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_releases, container, false);
        HeaderListView list = (HeaderListView)view.findViewById(android.R.id.list);
        //
        list.setAdapter(mAdapter);
        //questa è la vera lista
        mListView = list.getListView();
        //fix per crash -> http://stackoverflow.com/questions/25807332/crash-when-returning-to-fragment-as-a-tab-view-with-same-id
        mListView.setId(View.NO_ID);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Utils.d("onItemClick " + ((Comics) mAdapter.getItem(position)).getName());
                //TODO showReleaseEditor((ReleaseId) mAdapter.getItem(position), false);
            }
        });
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                //Utils.d("onItemCheckedStateChanged " + position);
                mode.setTitle(getString(R.string.selected_items, mListView.getCheckedItemCount()));
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                //Utils.d("onCreateActionMode");
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_releases_cab, menu);
                mActionMode = mode;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Utils.d("onActionItemClicked " + item.getTitle());
                //risponde alla selezione di una azione del menu_releases_cab
                long menuId = item.getItemId();
                if (menuId == R.id.action_release_delete) {
                    //TODO delete
//                    long[] ags = mListView.getCheckedItemIds();
//                    Long[] lgs = new Long[ags.length];
//                    for (int ii = 0; ii < ags.length; ii++) {
//                        lgs[ii] = ags[ii];
//                    }
//                    new RemoveComicsAsyncTask().execute(lgs);
                    finishActionMode();
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }
        });

        return  view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //TODO prepara il menu in base a cosa è stato selezionato
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_releases, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Utils.d("onOptionsItemSelected " + item.getTitle());
        if (id == R.id.action_release_add) {
            //TODO apri l'editor showComicsEditor(..., true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //TODO risponde all'editor delle release
    }

    public void refreshData() {
        //TODO indicare per quali fumetti leggere i dati
        //recupero le preferenze
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mGroupByMonth = sharedPref.getBoolean(SettingsActivity.KEY_PREF_GROUP_BY_MONTH, false);
        mWeekStartOnMonday = sharedPref.getBoolean(SettingsActivity.KEY_PREF_WEEK_START_ON_MONDAY, false);
        //
        new ReadReleasesAsyncTask().execute();
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void finishActionMode() {
        if (mActionMode != null)
            mActionMode.finish();
    }

    private void showReleaseEditor(ReleaseId releaseId, boolean isNew) {
        //TODO apri editor release
//        Intent intent = new Intent(getActivity(), ComicsEditorActivity.class);
//        intent.putExtra(ComicsEditorActivity.EXTRA_ENTRY, comics);
//        intent.putExtra(ComicsEditorActivity.EXTRA_IS_NEW, isNew);
//        startActivityForResult(intent, ComicsEditorActivity.EDIT_COMICS_REQUEST);
    }

    /**
     * Task asincrono per la lettura dei dati
     */
    private class ReadReleasesAsyncTask extends AsyncTask<Void, Release, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            ReleaseListFragment.this.mAdapter.refresh(ReleaseListFragment.this.mComics,
                    ReleaseListFragment.this.mGroupByMonth, ReleaseListFragment.this.mWeekStartOnMonday);
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            ReleaseListFragment.this.mAdapter.notifyDataSetChanged();
        }
    }

    //TODO udpate task
    //TODO remove task


}
