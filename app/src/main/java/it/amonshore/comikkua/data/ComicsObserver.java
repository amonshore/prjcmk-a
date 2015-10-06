package it.amonshore.comikkua.data;

/**
 * Created by Narsenico on 01/06/2015.
 */
public interface ComicsObserver {

    /**
     *
     * @param cause la causa del cambiamento (vedi DataManager.CAUSE_xxx)
     */
    void onChanged(int cause);

}
