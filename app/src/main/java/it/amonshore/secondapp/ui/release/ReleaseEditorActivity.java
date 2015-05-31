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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.datetimepicker.date.DatePickerDialog;
import com.marvinlabs.widget.floatinglabel.edittext.FloatingLabelEditText;
import com.marvinlabs.widget.floatinglabel.instantpicker.FloatingLabelDatePicker;
import com.marvinlabs.widget.floatinglabel.instantpicker.FloatingLabelInstantPicker;
import com.marvinlabs.widget.floatinglabel.instantpicker.JavaDateInstant;

import java.util.GregorianCalendar;

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

    private FloatingLabelEditText mTxtNumber, mTxtPrice, mTxtNotes;
    private FloatingLabelDatePicker<JavaDateInstant> mTxtDate;
    private Switch mChkPurchased, mChkOrdered;

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
            mRelease = mComics.createRelease(true);
        } else {
            mIsNew = false;
            mRelease = mComics.getRelease(releaseNumber);
        }
        //
        //imposto i valori e creo i listener
        mTxtNumber = (FloatingLabelEditText)findViewById(R.id.txt_editor_release_number);
        mTxtNumber.setInputWidgetText(Integer.toString(mRelease.getNumber()));
        //
        //TODO come si fa ad annullare la data???
        mTxtDate = (FloatingLabelDatePicker<JavaDateInstant>)findViewById(R.id.txt_editor_release_date);
        final JavaDateInstant instant;
        if (mRelease.getDate() == null) {
            instant = new JavaDateInstant();
            //non impostare setSelectedInstant
        } else {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(mRelease.getDate());
            instant = new JavaDateInstant(calendar);
            mTxtDate.setSelectedInstant(instant);
        }
        mTxtDate.setWidgetListener(new FloatingLabelInstantPicker.OnWidgetEventListener() {
            @Override
            public void onShowInstantPickerDialog(FloatingLabelInstantPicker floatingLabelInstantPicker) {
                DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {
                        //
                        instant.setYear(year);
                        instant.setMonthOfYear(monthOfYear - 1);
                        instant.setDayOfMonth(dayOfMonth);
                        mTxtDate.setSelectedInstant(instant);
                    }
                }, instant.getYear(), instant.getMonthOfYear() + 1, instant.getDayOfMonth()).show(getFragmentManager(), "datePicker");
            }
        });
        //http://stackoverflow.com/questions/10666174/implement-onclick-only-for-a-textview-compound-drawable
        //al touch sull'icona alla destra della textview, pulisco la data
        //  l'icona è definita nel layout come drawableRight
        mTxtDate.getInputWidget().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //if (event.getRawX() <= ((TextView) v).getTotalPaddingLeft()) {
                    TextView tv = ((TextView) v);
                    if (event.getRawX() >= tv.getRight() - tv.getTotalPaddingRight()) {
                        mTxtDate.setSelectedInstant(null);
                        return true;
                    }
                }
                return false;
            }
        });
        //
        mTxtPrice = (FloatingLabelEditText)findViewById(R.id.txt_editor_release_price);
        mTxtPrice.setInputWidgetText(Double.toString(mRelease.getPrice()));
        //
        mTxtNotes = (FloatingLabelEditText)findViewById(R.id.txt_editor_release_notes);
        mTxtNotes.setInputWidgetText(mRelease.getNotes());
        //
        mChkPurchased = (Switch)findViewById(R.id.chk_editor_release_purchased);
        mChkPurchased.setChecked(mRelease.isPurchased());
        //
        mChkOrdered = (Switch)findViewById(R.id.chk_editor_release_ordered);
        mChkOrdered.setChecked(mRelease.isOrdered());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_release_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            if (validateAll()) {
                //TODO eseguire i controlli sui dati
                //preparo i dati per la risposta
                mRelease.setNumber(getViewInt(mTxtNumber.getInputWidget(),0));
                mRelease.setPrice(getViewDouble(mTxtPrice.getInputWidget()));
                mRelease.setNotes(getViewText(mTxtNotes.getInputWidget()));
                JavaDateInstant instant = mTxtDate.getSelectedInstant();
                if (instant == null) {
                    mRelease.setDate(null);
                } else {
                    GregorianCalendar calendar = new GregorianCalendar(instant.getYear(),
                            instant.getMonthOfYear() + 1, instant.getDayOfMonth());
                    mRelease.setDate(calendar.getTime());
                }
                mRelease.setPurchased(mChkPurchased.isChecked());
                mRelease.setOrdered(mChkOrdered.isChecked());

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
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String getViewText(EditText view) {
        return view.getText().toString().trim();
    }

    private int getViewInt(EditText view, int def) {
        try {
            return Integer.parseInt(view.getText().toString());
        } catch (NumberFormatException nfex) {
            return def;
        }
    }

    private double getViewDouble(EditText view) {
        try {
            return Double.parseDouble(view.getText().toString());
        } catch (NumberFormatException nfex) {
            return 0.0d;
        }
    }

    private boolean validateAll() {
        if (TextUtils.getTrimmedLength(mTxtNumber.getInputWidgetText()) == 0) {
            mTxtNumber.getInputWidget().setError(getString(R.string.editor_release_number_empty));
            return false;
        } else {
            int number = getViewInt(mTxtNumber.getInputWidget(), -1);
            if (number < 0) {
                mTxtNumber.getInputWidget().setError(getString(R.string.editor_release_number_negative));
                return false;
            }else if (mIsNew) {
                if (mComics.getRelease(number) == null) {
                    mTxtNumber.getInputWidget().setError(null);
                    return true;
                } else {
                    mTxtNumber.getInputWidget().setError(getString(R.string.editor_release_number_duplicate));
                    return false;
                }
            } else {
                //se non è nuovo, può essere uguale a quello attuale
                Release release = mComics.getRelease(number);
                if (release == null || release.getNumber() == mRelease.getNumber()) {
                    mTxtNumber.getInputWidget().setError(null);
                    return true;
                } else {
                    mTxtNumber.getInputWidget().setError(getString(R.string.editor_release_number_duplicate));
                    return false;
                }
            }
        }
    }

}
