package it.amonshore.comikkua.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * Created by Narsenico on 24/05/2015.
 */
class ComicsBestReleaseHelper {

    /**
     * A0046
     *
     * @param comics    comics per cui leggere la best release
     * @param showLastPurchased
     * @return la prima uscita da comprare o l'ultima comprata in base alle preferenze
     */
    public static ReleaseInfo getComicsBestRelease(Comics comics, boolean showLastPurchased) {
        if (showLastPurchased) {
            return getLastReleasePurchased(comics);
        } else {
            return getFirstReleaseToPurchase(comics);
        }
    }

    private static ReleaseInfo getLastReleasePurchased(Comics comics) {
        Release lastPurchased = null;
        for (Release release : comics.getReleases()) {
            if (!release.isPurchased()) continue;
            if (lastPurchased == null) {
                lastPurchased = release;
            } else if (lastPurchased.getNumber() < release.getNumber()) {
                lastPurchased = release;
            }
        }

        if (lastPurchased == null) {
            return null;
        } else {
            return new ReleaseInfo(ReleaseGroupHelper.GROUP_PURCHASED, lastPurchased);
        }
    }

    private static ReleaseInfo getFirstReleaseToPurchase(Comics comics) {
        //imposto le date
        final TimeZone timeZone = TimeZone.getDefault();
        final long today = DateTime.today(timeZone).getStartOfDay().getMilliseconds(timeZone);

        List<Release> releases = Arrays.asList(comics.getReleases());
        Collections.sort(releases, new Comparator<Release>() {
            @Override
            public int compare(Release lhs, Release rhs) {
                Date ldt = lhs.getDate();
                Date rdt = rhs.getDate();
                int res;
                if (ldt != null && rdt != null) {
                    res = ldt.compareTo(rdt);
                } else if (ldt == null && rdt == null) {
                    res = lhs.getNumber() - rhs.getNumber();
                } else if (ldt == null) {
                    res = -1;
                } else {
                    res = 1;
                }

                return res;
            }
        });

        ReleaseInfo found = null;
        //cerco la prima scaduta e non acquistata
        for (Release rel : releases) {
            if (!rel.isPurchased() && rel.getDate() != null && rel.getDate().getTime() < today) {
                found = new ReleaseInfo(ReleaseGroupHelper.GROUP_EXPIRED, rel);
                break;
            }
        }
        if (found == null) {
            //cerco la prima NON scaduta e non acquistata
            for (Release rel : releases) {
                if (!rel.isPurchased() && rel.getDate() != null) {
                    if (rel.getDate().getTime() > today) {
                        found = new ReleaseInfo(ReleaseGroupHelper.GROUP_TO_PURCHASE, rel);
                        break;
                    } else if (rel.getDate().getTime() == today) {
                        found = new ReleaseInfo(ReleaseGroupHelper.GROUP_TO_PURCHASE, true, false, rel);
                        break;
                    }
                }
            }
            if (found == null) {
                //cerco la prima senza data e non acquistata
                for (Release rel : releases) {
                    if (!rel.isPurchased() && rel.getDate() == null) {
                        found = new ReleaseInfo(ReleaseGroupHelper.GROUP_WISHLIST, rel);
                        break;
                    }
                }
            }
        }

        return found;
    }

}
