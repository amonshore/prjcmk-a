package it.amonshore.secondapp.ui;

import android.content.Context;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;

/**
 * Created by Calgia on 15/05/2015.
 *
 * TODO alla classe può essere specificato cosa far vedere (tutto, wishlist, expired, etc)
 */
public class ReleaseListAdapter extends BaseAdapter {

    private Context mContext;
    private DataManager mDataManager;
    private ArrayList<Integer> mSortedIds;

    public ReleaseListAdapter(Context context) {
        this.mContext = context;
        this.mDataManager = DataManager.getDataManager(context);
        this.mSortedIds = new ArrayList<>();
    }

    /**
     *
     * @param comics se specificato aggiorna i dati con i soli
     * @return
     */
    public int refresh(Comics comics) {
        if (comics == null) {
            //TODO estraggo le release da tutti i comics
            return 0;
        } else {
            //TODO estraggo le release dal solo comics in parametro
            return 0;
        }
    }

    @Override
    public boolean hasStableIds() {
        //visto che tutti gli id degli elementi non possono cambiare nel tempo
        //  ritorno true, questo fa in modo che ListView.getCheckedItemIds() ritorni
        //  gli id degli elementi checkati (altrimenti non funziona)
        return true;
    }

    @Override
    public int getCount() {
        return this.mSortedIds.size();
    }
}
