package it.amonshore.comikkua.ui.comics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
    private ImageView mImageView;

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
        // gestito direttamente nel fragment
//        //listener fab
//        findViewById(R.id.fab_release_add).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showReleaseEditor(mComics.getId());
//            }
//        });
        //
        ReleaseListFragment mReleaseListFragment = ((ReleaseListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.frg_release_list));
        mReleaseListFragment.setComics(mComics, ReleaseGroupHelper.MODE_COMICS);
        mReleaseListFragment.onDataChanged(DataManager.CAUSE_LOADING);
        //A0024 gestisco il click sull'immagine per poterne scegliere una dalla libreria

        mImageView = (ImageView)findViewById(R.id.imageView);
        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!Utils.isNullOrEmpty(mComics.getImage())) {
                    showComicsImageDialog();
                } else {
                    showComicsImageSelector();
                }
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
            if (requestCode == RequestCodes.EDIT_COMICS_REQUEST) {
                updateHeader();
                //A0056
                //A0049
//                mDataManager.updateData(DataManager.ACTION_UPD, mComics.getId(), DataManager.NO_RELEASE);
                mDataManager.notifyChanged(DataManager.CAUSE_COMICS_CHANGED);
            } else if (requestCode == RequestCodes.EDIT_RELEASE_REQUEST) {
                mDataManager.updateBestRelease(mComics.getId());
                mDataManager.notifyChanged(DataManager.CAUSE_RELEASE_ADDED);
                //A0056
//                //A0049
//                int releaseNumber = data.getIntExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, DataManager.NO_RELEASE);
//                mDataManager.updateData(DataManager.ACTION_ADD, mComics.getId(), releaseNumber);
            } else if (requestCode == RequestCodes.LOAD_IMAGE) {
                //A0024
                //elimino il file precedente
                Uri imageUri = data.getData();
                //A0054 crop immagine
                Intent cropIntenet = new Intent(this, ComicsCropActivity.class);
                cropIntenet.putExtra(ComicsCropActivity.EXTRA_COMICS_ID, mComics.getId());
                cropIntenet.putExtra(ComicsCropActivity.EXTRA_IMAGE_URI, imageUri.toString());
                startActivityForResult(cropIntenet, RequestCodes.CROP_IMAGE);
            } else if (requestCode == RequestCodes.CROP_IMAGE) {
                //A0054 ok cropped bitmap: aggiorno header
                //  in caso di selezione del medesimo file presentare il rettangolo di crop con le coordinate salvate
                String destFileName = Comics.getDefaultImageFileName(mComics.getId());
                mComics.setImage(destFileName);
                mDataManager.updateData(DataManager.ACTION_UPD, mComics.getId(), DataManager.NO_RELEASE);
                //applica i filtri all'immagine, la salva e quindi la carica a video
                createComicsImageFromFile(data.getStringExtra(ComicsCropActivity.EXTRA_TEMP_FILE), destFileName);
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
        startActivityForResult(intent, RequestCodes.EDIT_COMICS_REQUEST);
    }

    // gestito direttamente nel fragment
//    private void showReleaseEditor(long comicsId) {
//        Intent intent = new Intent(this, ReleaseEditorActivity.class);
//        intent.putExtra(ReleaseEditorActivity.EXTRA_COMICS_ID, comicsId);
//        intent.putExtra(ReleaseEditorActivity.EXTRA_RELEASE_NUMBER, ReleaseEditorActivity.RELEASE_NEW);
//        startActivityForResult(intent, RequestCodes.EDIT_RELEASE_REQUEST);
//    }

    private void showComicsImageSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.comics_detail_choose_image)), RequestCodes.LOAD_IMAGE);
    }

    private void createComicsImageFromFile(String inputFileName, String outputFileName) {
        final File inputFile = new File(inputFileName);
        final File outputFile = FileHelper.getExternalFile(this, outputFileName);

        //A0024 cambia il colore dell'immagine (scala di grigio + filtro colore) e la salva compressa (jpg)
        Glide.with(this).load(Uri.fromFile(inputFile))
                .asBitmap()
                .transform(
                        new CenterCrop(this),
                        new GrayscaleTransformation(this),
                        new ColorFilterTransformation(this, getResources().getColor(R.color.comikku_comics_image_color))
                )
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        FileOutputStream outputStream = null;
                        try {
                            outputStream = new FileOutputStream(outputFile);
                            resource.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                            //
                            loadComicsImage(outputFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            //elimino il file di input
                            if (!inputFile.delete()) {
                                Utils.w(ComicsDetailActivity.class, "Cannot delete " + inputFile.getPath());
                            }
                        }
                    }
                });

    }

    private void loadComicsImage(File file) {
        if (file == null) {
            mImageView.setImageResource(R.drawable.bck_detail_lightblue);
        } else if (file.exists()) {
            //se è stato caricato in precedenza un file con lo stesso, l'imageView non carica quello nuovo
            //  perché l'Uri non è cambiato, è necessario quindi passre null per forzare il refresh
            mImageView.setImageDrawable(null);
            mImageView.setImageURI(Uri.fromFile(file));
        }
    }

    private void removeComicsImage() {
        String fileName = mComics.getImage();
        if (!Utils.isNullOrEmpty(fileName)) {
            loadComicsImage(null);
            //
            mComics.setImage(null);
            mDataManager.updateData(DataManager.ACTION_UPD, mComics.getId(), DataManager.NO_RELEASE);
            //
            File file = FileHelper.getExternalFile(this, fileName);
            if (!file.exists() || !file.delete()) {
                Utils.w(this.getClass(), "Cannot delete " + file.getPath());
            }
        }
    }

    private void showComicsImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(R.array.comics_detail_image_choose,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showComicsImageSelector();
                        } else {
                            removeComicsImage();
                        }
                    }
                });
        builder.create().show();
    }

}
