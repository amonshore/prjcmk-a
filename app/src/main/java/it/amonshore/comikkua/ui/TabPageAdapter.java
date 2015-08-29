package it.amonshore.comikkua.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.ui.comics.ComicsListFragment;
import it.amonshore.comikkua.ui.release.ReleaseListFragment;

/**
 * Created by Narsenico on 06/05/2015.
 */
public class TabPageAdapter extends FragmentStatePagerAdapter {

    private final Context mContext;
    private final ArrayList<AFragment> mPages;

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
//        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseGroupHelper.MODE_CALENDAR);
        frg = new ReleaseListFragment();
        frg.setArguments(args);
        mPages.add(frg);
//        //
//        args = new Bundle();
//        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseGroupHelper.MODE_LAW);
//        frg = new ReleaseListFragment();
//        frg.setArguments(args);
//        mPages.add(frg);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        //see http://stackoverflow.com/questions/17629463/fragmentpageradapter-how-to-handle-orientation-changes
        //altrimenti sbarella durante la rotazione della schermo
        //  perché l'adapter mantiene in memoria i "vecchi" fragment
        //return super.instantiateItem(container, position);
        Utils.d(this.getClass(), "new fragment");
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
        if (position == 0) {
            return mContext.getString(R.string.title_page_comics);
//        } else if (position == 1) {
//            return mContext.getString(R.string.title_page_calendar);
//        } else {
//            return mContext.getString(R.string.title_page_wishlist);
//        }
        } else {
            return mContext.getString(R.string.title_page_releases);
        }
    }

}
