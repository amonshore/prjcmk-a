package it.amonshore.secondapp.ui;

import android.support.v4.app.Fragment;

/**
 * Created by Narsenico on 21/05/2015.
 */
public abstract class AFragment extends Fragment {

    public static final int CAUSE_SAFE = 1;
    public static final int CAUSE_LOADING = 2;
    public static final int CAUSE_SETTINGS_CHANGED = 4;
    public static final int CAUSE_ROTATION = 8;
    public static final int CAUSE_PAGE_CHANGED = 16;

    /**
     * Avverte il fragment che è richiesto l'aggiornamento dei dati
     * @param cause
     */
    public abstract void needDataRefresh(int cause);

    /**
     * Da chiamare per terminare la barra contestuale
     */
    public abstract void finishActionMode();
}
