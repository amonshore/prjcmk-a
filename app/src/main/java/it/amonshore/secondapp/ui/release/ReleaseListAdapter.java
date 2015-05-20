package it.amonshore.secondapp.ui.release;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.applidium.headerlistview.SectionAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseId;
import it.amonshore.secondapp.data.SortedList;
import it.amonshore.secondapp.Utils;

/**
 * Created by Calgia on 15/05/2015.
 */
public class ReleaseListAdapter extends SectionAdapter {

    /**
     * Modalità shopping: periodo attuale (settimana/mese), persi, wishlist
     */
    public final static int MODE_SHOPPING = 1;
    /**
     * Modalità calendario uscite: gruppi per periodo (settimana/mese)
     */
    public final static int MODE_CALENDAR = 2;
    /**
     * Modalità persi & desiderati: persi, wishlist
     */
    public final static int MODE_LAW = 3;
    /**
     * Modalità comics: da acquistare, acquistati, persi, wishlist
     */
    public final static int MODE_COMICS = 4;

    /**
     * Gruppo periodo corrente
     */
    private final static int SECTION_PERIOD = 1;
    /**
     * Gruppo periodo successivo
     */
    private final static int SECTION_PERIOD_NEXT = 2;
    /**
     * Gruppo periodo altro
     */
    private final static int SECTION_PERIOD_OTHER = 3;
    /**
     * Gruppo persi: release non acquistate, con data specificata e scaduta (inferiore a inizio periodo)
     */
    private final static int SECTION_LOST = 10;
    /**
     * Gruppo desiderati: release non acquistate, con data non specificata
     */
    private final static int SECTION_WISHLIST = 20;
    /**
     * Gruppo da acquistare: release non acquistate
     */
    private final static int SECTION_TO_PURCHASE = 30;
    /**
     * Gruppo acquistati: release acquistate
     */
    private final static int SECTION_PURCHASED = 40;
    /**
     * Gruppo scaduti: release non acquistate, con data speficiata e scaduta (inferiore a oggi)
     */
    private final static int SECTION_EXPIRED = 50;
    /**
     * Gruppo sconosciuto
     */
    private final static int SECTION_UNKNOWN = -1;

    private Context mContext;
    private DataManager mDataManager;
    //elenco delle sezioni
    private SparseArray<Section> mSections;
    //
    private SimpleDateFormat mDateFormat;
    //modalità: indica cosa far vedere e come deve essere raggruppato
    private int mMode;
    //
    private boolean mGroupByMonth = false;
    //
    private boolean mWeekStartOnMonday = true;
    //
    private DateTime mToday, mPeriodStart, mPeriodEnd,
            mNextPeriodStart, mNextPeriodEnd;
    private long mTodayMs, mPeriodStartMs, mPeriodEndMs,
            mNextPeriodStartMs, mNextPeriodEndMs;

    /**
     *
     * @param context
     * @param mode una delle costanti MODE
     * @param groupByMonth
     * @param weekStartOnMonday
     */
    public ReleaseListAdapter(Context context, int mode, boolean groupByMonth, boolean weekStartOnMonday) {
        mContext = context;
        mDataManager = DataManager.getDataManager(context);
        mMode = mode;
        mGroupByMonth = groupByMonth;
        mWeekStartOnMonday = weekStartOnMonday;
        mSections = new SparseArray<>();
        mDateFormat = new SimpleDateFormat("c dd MMM", Locale.getDefault());
        TimeZone timeZone = TimeZone.getDefault();
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

    /**
     *
     * @return
     */
    public int getMode() {
        return mMode;
    }

    /**
     * Ritorna se la sezione SECTION_PERIOD è relativa al mese o alla settimana.
     * @return  true considera il mese, altrimenti la settimana
     */
    public boolean isGroupByMonth() {
        return mGroupByMonth;
    }

    /**
     *
     * @return  true la settimana inizia di lunedì, altrimenti di domenica
     */
    public boolean isWeekStartOnMonday() {
        return mWeekStartOnMonday;
    }

    /**
     *
     * @param release
     * @return  ritorna la posizione dell'elemento
     */
    public int insertOrUpdate(Release release) {
        //TODO insertOrUpdate
//        Comics comics = mDataManager.getComics(release.getComicsId());
//        if (comics.putRelease(release)) {
//            //è un nuovo elemento
//            mSortedIds.add(new ReleaseId(comics.getId(), release.getNumber()));
//            //TODO ordinare, raggruppare, etc.
//            return mSortedIds.indexOf(release);
//        } else {
//            //è un elemento già esistente
//            //TODO ordinare, raggruppare, etc.
//            return mSortedIds.indexOf(release);
//        }
        return -1;
    }

    /**
     *
     * @param comics
     * @return
     */
    public Release createNewRelease(Comics comics) {
        return comics.createRelease();
    }

    /**
     *
     * @param release
     * @return
     */
    public boolean remove(Release release) {
        Comics comics = mDataManager.getComics(release.getComicsId());
        return comics.removeRelease(release.getNumber());
    }

    /**
     *
     * @param comics se specificato aggiorna i dati con i soli
     * @return
     */
    public int refresh(Comics comics) {
        Utils.d("ReleaseListAdapter.refresh");
        clear();
        int tot = 0;
        if (comics == null) {
            //estraggo le release da tutti i comics
            for (long comicsId: mDataManager.getComics()) {
                for (Release release : mDataManager.getComics(comicsId).getReleases()) {
                    if (putReleaseInSection(release) != SECTION_UNKNOWN) tot++;
                }
            }
        } else {
            //estraggo le release dal solo comics in parametro
            for (Release release : comics.getReleases()) {
                if (putReleaseInSection(release) != SECTION_UNKNOWN) tot++;
            }
        }
        return tot;
    }

    /**
     *
     */
    public void clear() {
        for (int ii = 0; ii < mSections.size(); ii++) {
            mSections.valueAt(ii).releases.clear();
        }
    }

    @Override
    public boolean hasStableIds() {
        //visto che tutti gli id degli elementi non possono cambiare nel tempo
        //  ritorno true, questo fa in modo che ListView.getCheckedItemIds() ritorni
        //  gli id degli elementi checkati (altrimenti non funziona)
        return true;
    }

    private int putReleaseInSection(Release release) {
        if (mMode == MODE_SHOPPING) {
            if (tryPutInPeriod(release)) return SECTION_PERIOD;
            else if (tryPutInLost(release)) return SECTION_LOST;
            else if (tryPutInWishlist(release)) return SECTION_WISHLIST;
            else return SECTION_UNKNOWN;
        } else if (mMode == MODE_CALENDAR) {
            if (tryPutInPeriod(release)) return SECTION_PERIOD;
            else if (tryPutInPeriodNext(release)) return SECTION_PERIOD_NEXT;
            else if (tryPutInPeriodOther(release)) return SECTION_PERIOD_OTHER;
            else return SECTION_UNKNOWN;
        } else if (mMode == MODE_LAW) {
            if (tryPutInExpired(release)) return SECTION_EXPIRED;
            //else if (tryPutInLost(release)) return SECTION_LOST;
            else if (tryPutInWishlist(release)) return SECTION_WISHLIST;
            else return SECTION_UNKNOWN;
        } else if (mMode == MODE_COMICS) {
            if (tryPutInToPurchase(release)) return SECTION_TO_PURCHASE;
            else if (tryPutInPurchased(release)) return SECTION_PURCHASED;
            else return SECTION_UNKNOWN;
        } else {
            return SECTION_UNKNOWN;
        }
    }

    private void checkSection(int section) {
        if (mSections.indexOfKey(section) < 0) {
            switch (section) {
                case SECTION_PERIOD:
                    mSections.put(SECTION_PERIOD,
                            new Section(SECTION_PERIOD, R.layout.list_release_header_period, R.id.txt_list_release_header,
                                    mContext.getString(mGroupByMonth ? R.string.title_release_section_current_month :
                                            R.string.title_release_section_current_week)));
                    break;
                case SECTION_LOST:
                    mSections.put(SECTION_LOST,
                            new Section(SECTION_LOST, R.layout.list_release_header_lost, R.id.txt_list_release_header,
                                    mContext.getString(R.string.title_release_section_lost)));
                    break;
                case SECTION_WISHLIST:
                    mSections.put(SECTION_WISHLIST,
                            new Section(SECTION_WISHLIST, R.layout.list_release_header_wishlist, R.id.txt_list_release_header,
                                    mContext.getString(R.string.title_release_section_wishlist)));
                    break;
                case SECTION_PERIOD_NEXT:
                    mSections.put(SECTION_PERIOD_NEXT,
                            new Section(SECTION_PERIOD_NEXT, R.layout.list_release_header, R.id.txt_list_release_header,
                                    mContext.getString(mGroupByMonth ? R.string.title_release_section_next_month :
                                            R.string.title_release_section_next_week)));
                    break;
                case SECTION_PERIOD_OTHER:
                    mSections.put(SECTION_PERIOD_OTHER,
                            new Section(SECTION_PERIOD_OTHER, R.layout.list_release_header, R.id.txt_list_release_header,
                                    mContext.getString(R.string.title_release_section_future)));
                    break;
                case SECTION_EXPIRED:
                    mSections.put(SECTION_EXPIRED,
                            new Section(SECTION_EXPIRED, R.layout.list_release_header, R.id.txt_list_release_header,
                                    mContext.getString(R.string.title_release_section_expired)));
                    break;
                case SECTION_TO_PURCHASE:
                    mSections.put(SECTION_TO_PURCHASE,
                            new Section(SECTION_TO_PURCHASE, R.layout.list_release_header, R.id.txt_list_release_header,
                                    mContext.getString(R.string.title_release_section_to_purchase)));
                    break;
                case SECTION_PURCHASED:
                    mSections.put(SECTION_PURCHASED,
                            new Section(SECTION_PURCHASED, R.layout.list_release_header, R.id.txt_list_release_header,
                                    mContext.getString(R.string.title_release_section_purchased)));
                    break;
            }
        }
    }

    private boolean tryPutInPeriod(Release release) {
        //data specificata e nel periodo
        if (release.getDate() != null &&
                mPeriodStartMs <= release.getDate().getTime() &&
                mPeriodEndMs >= release.getDate().getTime()) {
            checkSection(SECTION_PERIOD);
            mSections.get(SECTION_PERIOD).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
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
            checkSection(SECTION_PERIOD_NEXT);
            mSections.get(SECTION_PERIOD_NEXT).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPeriodOther(Release release) {
        //data specificata e nel periodo altro
        if (release.getDate() != null &&
                release.getDate().getTime() > mNextPeriodEndMs) {
            checkSection(SECTION_PERIOD_OTHER);
            mSections.get(SECTION_PERIOD_OTHER).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInExpired(Release release) {
        //non acquisati, data specificata e < oggi
        if (!release.isPurchased() && release.getDate() != null &&
                release.getDate().getTime() < mTodayMs) {
            checkSection(SECTION_EXPIRED);
            mSections.get(SECTION_EXPIRED).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInLost(Release release) {
        //non acquistati, data specificata e < start_period
        if (!release.isPurchased() && release.getDate() != null &&
                release.getDate().getTime() < mPeriodStartMs) {
            checkSection(SECTION_LOST);
            mSections.get(SECTION_LOST).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInWishlist(Release release) {
        //non acquistati, data non specificata
        if (!release.isPurchased() && release.isWishlist()) {
            checkSection(SECTION_WISHLIST);
            mSections.get(SECTION_WISHLIST).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInToPurchase(Release release) {
        if (!release.isPurchased()) {
            checkSection(SECTION_TO_PURCHASE);
            mSections.get(SECTION_TO_PURCHASE).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPurchased(Release release) {
        if (release.isPurchased()) {
            checkSection(SECTION_PURCHASED);
            mSections.get(SECTION_PURCHASED).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int numberOfSections() {
        return mSections.size();
    }

    @Override
    public int numberOfRows(int section) {
        if (section < 0 || section >= mSections.size()) {
            //Utils.d("numberOfRows " + section + " " + mSections.size());
            return 0;
        }
        return mSections.valueAt(section).releases.size();
    }

    @Override
    public Object getRowItem(int section, int row) {
        return mSections.valueAt(section).releases.get(row);
    }

    @Override
    public boolean hasSectionHeaderView(int section) {
        return true;
    }

    @Override
    public View getRowView(int section, int row, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_release_item, null);
        }

        ReleaseId rid = (ReleaseId)getRowItem(section, row);
        Comics comics = mDataManager.getComics(rid.getComicsId());
        Release release = comics.getRelease(rid.getNumber());
        String relDate = "";
        if (release.getDate() != null) {
            relDate = mDateFormat.format(release.getDate());
        }
//        Utils.d("getRowView @" + section + "/" + row + " " + comics.getName() + " " + convertView);
//        if (convertView.findViewById(R.id.txt_list_release_name) == null) {
//            Utils.d("isnull " + convertView.getId() + " " + R.layout.list_release_item);
//        }
        ((TextView)convertView.findViewById(R.id.txt_list_release_name)).setText(comics.getName());
        ((TextView)convertView.findViewById(R.id.txt_list_release_info))
                .setText(String.format("#%s - %s - p %s", release.getNumber(), relDate, release.isPurchased()));

        return convertView;
    }

    @Override
    public int getSectionHeaderViewTypeCount() {
        //ATTENZIONE incasina tutto
        return mSections.size();
        //return 1;
    }

    @Override
    public int getSectionHeaderItemViewType(int section) {
        Utils.d("getSectionHeaderItemViewType " + section);
        ////ATTENZIONE incasina tutto return mSections.valueAt(section).group;
        //return 0;
        return section;
    }

    @Override
    public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
        Section sect = mSections.valueAt(section);
        Utils.d("getSectionHeaderView " + section + " " + sect.caption + " " + (convertView != null));
        //
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(sect.layoutResId, null);
        }
//        Utils.d("getSectionHeaderView @" + section + " " + convertView);
//        if (convertView.findViewById(R.id.txt_list_release_header) == null) {
//            Utils.d("isnull2 " + convertView.getId() + " " + R.layout.list_release_header);
//        }
        ((TextView) convertView.findViewById(sect.textResId)).setText(sect.caption);
        return convertView;
    }

    @Override
    public void onRowItemClick(AdapterView<?> parent, View view, int section, int row, long id) {
        super.onRowItemClick(parent, view, section, row, id);
        Toast.makeText(mContext, "Section " + section + " row " + row, Toast.LENGTH_SHORT).show();
    }

    /**
     *
     */
    private final static class Section {
        //codice gruppo, chiave di mGroups
        public int group;
        //layout da utilizzare per la sezione
        public int layoutResId;
        //text view
        public int textResId;
        //
        public CharSequence caption;
        //
        public SortedList<ReleaseId> releases;

        public Section(int group, int layoutResId, int textResId, CharSequence caption) {
            this.group = group;
            this.layoutResId = layoutResId;
            this.textResId = textResId;
            this.caption = caption;
            //TODO passare comparator
            this.releases = new SortedList<>();
        }
    }

}
