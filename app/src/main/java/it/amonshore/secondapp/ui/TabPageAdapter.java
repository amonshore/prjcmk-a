package it.amonshore.secondapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.ui.comics.ComicsListFragment;
import it.amonshore.secondapp.ui.release.ReleaseListAdapter;
import it.amonshore.secondapp.ui.release.ReleaseListFragment;

/**
 * Created by Calgia on 06/05/2015.
 */
public class TabPageAdapter extends FragmentStatePagerAdapter {

    public final static int PAGE_COMICS = 0;
    public final static int PAGE_SHOPPING = 1;
    public final static int PAGE_LAW = 2;
    //
    private Context mContext;
    private ArrayList<AFragment> mPages;

    public TabPageAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
        //preparo le pagine
        mPages = new ArrayList<>();
        mPages.add(new ComicsListFragment());
        //
        Bundle args;
        AFragment frg;
        //
        args = new Bundle();
        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseGroupHelper.MODE_SHOPPING);
        frg = new ReleaseListFragment();
        frg.setArguments(args);
        mPages.add(frg);
        //
        args = new Bundle();
        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseGroupHelper.MODE_LAW);
        frg = new ReleaseListFragment();
        frg.setArguments(args);
        mPages.add(frg);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        //see http://stackoverflow.com/questions/17629463/fragmentpageradapter-how-to-handle-orientation-changes
        //altrimenti sbarella durante la rotazione della schermo
        //  perché l'adapter mantiene in memoria i "vecchi" fragment
        //return super.instantiateItem(container, position);
        AFragment fragment = (AFragment)super.instantiateItem(container, position);
        mPages.set(position, fragment);
        return fragment;
    }

    @Override
    public Fragment getItem(int position) {
        return mPages.get(position);
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == PAGE_COMICS) {
            return mContext.getString(R.string.title_page_comics);
        } else if (position == PAGE_SHOPPING) {
            return mContext.getString(R.string.title_page_shopping);
        } else if (position == PAGE_LAW) {
            return mContext.getString(R.string.title_page_wishlist);
        } else {
            return "Page " + (position + 1);
        }
    }

    public void refreshDataOnFragments(int cause) {
        for (AFragment frg : mPages) {
            frg.needDataRefresh(cause);
        }
    }
}
