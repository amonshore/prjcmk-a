package it.amonshore.secondapp.ui;

import android.content.Context;
import android.os.Environment;
import android.sax.RootElement;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseId;

/**
 * Created by Calgia on 15/05/2015.
 *
 * TODO alla classe può essere specificato cosa far vedere (tutto, wishlist, expired, etc)
 */
public class ReleaseListAdapter extends BaseAdapter {

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
     * Gruppo periodo di base. A questo va aggiunto 1 per ogni periodo successivo.
     * Es: 0 -> settimana corrente, 1 -> settimana successiva, etc
     */
    private final static int GROUP_PERIOD_BASE = 0;
    /**
     * Gruppo persi: release non acquistate, con data specificata e scaduta (inferiore a inizio periodo)
     */
    private final static int GROUP_LOST = -1;
    /**
     * Gruppo desiderati: release non acquistate, con data non specificata
     */
    private final static int GROUP_WISHLIST = -2;
    /**
     * Gruppo da acquistare: release non acquistate
     */
    private final static int GROUP_TO_PURCHASE = -3;
    /**
     * Gruppo acquistati: release acquistate
     */
    private final static int GROUP_PURCHASED = -4;
    /**
     * Gruppo scaduti: release non acquistate, con data speficiata e scaduta (inferiore a oggi)
     */
    private final static int GROUP_EXPIRED = -5;
    /**
     * Gruppo sconosciuto
     */
    private final static int GROUP_UNKNOWN = -666;

    private Context mContext;
    private DataManager mDataManager;
    //private ArrayList<ReleaseId> mSortedIds;
    //contiene le release suddivise per gruppo (la chiave)
    private SortedSetMultimap<Integer, ReleaseId> mGroups;
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
        //mSortedIds = new ArrayList<>();
        mGroups = Multimaps.newSortedSetMultimap(new HashMap<Integer, Collection<ReleaseId>> (), new Supplier<SortedSet<ReleaseId>>() {
            @Override
            public SortedSet<ReleaseId> get() {
                //TODO passare un comparator
                return new TreeSet<ReleaseId>();
            }
        });
        //
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
        Comics comics = mDataManager.getComics(release.getComicsId());
        if (comics.putRelease(release)) {
            //è un nuovo elemento
            mSortedIds.add(new ReleaseId(comics.getId(), release.getNumber()));
            //TODO ordinare, raggruppare, etc.
            return mSortedIds.indexOf(release);
        } else {
            //è un elemento già esistente
            //TODO ordinare, raggruppare, etc.
            return mSortedIds.indexOf(release);
        }
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
        //mSortedIds.clear();
        mGroups.clear();
        if (comics == null) {
            //estraggo le release da tutti i comics
            for (long comicsId: mDataManager.getComics()) {
                for (Release release : mDataManager.getComics(comicsId).getReleases()) {
                    mSortedIds.add(new ReleaseId(comicsId, release.getNumber()));
                }
            }
        } else {
            //estraggo le release dal solo comics in parametro
            for (Release release : comics.getReleases()) {
                mSortedIds.add(new ReleaseId(comics.getId(), release.getNumber()));
            }
        }
        //TODO ordinare, raggruppare etc
        return mSortedIds.size();
    }

    private void insertReleaseInGroup()

    @Override
    public boolean hasStableIds() {
        //visto che tutti gli id degli elementi non possono cambiare nel tempo
        //  ritorno true, questo fa in modo che ListView.getCheckedItemIds() ritorni
        //  gli id degli elementi checkati (altrimenti non funziona)
        return true;
    }

    @Override
    public int getCount() {
        return mSortedIds.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_release_item, null);
        }

        ReleaseId rid = (ReleaseId)getItem(position);
        Comics comics = mDataManager.getComics(rid.getComicsId());
        Release release = comics.getRelease(rid.getNumber());
        String relDate = "";
        if (release.getDate() != null) {
            relDate = mDateFormat.format(release.getDate());
        }
        //Utils.d("getView @" + position + " id " + comics.getId() + " " + comics.getName());
                ((TextView) convertView.findViewById(R.id.txt_list_release_name)).setText(comics.getName());
        ((TextView)convertView.findViewById(R.id.txt_list_release_info))
                .setText(String.format("#%s - %s", release.getNumber(), relDate));

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return (long)position;
    }

    @Override
    public Object getItem(int position) {
        return mSortedIds.get(position);
    }

    private int putReleaseInGroup(Release release) {
        if (mMode == MODE_SHOPPING) {
            if (tryPutInPeriod(release)) return GROUP_PERIOD_BASE;
            else if (tryPutInLost(release)) return GROUP_LOST;
            else if (tryPutInWishlist(release)) return GROUP_WISHLIST;
            else return GROUP_UNKNOWN;
        } else if (mMode == MODE_CALENDAR) {
            if (tryPutInPeriod(release)) return GROUP_PERIOD_BASE;
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

    private boolean tryPutInPeriod(Release release) {
        //TODO data specificata e >= start_period, 0 attuale, 1 successivo, etc
        return false;
    }

    private boolean tryPutInExpired(Release release) {
        //TODO non acquisati, data specificata e < oggi
        if (!release.isPurchased() && release.getDate() != null && release.getDate().getTime() < )
        return  false;
    }

    private boolean tryPutInLost(Release release) {
        //TODO non acquistati, data specificata e < start_period
        return false;
    }

    private boolean tryPutInWishlist(Release release) {
        //TODO non acquistati, data non specificata
        if (!release.isPurchased() && release.isWishlist()) {
            mGroups.put(GROUP_WISHLIST, new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInToPurchase(Release release) {
        if (!release.isPurchased()) {
            mGroups.put(GROUP_TO_PURCHASE, new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean tryPutInPurchased(Release release) {
        if (release.isPurchased()) {
            mGroups.put(GROUP_PURCHASED, new ReleaseId(release.getComicsId(), release.getNumber()));
            return true;
        } else {
            return false;
        }
    }

    private boolean isExpired(Date date) {

    }

}
