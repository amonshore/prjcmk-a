package it.amonshore.comikkua.ui;

import android.app.SearchManager;
import android.database.Cursor;
import android.provider.SearchRecentSuggestions;
import android.support.v7.widget.SearchView;

import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by narsenico on 21/02/16.
 *
 * RxSearchViewQueryTextListener
 *      .create()                   -> crea nuova istanza
 *      .setOnQueryListener(...)    -> imposta listener eventi
 *      .provideSuggestions(...)    -> suggerimenti
 *      .listenOn(...)              -> SearchView da ascoltare
 *      .bind()                     -> attiva collegamenti
 *
 * al termine chiamare unbind()
 */
public class RxSearchViewQueryTextListener {

    private RxBus<String> mEventBus;
    private Observable<String> mObservable;
    private Subscription mSubscription;
    private OnQueryListener mListener;
    private String mSuggestionAurhority;
    private int mSuggestionMode;
    private SearchRecentSuggestions mSearchRecentSuggestions;

    private RxSearchViewQueryTextListener() {
        mEventBus = new RxBus<>();
        mObservable = mEventBus.toObserverable()
                .distinctUntilChanged()
                .debounce(1000, TimeUnit.MILLISECONDS)
//                .skip(1) // salto il primo evento perché scatenato dall'apertura della SearchView ed è vuoto
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static RxSearchViewQueryTextListener create() {
        return new RxSearchViewQueryTextListener();
    }

    public RxSearchViewQueryTextListener provideSuggestions(String authority, int mode) {
        mSuggestionAurhority = authority;
        mSuggestionMode = mode;

        return this;
    }

    public RxSearchViewQueryTextListener listenOn(final SearchView searchView) {
        if (!Utils.isNullOrEmpty(mSuggestionAurhority)) {
            mSearchRecentSuggestions = new SearchRecentSuggestions(searchView.getContext(),
                    mSuggestionAurhority, mSuggestionMode);
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return true;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    // recupero il suggerimento selezionato ed eseguo la query
                    Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
                    int indexColumnSuggestion = cursor.getColumnIndex( SearchManager.SUGGEST_COLUMN_TEXT_1 );
                    searchView.setQuery(cursor.getString(indexColumnSuggestion), false);
                    return true;
                }
            });
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
//                Utils.d("A0061", "onQueryTextSubmit " + query);
                mEventBus.send(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
//                Utils.d("A0061", "onQueryTextChange " + newText);
                mEventBus.send(newText);
                return true;
            }
        });
        return this;
    }

    public RxSearchViewQueryTextListener setOnQueryListener(OnQueryListener listener) {
        mListener = listener;
        return this;
    }

    public RxSearchViewQueryTextListener bind() {
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            unbind();
        }

        Utils.d("A0061", "bind");
        mSubscription = mObservable
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Utils.d("A0061", "RxSearchViewQueryTextListener onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Utils.e(RxSearchViewQueryTextListener.this.getClass(), "Observer.onError", e);
                    }

                    @Override
                    public void onNext(String s) {
                        if (mSearchRecentSuggestions != null) {
                            mSearchRecentSuggestions.saveRecentQuery(s, "prova prova");
                        }

                        mListener.onQuery(s);
                    }
                });

        return this;
    }

    public RxSearchViewQueryTextListener unbind() {
        Utils.d("A0061", "unbind");
        if (mSubscription != null) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }
        return this;
    }

    public interface OnQueryListener {

        void onQuery(String query);
    }

}
