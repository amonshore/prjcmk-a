package it.amonshore.secondapp.ui.comics;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.github.clans.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.UndoHelper;
import it.amonshore.secondapp.ui.AFragment;
import it.amonshore.secondapp.ui.MainActivity;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class ComicsListFragment extends AFragment {

    //usato per lo stato dell'istanza
    private final static String STATE_ORDER = " stateOrder";

    private AbsListView mListView;
    private ComicsListAdapter mAdapter;
    private ActionMode mActionMode;
    private FloatingActionButton mBtnAdd;

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
        mAdapter = new ComicsListAdapter(getActivity().getApplicationContext(), order);
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
        View view = inflater.inflate(R.layout.fragment_comics_list, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        //
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showComicsDetail(((Comics) mAdapter.getItem(position)).getId());
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
                mBtnAdd.setVisibility(View.INVISIBLE);
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
                mBtnAdd.setVisibility(View.VISIBLE);
            }
        });

        //listener fab
        mBtnAdd = ((FloatingActionButton)view.findViewById(R.id.fab_comics_add));
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComicsEditor(0);
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if ((mAdapter.getOrder() & ComicsListAdapter.ORDER_BY_NAME) == ComicsListAdapter.ORDER_BY_NAME) {
            menu.findItem(R.id.action_comics_sort_by_name).setVisible(false);
            menu.findItem(R.id.action_comics_sort_by_release).setVisible(true);
        } else {
            menu.findItem(R.id.action_comics_sort_by_name).setVisible(true);
            menu.findItem(R.id.action_comics_sort_by_release).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Utils.d("onOptionsItemSelected " + item.getTitle());

        if (id == R.id.action_comics_sort_by_name) {
            mAdapter.setOrder(ComicsListAdapter.ORDER_BY_NAME);
            mAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
            return true;
        }  else if (id == R.id.action_comics_sort_by_release) {
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
            long comicsId = data.getLongExtra(ComicsEditorActivity.EXTRA_COMICS_ID, 0);
            Comics comics = getDataManager().getComics(comicsId);
            int position = mAdapter.insertOrUpdate(comics);
            mAdapter.notifyDataSetChanged();
            getDataManager().notifyChangedButMe(DataManager.CAUSE_COMICS_CHANGED, this);
        }
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
    public void onDataChanged(int cause, boolean wasPosponed) {
        Utils.d(this.getClass(), "onDataChanged " + cause);
        //
        if (cause == DataManager.CAUSE_COMICS_REMOVED) {
            mAdapter.notifyDataSetChanged();
            //la gestione dell'undo avviene tramite la classe UndoHelper
            //  che può tenere traccia degli elementi rimossi "marchiandoli" con un tag
            //  quindi una volta visualizzata la snackbar vengono "machiati" gli ultimi elementi eliminati
            //  e la snackbar potrà successivamente gestire (ripristinare o eliminare definitivamente)
            //  solo i suoi elementi (il tag è memorizzato nell'istanza della snackbar)
            final DataManager dataManager = getDataManager();
            final UndoHelper<Comics> undoComics = dataManager.getUndoComics();
            SnackbarManager.show(
                    Snackbar
                            .with(getActivity().getApplicationContext())
                            .text(R.string.comics_removed)
                            .actionLabel(R.string.undo)
                            .duration(5000L)
                            .actionListener(new ActionClickListener() {
                                @Override
                                public void onActionClicked(Snackbar snackbar) {
                                    int tag = (int) snackbar.getTag();
                                    Comics comics;
//                                    Utils.d(this.getClass(), "undo ... " + tag);
                                    while ((comics = undoComics.pop(tag)) != null) {
//                                        Utils.d(this.getClass(), "undo " + comics.getName());
                                        dataManager.put(comics);
                                    }
                                    dataManager.notifyChanged(DataManager.CAUSE_COMICS_ADDED);
                                }
                            })
                            .eventListener(new com.nispok.snackbar.listeners.EventListenerAdapter() {
                                @Override
                                public void onShow(Snackbar snackbar) {
                                    int tag = undoComics.retainElements();
//                                    Utils.d(this.getClass(), "undo show " + tag);
                                    snackbar.setTag(tag);
                                }

                                @Override
                                public void onShowByReplace(Snackbar snackbar) {
                                    int tag = undoComics.retainElements();
//                                    Utils.d(this.getClass(), "undo show r " + tag);
                                    snackbar.setTag(tag);
                                }

                                @Override
                                public void onDismiss(Snackbar snackbar) {
                                    int tag = (int) snackbar.getTag();
//                                    Utils.d(this.getClass(), "dismiss r -> clear undo " + tag);
                                    undoComics.removeElements(tag);
                                }

                                @Override
                                public void onDismissByReplace(Snackbar snackbar) {
                                    int tag = (int) snackbar.getTag();
//                                    Utils.d(this.getClass(), "dismiss r -> clear undo r " + tag);
                                    undoComics.removeElements(tag);
                                }

                            }),
                    getActivity());
        } else
        //se la causa è la modifica dela modalità di visualizzazione delle release non mi ineressa
        if (cause != DataManager.CAUSE_RELEASES_MODE_CHANGED) {
            //se la causa è il cambio pagina aggiorno i dati solo se l'adapter è vuoto
            if ((cause & DataManager.CAUSE_PAGE_CHANGED) != DataManager.CAUSE_PAGE_CHANGED || mAdapter.isEmpty()) {
                new UpdateListAsyncTask().execute();
            }
        }
    }

    private void showComicsEditor(long comicsId) {
        Intent intent = new Intent(getActivity(), ComicsEditorActivity.class);
        intent.putExtra(ComicsEditorActivity.EXTRA_COMICS_ID, comicsId);
        startActivityForResult(intent, ComicsEditorActivity.EDIT_COMICS_REQUEST);
    }

    private void showComicsDetail(long comicsId) {
        Intent intent = new Intent(getActivity(), ComicsDetailActivity.class);
        intent.putExtra(ComicsDetailActivity.EXTRA_COMICS_ID, comicsId);
        startActivity(intent);
    }

    /**
     * Task asincrono per aggiornamento della lista
     */
    private class UpdateListAsyncTask extends AsyncTask<Void, Comics, Integer> {
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
     * Task asincrono per la rimoazione dei dati
     */
    private class RemoveComicsAsyncTask extends AsyncTask<Long, Long, Integer> {
        @Override
        protected Integer doInBackground(Long... params) {
            for (Long id : params) {
                publishProgress(id);
            }
            return params.length;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
//            boolean res =
              ComicsListFragment.this.mAdapter.remove(values[0]);
//            Utils.d("delete comics " + values[0] + " -> " + res);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            ComicsListFragment.this.getDataManager().notifyChanged(DataManager.CAUSE_COMICS_REMOVED);
        }
    }

}
