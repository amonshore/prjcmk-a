package it.amonshore.secondapp.data;

/**
 * Created by Narsenico on 23/05/2015.
 */
public final class ReleaseInfo {

    private int group;
    private boolean releasedToday;
    private Release release;

    public ReleaseInfo(int group, Release release) {
        this(group, false, release);
    }

    public ReleaseInfo(int group, boolean releasedToday, Release release) {
        this.group = group;
        this.releasedToday = releasedToday;
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
    public boolean isReleasedToday() { return releasedToday; }

    /**
     *
     * @return
     */
    public Release getRelease() { return release; }
}
