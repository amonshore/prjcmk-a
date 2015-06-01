package it.amonshore.secondapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.ComicsObserver;
import it.amonshore.secondapp.data.DataManager;

/**
 * Created by Narsenico on 21/05/2015.
 */
public abstract class AFragment extends Fragment implements ComicsObserver {

    private DataManager mDataManager;
    private int mChangedCause;

    public DataManager getDataManager() {
        return mDataManager;
    }

    /**
     *
     * @return
     */
    public boolean isChanged() {
        return mChangedCause == 0;
    }

    /**
     * Avverte il fragment che è richiesto l'aggiornamento dei dati
     * @param cause
     */
    public abstract void onDataChanged(int cause);

    /**
     * Da chiamare per terminare la barra contestuale
     */
    public abstract void finishActionMode();

    @Override
    public void onChanged(int cause) {
        if (this.isResumed() || (cause & DataManager.CAUSE_SAFE) == DataManager.CAUSE_SAFE) {
            onDataChanged(cause);
            mChangedCause = 0;
        } else {
            Utils.d(this.getClass(), "onChanged posponed " + cause);
            mChangedCause = cause;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataManager = DataManager.getDataManager(getActivity().getApplicationContext());
        Utils.d(this.getClass(), "onCreate -> register observer");
        mDataManager.registerObserver(this);
        //TODO se l'app va in background e poi ripristinata, capita che la lista dei comics sia vuota
        //  perché? forse la notifica viene chiamata e il fragment ancora non è stato creato?
        //  chiamando onChanged qua si risolve la cosa, ma viene chiamato due volte all'avvio dell'app
        //  nell' onChanged se la causa è CREATE si potrebbe consideare di chiamare onDataChanged (con LOADING)
        //      se veramente è cambiato qualcosa
        // onChanged(DataManager.CAUSE_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //TODO non sono sicuro se farlo qua o in onDetach()
        Utils.d(this.getClass(), "onDestroy -> unregister observer");
        mDataManager.unregisterObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChangedCause != 0) {
            onDataChanged(mChangedCause);
            mChangedCause = 0;
        }
    }

}
