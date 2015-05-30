package it.amonshore.secondapp.ui.release;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.marvinlabs.widget.floatinglabel.edittext.FloatingLabelEditText;
import com.marvinlabs.widget.floatinglabel.instantpicker.FloatingLabelDatePicker;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Release;
import it.amonshore.secondapp.ui.SimpleTextWatcher;

public class ReleaseEditorActivity extends ActionBarActivity {

    public final static int EDIT_RELEASE_REQUEST = 2001;

    public final static String EXTRA_COMICS_ID = "comicsId";
    public final static String EXTRA_RELEASE_NUMBER = "releaseNumber";
    public final static long COMICS_ID_NONE = 0;
    public final static int RELEASE_NEW = -1;

    private Comics mComics;
    private Release mRelease;
    private boolean mIsNew;
    private DataManager mDataManager;
    private boolean bCanSave;

    private FloatingLabelEditText mTxtNumber, mTxtPrice, mTxtNotes;
    private FloatingLabelDatePicker mTxtDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_editor);
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager(getApplicationContext());
        //leggo i parametri
        Intent intent = getIntent();
        long comicsId = intent.getLongExtra(EXTRA_COMICS_ID, COMICS_ID_NONE);
        int releaseNumber = intent.getIntExtra(EXTRA_RELEASE_NUMBER, RELEASE_NEW);
        mComics = mDataManager.getComics(comicsId);
        setTitle(mComics.getName());
        if (releaseNumber == RELEASE_NEW) {
            mIsNew = true;
            //TODO imposta in automatico i valori di number e date
            mRelease = mComics.createRelease();
        } else {
            mIsNew = false;
            mRelease = mComics.getRelease(releaseNumber);
        }
        //
        //imposto i valori e creo i listener
        mTxtNumber = (FloatingLabelEditText)findViewById(R.id.txt_editor_release_number);
        mTxtNumber.setInputWidgetText(Integer.toString(mRelease.getNumber()));
        mTxtNumber.addInputWidgetTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkReleaseNumber();
            }
        });
        //
        //TODO mTxtDate
        //
        mTxtPrice = (FloatingLabelEditText)findViewById(R.id.txt_editor_release_price);
        mTxtPrice.setInputWidgetText(Double.toString(mRelease.getPrice()));
        //
        mTxtNotes = (FloatingLabelEditText)findViewById(R.id.txt_editor_release_notes);
        mTxtNotes.setInputWidgetText(mRelease.getNotes());
        //
        checkReleaseNumber();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_release_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_save).setEnabled(bCanSave);
        return true;
    }

    private AlertDialog mAlertDialog;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            if (isReleaseNumberValid()) {
                //TODO eseguire i controlli sui dati
                //preparo i dati per la risposta
                mRelease.setNumber(getViewInt(mTxtNumber.getInputWidget()));
                //TODO set date
                mComics.setNotes(getViewText(mTxtNotes.getInputWidget()));

                if (mIsNew) {
                    if (!mComics.putRelease(mRelease)) {
                        Utils.w("Release editor: release wasn't new");
                    }
                }

                //
                Intent intent = new Intent();
                intent.putExtra(EXTRA_RELEASE_NUMBER, mRelease.getNumber());
                setResult(Activity.RESULT_OK, intent);
                finish();
            } else {
                if (mAlertDialog == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setMessage(R.string.editor_release_number_duplicate);
                    mAlertDialog = builder.create();
                }
                mAlertDialog.show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String getViewText(EditText view) {
        return view.getText().toString().trim();
    }

    private int getViewInt(EditText view) {
        try {
            return Integer.parseInt(view.getText().toString());
        } catch (NumberFormatException nfex) {
            return 0;
        }
    }

    private double getViewDouble(EditText view) {
        try {
            return Double.parseDouble(view.getText().toString());
        } catch (NumberFormatException nfex) {
            return 0.0d;
        }
    }

    private void checkReleaseNumber() {
        //TODO rivedere i controlli, soprattutto unique che sempra lento
        if (TextUtils.getTrimmedLength(mTxtNumber.getInputWidgetText()) == 0) {
            mTxtNumber.getInputWidget().setError(getString(R.string.editor_release_number_empty));
            bCanSave = false;
        } else {
            mTxtNumber.getInputWidget().setError(null);
            bCanSave = true;
        }

        invalidateOptionsMenu();
    }

    private boolean isReleaseNumberValid() {
        if (mIsNew) {
            return (mComics.getRelease(getViewInt(mTxtNumber.getInputWidget())) == null);
        } else {
            //se non è nuovo, può essere uguale a quello attuale
            Release release = mComics.getRelease(getViewInt(mTxtNumber.getInputWidget()));
            return (release == null || release.getNumber() == mRelease.getNumber());
        }
    }

}
