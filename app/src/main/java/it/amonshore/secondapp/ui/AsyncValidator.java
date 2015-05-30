package it.amonshore.secondapp.ui;

import android.os.AsyncTask;
import android.text.Editable;
import android.widget.TextView;

import java.util.Stack;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import it.amonshore.secondapp.Utils;

/**
 * Created by Narsenico on 30/05/2015.
 *
 * Chiamare start() in Activity.onResume()
 * Chiamare stop() in Activity.onPause() e Activity.onStop()
 */
public class AsyncValidator {

    private boolean pStop;
    private Semaphore pSemaphore;
    private TextView pTarget;
    private OnValidateListener pListener;
    private SimpleTextWatcher pTextWatcher;
    private Stack<CharSequence> pDataToCheck;

    public AsyncValidator(TextView target, OnValidateListener listener) {
        pDataToCheck = new Stack<CharSequence>();
        pTarget = target;
        pListener = listener;
        pTextWatcher = new SimpleTextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                checkData(s.toString());
            }

        };
    }

    protected void checkData(CharSequence data) {
        pDataToCheck.push(data);
        pSemaphore.release();
    }

    /**
     *
     * @param interval  tempo di attesa (in millisecondi) senza modifiche al testo prima di scatenare la validazione
     */
    public void start(final int interval) {
        if (pSemaphore == null) {
            pStop = false;
            pSemaphore = new Semaphore(-1);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        while (!pStop) {
                            if (!pSemaphore.tryAcquire(interval, TimeUnit.MILLISECONDS)) {
                                //tempo scaduto, non ci sono state altre variazioni nel tempo stabilito
                                if (!pDataToCheck.empty()) {
                                    publishProgress();
                                }
                            }
                        }
                    } catch (InterruptedException iex) {}
                    return null;
                }

                @Override
                protected void onProgressUpdate(Void... values) {
                    pListener.onValidate(pTarget, pDataToCheck.pop());
                    pDataToCheck.clear();
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Utils.d("validator exit task");
                }
            }.execute();

            //registro il listerner su pTarget, al cambiamento del testo scatena checkData
            pTarget.addTextChangedListener(pTextWatcher);
        }
    }

    /**
     *
     */
    public void stop() {
        if (pSemaphore != null) {
            pStop = true;
            pSemaphore.release();
            pTarget.removeTextChangedListener(pTextWatcher);
            pSemaphore = null;
        }
    }

    /**
     *
     */
    public interface OnValidateListener {
        public void onValidate(TextView target, CharSequence text);
    }
}