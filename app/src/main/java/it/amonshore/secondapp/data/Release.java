package it.amonshore.secondapp.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Calgia on 15/05/2015.
 *
 * Per creare una nuova istanza usare Comics.createRelease()
 */
public class Release implements Serializable {

    private long comicsId;
    private int number;
    private Date date;
    private double price;
    private boolean reminder;
    private boolean purchased;

    protected Release(long comicsId) {
        this.comicsId = comicsId;
    }

    public long getComicsId() {
        return comicsId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isReminder() {
        return reminder;
    }

    public void setReminder(boolean reminder) {
        this.reminder = reminder;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }
}
