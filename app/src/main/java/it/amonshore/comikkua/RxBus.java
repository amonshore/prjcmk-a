package it.amonshore.comikkua;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Created by narsenico on 19/01/16.
 *
 * http://nerds.weddingpartyapp.com/tech/2014/12/24/implementing-an-event-bus-with-rxjava-rxbus/
 */
public class RxBus<T> {

    private final Subject<T, T> mBus;

    public RxBus() {
        PublishSubject<T> subject = PublishSubject.create();
        mBus = new SerializedSubject<>(subject);
    }

    public void send(T t) {
        mBus.onNext(t);
    }

    public void end() {
        mBus.onCompleted();
    }

    public Observable<T> toObserverable() {
        return mBus;
    }

    public boolean hasObsevers() {
        return mBus.hasObservers();
    }
}
