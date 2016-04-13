package it.amonshore.comikkua.ui.comics;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.RequestCodes;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.UndoHelper;
import it.amonshore.comikkua.ui.AFragment;
import it.amonshore.comikkua.ui.MainActivity;
import it.amonshore.comikkua.ui.RxSearchViewQueryTextListener;
import it.amonshore.comikkua.ui.ScrollToTopListener;
import it.amonshore.comikkua.ui.remote.RemoteComicsActivity;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class ComicsListFragment extends AFragment implements ScrollToTopListener {

    //usato per lo stato dell'istanza
    private final static String STATE_ORDER = " stateOrder";

    private ListView mListView;
    private ComicsListAdapter mAdapter;
    private ActionMode mActionMode;
    private FloatingActionMenu mBtnMenu;
    //A0061
    private DataManager mDataManager;
    private RxSearchViewQueryTextListener mOnQueryTextListener;
    //
    private Animation mSlideDownAnimation;
    private Animation mSlideUpAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //deve essere chiamato in onCreate
        setHasOptionsMenu(true);
        //
        int order;
        if (savedInstanceState != null) {
            //recupero l'ordine usato in precedenza e salvato alla chiusura dell'activity
            order = savedInstanceState.getInt(STATE_ORDER, ComicsListAdapter.ORDER_BY_NAME);
        } else {
            //recupero l'ordinamento dalle preferenze
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            order = settings.getInt(STATE_ORDER, ComicsListAdapter.ORDER_BY_NAME);
        }
        mAdapter = new ComicsListAdapter(getActivity().getApplicationContext(), order);
        //A0061
        mDataManager = DataManager.getDataManager();
        mOnQueryTextListener = RxSearchViewQueryTextListener
                .create()
                .setOnQueryListener(new RxSearchViewQueryTextListener.OnQueryListener() {

                    @Override
                    public void onLocalQuery(String query) {
                        Utils.d("A0061", "onLocalQuery " + query);

                        mDataManager.setComicsFilter(query);
                        mDataManager.notifyChanged(DataManager.CAUSE_COMICS_FILTERED | DataManager.CAUSE_LOADING);
                    }
                });
        //
        mSlideDownAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down);
        mSlideUpAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);
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
        editor.apply();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comics_list, container, false);

        // listener fab
        mBtnMenu = (FloatingActionMenu) view.findViewById(R.id.fab_comics_menu);
        final FloatingActionButton btnAdd = (FloatingActionButton)view.findViewById(R.id.fab_comics_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComicsEditor();
            }
        });
        final Context context = getActivity();
        final FloatingActionButton btnSearchRemote = (FloatingActionButton)view.findViewById(R.id.fab_comics_search);
        btnSearchRemote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, RemoteComicsActivity.class);
                startActivityForResult(intent, RequestCodes.QUERY_REMOTE_REQUEST);
            }
        });
        //
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        //A0022
        TextView emptyView = (TextView)view.findViewById(android.R.id.empty);
        emptyView.setText(getString(R.string.comics_empty_list));
        mListView.setEmptyView(emptyView);
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
                inflater.inflate(R.menu.menu_comics_cab, menu);
                mActionMode = mode;
                // chiudo il menu, avvio l'animazione e nascondo
                mBtnMenu.close(false);
                mBtnMenu.startAnimation(mSlideDownAnimation);
                mBtnMenu.setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                final DataManager dataManager = DataManager.getDataManager();
//                Utils.d("onActionItemClicked " + item.getTitle());
                //risponde alla selezione di una azione del menu_comics_cab
                long menuId = item.getItemId();
                if (menuId == R.id.action_comics_delete) {
                    long[] ags = mListView.getCheckedItemIds();
                    for (int ii = ags.length - 1; ii >= 0; ii--) {
                        // TODO: rimuovere con DataManager e non con l'adapter
//                        mAdapter.remove(ags[ii]);
                        if (dataManager.remove(ags[ii])) {
                            //A0049
                            dataManager.updateData(DataManager.ACTION_DEL, ags[ii], DataManager.NO_RELEASE);
                        }
                    }
                    mAdapter.refresh();
                    dataManager.notifyChanged(DataManager.CAUSE_COMICS_REMOVED);
                    finishActionMode();
                    return true;
                } else if (menuId == R.id.action_comics_share) {
                    //A0034
                    long[] ags = mListView.getCheckedItemIds();
                    String[] rows = new String[ags.length];
                    for (int ii = 0; ii < rows.length; ii++) {
                        rows[ii] = dataManager.getComics(ags[ii]).getName();
                    }
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, Utils.join("\n", false, rows));
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share)));
                    return true;
                } else if (menuId == R.id.action_comics_web_search) {
                    //A0042 cerco solo il primo elemento selezionato
                    Comics comics = getDataManager().getComics(mListView.getCheckedItemIds()[0]);
                    String query = Utils.join(" ", true, comics.getName(), comics.getAuthors(),
                            comics.getPublisher());
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, query);
                    startActivity(intent);
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
                mBtnMenu.startAnimation(mSlideUpAnimation);
                mBtnMenu.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //A0061
        mOnQueryTextListener.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics, menu);

        //A0061
        final MenuItem menuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();
        final SearchManager searchManager = (SearchManager) this.getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getActivity().getComponentName()));

        // associo la view al listener e bindo gli eventi
        mOnQueryTextListener
//                .enableRemoteQuery(true) // TODO: leggere da preferenze
                .listenOn(searchView)
                .bind();
        // se i comics sono filtrati apro la searchview e imposto il filtro
        if (!Utils.isNullOrEmpty(mDataManager.getComicsFilter())) {
            menuItem.expandActionView();
            // non ho bisogno di rieseguire la query perché i fumetti dovrebbero essere già filtrati
            searchView.setQuery(mDataManager.getComicsFilter(), false);
        }
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
//        Utils.d("onOptionsItemSelected " + item.getTitle());

        if (id == R.id.action_comics_sort_by_name) {
            mAdapter.setOrder(ComicsListAdapter.ORDER_BY_NAME);
            mAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
            mListView.setFastScrollEnabled(true);
            mListView.setFastScrollAlwaysVisible(true);
            return true;
        }  else if (id == R.id.action_comics_sort_by_release) {
            mAdapter.setOrder(ComicsListAdapter.ORDER_BY_BEST_RELEASE);
            mAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
            mListView.setFastScrollEnabled(false);
            mListView.setFastScrollAlwaysVisible(false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == RequestCodes.EDIT_COMICS_REQUEST) {
            mAdapter.notifyDataSetChanged();
            final DataManager dataManager = DataManager.getDataManager();
            final long comicsId = data.getLongExtra(ComicsEditorActivity.EXTRA_COMICS_ID, DataManager.NO_COMICS);
            //A0061 dataManager.notifyChangedButMe(DataManager.CAUSE_COMICS_CHANGED, this);
            dataManager.notifyChanged(DataManager.CAUSE_COMICS_CHANGED);
            //A0056
            //A0049
//            dataManager.updateData(DataManager.ACTION_ADD, comicsId, DataManager.NO_RELEASE);
            //A0047 mostro il dettaglio del comics, mi pare un'idea migliore
            // rispetto all'editare una nuova release
            showComicsDetail(comicsId);
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
                                        //A0049
                                        dataManager.updateData(DataManager.ACTION_ADD, comics.getId(), DataManager.NO_RELEASE);
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
        } else if (cause != DataManager.CAUSE_RELEASES_MODE_CHANGED &&
                ((cause & DataManager.CAUSE_PAGE_CHANGED) != DataManager.CAUSE_PAGE_CHANGED || mAdapter.isEmpty())) {
            //se la causa è la modifica della modalità di visualizzazione delle release non mi ineressa
            //se la causa è il cambio pagina aggiorno i dati solo se l'adapter è vuoto
            //A0040 new UpdateListAsyncTask().execute();
            Handler mh = new Handler(getActivity().getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        mAdapter.refresh();
                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getOrder() == ComicsListAdapter.ORDER_BY_NAME) {
                            mListView.setFastScrollEnabled(true);
                            mListView.setFastScrollAlwaysVisible(true);
                        } else {
                            mListView.setFastScrollEnabled(false);
                            mListView.setFastScrollAlwaysVisible(false);
                        }
                    } catch (Exception ex) {
                        Utils.e("A0040 update comics list", ex);
                    }
                }
            };
            mh.post(runnable);
        }
    }

    private void showComicsEditor() {
        Intent intent = new Intent(getActivity(), ComicsEditorActivity.class);
        intent.putExtra(ComicsEditorActivity.EXTRA_COMICS_ID, ComicsEditorActivity.COMICS_ID_NEW);
        startActivityForResult(intent, RequestCodes.EDIT_COMICS_REQUEST);
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
