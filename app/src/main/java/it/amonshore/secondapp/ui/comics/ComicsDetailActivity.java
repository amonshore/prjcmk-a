package it.amonshore.secondapp.ui.comics;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.poliveira.apps.parallaxlistview.ParallaxListView;
import com.poliveira.apps.parallaxlistview.ParallaxScrollView;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.ui.AFragment;
import it.amonshore.secondapp.ui.release.ReleaseListAdapter;
import it.amonshore.secondapp.ui.release.ReleaseListFragment;

/**
 * Created by Narsenico on 20/05/2015.
 */
public class ComicsDetailActivity extends ActionBarActivity {

    public final static String EXTRA_ENTRY = "entry";

    private Comics mComics;
    private DataManager mDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_detail);
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager(getApplicationContext());
        //leggo i parametri
        Intent intent = getIntent();
        mComics = (Comics)intent.getSerializableExtra(EXTRA_ENTRY);

//        ParallaxScrollView parallaxScrollView = (ParallaxScrollView) findViewById(R.id.parallax);
//        parallaxScrollView.setParallaxView(getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2,
//                parallaxScrollView, false));
//
//        ((TextView)findViewById(android.R.id.text1)).setText(mComics.getName());
//        ((TextView)findViewById(android.R.id.text2)).setText(mComics.getPublisher());

        ((TextView)findViewById(R.id.text1)).setText(mComics.getName());

        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        ReleaseListFragment fragment = new ReleaseListFragment();
//        Bundle args = new Bundle();
//        args.putInt(ReleaseListFragment.ARG_MODE, ReleaseListAdapter.MODE_SHOPPING);
//        fragment.setArguments(args);
//        fragmentTransaction.add(R.id.parallax, fragment);
//        fragmentTransaction.commit();

        ReleaseListFragment fragment = ((ReleaseListFragment)fragmentManager.findFragmentById(R.id.fragment));
        fragment.setComics(mComics, ReleaseListAdapter.MODE_COMICS);
        fragment.needDataRefresh(AFragment.CAUSE_LOADING);
    }

}
