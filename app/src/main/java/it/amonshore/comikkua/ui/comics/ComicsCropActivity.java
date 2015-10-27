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

import com.bumptech.glide.Glide;
import com.isseiaoki.simplecropview.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.DataManager;

public class ComicsCropActivity extends ActionBarActivity {

    public final static String EXTRA_COMICS_ID = "comicsId";
    public final static String EXTRA_IMAGE_URI = "imageUri";
    public final static String EXTRA_TEMP_URI = "tempUri";

    private CropImageView mCropImageView;
    private Comics mComics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_crop);
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        //
        final Intent intent = getIntent();
        final long comicsId = intent.getLongExtra(EXTRA_COMICS_ID, 0);
        final Uri imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI));
        mComics = DataManager.getDataManager().getComics(comicsId);
        setTitle(mComics.getName());
        //l'immagine deve avere una ratio di 10:4
        mCropImageView = (CropImageView)findViewById(R.id.cropImageView);
        mCropImageView.setCustomRatio(10, 4);
        //imposto l'immagine alla vista
        Glide.with(this).load(imageUri).asBitmap().into(mCropImageView);
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
        if (id == R.id.action_save) {

            //salvo l'immagine in un file temporaneo
            FileOutputStream outputStream = null;
            try {
                File tempFile = File.createTempFile("cropped", ".jpg");
                outputStream = new FileOutputStream(tempFile);
                mCropImageView.getCroppedBitmap().compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                //
                Intent data = new Intent();
                data.putExtra(EXTRA_TEMP_URI, Uri.fromFile(tempFile).toString());
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

}
