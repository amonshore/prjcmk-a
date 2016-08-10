package it.amonshore.comikkua.ui;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import it.amonshore.comikkua.R;

/**
 * Created by narsenico on 21/05/16.
 */
public class AnimationHelper {

    public final static int NORMAL = 0;
    public final static int SLOW = 1;
    public final static int FAST = 2;

    private Animation mCollapseNormal;
    private Animation mCollapseFast;
    private Animation mCollapseSlow;
    private Animation mExpandNormal;
    private Animation mExpandFast;
    private Animation mExpandSlow;

    public AnimationHelper(Context context) {
        //
        mCollapseNormal = AnimationUtils.loadAnimation(context, R.anim.collapse_in);
        mCollapseFast = AnimationUtils.loadAnimation(context, R.anim.collapse_in);
        mCollapseFast.setDuration(mCollapseFast.getDuration() - 200L);
        mCollapseSlow = AnimationUtils.loadAnimation(context, R.anim.collapse_in);
        mCollapseSlow.setDuration(mCollapseFast.getDuration() + 200L);
        //
        mExpandNormal = AnimationUtils.loadAnimation(context, R.anim.expand_in);
        mExpandFast = AnimationUtils.loadAnimation(context, R.anim.expand_in);
        mExpandFast.setDuration(mExpandFast.getDuration() - 200L);
        mExpandSlow = AnimationUtils.loadAnimation(context, R.anim.expand_in);
        mExpandSlow.setDuration(mExpandSlow.getDuration() + 200L);
    }

    /**
     * Avvia l'animazione (a velocità normale) corrispondente allo stato di visibilità che si vuole ottenere.
     *
     * @param view  vista a cui applicare l'animazione
     * @param visibility    stato di visibilità da associare alla vista al termine dell'animazione
     */
    public void popup(View view, int visibility) {
        popup(view, visibility, NORMAL);
    }

    /**
     * Avvia l'animazione corrispondente allo stato di visibilità che si vuole ottenere.
     *
     * @param view  vista a cui applicare l'animazione
     * @param visibility    stato di visibilità da associare alla vista al termine dell'animazione
     * @param speed velocità dell'animazione
     */
    public void popup(View view, int visibility, int speed) {

        if (visibility == View.VISIBLE) {
            if (speed == FAST)
                view.startAnimation(mExpandFast);
            else if (speed == SLOW)
                view.startAnimation(mExpandSlow);
            else
                view.startAnimation(mExpandNormal);
        } else {
            if (speed == FAST)
                view.startAnimation(mCollapseFast);
            else if (speed == SLOW)
                view.startAnimation(mCollapseSlow);
            else
                view.startAnimation(mCollapseNormal);
        }
        view.setVisibility(visibility);
    }

}
