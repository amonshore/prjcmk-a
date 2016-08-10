package it.amonshore.comikkua.ui.sync;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.Result;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.RequestCodes;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by narsenico on 10/08/16.
 */
public class SyncScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synccode_scanner);
        //Toolbar
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //
        final ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);
        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);
        //verifico che l'app abbia i permessi per la scrittura su disco (API >= 23)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //controllo se è già stato chiesto il permesso in precedenza e l'utente l'ha negato (true)
            if (this.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                //si richiedono spiegazioni, l'utente non ha dato il consenso in una precedente richiesta
                final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            requestPermissions();
                        } else {
                            finish();
                        }
                    }
                };
                final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setMessage(R.string.syncscanner_permissions_explanation)
                        .setPositiveButton(R.string.yes, onClickListener)
                        .setNegativeButton(R.string.no, onClickListener);
                builder.show();
            } else {
                //non si richiedono spiegazioni
                //oppure è stata selezionata "Don't ask again" in una precedente richiesta
                requestPermissions();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RequestCodes.CAMERA_PERMISSION_REQUEST) {
            //grantResults è vuota se la richiesta viene annullata dall'utente
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mScannerView.resumeCameraPreview(SyncScannerActivity.this);
            } else {
                //avviso l'utente che senza gli opportuni permessi non sarà possibile gestire i backup
                final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.syncscanner_permissions_need_title)
                        .setMessage(R.string.syncscanner_permissions_need_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                builder.show();
            }
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.CAMERA},
                RequestCodes.CAMERA_PERMISSION_REQUEST);
        //callback -> onRequestPermissionsResult
    }


    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        //TODO se viene letto un url valido è inutile ripristinare la camera

        Toast.makeText(this, "Contents = " + result.getText() +
                ", Format = " + result.getBarcodeFormat().toString(), Toast.LENGTH_SHORT).show();

        // Note:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(SyncScannerActivity.this);
            }
        }, 2000);
    }
}
