package it.amonshore.secondapp.ui;

import android.app.Activity;
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
        setHasOptionsMenu(true);

        //TODO recuperare l'ordinamento dalle preferenze
        mAdapter = new ComicsListAdapter(getActivity(), ComicsListAdapter.ORDER_ASC | ComicsListAdapter.ORDER_BY_NAME);
        //leggo i dati in modalità asincrona
        refreshData();
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
                if (item.getItemId() == R.id.action_comics_delete) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_comics_add) {
            new UpdateComicsAsyncTask()
                    .execute(new Comics(System.currentTimeMillis(), "Item " + (mAdapter.getCount() + 1)));
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
            List<Comics> list = DataManager.readComics();
            ComicsListFragment.this.mAdapter.clear();

            for (int ii=0; ii<list.size(); ii++) {
                publishProgress(list.get(ii));
                if (isCancelled()) break;
            }

            return list.size();
        }

        @Override
        protected void onProgressUpdate(Comics... values) {
            ComicsListFragment.this.mAdapter.insertOrUpdate(values[0]);
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
            //TODO salvare i dati con DataManager

            for (Comics comics : params) {
                publishProgress(comics);
            }
            return params.length;
        }

        @Override
        protected void onProgressUpdate(Comics... values) {
            int index = ComicsListFragment.this.mAdapter.insertOrUpdate(values[0]);
            //TODO aggiornare solo la vista dell'elemento modificato

        }

        @Override
        protected void onPostExecute(Integer integer) {
            //ComicsListFragment.this.mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Task asincrono per la rimoazione dei dati
     */
    private class RemoveComicsAsyncTask extends AsyncTask<Comics, Comics, Integer> {
        @Override
        protected Integer doInBackground(Comics... params) {
            return null;
        }

        @Override
        protected void onProgressUpdate(Comics... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            ComicsListFragment.this.mAdapter.notifyDataSetChanged();
        }
    }

}
