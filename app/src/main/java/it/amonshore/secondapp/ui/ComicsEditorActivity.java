package it.amonshore.secondapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;

public class ComicsEditorActivity extends ActionBarActivity {

    public final static int EDIT_COMICS_REQUEST = 1001;

    public final static String EXTRA_ENTRY = "entry";
    public final static String EXTRA_IS_NEW = "isnew";

    private Comics mComics;
    private boolean mIsNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics_editor);
        //leggo i parametri
        Intent intent = getIntent();
        mComics = (Comics)intent.getSerializableExtra(EXTRA_ENTRY);
        mIsNew = intent.getBooleanExtra(EXTRA_IS_NEW, true);
        //
        TextView txtName = (TextView)findViewById(R.id.txtEditorComicsName);
        txtName.setText(mComics.getName());
        txtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //TODO validazione dati
                //...TextView.setError
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comics_editor, menu);
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
            Intent intent = new Intent();
            intent.putExtra(EXTRA_ENTRY, mComics);
            intent.putExtra(EXTRA_IS_NEW, mIsNew);
            setResult(Activity.RESULT_OK, intent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
