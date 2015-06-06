package it.amonshore.secondapp.ui.comics;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.ReleaseGroupHelper;
import it.amonshore.secondapp.ui.AFragment;
import it.amonshore.secondapp.ui.release.ReleaseEditorActivity;
import it.amonshore.secondapp.ui.release.ReleaseListFragment;

/**
 * Created by Narsenico on 20/05/2015.
 */
public class ComicsDetailActivity extends ActionBarActivity {

    public final static String EXTRA_COMICS_ID = "comicsId";

    private Comics mComics;
    private DataManager mDataManager;
    private ReleaseListFragment mReleaseListFragment;
    private TextView mTxtName, mTxtPublisher, mTxtNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_detail);
        //TODO cambiare il colore della status bar in base al colore primario dell'immagine dell'header
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#02B1CE"));
        }
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager();
        //leggo i parametri
        Intent intent = getIntent();
        //presumo che l'id sia valido
        mComics = mDataManager.getComics(intent.getLongExtra(EXTRA_COMICS_ID, 0));
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //
        mTxtName = ((TextView)findViewById(R.id.txt_detail_comics_name));
        mTxtPublisher = ((TextView)findViewById(R.id.txt_detail_comics_publisher));
        mTxtNotes = ((TextView)findViewById(R.id.txt_detail_comics_notes));
        updateHeader();
        //
        ((FloatingActionButton)findViewById(R.id.fab_comics_edit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComicsEditor(mComics);
            }
        });
        //listener fab
        ((FloatingActionButton)findViewById(R.id.fab_release_add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseEditor(mComics.getId(), -1);
            }
        });
        //
        mReleaseListFragment = ((ReleaseListFragment)getSupportFragmentManager().findFragmentById(R.id.frg_release_list));
        mReleaseListFragment.setComics(mComics, ReleaseGroupHelper.MODE_COMICS);
        mReleaseListFragment.onDataChanged(DataManager.CAUSE_LOADING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //devo richiamare super per far gestire il risultato dal fragment
        super.onActivityResult(requestCode, resultCode, data);
        //
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ComicsEditorActivity.EDIT_COMICS_REQUEST) {
                updateHeader();
                mDataManager.notifyChanged(DataManager.CAUSE_COMICS_CHANGED);
            } else if (requestCode == ReleaseEditorActivity.EDIT_RELEASE_REQUEST) {
                mDataManager.updateBestRelease(mComics.getId());
                mDataManager.notifyChanged(DataManager.CAUSE_RELEASE_ADDED);
            }
        }
    }

    private void updateHeader() {
        mTxtName.setText(mComics.getName());
        mTxtPublisher.setText(Utils.nvl(mComics.getPublisher(), ""));
        mTxtNotes.setText(Utils.join("\n", true, mComics.getAuthors(), mComics.getNotes()));
    }

    private void showComicsEditor(Comics comics) {
        Intent intent = new Intent(this, ComicsEditorActivity.class);
        intent.putExtra(ComicsEditorActivity.EXTRA_COMICS_ID, comics.getId());
        startActivityForResult(intent, ComicsEditorActivity.EDIT_COMICS_REQUEST);
    }

    private void showReleaseEditor(long comicsId, int number) {
        Intent intent = new Intent(this, ReleaseEditorActivity.class);
        intent.putExtra(ReleaseEditorActivity.EXTRA_COMICS_ID, comicsId);
        intent.putExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, number);
        startActivityForResult(intent, ReleaseEditorActivity.EDIT_RELEASE_REQUEST);
    }

}
