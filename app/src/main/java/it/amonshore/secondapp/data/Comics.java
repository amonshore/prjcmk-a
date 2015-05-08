package it.amonshore.secondapp.data;

/**
 * Created by Calgia on 07/05/2015.
 */
public class Comics {

    private long id;
    private String name;

    public Comics(long id, String name) {
        this.id = id;
        this.name = name;
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
}
