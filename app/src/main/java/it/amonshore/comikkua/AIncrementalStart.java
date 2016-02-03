package it.amonshore.comikkua;

/**
 * Created by narsenico on 03/02/16.
 *
 * Ogni volta che viene chiamato il metodo start() un contatore interno viene incrementato di uno,
 * e decrementato allo stop().
 * Solo quando il contatore vale 1 viene chiamato il vero metodo start(), e solo quando è 0 viene
 * chiamato il vero stop().
 */
public abstract class AIncrementalStart {

    private final Object mSyncObj = new Object();

    private int mStartCount = 0;

    public void start() {
        synchronized (mSyncObj) {
            if (++mStartCount == 1) {
                safeStart();
            }
        }
    }

    public void stop() {
        synchronized (mSyncObj) {
            if (--mStartCount <= 0) {
                mStartCount = 0;
                safeStop();
            }
        }
    }

    protected abstract void safeStart();

    protected abstract void safeStop();

}
