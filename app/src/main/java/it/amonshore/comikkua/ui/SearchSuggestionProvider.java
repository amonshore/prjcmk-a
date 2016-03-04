package it.amonshore.comikkua.ui;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;

/**
 * Created by narsenico on 27/02/16.
 */
public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "it.amonshore.comikkua.ui.SearchSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;

    public SearchSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        final Cursor suggestionCursor = super.query(uri, projection, selection, selectionArgs, sortOrder);
        final MatrixCursor topCursor = new MatrixCursor(suggestionCursor.getColumnNames(), 1);
        final ContentValues values = new ContentValues();
        values.put(SearchManager.SUGGEST_COLUMN_TEXT_1, "Search remote");
        values.put(SearchManager.SUGGEST_COLUMN_TEXT_2, selectionArgs[0]);
        values.put(SearchManager.SUGGEST_COLUMN_ICON_1, Uri.parse("android.resource://it.amonshore.comikkua/drawable/ic_web").toString());
        addRow(topCursor, values);

        return new MergeCursor(new Cursor[] { topCursor, suggestionCursor });
    }

    private void addRow(MatrixCursor cursor, ContentValues values) {
        String[] columns = cursor.getColumnNames();
        Object[] objects = new Object[columns.length];
        for (int ii=0; ii<columns.length; ii++) {
            objects[ii] = values.get(columns[ii]);
        }
        cursor.addRow(objects);
    }
}
