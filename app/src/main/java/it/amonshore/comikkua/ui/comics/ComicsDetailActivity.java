package it.amonshore.comikkua.ui.comics;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
            loadComicsImage(FileHelper.getExternalFile(this, mComics.getImage()));
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
                //A0024
                Uri imageUri = data.getData();
                String destFileName = Comics.getDefaultImageFileName(mComics);
                if (Utils.isNullOrEmpty(mComics.getImage())) {
                    mComics.setImage(destFileName);
                    mDataManager.updateData(DataManager.ACTION_UPD, mComics.getId(), DataManager.NO_RELEASE);
                }
                //applica i filtri all'immagine, la salva e quindi la carica a video
                createComicsImageFromUri(imageUri, FileHelper.getExternalFile(this, destFileName));
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

    private void createComicsImageFromUri(Uri uri, final File file) {
        //A0024 cambia il colore dell'immagine (scala di grigio + filtro colore) e la salva compressa (jpg)
        final Context context = this;
        new AsyncTask<Uri, Void, BitmapRequestBuilder<Uri, Bitmap>>() {
            @Override
            protected BitmapRequestBuilder<Uri, Bitmap> doInBackground(Uri... params) {
                return
                Glide.with(context).load(params[0])
                        .asBitmap()
                        .transform(
//                                new CenterCrop(context),
                                new GrayscaleTransformation(context),
//                        new BlurTransformation(this, 12, 2),
                                new ColorFilterTransformation(context, getResources().getColor(R.color.comikku_comics_image_color))
                        );
            }

            @Override
            protected void onPostExecute(BitmapRequestBuilder<Uri, Bitmap> bitmapRequestBuilder) {
                //integerDrawableRequestBuilder.into(imageView);
                bitmapRequestBuilder.into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        try {
                            OutputStream outputStream = new FileOutputStream(file);
                            resource.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                            outputStream.close();
                            loadComicsImage(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.execute(uri);
    }

    private void loadComicsImage(File file) {
        if (file.exists()) {
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            //se è stato caricato in precedenza un file con lo stesso, l'imageView non carica quello nuovo
            //  perché l'Uri non è cambiato, è necessario quindi passre null per forzare il refresh
            imageView.setImageDrawable(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(Uri.fromFile(file));
        }
    }
}
