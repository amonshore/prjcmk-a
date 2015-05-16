package it.amonshore.secondapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Arrays;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;
import it.amonshore.secondapp.data.DataManager;
import it.amonshore.secondapp.data.Utils;

public class ComicsEditorActivity extends ActionBarActivity {

    public final static int EDIT_COMICS_REQUEST = 1001;

    public final static String EXTRA_ENTRY = "entry";
    public final static String EXTRA_IS_NEW = "isnew";

    private Comics mComics;
    private boolean mIsNew;
    private DataManager mDataManager;
    private boolean bCanSave;
    private String[] mPeriodicityKeys;

    private EditText mTxtName, mTxtSeries, mTxtAuthors, mTxtPrice;
    private AutoCompleteTextView mTxtPublisher;
    private Spinner mSpPeriodicity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_editor2);
        //uso il contesto dell'applicazione, usato anche nell'Activity principale
        mDataManager = DataManager.getDataManager(getApplicationContext());
        //leggo i parametri
        Intent intent = getIntent();
        mComics = (Comics)intent.getSerializableExtra(EXTRA_ENTRY);
        mIsNew = intent.getBooleanExtra(EXTRA_IS_NEW, true);
        if (mIsNew) {
            setTitle(R.string.title_activity_comics_editor);
        } else {
            setTitle(mComics.getName());
        }
        //imposto i valori e creo i listener
        mTxtName = (EditText)findViewById(R.id.txt_editor_comics_name);
        mTxtName.setText(mComics.getName());
        mTxtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkComicsName();
            }
        });
        //
        mTxtPublisher = (AutoCompleteTextView)findViewById(R.id.txt_editor_comics_publisher);
        mTxtPublisher.setText(mComics.getPublisher());
        mTxtPublisher.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                mDataManager.getPublishers()));
        //
        mTxtSeries = (EditText)findViewById(R.id.txt_editor_comics_series);
        mTxtSeries.setText(mComics.getSeries());
        //
        mTxtAuthors = (EditText)findViewById(R.id.txt_editor_comics_authors);
        mTxtAuthors.setText(mComics.getAuthors());
        //
        mTxtPrice = (EditText)findViewById(R.id.txt_editor_comics_price);
        mTxtPrice.setText(Double.toString(mComics.getPrice()));
        //
        mSpPeriodicity = (Spinner)findViewById(R.id.txt_editor_comics_periodicity);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.periodicity_value_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpPeriodicity.setAdapter(adapter);
        mPeriodicityKeys = getResources().getStringArray(R.array.periodicity_key_array);
        mSpPeriodicity.setSelection(Utils.indexOf(mPeriodicityKeys, mComics.getPeriodicity(), 0));
        //
        checkComicsName();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            //TODO eseguire i controlli sui dati
            //preparo i dati per la risposta
            mComics.setName(getViewText(mTxtName));
            mComics.setPublisher(getViewText(mTxtPublisher));
            mComics.setSeries(getViewText(mTxtSeries));
            mComics.setAuthors(getViewText(mTxtAuthors));
            mComics.setPrice(getViewDouble(mTxtPrice));
            mComics.setPeriodicity(mPeriodicityKeys[mSpPeriodicity.getSelectedItemPosition()]);
            //
            Intent intent = new Intent();
            intent.putExtra(EXTRA_ENTRY, mComics);
            intent.putExtra(EXTRA_IS_NEW, mIsNew);
            setResult(Activity.RESULT_OK, intent);
            finish();

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
        if (TextUtils.getTrimmedLength(mTxtName.getText()) == 0) {
            mTxtName.setError(getString(R.string.editor_comics_name_empty));
            bCanSave = false;
        } else {
            mTxtName.setError(null);
            bCanSave = true;
        }
        invalidateOptionsMenu();
    }
}
