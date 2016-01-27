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

//    private final Subject<Object, Object> mBus = new SerializedSubject<>(PublishSubject.create());

    private final Subject<T, T> mBus;

    public RxBus() {
        PublishSubject<T> subject = PublishSubject.create();
        mBus = new SerializedSubject<>(subject);
    }

    public void send(T e) {
        mBus.onNext(e);
    }

    public Observable<T> toObservable() {
        return mBus;
    }
}
