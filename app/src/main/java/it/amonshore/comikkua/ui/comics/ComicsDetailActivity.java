package it.amonshore.comikkua.ui.comics;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.RequestCodes;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.FileHelper;
import it.amonshore.comikkua.data.ReleaseGroupHelper;
import it.amonshore.comikkua.ui.release.ReleaseEditorActivity;
import it.amonshore.comikkua.ui.release.ReleaseListFragment;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;
import jp.wasabeef.glide.transformations.GrayscaleTransformation;

/**
 * Created by Narsenico on 20/05/2015.
 */
public class ComicsDetailActivity extends ActionBarActivity {

    public final static String EXTRA_COMICS_ID = "comicsId";

    private Comics mComics;
    private DataManager mDataManager;
    private TextView mTxtName, mTxtAuthors, mTxtNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_detail2);
        //TODO cambiare il colore della status bar in base al colore primario dell'immagine dell'header
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(Color.parseColor("#02B1CE"));
//        }
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager();
        //leggo i parametri
        Intent intent = getIntent();
        //presumo che l'id sia valido
        mComics = mDataManager.getComics(intent.getLongExtra(EXTRA_COMICS_ID, 0));
        //Toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        //tolgo il titolo dell'activity
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //
        mTxtName = ((TextView)findViewById(R.id.txt_detail_comics_name));
        mTxtAuthors = ((TextView)findViewById(R.id.txt_detail_comics_authors));
        mTxtNotes = ((TextView)findViewById(R.id.txt_detail_comics_notes));
        updateHeader();
        //listener fab
        findViewById(R.id.fab_release_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseEditor(mComics.getId());
            }
        });
        //
        ReleaseListFragment mReleaseListFragment = ((ReleaseListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.frg_release_list));
        mReleaseListFragment.setComics(mComics, ReleaseGroupHelper.MODE_COMICS);
        mReleaseListFragment.onDataChanged(DataManager.CAUSE_LOADING);
        //A0024 gestisco il click sull'immagine per poterne scegliere una dalla libreria
        findViewById(R.id.imageView).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showImageSelector();
                return true;
            }
        });
        //A0024 carico l'immagine del comics (se esiste)
        if (!Utils.isNullOrEmpty(mComics.getImage())) {
            loadComicsImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //devo richiamare super per far gestire il risultato dal fragment
        super.onActivityResult(requestCode, resultCode, data);
        //
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ComicsEditorActivity.EDIT_COMICS_REQUEST) {
                updateHeader();
                //A0049
                mDataManager.updateData(DataManager.ACTION_UPD, mComics.getId(), DataManager.NO_RELEASE);
                mDataManager.notifyChanged(DataManager.CAUSE_COMICS_CHANGED);
            } else if (requestCode == RequestCodes.EDIT_RELEASE_REQUEST) {
                mDataManager.updateBestRelease(mComics.getId());
                mDataManager.notifyChanged(DataManager.CAUSE_RELEASE_ADDED);
                //A0049
                int releaseNumber = data.getIntExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, DataManager.NO_RELEASE);
                mDataManager.updateData(DataManager.ACTION_ADD, mComics.getId(), releaseNumber);
            } else if (requestCode == RequestCodes.LOAD_IMAGES) {
                //A0024 TODO per le versioni di android inferiori a KitKat non va bene come recupero il path del file dall'uri
                Uri imageUri = data.getData();
                File imageFile = FileHelper.getFile(this, imageUri);
                if (imageFile != null) {
                    File destFile;
                    //se esite già un file lo cancello
                    if (!Utils.isNullOrEmpty(mComics.getImage())) {
                        destFile = FileHelper.getExternalFile(this, mComics.getImage());
                        if (destFile.exists()) {
                            destFile.delete();
                        }
                    }
                    //genero un nuovo nome di file
                    String destFileName = UUID.randomUUID().toString();
                    mComics.setImage(destFileName);
                    mDataManager.updateData(DataManager.ACTION_UPD, mComics.getId(), DataManager.NO_RELEASE);
                    destFile = FileHelper.getExternalFile(this, destFileName);
                    //copio il file nella cartella dell'app
                    try {
                        boolean res = FileHelper.copyFile(imageFile, destFile);
                        Utils.d(this.getClass(), "A0024 to " + imageFile + " to " + destFile + " -> " + res);
                        if (res) {
                            loadComicsImage();
                        }
                    } catch (IOException ioex) {
                        Utils.e(this.getClass(), "A0024", ioex);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comics_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //
        if (id == R.id.action_comics_edit) {
            showComicsEditor(mComics);
            return true;
        } else if (id == R.id.action_comics_share) {
            //A0034
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, Utils.join("\n", true,
                    mComics.getName(),
                    mComics.getAuthors(),
                    mComics.getPublisher(),
                    mComics.getNotes()));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share)));
            return true;
        } else if (id == R.id.action_comics_search) {
            //A0042
            String query = Utils.join(" ", true, mComics.getName(), mComics.getAuthors(),
                    mComics.getPublisher());
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateHeader() {
        mTxtName.setText(mComics.getName());
        mTxtAuthors.setText(Utils.join(" - ", true, mComics.getPublisher(), mComics.getAuthors()));
        mTxtNotes.setText(Utils.nvl(mComics.getNotes(), ""));
        //TODO impostare icona "reserved" alla destra delle note
//        if (mComics.isReserved()) { ... }
    }

    private void showComicsEditor(Comics comics) {
        Intent intent = new Intent(this, ComicsEditorActivity.class);
        intent.putExtra(ComicsEditorActivity.EXTRA_COMICS_ID, comics.getId());
        startActivityForResult(intent, ComicsEditorActivity.EDIT_COMICS_REQUEST);
    }

    private void showReleaseEditor(long comicsId) {
        Intent intent = new Intent(this, ReleaseEditorActivity.class);
        intent.putExtra(ReleaseEditorActivity.EXTRA_COMICS_ID, comicsId);
        intent.putExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, ReleaseEditorActivity.RELEASE_NEW);
        startActivityForResult(intent, RequestCodes.EDIT_RELEASE_REQUEST);
    }

    private void showImageSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Choose Picture"), RequestCodes.LOAD_IMAGES);
    }

    private void loadComicsImage() {
        //A0024 NB: se l'uri dell'immagine è la stessa l'immagine non viene modificata
        final Context context = this;
        final File imageFile = FileHelper.getExternalFile(this, mComics.getImage());
        final Uri backgroundUri;
        if (imageFile.exists() && imageFile.canRead()) {
            backgroundUri = Uri.fromFile(imageFile);
        } else {
            backgroundUri = Uri.parse("android.resource://it.amonshore.comikkua/" + R.drawable.bck_detail);
        }
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        new AsyncTask<Uri, Void, DrawableRequestBuilder<Uri>>() {
            @Override
            protected DrawableRequestBuilder<Uri> doInBackground(Uri... params) {
                return
                Glide.with(context).load(params[0])
                        .bitmapTransform(
                                new CenterCrop(context),
                                new GrayscaleTransformation(context),
//                        new BlurTransformation(this, 12, 2),
                                new ColorFilterTransformation(context, Color.parseColor("#AA1976D2"))
                        );
            }

            @Override
            protected void onPostExecute(DrawableRequestBuilder<Uri> integerDrawableRequestBuilder) {
                integerDrawableRequestBuilder.into(imageView);
            }
        }.execute(backgroundUri);
    }
}
