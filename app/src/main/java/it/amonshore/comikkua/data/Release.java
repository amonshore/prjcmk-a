package it.amonshore.comikkua.data;

import java.util.Date;

/**
 * Created by Calgia on 15/05/2015.
 *
 * Per creare una nuova istanza usare Comics.createRelease()
 */
public class Release {

    public final static int FLAG_NONE = 0;
    public final static int FLAG_REMINDER = 1;
    public final static int FLAG_ORDERED = 2;
    public final static int FLAG_PURCHASED = 4;

    private final long comicsId;
    private int number;
    private Date date;
    private double price;
    private int flags;
    private String notes;

    protected Release(long comicsId) {
        this.comicsId = comicsId;
        this.number = -1;
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
        return (flags & FLAG_REMINDER) == FLAG_REMINDER;
    }

    public void setReminder(boolean reminder) {
        if (reminder)
            this.flags |= FLAG_REMINDER;
        else
            this.flags &= ~FLAG_REMINDER;
    }

    public boolean isOrdered() {
        return (flags & FLAG_ORDERED) == FLAG_ORDERED;
    }

    public void setOrdered(boolean ordered) {
        if (ordered)
            this.flags |= FLAG_ORDERED;
        else
            this.flags &= ~FLAG_ORDERED;
    }

    public boolean isPurchased() {
        return (flags & FLAG_PURCHASED) == FLAG_PURCHASED;
    }

    public void setPurchased(boolean purchased) {
        if (purchased)
            this.flags |= FLAG_PURCHASED;
        else
            this.flags &= ~FLAG_PURCHASED;
    }

    public boolean togglePurchased() {
        setPurchased(!isPurchased());
        return isPurchased();
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isWishlist() {
        return date == null;
    }
}
