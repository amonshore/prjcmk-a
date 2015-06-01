package it.amonshore.secondapp.ui.release;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.ReleaseInfo;
import it.amonshore.secondapp.ui.AFragment;
import it.amonshore.secondapp.ui.MainActivity;
import it.amonshore.secondapp.ui.SettingsActivity;
import it.amonshore.secondapp.ui.comics.ComicsListAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Calgia on 15/05/2015.
 */
public class ReleaseListFragment extends AFragment {

    //usato per lo stato dell'istanza
    private final static String STATE_MODE = " stateMode";
    //public final static String ARG_MODE = "arg_mode";
    //public final static String ARG_COMICS_ID = "arg_comics_id";

    private AbsListView mListView;
    private ReleaseListAdapter mAdapter;
    private ActionMode mActionMode;
    private DataManager mDataManager;
    private Comics mComics;
    private int mGroupMode;
    private boolean mGroupByMonth;
    private boolean mWeekStartOnMonday;
    private boolean mNeedUpdateOnResume;

    /**
     *
     * @return
     */
    public int getGroupMode() {
        return mGroupMode;
    }

    /**
     *
     * @param comics
     * @param groupMode
     */
    public void setComics(Comics comics, int groupMode) {
        mComics = comics;
        mGroupMode = groupMode;
        //se il fragment è caricato nel dettaglio non faccio vedere il menu
        if (mComics != null) {
            setHasOptionsMenu(false);
        } else {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //deve essere chiamato in onCreate
        setHasOptionsMenu(true);
        mDataManager = DataManager.getDataManager(getActivity().getApplicationContext());
//        //recupero i parametri
//        int mode = ReleaseGroupHelper.MODE_SHOPPING;
//        Bundle args = getArguments();
//        if (args != null) {
//            long comicsId = args.getLong(ARG_COMICS_ID);
//            mGroupMode = args.getInt(ARG_MODE, ReleaseGroupHelper.MODE_SHOPPING);
//            if (comicsId != 0) {
//                mComics = mDataManager.getComics(comicsId);
//            }
//        }
        if (savedInstanceState != null) {
            //recupero la modalità in precedenza e salvato alla chiusura dell'activity
            mGroupMode = savedInstanceState.getInt(STATE_MODE, ReleaseGroupHelper.MODE_CALENDAR);
        } else {
            //recupero la modalità dalle preferenze
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            mGroupMode = settings.getInt(STATE_MODE, ReleaseGroupHelper.MODE_CALENDAR);
        }
        //
        mAdapter = new ReleaseListAdapter(getActivity().getApplicationContext());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        //salvo la modalità
        savedInstanceState.putInt(STATE_MODE, mGroupMode);
        //
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();

        //salvo le preferenze (ma solo se il fragment non è caricato nel dettaglio)
        if (mComics == null) {
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(STATE_MODE, mGroupMode);
            editor.commit();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_releases_list, container, false);
        StickyListHeadersListView list = (StickyListHeadersListView)view.findViewById(R.id.lst_releases);
        //
        list.setAdapter(mAdapter);
        //questa è la vera lista
        mListView = list.getWrappedList();
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Release release = ((ReleaseInfo) mAdapter.getItem(position)).getRelease();
                showReleaseEditor(release.getComicsId(), release.getNumber());
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
                    long[] ags = mListView.getCheckedItemIds();
                    Long[] lgs = new Long[ags.length];
                    for (int ii = 0; ii < ags.length; ii++) {
                        lgs[ii] = ags[ii];
                    }

                    Utils.d(TextUtils.join(", ", lgs));

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ReleaseEditorActivity.EDIT_RELEASE_REQUEST) {
                needDataRefresh(AFragment.CAUSE_DATA_CHANGED);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_releases, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_releases_mode_calendar).setVisible(mGroupMode != ReleaseGroupHelper.MODE_CALENDAR);
        menu.findItem(R.id.action_releases_mode_shopping).setVisible(mGroupMode != ReleaseGroupHelper.MODE_SHOPPING);
        menu.findItem(R.id.action_releases_mode_law).setVisible(mGroupMode != ReleaseGroupHelper.MODE_LAW);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Utils.d("onOptionsItemSelected " + item.getTitle());
        //
        if (id == R.id.action_releases_mode_calendar) {
            mGroupMode = ReleaseGroupHelper.MODE_CALENDAR;
            needDataRefresh(AFragment.CAUSE_LOADING);
            getActivity().invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_releases_mode_shopping) {
            mGroupMode = ReleaseGroupHelper.MODE_SHOPPING;
            needDataRefresh(AFragment.CAUSE_LOADING);
            getActivity().invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_releases_mode_law) {
            mGroupMode = ReleaseGroupHelper.MODE_LAW;
            needDataRefresh(AFragment.CAUSE_LOADING);
            getActivity().invalidateOptionsMenu();
            return true;
        }
        //
        return false;
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

    @Override
    public void needDataRefresh(int cause) {
        //leggo i dati in modalità asincrona, se il fragment non è ancora visisbile pospongo il caricamento
        if (this.isResumed() || (cause & AFragment.CAUSE_SAFE) == AFragment.CAUSE_SAFE) {
            //se la causa è il cambio pagina aggiorno i dati solose l'adapter è vuoto
            if ((cause & AFragment.CAUSE_PAGE_CHANGED) != AFragment.CAUSE_PAGE_CHANGED || mAdapter.isEmpty()) {
                mNeedUpdateOnResume = false;
                Utils.d("needDataRefresh ok");
                new ReadReleasesAsyncTask().execute();
            }
        } else {
            Utils.d("needDataRefresh posponed");
            mNeedUpdateOnResume = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNeedUpdateOnResume) {
            Utils.d("onResume refresh");
            mNeedUpdateOnResume = false;
            new ReadReleasesAsyncTask().execute();
        }
    }

    private void showReleaseEditor(long comicsId, int number) {
        Intent intent = new Intent(getActivity(), ReleaseEditorActivity.class);
        intent.putExtra(ReleaseEditorActivity.EXTRA_COMICS_ID, comicsId);
        intent.putExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, number);
        startActivityForResult(intent, ReleaseEditorActivity.EDIT_RELEASE_REQUEST);
    }

    /**
     * Task asincrono per la lettura dei dati
     */
    private class ReadReleasesAsyncTask extends AsyncTask<Void, Release, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            //TODO settings -> che schifo! rivedere dove leggere le preferenze
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mGroupByMonth = sharedPref.getBoolean(SettingsActivity.KEY_PREF_GROUP_BY_MONTH, false);
            mWeekStartOnMonday = sharedPref.getBoolean(SettingsActivity.KEY_PREF_WEEK_START_ON_MONDAY, false);

            return ReleaseListFragment.this.mAdapter.refresh(ReleaseListFragment.this.mComics,
                    ReleaseListFragment.this.mGroupMode,
                    ReleaseListFragment.this.mGroupByMonth,
                    ReleaseListFragment.this.mWeekStartOnMonday);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Utils.d("ReadReleasesAsyncTask " + result);
            ReleaseListFragment.this.mAdapter.notifyDataSetInvalidated();
        }
    }

    //TODO udpate task
    //TODO remove task


}
