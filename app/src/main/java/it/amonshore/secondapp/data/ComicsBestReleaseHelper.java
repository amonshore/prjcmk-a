package it.amonshore.secondapp.data;

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
    public static Release getComicsBestRelease(Comics comics) {
        //imposto le date
        TimeZone timeZone = TimeZone.getDefault();
        long today = DateTime.today(timeZone).getStartOfDay().getMilliseconds(timeZone);;

        List<Release> releases = Arrays.asList(comics.getReleases());
        Collections.sort(releases, new Comparator<Release>() {
            @Override
            public int compare(Release lhs, Release rhs) {
                Date ldt = lhs.getDate();
                Date rdt = rhs.getDate();
                if (ldt != null && rdt != null) {
                    return ldt.compareTo(rdt);
                } else if (ldt == null && rdt == null) {
                    return lhs.getNumber() - rhs.getNumber();
                } else if (ldt == null) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        Release found = null;
        //cerco la prima scaduta e non acquistata
        for (Release rel : releases) {
            if (!rel.isPurchased() && rel.getDate() != null && rel.getDate().getTime() < today) {
                found = rel;
                break;
            }
        }
        if (found == null) {
            //cerco la prima NON scaduta e non acquistata
            for (Release rel : releases) {
                if (!rel.isPurchased() && rel.getDate() != null && rel.getDate().getTime() >= today) {
                    found = rel;
                    break;
                }
            }
            if (found == null) {
                //cerco la prima senza data e non acquistata
                for (Release rel : releases) {
                    if (!rel.isPurchased() && rel.getDate() == null) {
                        found = rel;
                        break;
                    }
                }
            }
        }

        return found;
    }

}
