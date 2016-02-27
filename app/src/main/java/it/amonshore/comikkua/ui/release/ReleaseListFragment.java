package it.amonshore.comikkua.ui.release;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import it.amonshore.comikkua.RequestCodes;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.MultiReleaseInfo;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.data.ReleaseGroupHelper;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.ReleaseInfo;
import it.amonshore.comikkua.data.UndoHelper;
import it.amonshore.comikkua.ui.AFragment;
import it.amonshore.comikkua.ui.MainActivity;
import it.amonshore.comikkua.ui.ScrollToTopListener;
import it.amonshore.comikkua.ui.comics.ComicsDetailActivity;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Narsenico on 15/05/2015.
 */
public class ReleaseListFragment extends AFragment implements ScrollToTopListener {

    //usato per lo stato dell'istanza
    public final static String STATE_GROUP_MODE = " stateMode";

    private ListView mListView;
    private ReleaseListAdapter mAdapter;
    private ActionMode mActionMode;
    private Comics mComics;
    private int mGroupMode;
    private boolean mGroupByMonth;
    private boolean mWeekStartOnMonday;

    /**
     *
     * @return  ritorna la modalità di raggruppamento risorse attuale
     */
    public int getGroupMode() {
        return mGroupMode;
    }

    /**
     *
     * @param comics    usato per filtrare per release, null per considearle tutte
     * @param groupMode modalità di raggruppamento delle release
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
        if (savedInstanceState != null) {
            //recupero la modalità in precedenza e salvato alla chiusura dell'activity
            mGroupMode = savedInstanceState.getInt(STATE_GROUP_MODE, ReleaseGroupHelper.MODE_CALENDAR);
        } else {
            //recupero la modalità dalle preferenze
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            mGroupMode = settings.getInt(STATE_GROUP_MODE, ReleaseGroupHelper.MODE_CALENDAR);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        //salvo la modalità
        savedInstanceState.putInt(STATE_GROUP_MODE, mGroupMode);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();

        //salvo le preferenze (ma solo se il fragment non è caricato nel dettaglio)
        if (mComics == null) {
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(STATE_GROUP_MODE, mGroupMode);
            editor.apply();
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
                final int position = list.getPositionForView(v);
                final DataManager dataManager = DataManager.getDataManager();
                if (position != ListView.INVALID_POSITION) {
                    //Utils.d(this.getClass(), "click number on " + position);
                    Release release = ((ReleaseInfo) mAdapter.getItem(position)).getRelease();
                    release.togglePurchased();
                    dataManager.updateBestRelease(release.getComicsId());
                    //invece di aggionarnare tutta la lista, con conseguente scomparsa degli item (visto che possono cambiare gruppo)
                    //  aggiorno solo l'elemento corrente, così rimarrà nel suo gruppo semplicemente con uno stato diverso
                    dataManager.notifyChangedButMe(DataManager.CAUSE_RELEASE_CHANGED, ReleaseListFragment.this);
                    //A0049
                    dataManager.updateData(DataManager.ACTION_UPD, release.getComicsId(), release.getNumber());
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
        TextView emptyView = (TextView)view.findViewById(android.R.id.empty);
        emptyView.setText(getString(R.string.release_empty_list));
        mListView.setEmptyView(emptyView);
        //
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //A0041 se MultiReleaseInfo apro il dettaglio comics
                ReleaseInfo ri = (ReleaseInfo) mAdapter.getItem(position);
                Release release = ri.getRelease();
                if (ri instanceof MultiReleaseInfo) {
                    showComicsDetail(release.getComicsId());
                } else {
                    showReleaseEditor(release.getComicsId(), release.getNumber());
                }
            }
        });
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                //Utils.d("onItemCheckedStateChanged " + position);
                int count = mListView.getCheckedItemCount();
                if (count == 1) {
                    mode.setTitle(getString(R.string.selected_item, count));
                } else {
                    mode.setTitle(getString(R.string.selected_items, count));
                }
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
                final DataManager dataManager = DataManager.getDataManager();
                //risponde alla selezione di una azione del menu_releases_cab
                long menuId = item.getItemId();
                if (menuId == R.id.action_release_delete) {
                    //sono già ordinati in ordine crescente
                    long[] ags = mListView.getCheckedItemIds();
                    //visto che l'adapter considera come id la posizione dell'elemento
                    //posso usare l'id come posizione per rimuoverli dall'adapter
                    for (int ii = ags.length - 1; ii >= 0; ii--) {
                        //A0049
                        ReleaseInfo ri = (ReleaseInfo)mAdapter.getItem((int) ags[ii]);
                        dataManager.updateData(DataManager.ACTION_DEL, ri.getRelease().getComicsId(), ri.getRelease().getNumber());
                        mAdapter.remove((int) ags[ii]);
                    }
                    dataManager.notifyChanged(DataManager.CAUSE_RELEASE_REMOVED);
                    finishActionMode();
                    return true;
                } else if (menuId == R.id.action_release_share) {
                    //A0034
                    long[] ags = mListView.getCheckedItemIds();
                    String[] rows = new String[ags.length];
                    for (int ii = 0; ii < rows.length; ii++) {
                        ReleaseInfo ri = (ReleaseInfo) mAdapter.getItem((int) ags[ii]);
                        rows[ii] = dataManager.getComics(ri.getRelease().getComicsId()).getName() +
                                " #" + ri.getRelease().getNumber() + (ri.getRelease().isWishlist() ? "" : " - " + Utils.formatReleaseLongDate(ri.getRelease().getDate()));
                    }
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, Utils.join("\n", false, rows));
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share)));
                    return true;
                } else if (menuId == R.id.action_comics_web_search) {
                    long[] ags = mListView.getCheckedItemIds();
                    ReleaseInfo ri = (ReleaseInfo) mAdapter.getItem((int) ags[0]);
                    Comics comics = dataManager.getComics(ri.getRelease().getComicsId());
//                    String query = Utils.join(" ", true, comics.getName(), comics.getAuthors(),
//                            comics.getPublisher());
                    String query = Utils.join(" ", true, comics.getPublisher(),
                            comics.getName(), "" + ri.getRelease().getNumber());
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, query);
                    startActivity(intent);
                    return true;
                } else if (menuId == R.id.action_release_ordered) { //A0057
                    long[] ags = mListView.getCheckedItemIds();
                    boolean ordered = false;
                    for (int ii = 0; ii < ags.length; ii++) {
                        ReleaseInfo ri = (ReleaseInfo)mAdapter.getItem((int) ags[ii]);
                        if (ii == 0) ordered = !ri.getRelease().isOrdered();
                        ri.getRelease().setOrdered(ordered);
                        dataManager.updateData(DataManager.ACTION_UPD, ri.getRelease().getComicsId(), ri.getRelease().getNumber());
                    }
                    dataManager.notifyChanged(DataManager.CAUSE_RELEASE_CHANGED);
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
            if (requestCode == RequestCodes.EDIT_RELEASE_REQUEST) {
                final DataManager dataManager = DataManager.getDataManager();
                final long comicsId = data.getLongExtra(ReleaseEditorActivity.EXTRA_COMICS_ID,
                        ReleaseEditorActivity.COMICS_ID_NONE);
                if (comicsId != ReleaseEditorActivity.COMICS_ID_NONE) {
                    dataManager.updateBestRelease(comicsId);
                    dataManager.notifyChanged(DataManager.CAUSE_RELEASE_CHANGED);
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

    @Override
    public void finishActionMode() {
        if (mActionMode != null)
            mActionMode.finish();
    }

    @Override
    public void onDataChanged(int cause, boolean wasPosponed) {
        Utils.d(this.getClass(), "onDataChanged " + cause);
        final DataManager dataManager = getDataManager();
        //se è stato posticipato significa che è stato causato dal dettaglio
        // quindi non gestisco l'undo anche qua
        if (cause == DataManager.CAUSE_RELEASE_REMOVED && !wasPosponed) {
            mAdapter.notifyDataSetChanged();
            //la gestione dell'undo avviene tramite la classe UndoHelper
            //  che può tenere traccia degli elementi rimossi "marchiandoli" con un tag
            //  quindi una volta visualizzata la snackbar vengono "machiati" gli ultimi elementi eliminati
            //  e la snackbar potrà successivamente gestire (ripristinare o eliminare definitivamente)
            //  solo i suoi elementi (il tag è memorizzato nell'istanza della snackbar)
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
                                        //A0049
                                        dataManager.updateData(DataManager.ACTION_ADD, release.getComicsId(), release.getNumber());
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
                Utils.d(this.getClass(), "needDataRefresh ok " + mComics);
                //A0040 new ReadReleasesAsyncTask().execute();

                mGroupByMonth = dataManager.getPreference(DataManager.KEY_PREF_GROUP_BY_MONTH, false);
                mWeekStartOnMonday = dataManager.getPreference(DataManager.KEY_PREF_WEEK_START_ON_MONDAY, false);

                Handler mh = new Handler(getActivity().getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mAdapter.refresh(mComics,
                                    mGroupMode,
                                    mGroupByMonth,
                                    mWeekStartOnMonday);
                            mAdapter.notifyDataSetInvalidated();
                        } catch (Exception ex) {
                            Utils.e("A0040 refresh", ex);
                        }
                    }
                };
                mh.post(runnable);
            }
        }
    }

    private void showReleaseEditor(long comicsId, int number) {
        Intent intent = new Intent(getActivity(), ReleaseEditorActivity.class);
        intent.putExtra(ReleaseEditorActivity.EXTRA_COMICS_ID, comicsId);
        intent.putExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, number);
        startActivityForResult(intent, RequestCodes.EDIT_RELEASE_REQUEST);
    }

    private void showComicsDetail(long comicsId) {
        Intent intent = new Intent(getActivity(), ComicsDetailActivity.class);
        intent.putExtra(ComicsDetailActivity.EXTRA_COMICS_ID, comicsId);
        startActivity(intent);
    }

    @Override
    public void scrollToTop() {
        //A0053
        if (mAdapter.getCount() > 0) {
            mListView.smoothScrollToPosition(0);
        }
    }

}
