package it.amonshore.comikkua.data;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.AIncrementalStart;
import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by narsenico on 07/06/16.
 *
 * A0068 Gestore eventi per la sincronizzazione remota dei dati
 */
class SyncEventHelper extends AIncrementalStart {

    public final static int SYNC_STARTED = 0;
    public final static int SYNC_RECEIVED = 1;
    public final static int SYNC_SENT = 2;
    public final static int SYNC_REFUSED = 101;
    public final static int SYNC_ERR = 102;
    public final static int SYNC_TIMEOUT = 103;

    private final static String SYNC_URL = "http://192.168.0.3:3000";

    public interface SyncListener {

        /**
         *
         * @param response  codice di risposta ricevuto dal server
         */
        void onResponse(int response);

        /**
         * TODO quali dati ricevere?
         *
         * @param data  dati ricevuti dal server
         */
        void onDataReceived(Object data);
    }

    private RxBus<DataEvent> mEventBus;
    private Observer<Long> mRemoteCheckStop;
    private Context mContext;
    private String mSyncId;
    private SyncListener mSyncListener;

    public SyncEventHelper(Context context) {
        mContext = context;
    }

    /**
     * Presenta il codice di sincronizzazione al server.
     * Tramite l'ascoltatore si potrà conoscere lo stato della sincronizzazione:
     * - SYNC_STARTED: codice sincronizzazione accettato
     * - SYNC_RECEIVED: sono stati ricevuti dei nuovi dati dal server
     * - SYNC_SENT: sono stati inviati con successo dei dati al server
     * - SYNC_REFUSED: il codice di sincronizzazione non è stato accettato
     * - SYNC_ERR: si è verifcato un errore durante la sincronizzazione
     * - SYNC_TIMEOUT: il codice di sincronizzazione non è più valido
     *
     * @param syncId    codice di sincronizzazione da usare in tutte le richieste al server
     * @param listener  ascoltatore per ricevere eventi dal processo di sincronizzazione
     */
    public void applySyncId(String syncId, SyncListener listener) {
        mSyncId = syncId;
        mSyncListener = listener;
        // TODO avvio sincronizzazione con la richiesta POST /v1/sync/<syncid>
        final String url = SYNC_URL + "/v1/hello";
        final JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Utils.d(SyncEventHelper.class, response.toString());
                        mRemoteCheckStop = checkRemote();
                        mSyncListener.onResponse(SYNC_STARTED);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO intercettare lo stato HTTP per sapere cosa rispondere (rifiuto syncid o errore)
                Utils.e(SyncEventHelper.class, "SYNC ERR", error);
                mSyncListener.onResponse(SYNC_ERR);
            }
        });

        VolleyHelper.getInstance(mContext).addToRequestQueue(req);
    }

    private Observer<Long> checkRemote() {
        final String url = SYNC_URL + "/v1/comics";
        final JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Utils.d(SyncEventHelper.class, "SYNC Data received");
                        //mSyncListener.onResponse(SYNC_RECEIVED);
                        // TODO se non ricevo dati per più di n volte segnalare timeout
                        //mSyncListener.onResponse(SYNC_TIMEOUT);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO intercettare lo stato HTTP per sapere cosa rispondere (rifiuto syncid o errore)
                Utils.e(SyncEventHelper.class, "SYNC ERR", error);
                mSyncListener.onResponse(SYNC_ERR);
            }
        });

        final PublishSubject<Long> stop = PublishSubject.create();

        Observable
                .interval(2, 5, TimeUnit.SECONDS)
                .takeUntil(stop)
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        Utils.d(SyncEventHelper.class, "SYNC check remote complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Utils.d(SyncEventHelper.class, "SYNC check remote error");
                    }

                    @Override
                    public void onNext(Long aLong) {
                        Utils.d(SyncEventHelper.class, "SYNC check remote " + aLong);
                        VolleyHelper.getInstance(mContext).addToRequestQueue(req);
                    }
                });

        return stop;
    }

    /**
     * Invia una azione che verrà gestita insieme ad altre sucessivamente.
     *
     * @param action    azione da eseguire sui dati (ACTION_ADD, ACTION_UPD, ACTION_DEL)
     * @param comicsId  id del comics su cui operare l'azione
     * @param releaseNumber numero della release su cui operare l'azione (NO_RELEASE per nessuna)
     */
    public void send(int action, long comicsId, int releaseNumber) {
        if (mEventBus != null) {
            final DataEvent event = new DataEvent();
            event.Action = action;
            event.ComicsId = comicsId;
            event.ReleaseNumber = releaseNumber;
            mEventBus.send(event);
        }
    }

    @Override
    protected void safeStart() {
        // TODO chiamare periodicamente il server per vedere se ci sono novità
        //  il server potrebbe anche rispondere con un errore di timeout sincronizzazione (non ci sono stati scambi di dati per troppo tempo)

        if (mEventBus == null) {
            final DataManager dataManager = DataManager.getDataManager();
            mEventBus = new RxBus<>();
            mEventBus.toObserverable()
                    .observeOn(Schedulers.newThread()) //gli eventi verranno consumati in un nuuovo scheduler
                    //raggruppo una serie di eventi (buffer) e li gestisco dopo che è passato un certo periodo di tempo senza altri eventi (debouce)
                    // TODO provo con un timeout di 1 secondo
                    .publish(new Func1<Observable<DataEvent>, Observable<List<DataEvent>>>() {
                        @Override
                        public Observable<List<DataEvent>> call(Observable<DataEvent> stream) {
                            return stream.buffer(stream.debounce(1000, TimeUnit.MILLISECONDS));
                        }
                    })
                    .subscribe(new Subscriber<List<DataEvent>>() {
                        @Override
                        public void onCompleted() {
                            Utils.d("RX SYNC end " + Utils.isMainThread());
                        }

                        @Override
                        public void onError(Throwable e) {
                            Utils.e("RX SYNC error", e);
                        }

                        @Override
                        public void onNext(List<DataEvent> dataEvents) {
                            Utils.d("RX SYNC " + dataEvents.size() + " " + Utils.isMainThread());

                            // TODO raccogliere tutti gli eventi e inviare una sola richiesta al server
                            // TODO convertire tutti gli eventi in JSON

                            if (dataEvents.size() > 0) {

                                final String url = SYNC_URL + "/v1/hello";
                                final JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Utils.d(SyncEventHelper.class, response.toString());
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Utils.e(SyncEventHelper.class, "SYNC ERR", error);
                                                mSyncListener.onResponse(SYNC_ERR);
                                            }
                                        });

                                VolleyHelper.getInstance(mContext).addToRequestQueue(req);

//                                final String userName = dataManager.getUserName();
//                                SQLiteDatabase database = null;
//                                try {
//                                    database = mDBHelper.getWritableDatabase();
//                                    for (DataEvent event : dataEvents) {
////                                    Utils.d(String.format("RX DATA act %s cid %s rid %s", event.Action, event.ComicsId, event.ReleaseNumber));
//                                        switch (event.Action) {
//                                            case DataManager.ACTION_CLEAR:
//                                                clear(database, userName);
//                                                break;
//                                            case DataManager.ACTION_ADD:
//                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
//                                                    writeComics(database, userName, dataManager.getComics(event.ComicsId), true);
//                                                } else {
//                                                    writeRelease(database, userName, dataManager.getComics(event.ComicsId)
//                                                            .getRelease(event.ReleaseNumber), true);
//                                                }
//                                                break;
//                                            case DataManager.ACTION_UPD:
//                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
//                                                    writeComics(database, userName, dataManager.getComics(event.ComicsId), false);
//                                                } else {
//                                                    writeRelease(database, userName, dataManager.getComics(event.ComicsId)
//                                                            .getRelease(event.ReleaseNumber), false);
//                                                }
//                                                break;
//                                            case DataManager.ACTION_DEL:
//                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
//                                                    deleteComics(database, userName, event.ComicsId);
//                                                } else {
//                                                    deleteRelease(database, userName, event.ComicsId, event.ReleaseNumber);
//                                                }
//                                                break;
//                                        }
//                                    }
//                                } catch (Exception ex) {
//                                    Utils.e(this.getClass(), "Write data", ex);
//                                } finally {
//                                    if (database != null) {
//                                        database.close();
//                                    }
//                                }
                            }
                        }
                    });

        }
    }

    @Override
    protected void safeStop() {
        if (mEventBus != null) {
            mEventBus.end(); //scatena onCompleted
            mEventBus = null;
        }

        // TODO abortire richieste http in corso, fermare loop richieste periodiche
        if (mRemoteCheckStop != null) {
            mRemoteCheckStop.onNext(null);
            mRemoteCheckStop = null;
        }
    }

    private final static class DataEvent {
        public int Action;
        public long ComicsId;
        public int ReleaseNumber;
    }

}
