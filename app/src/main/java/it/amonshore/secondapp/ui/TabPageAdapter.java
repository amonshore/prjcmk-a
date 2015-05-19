package it.amonshore.secondapp.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

/**
 * Created by Calgia on 06/05/2015.
 */
public class TabPageAdapter extends FragmentStatePagerAdapter {

    public final static int PAGE_COMICS = 0;
    public final static int PAGE_SHOPPING = 1;
    public final static int PAGE_LAW = 2;
    //
    ArrayList<Fragment> mPages;

    public TabPageAdapter(FragmentManager fm) {
        super(fm);

        //preparo le pagine
        mPages = new ArrayList<>();
        mPages.add(new ComicsListFragment());

        Bundle args;
        Fragment frg;

        args = new Bundle();
        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseListAdapter.MODE_SHOPPING);
        frg = new ReleaseListFragment();
        frg.setArguments(args);
        mPages.add(frg);

//TODO switchando tra le page ricevo questo errore
        //java.lang.IllegalArgumentException: Wrong state class, expecting View State but received class android.widget.AbsListView$SavedState instead.
        //This usually happens when two views of different type have the same id in the same hierarchy. This view's id is id/list. Make sure other views do not use the same id.
//        args = new Bundle();
//        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseListAdapter.MODE_LAW);
//        frg = new ReleaseListFragment();
//        frg.setArguments(args);
//        mPages.add(frg);
    }

    @Override
    public Fragment getItem(int position) {
        //Utils.d("getItem " + position);
        return mPages.get(position);
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //TODO recuperare i tioli della pagine dalle risorse (direttamente dai fragment?)
        if (position == PAGE_COMICS) {
            return "Comics";
        } else if (position == PAGE_SHOPPING) {
            return "Releases";
        } else if (position == PAGE_LAW) {
            return "Wishlist";
        } else {
            return "Page " + (position + 1);
        }
    }
}
