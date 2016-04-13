package it.amonshore.comikkua.ui;

import android.support.v7.widget.SearchView;

import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

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

    private RxSearchViewQueryTextListener() {
        mEventBus = new RxBus<>();
        mObservable = mEventBus.toObserverable()
                .distinctUntilChanged()
                .debounce(500, TimeUnit.MILLISECONDS)
                .skip(1) // salto il primo evento perché scatenato dall'apertura della SearchView ed è vuoto
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static RxSearchViewQueryTextListener create() {
        return new RxSearchViewQueryTextListener();
    }

    public RxSearchViewQueryTextListener listenOn(final SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Utils.d("A0061", "onQueryTextSubmit " + query);
                mEventBus.send(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Utils.d("A0061", "onQueryTextChange " + newText);
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
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String query) {
                        mListener.onLocalQuery(query);
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

        void onLocalQuery(String query);
    }

}
