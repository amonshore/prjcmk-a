package it.amonshore.comikkua.data;

/**
 * Created by Narsenico on 23/05/2015.
 */
public final class ReleaseInfo {

    private int group;
    private boolean releasedToday;
    private boolean expired;
    private Release release;

    public ReleaseInfo(int group, Release release) {
        this(group, false, false, release);
    }

    public ReleaseInfo(int group, boolean releasedToday, boolean expired, Release release) {
        this.group = group;
        this.releasedToday = releasedToday;
        this.expired = expired;
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
    public boolean isExpired() { return expired; }

    /**
     *
     * @return
     */
    public Release getRelease() { return release; }
}
