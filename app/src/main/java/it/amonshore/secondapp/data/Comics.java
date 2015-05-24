package it.amonshore.secondapp.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Calgia on 07/05/2015.
 */
public class Comics {

    public final static String PERIODICITY_UNKNOWN = "";
    public final static String PERIODICITY_WEEKLY = "W1";
    public final static String PERIODICITY_MONTHLY = "M1";
    public final static String PERIODICITY_MONTHLY_X2 = "M2";
    public final static String PERIODICITY_MONTHLY_X3 = "M3";
    public final static String PERIODICITY_MONTHLY_X4 = "M4";
    public final static String PERIODICITY_MONTHLY_X6 = "M6";
    public final static String PERIODICITY_YEARLY = "Y1";

    private long id;
    private String name;
    private String series;
    private String publisher;
    private String authors;
    private double price;
    private String periodicity;
    private boolean reserved;
    private String notes;
    private ArrayList<Release> releases;

    protected Comics() {
        releases = new ArrayList<>();
    }

    public Comics(long id) {
        this();
        this.id = id;
    }

    public long getId() {
        return id;
    }

    protected void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(String periodicity) {
        this.periodicity = periodicity;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    private int indexOf(int number) {
        for (int ii = 0; ii < releases.size(); ii++) {
            if (releases.get(ii).getNumber() == number) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Crea una nuova release associata a questo fumetto. La release non viene aggiunta all'elenco: usare addRelease().
     * @return  nuova release
     */
    public Release createRelease() {
        return new Release(this.getId());
    }

    /**
     * Assegna una nuova release al fumetto. Deve essere creata con createRelease() della stessa istanza di Comics, pena
     * il lancio di una RuntimeException.
     * Se esiste già una release con lo stesso numero (getNumber()), la sostituirà.
     *
     * @param release
     * @return true se è stata aggiunta, false se ha sostiuito una release esistente.
     */
    public boolean putRelease(Release release) {
        if (release.getComicsId() != this.id) {
            throw new RuntimeException(String.format("Release comics id not valid: exptected %s, found %s", this.id, release.getComicsId()));
        }

        int index = indexOf(release.getNumber());
        if (index == -1) {
            releases.add(release);
            return true;
        } else {
            releases.set(index, release);
            return false;
        }
    }

    /**
     *
     * @param number
     * @return true se è stata elminata, false altrimenti
     */
    public boolean removeRelease(int number) {
        int index = indexOf(number);
        if (index >= 0) {
            releases.remove(index);
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     */
    public void removeAllReleases() {
        releases.clear();
    }

    /**
     *
     * @return
     */
    public int getReleaseCount() {
        return releases.size();
    }

    /**
     *
     * @return  tutte le release
     */
    public Release[] getReleases() {
        return releases.toArray(new Release[releases.size()]);
    }

    /**
     *
     * @param number
     * @return
     */
    public Release getRelease(int number) {
        for (Release release : releases) {
            if (release.getNumber() == number) return release;
        }
        return  null;
    }

    public void copyFrom(Comics comics) {
        setName(comics.getName());
        setPeriodicity(comics.getPeriodicity());
        setPublisher(comics.getPublisher());
        setAuthors(comics.getAuthors());
        setNotes(comics.getNotes());
        setPrice(comics.getPrice());
        setReserved(comics.isReserved());
        setSeries(comics.getSeries());
    }
}
