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
public class ComicsBestReleaseHelper {

    /**
     *
     * @param comics
     * @return
     */
    public static ReleaseInfo getComicsBestRelease(Comics comics) {
        //imposto le date
        final TimeZone timeZone = TimeZone.getDefault();
        final long today = DateTime.today(timeZone).getStartOfDay().getMilliseconds(timeZone);
        final DataManager dataManager = DataManager.getDataManager();

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

                //se tutto è uguale confronto alfabeticamente
                if (res == 0) {
                    res = dataManager.getComics(lhs.getComicsId()).getName()
                            .compareTo(dataManager.getComics(rhs.getComicsId()).getName());
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
