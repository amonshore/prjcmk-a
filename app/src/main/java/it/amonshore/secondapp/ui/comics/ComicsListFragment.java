package it.amonshore.secondapp.ui.comics;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.ui.MainActivity;
import it.amonshore.secondapp.ui.OnChangePageListener;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class ComicsListFragment extends Fragment implements OnChangePageListener {

    private final static String STATE_ORDER = " stateOrder";

    private AbsListView mListView;
    private ComicsListAdapter mAdapter;
    private ActionMode mActionMode;

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
        mAdapter = new ComicsListAdapter(getActivity().getApplicationContext(), order);
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
                //Utils.d("onItemClick " + ((Comics) mAdapter.getItem(position)).getName());
                //showComicsEditor((Comics) mAdapter.getItem(position), false);
                showComicsDetail((Comics) mAdapter.getItem(position));
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
                inflater.inflate(R.menu.menu_comics_cab, menu);
                mActionMode = mode;
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Utils.d("onActionItemClicked " + item.getTitle());
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
        Utils.d("onPrepareOptionsMenu " + mAdapter.getOrder());
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
        Utils.d("onOptionsItemSelected " + item.getTitle());

        if (id == R.id.action_comics_add) {
            showComicsEditor(mAdapter.createNewComics(), true);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == ComicsEditorActivity.EDIT_COMICS_REQUEST) {
            Comics comics = (Comics)data.getSerializableExtra(ComicsEditorActivity.EXTRA_ENTRY);
            boolean isnew = data.getBooleanExtra(ComicsEditorActivity.EXTRA_IS_NEW, true);
            //TODO aggiungere alla lista e posizionarsi sull'elemento
            int position = mAdapter.insertOrUpdate(comics);
            //Utils.d("onActivityResult @" + index + " id " + comics.getId() + " " + comics.getName());
            mAdapter.notifyDataSetChanged();
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

    private void showComicsEditor(Comics comics, boolean isNew) {
        Intent intent = new Intent(getActivity(), ComicsEditorActivity.class);
        intent.putExtra(ComicsEditorActivity.EXTRA_ENTRY, comics);
        intent.putExtra(ComicsEditorActivity.EXTRA_IS_NEW, isNew);
        startActivityForResult(intent, ComicsEditorActivity.EDIT_COMICS_REQUEST);
    }

    private void showComicsDetail(Comics comics) {
        Intent intent = new Intent(getActivity(), ComicsDetailActivity.class);
        intent.putExtra(ComicsDetailActivity.EXTRA_ENTRY, comics);
        startActivity(intent);
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
            Utils.d("update comics " + index);
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
            Utils.d("delete comics " + values[0] + " -> " + res);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            ComicsListFragment.this.mAdapter.notifyDataSetChanged();
        }
    }

}
