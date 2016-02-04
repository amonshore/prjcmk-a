package it.amonshore.comikkua.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import it.amonshore.comikkua.Utils;

/**
 * Created by Narsenico on 25/07/2015.
 */
public class MultiReleaseInfo extends ReleaseInfo implements Iterable<Release> {

    private final ArrayList<Release> mInnerReleases;

    public MultiReleaseInfo(int group, Release release) {
        this(group, false, false, release);
    }

    private MultiReleaseInfo(int group, boolean releasedToday, boolean expired, Release release) {
        super(group, releasedToday, expired, release);
        mInnerReleases = new ArrayList<>();
        addInnerRelease(release);
    }

    public void addInnerRelease(Release release) {
        mInnerReleases.add(release);
        //mi assicuro che la release principale sia quella con il numero più basso
        if (getRelease().getNumber() > release.getNumber()) {
            setRelease(release);
        }
    }

    public Iterator<Release> iterator() {
        return mInnerReleases.iterator();
    }

    /**
     *
     * @return l'elenco delle release, esclusa la prima, formattato
     */
    public String getFormattedReleaseNumbers() {
        if (mInnerReleases.size() > 1) {
            //escludo quella principale
            int[] numbers = new int[mInnerReleases.size() - 1];
            for (int ii = 0, jj = 0; ii < mInnerReleases.size(); ii++) {
                if (mInnerReleases.get(ii) == getRelease()) continue;
                numbers[jj++] = mInnerReleases.get(ii).getNumber();
            }
            Arrays.sort(numbers);
            return Utils.formatInterval(null, ", ", "-", numbers).toString();
        } else {
            return "";
        }
    }

    public int size() {
        return mInnerReleases.size();
    }
}
