package it.amonshore.secondapp.ui;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import it.amonshore.secondapp.R;

/**
 * http://developer.android.com/training/implementing-navigation/lateral.html#tabs
 */
public class MainActivity extends ActionBarActivity
        implements ComicsListFragment.OnFragmentInteractionListener {

    private final static String LOG_TAG = "MAC";

    TabPageAdapter mTabPageAdapter;
    ViewPager mViewPager;
    //salvo la page/fragment precedente
    int mPreviousPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //il layout contiene solo il ViewPager
        //l'action bar è fornita dalla super classe
        setContentView(R.layout.activity_main);

        //impsota i valori di default, il parametro false assicura che questo venga fatto una sola volta
        //  indipendentemente da quante volte viene chiamato il metodo
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //attenzione, usare getSupportActionBar() per recuperare l'action bar
        final ActionBar actionBar = getSupportActionBar();
        //imposto la modalità di navitazione a tab (sarà possibile aggiungere tab all'action bar)
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        //l'adapter fornisce i fragment che comporranno la vista a tab
        mTabPageAdapter = new TabPageAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mTabPageAdapter);
        mPreviousPage = 0;
        //gestisco la selezione delle tab allo swipe dei fragment
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                //chiudo l'ActionBar contestuale del fragment precedente
                ((OnChangePageListener)mTabPageAdapter.getItem(mPreviousPage)).finishActionMode();
                actionBar.setSelectedNavigationItem(mPreviousPage = position);
            }
        });
        //gesisco la selezione del fragment alla selezione di una tab
        final SimpleActionBarTabListener tabListener = new SimpleActionBarTabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                //chiudo l'ActionBar contestuale del fragment precedente
                ((OnChangePageListener)mTabPageAdapter.getItem(mPreviousPage)).finishActionMode();
                mViewPager.setCurrentItem(mPreviousPage = tab.getPosition());
            }
        };
        //per ogni fragment fornito dall'adapter creo una tab
        for (int ii=0; ii<mTabPageAdapter.getCount(); ii++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mTabPageAdapter.getPageTitle(ii))
                            .setTabListener(tabListener));
        }
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String id) {
        //TODO ComicsListFragment fornisce un rudimentale sistema di scambio dati con l'activity principale
    }
}
