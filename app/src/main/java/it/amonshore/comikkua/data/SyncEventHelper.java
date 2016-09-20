package it.amonshore.comikkua.data;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketHandler;
import it.amonshore.comikkua.AIncrementalStart;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.RxBus;
import it.amonshore.comikkua.Utils;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by narsenico on 07/06/16.
 *
 * A0068 Gestore eventi per la sincronizzazione remota dei dati.
 * - presentazione syncid (applySyncId) -> SYNC_READY
 */
class SyncEventHelper extends AIncrementalStart {

    final static int SYNC_READY = 0;
    final static int SYNC_STARTED = 10;
    final static int SYNC_SENT = 30;
    final static int SYNC_REFUSED = 101;
    final static int SYNC_ERR = 102;
    final static int SYNC_EXPIRED = 103;

    private final static String MESSAGE_HELLO = "hello";
    private final static String MESSAGE_SYNC_START = "sync start";
    private final static String MESSAGE_SYNC_TIMEOUT = "sync timeout";
    private final static String MESSAGE_PUT_COMICS = "put comics";
    private final static String MESSAGE_REMOVE_COMICS = "remove comics";
    private final static String MESSAGE_CLEAR_COMICS = "clear comics";
    private final static String MESSAGE_PUT_RELEASES = "put releases";
    private final static String MESSAGE_REMOVE_RELEASES = "remove releases";
    private final static String MESSAGE_STOP_SYNC = "stop sync";

    interface SyncListener {

        /**
         * @param response codice di risposta ricevuto dal server
         */
        void onResponse(int response);

        /**
         * TODO quali dati ricevere?
         *
         * @param data dati ricevuti dal server
         */
        void onDataReceived(Object data);
    }

    private RxBus<DataEvent> mEventBus;
    private WebSocketConnection mWebSocketClient;
    private SyncListener mSyncListener;
    private boolean mStopRequested;
    private JsonHelper mJsonHelper = new JsonHelper();

    private void sendMessage(String message) throws JSONException {
        sendMessage(message, null);
    }

    private void sendMessage(String message, Object data) throws JSONException {
        final JSONObject jsMessage = new JSONObject();
        jsMessage
                .put("message", message)
                .put("data", data);
        mWebSocketClient.sendTextMessage(jsMessage.toString());
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
     * @param syncHost host da contattare per la sincronizzazione
     * @param syncId   codice di sincronizzazione da usare in tutte le richieste al server
     * @param listener ascoltatore per ricevere eventi dal processo di sincronizzazione
     */
    void applySyncId(final String syncHost, final String syncId, SyncListener listener) {
        mSyncListener = listener;
        mStopRequested = false;

        try {
            final DataManager dataManager = DataManager.getDataManager();
            final String url = "ws://" + syncHost + "/sync/wsh/" + syncId;
            Utils.d(this.getClass(), "SYNC connect to " + url);

            mWebSocketClient = new WebSocketConnection();
            mWebSocketClient.connect(url, new WebSocketHandler() {
                @Override
                public void onOpen() {
                    Utils.d(SyncEventHelper.class, "SYNC ws open");

                    final JSONObject jsData = new JSONObject();
                    try {
                        jsData
                                .put("appVersion", BuildConfig.VERSION_CODE)
                                .put("sdkVersion", Build.VERSION.SDK_INT)
                                .put("debug", BuildConfig.DEBUG)
                                .put("comics", mJsonHelper.comics2json(dataManager.getRawComics(), true));
                        sendMessage(MESSAGE_HELLO, jsData);
                        mSyncListener.onResponse(SYNC_READY);
                    } catch (Exception ex2) {
                        Utils.e(SyncEventHelper.class, "SYNC ERR (ws)", ex2);
                        mSyncListener.onResponse(SYNC_ERR);
                    }
                }

                @Override
                public void onTextMessage(String payload) {
                    Utils.d(SyncEventHelper.class, "SYNC receive " + payload);
                    try {
                        final JSONTokener tokener = new JSONTokener(payload);
                        final JSONObject msg = (JSONObject) tokener.nextValue();
                        final String message = msg.getString("message");
                        if (MESSAGE_SYNC_START.equals(message)) {
                            mSyncListener.onResponse(SYNC_STARTED);
                        } else if (MESSAGE_SYNC_TIMEOUT.equals(message)) {
                            mSyncListener.onResponse(SYNC_EXPIRED);
                        } else {
                            mSyncListener.onResponse(SYNC_ERR);
                        }
                        // TODO: gestire gli altri messaggi
                    } catch (JSONException jsonex) {
                        Utils.e(SyncEventHelper.class, "SYNC ERR (ws)", jsonex);
                        mSyncListener.onResponse(SYNC_ERR);
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    Utils.d(SyncEventHelper.class, String.format("SYNC ws close: [%d] %s", code, Utils.nvl(reason, "unknown")));
                    // scateno l'errore solo se si è trattata di una chiusura imprevista
                    if (!mStopRequested) {
                        mSyncListener.onResponse(SYNC_ERR);
                    }
                }
            });
        } catch (Exception ex) {
            Utils.e(SyncEventHelper.class, "SYNC ERR (ws)", ex);
            mSyncListener.onResponse(SYNC_ERR);
        }
    }

    /**
     * Invia una azione che verrà gestita insieme ad altre sucessivamente.
     *
     * @param action        azione da eseguire sui dati (ACTION_ADD, ACTION_UPD, ACTION_DEL, ACTION_CLEAR)
     * @param comicsId      id del comics su cui operare l'azione
     * @param releaseNumber numero della release su cui operare l'azione (NO_RELEASE per nessuna)
     */
    void send(int action, long comicsId, int releaseNumber) {
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
        if (mEventBus == null) {
            final DataManager dataManager = DataManager.getDataManager();

            mEventBus = new RxBus<>();
            mEventBus.toObserverable()
                    .observeOn(Schedulers.newThread()) //gli eventi verranno consumati in un nuuovo scheduler
                    .onBackpressureBuffer()
                    .subscribe(new Subscriber<DataEvent>() {
                        @Override
                        public void onStart() {
                            request(1);
                        }

                        @Override
                        public void onCompleted() {
                            Utils.d("RX SYNC end " + Utils.isMainThread());
                        }

                        @Override
                        public void onError(Throwable e) {
                            Utils.e("RX SYNC error", e);
                        }

                        @Override
                        public void onNext(DataEvent dataEvent) {
                            Utils.d("RX SYNC " + dataEvent + " " + Utils.isMainThread());

                            try {
                                if (dataEvent.Action == DataManager.ACTION_CLEAR) {
                                    sendMessage(MESSAGE_CLEAR_COMICS);
                                } else if (dataEvent.Action == DataManager.ACTION_ADD) {
                                    final Comics comics = dataManager.getComics(dataEvent.ComicsId);
                                    if (dataEvent.ReleaseNumber == DataManager.NO_RELEASE) {
                                        sendMessage(MESSAGE_PUT_COMICS, mJsonHelper.comics2json(comics, false));
                                        // se il comics possiede già delle release le invio
                                        // (ad esempio capita quando ripristino il fumetto dopo una cancellazione oppure al ripristino da file)
                                        if (comics.getReleaseCount() > 0) {
                                            sendMessage(MESSAGE_PUT_RELEASES, mJsonHelper.releases2json(comics.getReleaseList()));
                                        }
                                    } else {
                                        sendMessage(MESSAGE_PUT_RELEASES, mJsonHelper.release2json(comics.getRelease(dataEvent.ReleaseNumber)));
                                    }
                                } else if (dataEvent.Action == DataManager.ACTION_UPD) {
                                    final Comics comics = dataManager.getComics(dataEvent.ComicsId);
                                    if (dataEvent.ReleaseNumber == DataManager.NO_RELEASE) {
                                        sendMessage(MESSAGE_PUT_COMICS, mJsonHelper.comics2json(comics, false));
                                    } else {
                                        sendMessage(MESSAGE_PUT_RELEASES, mJsonHelper.release2json(comics.getRelease(dataEvent.ReleaseNumber)));
                                    }
                                } else if (dataEvent.Action == DataManager.ACTION_DEL) {
                                    if (dataEvent.ReleaseNumber == DataManager.NO_RELEASE) {
                                        sendMessage(MESSAGE_REMOVE_COMICS, dataEvent.ComicsId);
                                    } else {
                                        final JSONObject relId = new JSONObject();
                                        relId.put("id", dataEvent.ComicsId);
                                        relId.put("number", dataEvent.ReleaseNumber);
                                        sendMessage(MESSAGE_REMOVE_RELEASES, relId);
                                    }
                                }

                                // richiedo il prossimo
                                request(1);
                            } catch (Exception ex) {
                                Utils.e(this.getClass(), "Write data", ex);
                                mSyncListener.onResponse(SYNC_ERR);
                            }
//                            }
                        }
                    });

        }
    }

    @Override
    protected void safeStop() {
        mStopRequested = true;

        if (mEventBus != null) {
            mEventBus.end(); //scatena onCompleted
            mEventBus = null;
        }

        if (mWebSocketClient != null) {
            mWebSocketClient.disconnect();
            mWebSocketClient = null;
        }
    }

    private final static class DataEvent {
        int Action;
        long ComicsId;
        int ReleaseNumber;
    }

}
