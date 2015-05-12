package it.amonshore.secondapp.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
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

import java.util.List;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class ComicsListFragment extends Fragment implements OnChangePageListener {

    private final static String LOG_TAG = "CLF";

    private final static String STATE_ORDER = " stateOrder";

    private AbsListView mListView;
    private ComicsListAdapter mAdapter;
    private ActionMode mActionMode;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ComicsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //deve essere chiamato in onCreate
        setHasOptionsMenu(true);
        //
        int order = 0;
        if (savedInstanceState != null) {
            //recupero l'ordine usato in precedenza e salvato alla chiusura dell'activity
            order = savedInstanceState.getInt(STATE_ORDER, ComicsListAdapter.ORDER_BY_NAME);
        } else {
            //recupero l'ordinamento dalle preferenze
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            order = settings.getInt(STATE_ORDER, ComicsListAdapter.ORDER_BY_NAME);
        }
        //
        mAdapter = new ComicsListAdapter(getActivity(), order);
        //leggo i dati in modalità asincrona
        refreshData();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        //salvo lo stato dell'ordine
        savedInstanceState.putInt(STATE_ORDER, mAdapter.getOrder());
        //
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();

        //salvo le preferenze
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(STATE_ORDER, mAdapter.getOrder());
        editor.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comics, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        //
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "onItemClick " + ((Comics) mAdapter.getItem(position)).getName());
            }
        });
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                //Log.d(LOG_TAG, "onItemCheckedStateChanged " + position);
                mode.setTitle(getString(R.string.selected_comics, mListView.getCheckedItemCount()));
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                //Log.d(LOG_TAG, "onCreateActionMode");
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_comics_cab, menu);
                mActionMode = mode;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.d(LOG_TAG, "onActionItemClicked " + item.getTitle());
                //risponde alla selezione di una azione del menu_comics_cab
                long menuId = item.getItemId();
                if (menuId == R.id.action_comics_delete) {
                    long[] ags = mListView.getCheckedItemIds();
                    Long[] lgs = new Long[ags.length];
                    for (int ii = 0; ii < ags.length; ii++) {
                        lgs[ii] = ags[ii];
                    }
                    new RemoveComicsAsyncTask().execute(lgs);
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
        Log.d(LOG_TAG, "onPrepareOptionsMenu " + mAdapter.getOrder());
        if ((mAdapter.getOrder() & ComicsListAdapter.ORDER_BY_NAME) == ComicsListAdapter.ORDER_BY_NAME) {
            menu.findItem(R.id.action_comics_sort_by_name).setVisible(false);
            menu.findItem(R.id.action_comics_sort_by_release).setVisible(true);
        } else {
            menu.findItem(R.id.action_comics_sort_by_name).setVisible(true);
            menu.findItem(R.id.action_comics_sort_by_release).setVisible(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(LOG_TAG, "onOptionsItemSelected " + item.getTitle());

        if (id == R.id.action_comics_add) {
            //TODO aprire l'editor
            new UpdateComicsAsyncTask()
                    .execute(mAdapter.createNewComics());
            return true;
        } else if (id == R.id.action_comics_sort_by_name) {
            //TODO sort by name
            ////invalido il menu in modo che venga ricreato, così da poter nascondere le voci che non interessano
            //getActivity().invalidateOptionsMenu();
            mAdapter.setOrder(ComicsListAdapter.ORDER_BY_NAME);
            mAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
            return true;
        }  else if (id == R.id.action_comics_sort_by_release) {
            //TODO sort by release
            ////invalido il menu in modo che venga ricreato, così da poter nascondere le voci che non interessano
            //getActivity().invalidateOptionsMenu();
            mAdapter.setOrder(ComicsListAdapter.ORDER_BY_BEST_RELEASE);
            mAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
            return true;
        } else {
            return false;
        }
    }

    public void refreshData() {
        new ReadComicsAsyncTask().execute();
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

    /**
     * Task asincrono per la lettura dei dati
     */
    private class ReadComicsAsyncTask extends AsyncTask<Void, Comics, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            ComicsListFragment.this.mAdapter.refresh();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            ComicsListFragment.this.mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Task asincrono per l'aggiornamento dei dati (nuovo, modifica)
     * Il terzo parametro indica l'indice dell'elemento aggiunto/modificato
     */
    private class UpdateComicsAsyncTask extends AsyncTask<Comics, Comics, Integer> {
        @Override
        protected Integer doInBackground(Comics... params) {
            for (Comics comics : params) {
                publishProgress(comics);
            }
            //TODO salvare i dati con DataManager
            return params.length;
        }

        @Override
        protected void onProgressUpdate(Comics... values) {
            int index = ComicsListFragment.this.mAdapter.insertOrUpdate(values[0]);
            Log.d(LOG_TAG, "update comics " + index);
            if (index < 0) {
                ComicsListFragment.this.mAdapter.notifyDataSetChanged();
            } else {
                //TODO aggiorno solo la visa dell'elemento modificao
            }
        }
    }

    /**
     * Task asincrono per la rimoazione dei dati
     */
    private class RemoveComicsAsyncTask extends AsyncTask<Long, Long, Integer> {
        @Override
        protected Integer doInBackground(Long... params) {
            for (Long id : params) {
                publishProgress(id);
            }
            //TODO salvare i dati con DataManager
            return params.length;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            boolean res = ComicsListFragment.this.mAdapter.remove(values[0]);
            Log.d(LOG_TAG, "delete comics " + values[0] + " -> " + res);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            ComicsListFragment.this.mAdapter.notifyDataSetChanged();
        }
    }

}
