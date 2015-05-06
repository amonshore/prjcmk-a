package it.amonshore.secondapp.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Calgia on 06/05/2015.
 */
public class TabPageAdapter extends FragmentStatePagerAdapter {

    public final static int TOT_PAGES = 3;
    public final static int PAGE_COMICS = 0;
    public final static int PAGE_RELEASES = 1;
    public final static int PAGE_WISHLIST = 2;
    //
    ArrayList<Fragment> mPages;

    public TabPageAdapter(FragmentManager fm) {
        super(fm);

        //preparo le pagine
        mPages = new ArrayList<>();
        mPages.add(ComicsListFragment.newInstance("P1", "P2"));
        mPages.add(FakeBlankFragment.newInstance("Fake page A"));
        mPages.add(FakeBlankFragment.newInstance("Fake page B"));
    }

    @Override
    public Fragment getItem(int position) {
        Log.d("XXX", "getItem " + position);
        return mPages.get(position);
    }

    @Override
    public int getCount() {
        return TOT_PAGES;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Log.d("XXX", "getPageTitle " + position);
        if (position == PAGE_COMICS) {
            return "Comics";
        } else {
            return "Page " + (position + 1);
        }
    }
}
