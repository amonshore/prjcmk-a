package it.amonshore.comikkua.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.ComicsObserver;
import it.amonshore.comikkua.data.DataManager;

/**
 * Created by Narsenico on 21/05/2015.
 */
public abstract class AFragment extends Fragment implements ComicsObserver {

    private DataManager mDataManager;
    private int mChangedCause;

    protected DataManager getDataManager() {
        return mDataManager;
    }

    /**
     * Avverte il fragment che è richiesto l'aggiornamento dei dati
     * @param cause
     * @param wasPosponed
     */
    protected abstract void onDataChanged(int cause, boolean wasPosponed);

    /**
     *
     * @param cause
     */
    public void onDataChanged(int cause) {
        onDataChanged(cause, false);
    }

    /**
     * Da chiamare per terminare la barra contestuale
     */
    public abstract void finishActionMode();

    @Override
    public void onChanged(int cause) {
        if (this.isResumed() || (cause & DataManager.CAUSE_SAFE) == DataManager.CAUSE_SAFE) {
            onDataChanged(cause, false);
            mChangedCause = 0;
        } else {
            Utils.d(this.getClass(), "onChanged posponed " + cause);
            mChangedCause = cause;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataManager = DataManager.getDataManager();
        Utils.d(this.getClass(), "onCreate -> register observer");
        mDataManager.registerObserver(this);
        //TODO se l'app va in background e poi ripristinata, capita che la lista dei comics sia vuota
        //  perché? forse la notifica viene chiamata e il fragment ancora non è stato creato?
        //  chiamando onChanged qua si risolve la cosa, ma viene chiamato due volte all'avvio dell'app
        //  nell' onChanged se la causa è CREATE si potrebbe consideare di chiamare onDataChanged (con LOADING)
        //      se veramente è cambiato qualcosa
        onChanged(DataManager.CAUSE_CREATED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.d(this.getClass(), "onDestroy -> unregister observer");
        mDataManager.unregisterObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChangedCause != 0) {
            onDataChanged(mChangedCause, true);
            mChangedCause = 0;
        }
    }

}
