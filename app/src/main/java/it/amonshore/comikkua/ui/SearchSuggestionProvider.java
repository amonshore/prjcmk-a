package it.amonshore.comikkua.ui;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by narsenico on 27/02/16.
 */
public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "it.amonshore.comikkua.ui.SearchSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;

    public SearchSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

//    @Override
//    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
//        return super.query(uri, projection, selection, selectionArgs, sortOrder);
//    }
}
