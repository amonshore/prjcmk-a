package it.amonshore.comikkua.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.ComicsObserver;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.ReleaseGroupHelper;
import it.amonshore.comikkua.ui.options.DataOptionsActivity;
import it.amonshore.comikkua.ui.release.ReleaseListFragment;

/**
 * http://developer.android.com/training/implementing-navigation/lateral.html#tabs
 */
public class MainActivity extends ActionBarActivity implements ComicsObserver {

    public final static String PREFS_NAME = "ComikkuPrefs";

    private TabPageAdapter mTabPageAdapter;
    private DataManager mDataManager;
    //salvo la page/fragment precedente
    private int mPreviousPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //il layout contiene solo il ViewPager
        //l'action bar è fornita dalla super classe
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main_sliding);
        //mDataManager = DataManager.init(this.getApplicationContext(), null); -> inizializzo in ComikkuApp
        mDataManager = DataManager.getDataManager();
        Utils.d(this.getClass(), "*********** MAIN onCreate -> register observer and start WH");
        mDataManager.registerObserver(this);
        mDataManager.startWriteHandler();
        //impsota i valori di default, il parametro false assicura che questo venga fatto una sola volta
        //  indipendentemente da quante volte viene chiamato il metodo
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//        //registro il listerner per il cambiamento dei settings
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        prefs.registerOnSharedPreferenceChangeListener(this);
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        //l'adapter fornisce i fragment che comporranno la vista a tab
        mTabPageAdapter = new TabPageAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = (ViewPager)findViewById(R.id.pager);
        viewPager.setAdapter(mTabPageAdapter);
        mPreviousPage = 0;
        // Give the SlidingTabLayout the ViewPager
        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        // Set custom tab layout
        slidingTabLayout.setCustomTabView(R.layout.activity_main_tab, 0);
        // Center the tabs in the layout
        slidingTabLayout.setDistributeEvenly(true);
        // Customize tab color
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.comikku_selected_tab_color);
            }
        });
        slidingTabLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Utils.d("slidingTabLayout OnPageChangeListener " + mPreviousPage + " -> " + position);
                if (mPreviousPage != position) {
                    //chiudo l'ActionBar contestuale del fragment precedente
                    ((AFragment) mTabPageAdapter.getItem(mPreviousPage)).finishActionMode();
                    //aggiorno i dati sulla page corrente causa cambio pagina (quindi se i dati ci sono non fa nulla)
                    ((AFragment) mTabPageAdapter.getItem(position)).onDataChanged(DataManager.CAUSE_PAGE_CHANGED);
                    mPreviousPage = position;
                } else {
                    //A0053 scroll top alla selezione della stessa tab
                    ((ScrollToTopListener) mTabPageAdapter.getItem(position)).scrollToTop();
                }
            }
        });
        slidingTabLayout.setViewPager(viewPager);
    }

    //spostato in DataManager
//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        Utils.d("onSharedPreferenceChanged " + key + " " + sharedPreferences.getBoolean(key, false));
//        //A0046 aggiorno le best release di tutti i fumetti
//        if (DataManager.KEY_PREF_LAST_PURCHASED.equals(key)) {
//            mDataManager.updateBestRelease();
//        }
//    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //A0040 new ReadDataAsyncTask().execute();
        Handler mh = new Handler(getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    mDataManager.readComics();
                    //forzo l'aggiornamento del titolo della tab
                    onChanged(DataManager.CAUSE_RELEASES_MODE_CHANGED);
                    mDataManager.notifyChanged(DataManager.CAUSE_LOADING);
                } catch (Exception ex) {
                    Utils.e("A0040 read comics", ex);
                }
            }
        };
        mh.post(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.d(this.getClass(), "*********** MAIN onDestroy -> unregister observer and stop WH");
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mDataManager.unregisterObserver(this);
        mDataManager.stopWriteHandler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_info) {
            Intent intent = new Intent(this, InfoActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_options) {
            Intent intent = new Intent(this, DataOptionsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onChanged(int cause) {
        switch (cause) {
            case DataManager.CAUSE_RELEASES_MODE_CHANGED:
                ReleaseListFragment fragment = (ReleaseListFragment)mTabPageAdapter.getItem(1);
                SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
                int groupMode = fragment.getGroupMode();
                if (groupMode == 0) {
                    SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                    groupMode = settings.getInt(ReleaseListFragment.STATE_GROUP_MODE, ReleaseGroupHelper.MODE_CALENDAR);
                }
                switch (groupMode) {
                    case ReleaseGroupHelper.MODE_LAW:
                        slidingTabLayout.setPageTitle(1, getString(R.string.title_page_wishlist));
                        break;
                    case ReleaseGroupHelper.MODE_CALENDAR:
                        slidingTabLayout.setPageTitle(1, getString(R.string.title_page_calendar));
                        break;
                    case ReleaseGroupHelper.MODE_SHOPPING:
                        slidingTabLayout.setPageTitle(1, getString(R.string.title_page_shopping));
                        break;
                    default:
                        slidingTabLayout.setPageTitle(1, getString(R.string.title_page_releases));
                        break;
                }
                break;
            case DataManager.CAUSE_COMICS_ADDED:
            case DataManager.CAUSE_COMICS_CHANGED:
            case DataManager.CAUSE_RELEASE_ADDED:
            case DataManager.CAUSE_RELEASE_CHANGED:
            case DataManager.CAUSE_COMICS_REMOVED:
            case DataManager.CAUSE_RELEASE_REMOVED:
                break;
        }
    }

//    A0040
//    /**
//     * Task asincrono per la lettura dei dati
//     */
//    private class ReadDataAsyncTask extends AsyncTask<Void, Comics, Integer> {
//        @Override
//        protected Integer doInBackground(Void... params) {
//            Utils.d(this.getClass(), "readComics");
//            MainActivity.this.mDataManager.readComics();
//            return DataManager.CAUSE_LOADING;
//        }
//
//        @Override
//        protected void onPostExecute(Integer result) {
//            //forzo l'aggiornamento del titolo della tab
//            MainActivity.this.onChanged(DataManager.CAUSE_RELEASES_MODE_CHANGED);
//            //
//            MainActivity.this.mDataManager.notifyChanged(result);
//        }
//    }

}
