package it.amonshore.secondapp.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by Calgia on 06/05/2015.
 */
public class TabPageAdapter extends FragmentStatePagerAdapter {

    private final static int TOT_PAGES = 3;

    public TabPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return null;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return null;
    }
}
