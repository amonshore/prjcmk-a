package it.amonshore.secondapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

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

    private Context mContext;
    private DataManager mDataManager;
    private ArrayList<ReleaseId> mSortedIds;

    public ReleaseListAdapter(Context context) {
        this.mContext = context;
        this.mDataManager = DataManager.getDataManager(context);
        this.mSortedIds = new ArrayList<>();
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
            return this.mSortedIds.indexOf(release);
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
        this.mSortedIds.clear();
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            //TODO convertView = LayoutInflater.from(mContext).inflate(R.layout.list_comics_item, null);
            convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_activated_2, null);
        }

        ReleaseId rid = (ReleaseId)getItem(position);
        Comics comics = mDataManager.getComics(rid.getComicsId());
        Release release = comics.getRelease(rid.getNumber());
        //Utils.d("getView @" + position + " id " + comics.getId() + " " + comics.getName());
        ((TextView)convertView.findViewById(android.R.id.text1)).setText(comics.getName());
        ((TextView)convertView.findViewById(android.R.id.text2)).setText(Integer.toString(release.getNumber()));

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
}
