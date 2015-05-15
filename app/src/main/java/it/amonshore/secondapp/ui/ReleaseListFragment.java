package it.amonshore.secondapp.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.Release;

/**
 * Created by Calgia on 15/05/2015.
 */
public class ReleaseListFragment extends Fragment implements OnChangePageListener {

    private final static String LOG_TAG = "RLF";

    private AbsListView mListView;
    private ReleaseListAdapter mAdapter;
    private ActionMode mActionMode;
    //TODO deve essere passato in qualche modo, può essere null
    private Comics mComics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //deve essere chiamato in onCreate
        setHasOptionsMenu(true);
        //
        mAdapter = new ReleaseListAdapter(getActivity().getApplicationContext());
        //leggo i dati in modalità asincrona
        refreshData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_releases, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
        //
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.d(LOG_TAG, "onItemClick " + ((Comics) mAdapter.getItem(position)).getName());
                showReleaseEditor((Release) mAdapter.getItem(position), false);
            }
        });
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                //Log.d(LOG_TAG, "onItemCheckedStateChanged " + position);
                mode.setTitle(getString(R.string.selected_items, mListView.getCheckedItemCount()));
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                //Log.d(LOG_TAG, "onCreateActionMode");
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_releases_cab, menu);
                mActionMode = mode;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.d(LOG_TAG, "onActionItemClicked " + item.getTitle());
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

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //TODO prepara il menu in base a cosa è stato selezionato
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_releases, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(LOG_TAG, "onOptionsItemSelected " + item.getTitle());
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

    private void showReleaseEditor(Release release, boolean isNew) {
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
            ReleaseListFragment.this.mAdapter.refresh(ReleaseListFragment.this.mComics);
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
