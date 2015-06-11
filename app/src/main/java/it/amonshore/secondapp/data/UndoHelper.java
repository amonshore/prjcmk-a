package it.amonshore.secondapp.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Created by Narsenico on 08/06/2015.
 */
public class UndoHelper<E> {

    private int mLastTag;
    private ArrayList<E> mElements;
    private HashMap<Integer, Stack<E>> mRetainedElements;

    protected UndoHelper() {
        mLastTag = 0;
        mElements = new ArrayList<>();
        mRetainedElements = new HashMap<>();
    }

    /**
     *
     * @param e
     */
    public void push(E e) {
        mElements.add(e);
    }

    /**
     * Memorizza gli ultimi elementi eliminati e restituisce un tag numerico che permette di recuperarli.
     *
     * @return  tag numerico con cui è possibile recuperare i elementi rimossi, oppure 0 se non ce ne sono
     */
    public int retainElements() {
        if (mElements.size() > 0) {
            Stack<E> stack = new Stack<>();
            stack.addAll(mElements);
            mElements.clear();
            mRetainedElements.put(++mLastTag, stack);
            return mLastTag;
        } else {
            return 0;
        }
    }

    /**
     * Rimuove definitivamente i elementi memorizzati con un certo tag
     *
     * @param tag   tag numerico restituito da retainElements()
     */
    public void removeElements(int tag) {
        Stack<E> stack = mRetainedElements.get(tag);
        if (stack != null) {
            mRetainedElements.remove(tag);
        }
    }

    /**
     *
     * @param tag   tag numerico restituito da retainElements()
     * @return il primo elementi disponibile, oppure null se non ce ne sono
     */
    public E pop(int tag) {
        Stack<E> stack = mRetainedElements.get(tag);
        if (stack != null && !stack.empty()) {
            return stack.pop();
        } else {
            return null;
        }
    }
}
