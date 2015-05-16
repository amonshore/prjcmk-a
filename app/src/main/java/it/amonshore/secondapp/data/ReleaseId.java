package it.amonshore.secondapp.data;

/**
 * Created by Narsenico on 16/05/2015.
 *
 * Rappresenta l'identificativo di una release composto dall'id del comics e dal numero della release
 */
public class ReleaseId {

    private long comicsId;
    private int number;

    public ReleaseId(long comicsId, int number) {
        this.comicsId = comicsId;
        this.number = number;
    }

    public long getComicsId() {
        return comicsId;
    }

    public int getNumber() {
        return number;
    }
}
