package it.amonshore.comikkua.ui.comics;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.isseiaoki.simplecropview.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;

public class ComicsCropActivity extends ActionBarActivity {

    public final static String EXTRA_COMICS_ID = "comicsId";
    public final static String EXTRA_IMAGE_URI = "imageUri";
    public final static String EXTRA_TEMP_FILE = "tempFile";

    private CropImageView mCropImageView;
    private ProgressBar mProgressBar;
    private Comics mComics;
    private String mTempFilePrefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_crop);
        //
        final Intent intent = getIntent();
        final long comicsId = intent.getLongExtra(EXTRA_COMICS_ID, 0);
        final Uri imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI));
        //occorre rendere univoco il nome del file croppato altrimenti verrà presentato sempre quello cachato
        mTempFilePrefix = UUID.randomUUID().toString();
        mComics = DataManager.getDataManager().getComics(comicsId);
        setTitle(mComics.getName());
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        //l'immagine deve avere una ratio di 10:4
        mCropImageView = (CropImageView)findViewById(R.id.cropImageView);
        mCropImageView.setCustomRatio(10, 4);
        //
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        //imposto l'immagine alla vista
        loadImageIntoView(imageUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_comics_crop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //
        if (id == android.R.id.home) {
            //intercetto il back
            //termino l'activity in modo che torni all'activity chiamante
            //  e non al padre indicato nel manifest
            finish();
            return true;
        } else if (id == R.id.action_save) {
            //salvo l'immagine in un file temporaneo
            FileOutputStream outputStream = null;
            try {
                //File tempFile = File.createTempFile("cropped", ".jpg");
                File tempFile = new File(getCacheDir(), mTempFilePrefix + ".jpg");
                outputStream = new FileOutputStream(tempFile);
                mCropImageView.getCroppedBitmap().compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                //
                Intent data = new Intent();
                data.putExtra(EXTRA_TEMP_FILE, tempFile.toString());
                setResult(Activity.RESULT_OK, data);
            } catch (IOException e) {
                e.printStackTrace();
                setResult(Activity.RESULT_CANCELED);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    private String getUriLastPart(Uri uri) {
//        int pp = uri.getPath().lastIndexOf('/');
//        if (pp >= 0) {
//            return uri.getPath().substring(pp);
//        } else {
//            return "" + System.currentTimeMillis();
//        }
//    }

    private void loadImageIntoView(Uri imageUri) {
        //nascondo la progress bar al termine del caricamento o in caso di errore
        Glide.with(this).load(imageUri).asBitmap().listener(new RequestListener<Uri, Bitmap>() {
            @Override
            public boolean onException(Exception e, Uri model, Target<Bitmap> target, boolean isFirstResource) {
                mProgressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Uri model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mProgressBar.setVisibility(View.GONE);
                return false;
            }
        }).into(mCropImageView);
    }

}
