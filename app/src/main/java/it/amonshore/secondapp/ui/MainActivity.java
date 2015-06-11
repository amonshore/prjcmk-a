package it.amonshore.secondapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.ComicsObserver;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.data.UndoHelper;
import it.amonshore.secondapp.ui.release.ReleaseListFragment;

/**
 * http://developer.android.com/training/implementing-navigation/lateral.html#tabs
 */
public class MainActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener, ComicsObserver {

    public final static String PREFS_NAME = "ComikkuPrefs";

    private TabPageAdapter mTabPageAdapter;
    private ViewPager mViewPager;
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
        //
        mDataManager = DataManager.init(this.getApplicationContext());
        Utils.d(this.getClass(), "*********** MAIN onCreate -> register observer and start WH");
        mDataManager.registerObserver(this);
        mDataManager.startWriteHandler();
        //impsota i valori di default, il parametro false assicura che questo venga fatto una sola volta
        //  indipendentemente da quante volte viene chiamato il metodo
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //registro il listerner per il cambiamento dei settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        //l'adapter fornisce i fragment che comporranno la vista a tab
        mTabPageAdapter = new TabPageAdapter(this, getSupportFragmentManager());
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mTabPageAdapter);
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
                return getResources().getColor(R.color.comikku_primary_color_900);
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
                }
            }
        });
        slidingTabLayout.setViewPager(mViewPager);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Utils.d("onSharedPreferenceChanged " + key + " " + sharedPreferences.getBoolean(key, false));
//        if (SettingsActivity.KEY_PREF_GROUP_BY_MONTH.equals(key)) {
//            ((AFragment)mTabPageAdapter.getItem(TabPageAdapter.PAGE_SHOPPING))
//                    .needDataRefresh(AFragment.CAUSE_SETTINGS_CHANGED);
//        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        new ReadDataAsyncTask().execute(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.d(this.getClass(), "*********** MAIN onDestroy -> unregister observer and stop WH");
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
        } else if (id == R.id.action_restore_backup) {
            mDataManager.restoreBackup();
            new ReadDataAsyncTask().execute(true);
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
                //slidingTabLayout.setPageTitle(1, "MODE " + fragment.getGroupMode());
                switch (fragment.getGroupMode()) {
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
                Utils.d(this.getClass(), "call writeComics");
                mDataManager.writeComics();
                break;
            case DataManager.CAUSE_COMICS_REMOVED:
            case DataManager.CAUSE_RELEASE_REMOVED:
                //TODO ho paura a gestirle per via dell'undo
                break;
        }
    }

    /**
     * Task asincrono per la lettura dei dati
     */
    private class ReadDataAsyncTask extends AsyncTask<Boolean, Comics, Integer> {
        @Override
        protected Integer doInBackground(Boolean... params) {
            Utils.d(this.getClass(), "readComics " + params[0]);
            MainActivity.this.mDataManager.readComics(params[0]);
            return DataManager.CAUSE_LOADING;
        }

        @Override
        protected void onPostExecute(Integer result) {
            //forzo l'aggiornamento del titolo della tab
            MainActivity.this.onChanged(DataManager.CAUSE_RELEASES_MODE_CHANGED);
            //
            MainActivity.this.mDataManager.notifyChanged(result);
        }
    }

}
