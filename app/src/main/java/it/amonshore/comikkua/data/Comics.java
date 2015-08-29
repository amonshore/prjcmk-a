package it.amonshore.comikkua.data;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Narsenico on 07/05/2015.
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

    private final long id;
    private String name;
    private String series;
    private String publisher;
    private String authors;
    private double price;
    private String periodicity;
    private boolean reserved;
    private String notes;
    private final ArrayList<Release> releases;

    public Comics(long id) {
        releases = new ArrayList<>();
        this.id = id;
    }

    public long getId() {
        return id;
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
     * @param autoFill se true imposta in automatico numero, data e prezzo
     * @return  nuova release
     */
    public Release createRelease(boolean autoFill) {
        Release newRelease = new Release(this.getId());
        if (autoFill) {
            //calcolo numero e data
            Release maxRelease = null;
            for (Release release : releases) {
                if (maxRelease == null || maxRelease.getNumber() < release.getNumber()) {
                    maxRelease = release;
                }
            }
            if (maxRelease == null) {
                newRelease.setNumber(1);
            } else {
                newRelease.setNumber(maxRelease.getNumber() + 1);
                if (maxRelease.getDate() != null && !TextUtils.isEmpty(this.getPeriodicity())) {
                    GregorianCalendar calendar = new GregorianCalendar();
                    calendar.setTime(maxRelease.getDate());
                    char type = this.getPeriodicity().charAt(0);
                    int amout = Integer.parseInt(this.getPeriodicity().substring(1));
                    if (type == 'W') {
                        calendar.add(Calendar.DAY_OF_MONTH, 7 * amout);
                    } else if (type == 'M') {
                        calendar.add(Calendar.MONTH, amout);
                    } else if (type == 'Y') {
                        calendar.add(Calendar.YEAR, amout);
                    }
                    newRelease.setDate(calendar.getTime());
                }
            }
            newRelease.setPrice(this.getPrice());
        }
        return newRelease;
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
     * @return la release rimossa oppure null se non viene trovata
     */
    Release removeRelease(int number) {
        int index = indexOf(number);
        if (index >= 0) {
            return releases.remove(index);
        } else {
            return null;
        }
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

}
