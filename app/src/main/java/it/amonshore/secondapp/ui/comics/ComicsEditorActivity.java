package it.amonshore.secondapp.ui.comics;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.marvinlabs.widget.floatinglabel.autocomplete.FloatingLabelAutoCompleteTextView;
import com.marvinlabs.widget.floatinglabel.edittext.FloatingLabelEditText;
import com.marvinlabs.widget.floatinglabel.itempicker.FloatingLabelItemPicker;
import com.marvinlabs.widget.floatinglabel.itempicker.ItemPickerListener;
import com.marvinlabs.widget.floatinglabel.itempicker.StringPickerDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.Utils;
import it.amonshore.secondapp.ui.SimpleTextWatcher;

public class ComicsEditorActivity extends ActionBarActivity implements ItemPickerListener<String> {

    public final static int EDIT_COMICS_REQUEST = 1001;

    public final static String EXTRA_COMICS_ID = "entry";
    public final static long COMICS_ID_NEW = 0;

    private Comics mComics;
    private boolean mIsNew;
    private DataManager mDataManager;
    private boolean bCanSave;
    private String[] mPeriodicityKeys;

    private FloatingLabelEditText mTxtName, mTxtSeries, mTxtAuthors, mTxtPrice;
    private FloatingLabelAutoCompleteTextView mTxtPublisher;
    private FloatingLabelItemPicker<String> mSpPeriodicity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_editor2);
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager(getApplicationContext());
        //leggo i parametri
        Intent intent = getIntent();
        long comicsId = intent.getLongExtra(EXTRA_COMICS_ID, COMICS_ID_NEW);
        if (comicsId == COMICS_ID_NEW) {
            mIsNew = true;
            mComics = new Comics(mDataManager.getSafeNewComicsId());
            setTitle(R.string.title_activity_comics_editor);
        } else {
            mIsNew = false;
            mComics = mDataManager.getComics(comicsId);
            setTitle(mComics.getName());
        }
        //
        mPeriodicityKeys = new String[] { Comics.PERIODICITY_UNKNOWN, Comics.PERIODICITY_WEEKLY,
                Comics.PERIODICITY_MONTHLY, Comics.PERIODICITY_MONTHLY_X2, Comics.PERIODICITY_MONTHLY_X3,
                Comics.PERIODICITY_MONTHLY_X4, Comics.PERIODICITY_MONTHLY_X6, Comics.PERIODICITY_YEARLY };
        //imposto i valori e creo i listener
        mTxtName = (FloatingLabelEditText)findViewById(R.id.txt_editor_comics_name);
        mTxtName.setInputWidgetText(mComics.getName());
        mTxtName.addInputWidgetTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkComicsName();
            }
        });
        //
        mTxtPublisher = (FloatingLabelAutoCompleteTextView)findViewById(R.id.txt_editor_comics_publisher);
        mTxtPublisher.setInputWidgetText(mComics.getPublisher());
        mTxtPublisher.setInputWidgetAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                mDataManager.getPublishers()));
        //
        mTxtSeries = (FloatingLabelEditText)findViewById(R.id.txt_editor_comics_series);
        mTxtSeries.setInputWidgetText(mComics.getSeries());
        //
        mTxtAuthors = (FloatingLabelEditText)findViewById(R.id.txt_editor_comics_authors);
        mTxtAuthors.setInputWidgetText(mComics.getAuthors());
        //
        mTxtPrice = (FloatingLabelEditText)findViewById(R.id.txt_editor_comics_price);
        mTxtPrice.setInputWidgetText(Double.toString(mComics.getPrice()));
        //
        mSpPeriodicity = (FloatingLabelItemPicker<String>)findViewById(R.id.txt_editor_comics_periodicity);
        mSpPeriodicity.setAvailableItems(Arrays.asList(getResources().getStringArray(R.array.periodicity_value_array)));
        mSpPeriodicity.setWidgetListener(new FloatingLabelItemPicker.OnWidgetEventListener<String>() {
            @Override
            public void onShowItemPickerDialog(FloatingLabelItemPicker<String> source) {
                // We use fragments because we'll be safe in edge cases like screen orientation
                // change. You could use a simple AlertDialog but really, no, you don't want to.
                StringPickerDialogFragment itemPicker = StringPickerDialogFragment.newInstance(
                        source.getId(),
                        getString(R.string.editor_comics_periodicity),
                        getString(android.R.string.ok),
                        getString(android.R.string.cancel),
                        false,
                        source.getSelectedIndices(),
                        new ArrayList<String>((Collection<String>) source.getAvailableItems()));

                // Optionally, you can set a target fragment to get the notifications
                // pickerFragment.setTargetFragment(MyFragment.this, 0);

                itemPicker.show(getSupportFragmentManager(), "ItemPicker");
            }
        });
        mSpPeriodicity.setSelectedIndices(new int[] { Utils.indexOf(mPeriodicityKeys, mComics.getPeriodicity(), 0) });
        //
        checkComicsName();
    }

    @Override
    public void onCancelled(int i) {
        //chiamato da FloatingLabelItemPicker
    }

    @Override
    public void onItemsSelected(int i, int[] ints) {
        //chiamato da FloatingLabelItemPicker
        mSpPeriodicity.setSelectedIndices(ints);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comics_editor, menu);
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
            if (isComicsNameValid()) {
                //TODO eseguire i controlli sui dati
                //preparo i dati per la risposta
                mComics.setName(getViewText(mTxtName.getInputWidget()));
                mComics.setPublisher(getViewText(mTxtPublisher.getInputWidget()));
                mComics.setSeries(getViewText(mTxtSeries.getInputWidget()));
                mComics.setAuthors(getViewText(mTxtAuthors.getInputWidget()));
                mComics.setPrice(getViewDouble(mTxtPrice.getInputWidget()));

                int[] selPer = mSpPeriodicity.getSelectedIndices();
                if (selPer != null && selPer.length > 0) {
                    mComics.setPeriodicity(mPeriodicityKeys[selPer[0]]);
                }

                if (mIsNew) {
                    if (!mDataManager.put(mComics)) {
                        Utils.w("Comics editor: comics wasn't new");
                    }
                }

                //
                Intent intent = new Intent();
                intent.putExtra(EXTRA_COMICS_ID, mComics.getId());
                setResult(Activity.RESULT_OK, intent);
                finish();
            } else {
                if (mAlertDialog == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setMessage(R.string.editor_comics_name_duplicate);
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

    private double getViewDouble(EditText view) {
        try {
            return Double.parseDouble(view.getText().toString());
        } catch (NumberFormatException nfex) {
            return 0.0d;
        }
    }

    private void checkComicsName() {
        //TODO rivedere i controlli, soprattutto unique che sempra lento
        if (TextUtils.getTrimmedLength(mTxtName.getInputWidgetText()) == 0) {
            mTxtName.getInputWidget().setError(getString(R.string.editor_comics_name_empty));
            bCanSave = false;
        } else {
            mTxtName.getInputWidget().setError(null);
            bCanSave = true;
        }

        invalidateOptionsMenu();
    }

    private boolean isComicsNameValid() {
        if (mIsNew) {
            return (mDataManager.getComicsByName(mTxtName.getInputWidgetText().toString()) == null);
        } else {
            //se non è nuovo, può essere uguale a quello attuale
            Comics comics = mDataManager.getComicsByName(mTxtName.getInputWidgetText().toString());
            return (comics == null || comics.getId() == mComics.getId());
        }
    }

}
