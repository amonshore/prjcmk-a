package it.amonshore.secondapp.ui;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.applidium.headerlistview.SectionAdapter;

import java.security.acl.Group;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseId;
import it.amonshore.secondapp.data.SortedList;
import it.amonshore.secondapp.data.Utils;

/**
 * Created by Calgia on 15/05/2015.
 */
public class ReleaseListAdapter extends SectionAdapter {

    /**
     * Il raggruppamento per periodo considera le settimane
     */
    public final static int BY_WEEK = 0;
    /**
     * Il raggruppamento per periodo considera i mesi
     */
    public final static int BY_MONTH = 1;

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
    private final static int GROUP_PERIOD = 1;
    /**
     * Gruppo periodo successivo
     */
    private final static int GROUP_PERIOD_NEXT = 2;
    /**
     * Gruppo periodo altro
     */
    private final static int GROUP_PERIOD_OTHER = 3;
    /**
     * Gruppo persi: release non acquistate, con data specificata e scaduta (inferiore a inizio periodo)
     */
    private final static int GROUP_LOST = 10;
    /**
     * Gruppo desiderati: release non acquistate, con data non specificata
     */
    private final static int GROUP_WISHLIST = 20;
    /**
     * Gruppo da acquistare: release non acquistate
     */
    private final static int GROUP_TO_PURCHASE = 30;
    /**
     * Gruppo acquistati: release acquistate
     */
    private final static int GROUP_PURCHASED = 40;
    /**
     * Gruppo scaduti: release non acquistate, con data speficiata e scaduta (inferiore a oggi)
     */
    private final static int GROUP_EXPIRED = 50;
    /**
     * Gruppo sconosciuto
     */
    private final static int GROUP_UNKNOWN = -1;

    private Context mContext;
    private DataManager mDataManager;
    //elenco dei gruppi
    SparseArray<GroupInfo> mGroupInfos;
    //
    private SimpleDateFormat mDateFormat;
    //modalità: indica cosa far vedere e come deve essere raggruppato
    private int mMode;
    //
    private Calendar mToday;

    /**
     *
     * @param context
     * @param mode una delle costanti MODE
     */
    public ReleaseListAdapter(Context context, int mode) {
        mContext = context;
        mDataManager = DataManager.getDataManager(context);
        mMode = mode;
        mGroupInfos = new SparseArray<>();
        mDateFormat = new SimpleDateFormat("c dd MMM", Locale.getDefault());
        mToday = Calendar.getInstance();
    }

    public int getMode() {
        return mMode;
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
                    if (putReleaseInGroup(release) != GROUP_UNKNOWN) tot++;
                }
            }
        } else {
            //estraggo le release dal solo comics in parametro
            for (Release release : comics.getReleases()) {
                if (putReleaseInGroup(release) != GROUP_UNKNOWN) tot++;
            }
        }
        return tot;
    }

    public void clear() {
        for (int ii = 0; ii < mGroupInfos.size(); ii++) {
            mGroupInfos.valueAt(ii).releases.clear();
        }
    }

    @Override
    public boolean hasStableIds() {
        //visto che tutti gli id degli elementi non possono cambiare nel tempo
        //  ritorno true, questo fa in modo che ListView.getCheckedItemIds() ritorni
        //  gli id degli elementi checkati (altrimenti non funziona)
        return true;
    }

    private int putReleaseInGroup(Release release) {
        if (mMode == MODE_SHOPPING) {
            if (tryPutInPeriod(release)) return GROUP_PERIOD;
            else if (tryPutInLost(release)) return GROUP_LOST;
            else if (tryPutInWishlist(release)) return GROUP_WISHLIST;
            else return GROUP_UNKNOWN;
        } else if (mMode == MODE_CALENDAR) {
            if (tryPutInPeriod(release)) return GROUP_PERIOD;
            else if (tryPutInPeriodNext(release)) return GROUP_PERIOD_NEXT;
            else if (tryPutInPeriodOther(release)) return GROUP_PERIOD_OTHER;
            else return GROUP_UNKNOWN;
        } else if (mMode == MODE_LAW) {
            if (tryPutInExpired(release)) return GROUP_EXPIRED;
            //else if (tryPutInLost(release)) return GROUP_LOST;
            else if (tryPutInWishlist(release)) return GROUP_WISHLIST;
            else return GROUP_UNKNOWN;
        } else if (mMode == MODE_COMICS) {
            if (tryPutInToPurchase(release)) return GROUP_TO_PURCHASE;
            else if (tryPutInPurchased(release)) return GROUP_PURCHASED;
            else return GROUP_UNKNOWN;
        } else {
            return GROUP_UNKNOWN;
        }
    }

    private void checkGroup(int group) {
        if (mGroupInfos.indexOfKey(group) < 0) {
            switch (group) {
                case GROUP_PERIOD:
                    mGroupInfos.put(GROUP_PERIOD,
                            new GroupInfo(GROUP_PERIOD, android.R.layout.simple_list_item_1, android.R.id.text1, "Periodo corrente"));
                    break;
                case GROUP_LOST:
                    mGroupInfos.put(GROUP_LOST,
                            new GroupInfo(GROUP_LOST, android.R.layout.simple_list_item_1, android.R.id.text1, "Persi"));
                    break;
                case GROUP_WISHLIST:
                    mGroupInfos.put(GROUP_WISHLIST,
                            new GroupInfo(GROUP_WISHLIST, android.R.layout.simple_list_item_1, android.R.id.text1, "Desiderati"));
                    break;
//        } else if (mMode == MODE_CALENDAR) {
//            mGroupInfos.put(GROUP_PERIOD, new GroupInfo(GROUP_PERIOD, android.R.layout.simple_list_item_1, android.R.id.text1, "Periodo corrente"));
//            mGroupInfos.put(GROUP_PERIOD_NEXT, new GroupInfo(GROUP_PERIOD_NEXT, android.R.layout.simple_list_item_1, android.R.id.text1, "Periodo successivo"));
//            mGroupInfos.put(GROUP_PERIOD_OTHER, new GroupInfo(GROUP_PERIOD_OTHER, android.R.layout.simple_list_item_1, android.R.id.text1, "Periodo altro"));
//        } else if (mMode == MODE_LAW) {
//            mGroupInfos.put(GROUP_EXPIRED, new GroupInfo(GROUP_EXPIRED, android.R.layout.simple_list_item_1, android.R.id.text1, "Scaduti"));
//            mGroupInfos.put(GROUP_WISHLIST, new GroupInfo(GROUP_WISHLIST, android.R.layout.simple_list_item_1, android.R.id.text1, "Desiderati"));
//        } else if (mMode == MODE_COMICS) {
//            mGroupInfos.put(GROUP_TO_PURCHASE, new GroupInfo(GROUP_TO_PURCHASE, android.R.layout.simple_list_item_1, android.R.id.text1, "Da acquistare"));
//            mGroupInfos.put(GROUP_PURCHASED, new GroupInfo(GROUP_PURCHASED, android.R.layout.simple_list_item_1, android.R.id.text1, "Acquistati"));
//        }

            }
        }
    }

    private boolean tryPutInPeriod(Release release) {
        //TODO data specificata e nel periodo
        if (release.getDate() != null) {
            checkGroup(GROUP_PERIOD);
            mGroupInfos.get(GROUP_PERIOD).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPeriodNext(Release release) {
        //TODO data specificata e nel periodo succssivo
        return false;
    }

    private boolean tryPutInPeriodOther(Release release) {
        //TODO data specificata e nel periodo altro
        return false;
    }

    private boolean tryPutInExpired(Release release) {
        //TODO non acquisati, data specificata e < oggi
        //if (!release.isPurchased() && release.getDate() != null && release.getDate().getTime() < )
        return  false;
    }

    private boolean tryPutInLost(Release release) {
        //TODO non acquistati, data specificata e < start_period
        return false;
    }

    private boolean tryPutInWishlist(Release release) {
        //TODO non acquistati, data non specificata
        if (!release.isPurchased() && release.isWishlist()) {
            checkGroup(GROUP_WISHLIST);
            mGroupInfos.get(GROUP_WISHLIST).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInToPurchase(Release release) {
        if (!release.isPurchased()) {
            mGroupInfos.get(GROUP_TO_PURCHASE).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPurchased(Release release) {
        if (release.isPurchased()) {
            mGroupInfos.get(GROUP_PURCHASED).releases.add(new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean isExpired(Date date) {
        //TODO
        return false;
    }

    @Override
    public int numberOfSections() {
        return mGroupInfos.size();
    }

    @Override
    public int numberOfRows(int section) {
        if (section < 0 || section >= mGroupInfos.size()) {
            //Utils.d("numberOfRows " + section + " " + mGroupInfos.size());
            return 0;
        }
        return mGroupInfos.valueAt(section).releases.size();
    }

    @Override
    public Object getRowItem(int section, int row) {
        return mGroupInfos.valueAt(section).releases.get(row);
    }

    @Override
    public boolean hasSectionHeaderView(int section) {
        return true;
    }

    @Override
    public View getRowView(int section, int row, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_release_item, null);
            //convertView.setId(R.layout.list_release_item);
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
                .setText(String.format("#%s - %s", release.getNumber(), relDate));

        return convertView;
    }

    @Override
    public int getSectionHeaderViewTypeCount() {
        //ATTENZIONE incasina tutto return mGroupInfos.size();
        return 1;
    }

    @Override
    public int getSectionHeaderItemViewType(int section) {
        //Utils.d("getSectionHeaderItemViewType " + section);
        ////ATTENZIONE incasina tutto return mGroupInfos.valueAt(section).group;
        return 0;
    }

    @Override
    public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
        //TODO
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_release_header, null);
            //convertView.setId(R.layout.list_release_header);
        }
//        Utils.d("getSectionHeaderView @" + section + " " + convertView);
//        if (convertView.findViewById(R.id.txt_list_release_header) == null) {
//            Utils.d("isnull2 " + convertView.getId() + " " + R.layout.list_release_header);
//        }
        GroupInfo group = mGroupInfos.valueAt(section);
        ((TextView) convertView.findViewById(R.id.txt_list_release_header)).setText(group.caption);
        return convertView;
    }

    @Override
    public void onRowItemClick(AdapterView<?> parent, View view, int section, int row, long id) {
        super.onRowItemClick(parent, view, section, row, id);
        Toast.makeText(mContext, "Section " + section + " row " + row, Toast.LENGTH_LONG).show();
    }

    /**
     *
     */
    private final static class GroupInfo {
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

        public GroupInfo(int group, int layoutResId, int textResId, CharSequence caption) {
            this.group = group;
            this.layoutResId = layoutResId;
            this.textResId = textResId;
            this.caption = caption;
            //TODO passare comparator
            this.releases = new SortedList<>();
        }
    }

}
