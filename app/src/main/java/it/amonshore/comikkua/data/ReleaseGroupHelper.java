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

    private final int mMode;
    private final ArrayList<ReleaseInfo> mList;
    //
    private final long mTodayMs, mPeriodStartMs, mPeriodEndMs,
            mNextPeriodStartMs, mNextPeriodEndMs;

    public ReleaseGroupHelper(int mode, boolean groupByMonth, boolean weekStartOnMonday) {
        mMode = mode;
        mList = new ArrayList<>();

        //imposto le date
        DateTime mToday, mPeriodStart, mPeriodEnd, mNextPeriodStart, mNextPeriodEnd;
        TimeZone timeZone = TimeZone.getDefault();
        mToday = DateTime.today(timeZone).getStartOfDay();
        //calcolo la data/ora di inizio/fine periodo
        if (groupByMonth) {
            mPeriodStart = mToday.getStartOfMonth();
            mPeriodEnd = mToday.getEndOfMonth();
            mNextPeriodStart = mPeriodStart.plus(0, 1, 0, 0, 0, 0, 0, DateTime.DayOverflow.Spillover);
            mNextPeriodEnd = mNextPeriodStart.getEndOfMonth();
        } else {
            mPeriodStart = Utils.getStartOfWeek(mToday, weekStartOnMonday).getStartOfDay();
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
        return release.getDate() != null &&
                mPeriodStartMs <= release.getDate().getTime() &&
                mPeriodEndMs >= release.getDate().getTime();
    }

    private boolean tryPutInPeriodNext(Release release) {
        //data specificata e nel periodo successivo
        return release.getDate() != null &&
                mNextPeriodStartMs <= release.getDate().getTime() &&
                mNextPeriodEndMs >= release.getDate().getTime();
    }

    private boolean tryPutInPeriodOther(Release release) {
        //data specificata e nel periodo altro
        return release.getDate() != null &&
                release.getDate().getTime() > mNextPeriodEndMs;
    }

    private boolean tryPutInExpired(Release release) {
        //non acquisati, data specificata e < oggi
        return !release.isPurchased() && release.getDate() != null &&
                release.getDate().getTime() < mTodayMs;
    }

    private boolean tryPutInLost(Release release) {
        //non acquistati, data specificata e < start_period
        return !release.isPurchased() && release.getDate() != null &&
                release.getDate().getTime() < mPeriodStartMs;
    }

    private boolean tryPutInWishlist(Release release) {
        //non acquistati, data non specificata
        return !release.isPurchased() && release.isWishlist();
    }

    private boolean tryPutInToPurchase(Release release) {
        return !release.isPurchased();
    }

    private boolean tryPutInPurchased(Release release) {
        return release.isPurchased();
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
//A0041
//        int group;
//        boolean releasedToday, expired;
//        //filtro i dati e inserisco le release in un gruppo
//        for (Release rel : releases) {
//            group = getGroup(rel);
//            if (group != GROUP_UNKNOWN) {
//                releasedToday = (group == GROUP_TO_PURCHASE || group == GROUP_PERIOD) &&
//                        rel.getDate() != null &&
//                        mTodayMs == rel.getDate().getTime();
//                expired = (group == GROUP_TO_PURCHASE || group == GROUP_PERIOD) &&
//                        rel.getDate() != null &&
//                        mTodayMs > rel.getDate().getTime();
//                mList.add(new ReleaseInfo(group, releasedToday, expired, rel));
//            }
//        }
        addReleases(false, releases);
    }

    /**
     * Si da per scontato che tutte le release siano relative al medesimo comics
     *
     * @param uniqueWishlist    se true tutte le wishlist sono raggruppate in un'unica release
     * @param releases
     */
    public void addReleases(boolean uniqueWishlist, Release... releases) {
        MultiReleaseInfo multiWishlistRelease = null;
        int group;
        boolean releasedToday, expired;
        //filtro i dati e inserisco le release in un gruppo
        for (Release rel : releases) {
            group = getGroup(rel);
            if (group != GROUP_UNKNOWN) {
                if (uniqueWishlist && group == GROUP_WISHLIST) {
                    if (multiWishlistRelease == null) {
                        multiWishlistRelease = new MultiReleaseInfo(group, rel);
                    } else {
                        multiWishlistRelease.addInnerRelease(rel);
                    }
                } else {
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
        if (multiWishlistRelease != null) {
            if (multiWishlistRelease.size() == 1) {
                mList.add(new ReleaseInfo(multiWishlistRelease.getGroup(), multiWishlistRelease.getRelease()));
            } else {
                mList.add(multiWishlistRelease);
            }
        }
    }

    /**
     *
     * @return
     */
    public ReleaseInfo[] getReleaseInfos( ) {
        //A0032
        Collections.sort(mList, new Comparator<ReleaseInfo>() {
            @Override
            public int compare(ReleaseInfo lhs, ReleaseInfo rhs) {
                int res = (lhs.getGroup() - rhs.getGroup());
                if (res == 0) {
                    Date ldt = lhs.getRelease().getDate();
                    Date rdt = rhs.getRelease().getDate();

                    if (ldt != null && rdt != null) {
                        res = ldt.compareTo(rdt);
                    } else if (ldt == null && rdt != null) {
                        res = -1;
                    } else if (ldt != null) {
                        res = 1;
                    }

                    if (res == 0) {
                        res =  lhs.getRelease().getNumber() - rhs.getRelease().getNumber();
                    }
                }

                return res;
            }
        });
        //
        return mList.toArray(new ReleaseInfo[mList.size()]);
    }

}
