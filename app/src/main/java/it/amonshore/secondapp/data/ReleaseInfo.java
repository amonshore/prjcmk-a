package it.amonshore.secondapp.data;

/**
 * Created by Narsenico on 23/05/2015.
 */
public final class ReleaseInfo {

    private int group;
    private Release release;

    public ReleaseInfo(int group, Release release) {
        this.group = group;
        this.release = release;
    }

    /**
     *
     * @return
     */
    public int getGroup() { return group; }

    /**
     *
     * @return
     */
    public Release getRelease() { return release; }
}
