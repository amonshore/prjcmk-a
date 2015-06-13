package it.amonshore.comikkua.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import it.amonshore.comikkua.Utils;

/**
 * Created by Narsenico on 22/05/2015.
 */
public class ReleaseGroupHelper {

    /**
     * Modalità shopping: periodo attuale (settimana/mese), persi, wishlist
     */
    public final static int MODE_SHOPPING = 1;
    /**
     * Modalità calendario uscite: gruppi per periodo (settimana/mese)
     */
    public final static int MODE_CALENDAR = 2;
    /**
     * Modalità persi & desiderati: scaduti, wishlist
     */
    public final static int MODE_LAW = 3;
    /**
     * Modalità comics: persi, da acquistare, acquistati
     */
    public final static int MODE_COMICS = 4;

    /**
     * Gruppo periodo corrente
     */
    public final static int GROUP_PERIOD = 1;
    /**
     * Gruppo periodo successivo
     */
    public final static int GROUP_PERIOD_NEXT = 2;
    /**
     * Gruppo periodo altro
     */
    public final static int GROUP_PERIOD_OTHER = 3;
    /**
     * Gruppo scaduti: release non acquistate, con data speficiata e scaduta (inferiore a oggi)
     */
    public final static int GROUP_EXPIRED = 10;
    /**
     * Gruppo persi: release non acquistate, con data specificata e scaduta (inferiore a inizio periodo)
     */
    public final static int GROUP_LOST = 20;
    /**
     * Gruppo desiderati: release non acquistate, con data non specificata
     */
    public final static int GROUP_WISHLIST = 30;
    /**
     * Gruppo da acquistare: release non acquistate
     */
    public final static int GROUP_TO_PURCHASE = 40;
    /**
     * Gruppo acquistati: release acquistate
     */
    public final static int GROUP_PURCHASED = 50;
    /**
     * Gruppo sconosciuto
     */
    public final static int GROUP_UNKNOWN = -1;

    private int mMode;
    private ArrayList<ReleaseInfo> mList;
    //
    private DateTime mToday, mPeriodStart, mPeriodEnd,
            mNextPeriodStart, mNextPeriodEnd;
    private long mTodayMs, mPeriodStartMs, mPeriodEndMs,
            mNextPeriodStartMs, mNextPeriodEndMs;
    //
    private boolean mGroupByMonth = false;
    //
    private boolean mWeekStartOnMonday = true;

    public ReleaseGroupHelper(int mode, boolean groupByMonth, boolean weekStartOnMonday) {
        mMode = mode;
        mList = new ArrayList<>();

        //imposto le date
        TimeZone timeZone = TimeZone.getDefault();
        mGroupByMonth = groupByMonth;
        mWeekStartOnMonday = weekStartOnMonday;
        mToday = DateTime.today(timeZone).getStartOfDay();
        //calcolo la data/ora di inizio/fine periodo
        if (groupByMonth) {
            mPeriodStart = mToday.getStartOfMonth();
            mPeriodEnd = mToday.getEndOfMonth();
            mNextPeriodStart = mPeriodStart.plus(0, 1, 0, 0, 0, 0, 0, DateTime.DayOverflow.Spillover);
            mNextPeriodEnd = mNextPeriodStart.getEndOfMonth();
        } else {
            mPeriodStart = Utils.getStartOfWeek(mToday, mWeekStartOnMonday).getStartOfDay();
            mPeriodEnd = mPeriodStart.plusDays(6).getEndOfDay();
            mNextPeriodStart = mPeriodStart.plusDays(7);
            mNextPeriodEnd = mPeriodEnd.plusDays(7);
        }
        //converto in ms per una comparazione più rapida
        mTodayMs = mToday.getMilliseconds(timeZone);
        mPeriodStartMs = mPeriodStart.getMilliseconds(timeZone);
        mPeriodEndMs = mPeriodEnd.getMilliseconds(timeZone);
        mNextPeriodStartMs = mNextPeriodStart.getMilliseconds(timeZone);
        mNextPeriodEndMs = mNextPeriodEnd.getMilliseconds(timeZone);

    }

    private boolean tryPutInPeriod(Release release) {
        //data specificata e nel periodo
        if (release.getDate() != null &&
                mPeriodStartMs <= release.getDate().getTime() &&
                mPeriodEndMs >= release.getDate().getTime()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPeriodNext(Release release) {
        //data specificata e nel periodo successivo
        if (release.getDate() != null &&
                mNextPeriodStartMs <= release.getDate().getTime() &&
                mNextPeriodEndMs >= release.getDate().getTime()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPeriodOther(Release release) {
        //data specificata e nel periodo altro
        if (release.getDate() != null &&
                release.getDate().getTime() > mNextPeriodEndMs) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInExpired(Release release) {
        //non acquisati, data specificata e < oggi
        if (!release.isPurchased() && release.getDate() != null &&
                release.getDate().getTime() < mTodayMs) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInLost(Release release) {
        //non acquistati, data specificata e < start_period
        if (!release.isPurchased() && release.getDate() != null &&
                release.getDate().getTime() < mPeriodStartMs) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInWishlist(Release release) {
        //non acquistati, data non specificata
        if (!release.isPurchased() && release.isWishlist()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInToPurchase(Release release) {
        if (!release.isPurchased()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPurchased(Release release) {
        if (release.isPurchased()) {
            return true;
        } else {
            return false;
        }
    }

    private int getGroup(Release release) {
        if (mMode == MODE_SHOPPING) {
            if (tryPutInPeriod(release)) return GROUP_PERIOD;
            else if (tryPutInLost(release)) return GROUP_LOST;
            else if (tryPutInWishlist(release)) return GROUP_WISHLIST;
        } else if (mMode == MODE_CALENDAR) {
            if (tryPutInPeriod(release)) return GROUP_PERIOD;
            else if (tryPutInPeriodNext(release)) return GROUP_PERIOD_NEXT;
            else if (tryPutInPeriodOther(release)) return GROUP_PERIOD_OTHER;
        } else if (mMode == MODE_LAW) {
            if (tryPutInExpired(release)) return GROUP_EXPIRED;
                //else if (tryPutInLost(release)) return GROUP_LOST;
            else if (tryPutInWishlist(release)) return GROUP_WISHLIST;
        } else if (mMode == MODE_COMICS) {
            if (tryPutInExpired(release)) return GROUP_EXPIRED;
            else if (tryPutInToPurchase(release)) return GROUP_TO_PURCHASE;
            else if (tryPutInPurchased(release)) return GROUP_PURCHASED;
        }
        return GROUP_UNKNOWN;
    }


    /**
     *
     * @param releases
     */
    public void addReleases(Release... releases) {
        int group;
        boolean releasedToday, expired;
        //filtro i dati e inserisco le release in un gruppo
        for (Release rel : releases) {
            group = getGroup(rel);
            if (group != GROUP_UNKNOWN) {
                releasedToday = (group == GROUP_TO_PURCHASE || group == GROUP_PERIOD) &&
                        rel.getDate() != null &&
                        mTodayMs == rel.getDate().getTime();
                expired = (group == GROUP_TO_PURCHASE || group == GROUP_PERIOD) &&
                        rel.getDate() != null &&
                        mTodayMs > rel.getDate().getTime();
                mList.add(new ReleaseInfo(group, releasedToday, expired, rel));
            }
        }
    }

    /**
     *
     * @return
     */
    public ReleaseInfo[] getReleaseInfos( ) {
        Collections.sort(mList, new Comparator<ReleaseInfo>() {
            @Override
            public int compare(ReleaseInfo lhs, ReleaseInfo rhs) {
                int dif = (lhs.getGroup() - rhs.getGroup());
                if (dif == 0) {
                    Date ldt = lhs.getRelease().getDate();
                    Date rdt = rhs.getRelease().getDate();
                    if (ldt != null && rdt != null) {
                        return ldt.compareTo(rdt);
                    } else if (ldt == null && rdt == null) {
                        return lhs.getRelease().getNumber() - rhs.getRelease().getNumber();
                    } else if (ldt == null) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return dif;
                }
            }
        });
        //
        return mList.toArray(new ReleaseInfo[mList.size()]);
    }

}
