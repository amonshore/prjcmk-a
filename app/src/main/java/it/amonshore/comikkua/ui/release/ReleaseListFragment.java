package it.amonshore.comikkua.ui.release;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.data.ReleaseGroupHelper;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.ReleaseInfo;
import it.amonshore.comikkua.data.UndoHelper;
import it.amonshore.comikkua.ui.AFragment;
import it.amonshore.comikkua.ui.MainActivity;
import it.amonshore.comikkua.ui.SettingsActivity;
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
    private Comics mComics;
    private int mGroupMode;
    private boolean mGroupByMonth;
    private boolean mWeekStartOnMonday;

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
        final StickyListHeadersListView list = (StickyListHeadersListView)view.findViewById(R.id.lst_releases);
        //
        mAdapter = new ReleaseListAdapter(getActivity().getApplicationContext());
        mAdapter.setOnNumberViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = list.getPositionForView(v);
                if (position != ListView.INVALID_POSITION) {
                    //Utils.d(this.getClass(), "click number on " + position);
                    Release release = ((ReleaseInfo) mAdapter.getItem(position)).getRelease();
                    release.togglePurchased();
                    getDataManager().updateBestRelease(release.getComicsId());
                    //invece di aggionarnare tutta la lista, con conseguente scomparsa degli item (visto che possono cambiare gruppo)
                    //  aggiorno solo l'elemento corrente, così rimarrà nel suo gruppo semplicemente con uno stato diverso
                    getDataManager().notifyChangedButMe(DataManager.CAUSE_RELEASE_CHANGED, ReleaseListFragment.this);
                    //per aggiornare solo questo item richiamo adapter.getView(...) -> http://stackoverflow.com/questions/4075975/redraw-a-single-row-in-a-listview
                    mAdapter.getView(position, (View) v.getParent(), list);
                }
            }
        });
        //
        list.setAdapter(mAdapter);
        //questa è la vera lista
        mListView = list.getWrappedList();
        //A0022
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        setEmptyText(getString(R.string.release_empty_list));
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
                    //sono già ordinati in ordine crescente
                    long[] ags = mListView.getCheckedItemIds();
                    Integer[] igs = new Integer[ags.length];
                    //ma ho bisogno di rimuoverli in ordine inverso
                    for (int ii = ags.length-1, jj = 0; ii >=0; ii--, jj++) {
                        igs[jj] = (int)ags[ii];
                    }
                    new RemoveReleasesAsyncTask().execute(igs);
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
                long comicsId = data.getLongExtra(ReleaseEditorActivity.EXTRA_COMICS_ID,
                        ReleaseEditorActivity.COMICS_ID_NONE);
                if (comicsId != ReleaseEditorActivity.COMICS_ID_NONE) {
                    getDataManager().updateBestRelease(comicsId);
                    getDataManager().notifyChanged(DataManager.CAUSE_RELEASE_CHANGED);
                }
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
            getDataManager().notifyChanged(DataManager.CAUSE_RELEASES_MODE_CHANGED);
            getActivity().invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_releases_mode_shopping) {
            mGroupMode = ReleaseGroupHelper.MODE_SHOPPING;
            getDataManager().notifyChanged(DataManager.CAUSE_RELEASES_MODE_CHANGED);
            getActivity().invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_releases_mode_law) {
            mGroupMode = ReleaseGroupHelper.MODE_LAW;
            getDataManager().notifyChanged(DataManager.CAUSE_RELEASES_MODE_CHANGED);
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
    public void onDataChanged(int cause, boolean wasPosponed) {
        Utils.d(this.getClass(), "onDataChanged " + cause);
        //se è stato posticipato significa che è stato causato dal dettaglio
        // quindi non gestisco l'undo anche qua
        if (cause == DataManager.CAUSE_RELEASE_REMOVED && !wasPosponed) {
            mAdapter.notifyDataSetChanged();
            //la gestione dell'undo avviene tramite la classe UndoHelper
            //  che può tenere traccia degli elementi rimossi "marchiandoli" con un tag
            //  quindi una volta visualizzata la snackbar vengono "machiati" gli ultimi elementi eliminati
            //  e la snackbar potrà successivamente gestire (ripristinare o eliminare definitivamente)
            //  solo i suoi elementi (il tag è memorizzato nell'istanza della snackbar)
            final DataManager dataManager = getDataManager();
            final UndoHelper<Release> undoRelease = dataManager.getUndoRelease();
            SnackbarManager.show(
                    Snackbar
                            .with(getActivity().getApplicationContext())
                            .text(R.string.release_removed)
                            .actionLabel(R.string.undo)
                            .duration(5000L)
                            .actionListener(new ActionClickListener() {
                                @Override
                                public void onActionClicked(Snackbar snackbar) {
                                    int tag = (int) snackbar.getTag();
                                    Release release;
//                                    Utils.d(this.getClass(), "undo ... " + tag);
                                    while ((release = undoRelease.pop(tag)) != null) {
//                                        Utils.d(this.getClass(), "undo " + comics.getName());
                                        dataManager.getComics(release.getComicsId()).putRelease(release);
                                        dataManager.updateBestRelease(release.getComicsId());
                                    }
                                    dataManager.notifyChanged(DataManager.CAUSE_RELEASE_ADDED);
                                }
                            })
                            .eventListener(new com.nispok.snackbar.listeners.EventListenerAdapter() {
                                @Override
                                public void onShow(Snackbar snackbar) {
                                    int tag = undoRelease.retainElements();
//                                    Utils.d(this.getClass(), "undo show " + tag);
                                    snackbar.setTag(tag);
                                }

                                @Override
                                public void onShowByReplace(Snackbar snackbar) {
                                    int tag = undoRelease.retainElements();
//                                    Utils.d(this.getClass(), "undo show r " + tag);
                                    snackbar.setTag(tag);
                                }

                                @Override
                                public void onDismiss(Snackbar snackbar) {
                                    int tag = (int) snackbar.getTag();
//                                    Utils.d(this.getClass(), "dismiss r -> clear undo " + tag);
                                    undoRelease.removeElements(tag);
                                }

                                @Override
                                public void onDismissByReplace(Snackbar snackbar) {
                                    int tag = (int) snackbar.getTag();
//                                    Utils.d(this.getClass(), "dismiss r -> clear undo r " + tag);
                                    undoRelease.removeElements(tag);
                                }

                            }),
                    getActivity());

        } else
        //se la causa è il cambio pagina aggiorno i dati solo se l'adapter è vuoto
        if (mAdapter != null) {
            if ((cause & DataManager.CAUSE_PAGE_CHANGED) != DataManager.CAUSE_PAGE_CHANGED || mAdapter.isEmpty()) {
                Utils.d("needDataRefresh ok");
                new ReadReleasesAsyncTask().execute();
            }
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

    /**
     * Task asincrono per la rimoazione dei dati
     */
    private class RemoveReleasesAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            for (Integer position : params) {
                publishProgress(position);
            }
            return params.length;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //boolean res =
            ReleaseListFragment.this.mAdapter.remove(values[0]);
            //Utils.d("delete release " + values[0] + " -> " + res);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            ReleaseListFragment.this.getDataManager().notifyChanged(DataManager.CAUSE_RELEASE_REMOVED);
        }
    }



}
