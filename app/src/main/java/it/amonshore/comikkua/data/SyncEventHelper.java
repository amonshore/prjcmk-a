package it.amonshore.comikkua.data;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import it.amonshore.comikkua.AIncrementalStart;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.VolleyNoRetryPolicy;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

/**
 * Created by narsenico on 07/06/16.
 *
 * A0068 Gestore eventi per la sincronizzazione remota dei dati.
 *
 * - presentazione syncid (applySyncId) -> SYNC_READY
 * -
 *
 */
class SyncEventHelper extends AIncrementalStart {

    public final static int SYNC_READY = 0;
    public final static int SYNC_STARTED = 10;
    public final static int SYNC_SENT = 30;
    public final static int SYNC_REFUSED = 101;
    public final static int SYNC_ERR = 102;
    public final static int SYNC_EXPIRED = 103;
    //TODO parametrizzare il timeout ricevendolo insieme al syncid?
    private final static long NODATA_TIMEOUT = 30_000L;
    private final static long NODATA_TIMEOUT_NANO = NODATA_TIMEOUT * 1_000_000L;

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
    private String mSyncUrl;
    private int mSyncTime;
    private long mLastDataChanged;
    private SyncListener mSyncListener;

    public SyncEventHelper(Context context) {
        mContext = context;
    }

    /**
     * Presenta il codice di sincronizzazione al server.
     * Tramite l'ascoltatore si potrà conoscere lo stato della sincronizzazione:
     * - SYNC_READY: codice sincronizzazione accettato (ma la sincronizzazione non è ancora attiva)
     * - SYNC_STARTED: la sincronizzazione dei dati è attiva
     * - SYNC_RECEIVED: sono stati ricevuti dei nuovi dati dal server
     * - SYNC_SENT: sono stati inviati con successo dei dati al server
     * - SYNC_REFUSED: il codice di sincronizzazione non è stato accettato
     * - SYNC_ERR: si è verifcato un errore durante la sincronizzazione
     * - SYNC_EXPIRED: il codice di sincronizzazione non è più valido
     *
     * @param syncUrl   url da contattare per la sincronizzazione
     * @param syncId    codice di sincronizzazione da usare in tutte le richieste al server
     * @param listener  ascoltatore per ricevere eventi dal processo di sincronizzazione
     */
    public void applySyncId(final String syncUrl, final String syncId, SyncListener listener) {
        mSyncListener = listener;
        //
        final String url = syncUrl + "/sync/" + syncId;
        final JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Utils.d(SyncEventHelper.class, response.toString());
                            if (syncId.equals(response.getString("sid"))) {
                                mSyncId = syncId;
                                mSyncUrl = syncUrl;
                                mSyncListener.onResponse(SYNC_READY);
                            } else {
                                Utils.e(SyncEventHelper.class, "applySincId error: wrong sid received");
                                mSyncListener.onResponse(SYNC_REFUSED);
                            }
                        } catch (JSONException jsonex) {
                            Utils.e(SyncEventHelper.class, "applySincId error", jsonex);
                            mSyncListener.onResponse(SYNC_ERR);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Utils.e(SyncEventHelper.class, "applySincId error", error);
                if (error.networkResponse != null) {
                    switch (error.networkResponse.statusCode) {
                        case 403:
                            mSyncListener.onResponse(SYNC_EXPIRED);
                            break;
                        case 404:
                            mSyncListener.onResponse(SYNC_REFUSED);
                            break;
                        default:
                            mSyncListener.onResponse(SYNC_ERR);
                            break;
                    }
                } else {
                    mSyncListener.onResponse(SYNC_ERR);
                }
            }
        });
        //volley inoltra nuovamente la richiesta a fronte di un 403
        //  prevengo questo comportamento modificando le policy
        req.setRetryPolicy(new VolleyNoRetryPolicy());
        VolleyHelper.getInstance(mContext).addToRequestQueue(req);
    }

    /**
     * Chiama periodicamente il server in attesa di nuovi dati.
     * Se viene emesso un item da mRemoteCheckStop il controllo remoto viene terminato.
     */
    private void startCheckRemote() {
        final Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Utils.d(SyncEventHelper.class, response.toString());
                try {
                    // resp: { "sid": <sid>, "data": [] }
                    if (response.getJSONArray("data").length() == 0) {
                        // se non ho ricevuto dati per troppo tempo invio il segnale di sync scaduta
                        if (System.nanoTime() - mLastDataChanged > NODATA_TIMEOUT_NANO) {
                            Utils.d(SyncEventHelper.class, "SYNC EXPIRED");
                            mSyncListener.onResponse(SYNC_EXPIRED);
                        }
                    } else {
                        //TODO: nuovi dati ricevuti
                        //mLastDataChanged = System.nanoTime();
                        //mSyncListener.onDataReceived(...);
                    }
                } catch (JSONException jsonex) {
                    Utils.e(SyncEventHelper.class, "SYNC ERR (time " + mSyncTime + ")", jsonex);
                    mSyncListener.onResponse(SYNC_ERR);
                }
            }
        };
        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Utils.e(SyncEventHelper.class, "SYNC ERR (time " + mSyncTime + ")", error);
                mSyncListener.onResponse(SYNC_ERR);
            }
        };
        final PublishSubject<Long> stop = PublishSubject.create();
        Observable
                .interval(2, 5, TimeUnit.SECONDS)
                .takeUntil(stop)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        //Utils.d(SyncEventHelper.class, "SYNC check remote " + aLong);
                        final String url = mSyncUrl + "/sync/" + mSyncId + "/" + mSyncTime;
                        final JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                                responseListener, errorListener);
                        VolleyHelper.getInstance(mContext).addToRequestQueue(req);
                    }
                });

        mRemoteCheckStop = stop;
    }

    private void startCheckLocal() {
//        //  il server potrebbe anche rispondere con un errore di timeout sincronizzazione (non ci sono stati scambi di dati per troppo tempo)
//        if (mEventBus == null) {
//            mEventBus = new RxBus<>();
//            mEventBus.toObserverable()
//                    .observeOn(Schedulers.newThread()) //gli eventi verranno consumati in un nuuovo scheduler
//                    //raggruppo una serie di eventi (buffer) e li gestisco dopo che è passato un certo periodo di tempo senza altri eventi (debouce)
//                    // TODO provo con un timeout di 1 secondo
//                    .publish(new Func1<Observable<DataEvent>, Observable<List<DataEvent>>>() {
//                        @Override
//                        public Observable<List<DataEvent>> call(Observable<DataEvent> stream) {
//                            return stream.buffer(stream.debounce(1000, TimeUnit.MILLISECONDS));
//                        }
//                    })
//                    .subscribe(new Subscriber<List<DataEvent>>() {
//                        @Override
//                        public void onCompleted() {
//                            Utils.d("RX SYNC end " + Utils.isMainThread());
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            Utils.e("RX SYNC error", e);
//                        }
//
//                        @Override
//                        public void onNext(List<DataEvent> dataEvents) {
//                            Utils.d("RX SYNC " + dataEvents.size() + " " + Utils.isMainThread());
//
//                            // TODO raccogliere tutti gli eventi e inviare una sola richiesta al server
//                            // TODO convertire tutti gli eventi in JSON
//
//                            if (dataEvents.size() > 0) {
//
//                                final String url = mSyncUrl + "/v1/hello";
//                                final JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
//                                        new Response.Listener<JSONObject>() {
//                                            @Override
//                                            public void onResponse(JSONObject response) {
//                                                Utils.d(SyncEventHelper.class, response.toString());
//                                            }
//                                        }, new Response.ErrorListener() {
//                                            @Override
//                                            public void onErrorResponse(VolleyError error) {
//                                                Utils.e(SyncEventHelper.class, "SYNC ERR", error);
//                                                mSyncListener.onResponse(SYNC_ERR);
//                                            }
//                                        });
//
//                                VolleyHelper.getInstance(mContext).addToRequestQueue(req);
//
////                                final String userName = dataManager.getUserName();
////                                SQLiteDatabase database = null;
////                                try {
////                                    database = mDBHelper.getWritableDatabase();
////                                    for (DataEvent event : dataEvents) {
//////                                    Utils.d(String.format("RX DATA act %s cid %s rid %s", event.Action, event.ComicsId, event.ReleaseNumber));
////                                        switch (event.Action) {
////                                            case DataManager.ACTION_CLEAR:
////                                                clear(database, userName);
////                                                break;
////                                            case DataManager.ACTION_ADD:
////                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
////                                                    writeComics(database, userName, dataManager.getComics(event.ComicsId), true);
////                                                } else {
////                                                    writeRelease(database, userName, dataManager.getComics(event.ComicsId)
////                                                            .getRelease(event.ReleaseNumber), true);
////                                                }
////                                                break;
////                                            case DataManager.ACTION_UPD:
////                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
////                                                    writeComics(database, userName, dataManager.getComics(event.ComicsId), false);
////                                                } else {
////                                                    writeRelease(database, userName, dataManager.getComics(event.ComicsId)
////                                                            .getRelease(event.ReleaseNumber), false);
////                                                }
////                                                break;
////                                            case DataManager.ACTION_DEL:
////                                                if (event.ReleaseNumber == DataManager.NO_RELEASE) {
////                                                    deleteComics(database, userName, event.ComicsId);
////                                                } else {
////                                                    deleteRelease(database, userName, event.ComicsId, event.ReleaseNumber);
////                                                }
////                                                break;
////                                        }
////                                    }
////                                } catch (Exception ex) {
////                                    Utils.e(this.getClass(), "Write data", ex);
////                                } finally {
////                                    if (database != null) {
////                                        database.close();
////                                    }
////                                }
//                            }
//                        }
//                    });
//
//        }
    }

    /**
     * Invia una azione che verrà gestita insieme ad altre sucessivamente.
     *
     * @param action    azione da eseguire sui dati (ACTION_ADD, ACTION_UPD, ACTION_DEL, ACTION_CLEAR)
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
        // converto tutti i dati in JSON e li invio al server, se tutto va bene invio SYNC_STARTED
        final DataManager dataManager = DataManager.getDataManager();
        final JsonHelper jsonHelper = new JsonHelper();
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject
                    .put("sid", mSyncId)
                    .put("utctime", System.currentTimeMillis()) //UTC
                    .put("version", BuildConfig.VERSION_CODE)
                    .put("debug", BuildConfig.DEBUG)
                    .put("comics", jsonHelper.comics2json(dataManager.getRawComics(), true));
            // invio i dati al tempo 0
            final String url = mSyncUrl + "/sync/" + mSyncId + "/0";
            final JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Utils.d(SyncEventHelper.class, response.toString());
                                if (mSyncId.equals(response.getString("sid"))) {
                                    mSyncTime = 0;
                                    mLastDataChanged = System.nanoTime();
                                    startCheckLocal();
                                    startCheckRemote();
                                    mSyncListener.onResponse(SYNC_STARTED);
                                } else {
                                    Utils.e(SyncEventHelper.class, "SYNC ERR (time 0): wrong sid received");
                                    mSyncListener.onResponse(SYNC_ERR);
                                }
                            } catch (JSONException jsonex) {
                                Utils.e(SyncEventHelper.class, "SYNC ERR (time 0)", jsonex);
                                mSyncListener.onResponse(SYNC_ERR);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Utils.e(SyncEventHelper.class, "SYNC ERR (time 0)", error);
                            mSyncListener.onResponse(SYNC_ERR);
                        }
                    });
            VolleyHelper.getInstance(mContext).addToRequestQueue(req);
        } catch (JSONException|IOException ex) {
            Utils.e(SyncEventHelper.class, "SYNC ERR (time 0)", ex);
            mSyncListener.onResponse(SYNC_ERR);
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
