package it.amonshore.comikkua;

import com.android.volley.DefaultRetryPolicy;

/**
 * Created by narsenico on 12/08/16.
 *
 * Retry policy di Volley con il massimo numero di tentativi impostato a zero.
 */
public class VolleyNoRetryPolicy extends DefaultRetryPolicy {

    public VolleyNoRetryPolicy() {
        super(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

}
