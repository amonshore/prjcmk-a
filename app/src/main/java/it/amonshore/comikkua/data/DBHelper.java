package it.amonshore.comikkua.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import it.amonshore.comikkua.Utils;

/**
 * A0049
 *
 * Created by Narsenico on 04/10/2015.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "comikkua.db";

    public static final class ComicsTable {
        public static final String NAME = "tComics";
        public static final String COL_ID = "_id";
        public static final String COL_USER = "user";
        public static final String COL_NAME = "name";
        public static final String COL_SERIES = "series";
        public static final String COL_PUBLISHER = "publisher";
        public static final String COL_AUTHORS = "authors";
        public static final String COL_PRICE = "price";
        public static final String COL_PERIODICITY = "periodicity";
        public static final String COL_RESERVED = "reserved";
        public static final String COL_NOTES = "notes";

        public static final int IDX_ID = 0;
        public static final int IDX_USER = 1;
        public static final int IDX_NAME = 2;
        public static final int IDX_SERIES = 3;
        public static final int IDX_PUBLISHER = 4;
        public static final int IDX_AUTHORS = 5;
        public static final int IDX_PRICE = 6;
        public static final int IDX_PERIODICITY = 7;
        public static final int IDX_RESERVED = 8;
        public static final int IDX_NOTES = 9;

        public static final String[] COLUMNS = {COL_ID, COL_USER, COL_NAME,
                COL_SERIES, COL_PUBLISHER, COL_AUTHORS, COL_PRICE,
                COL_PERIODICITY, COL_RESERVED, COL_NOTES};

        public static final String SQL_CREATE = "create table " + NAME +
                " (_id INTEGER NOT NULL, " +
                "user TEXT NOT NULL, name TEXT NOT NULL, series TEXT, publisher TEXT, authors TEXT, " +
                "price REAL, periodicity TEXT, reserved TEXT, notes TEXT, PRIMARY KEY(_id, user))";

        public static ContentValues getContentValues(Comics comics, String user) {
            ContentValues cv = new ContentValues();
            cv.put(COL_ID, comics.getId());
            cv.put(COL_USER, user);
            cv.put(COL_NAME, comics.getName());
            cv.put(COL_SERIES, comics.getSeries());
            cv.put(COL_PUBLISHER, comics.getPublisher());
            cv.put(COL_AUTHORS, comics.getAuthors());
            cv.put(COL_PRICE, comics.getPrice());
            cv.put(COL_PERIODICITY, comics.getPeriodicity());
            cv.put(COL_RESERVED, comics.isReserved() ? "T" : "F");
            cv.put(COL_NOTES, comics.getNotes());
            return cv;
        }
    }

    public static final class ReleasesTable {
        public static final String NAME = "tReleases";
        public static final String COL_ID = "_id";
        public static final String COL_USER = "user";
        public static final String COL_COMICS_ID = "comics_id";
        public static final String COL_NUMBER = "number";
        public static final String COL_DATA = "date";
        public static final String COL_PRICE = "price";
        public static final String COL_FLAGS = "flags";
        public static final String COL_NOTES = "notes";

        public static final int IDX_ID = 0;
        public static final int IDX_USER = 1;
        public static final int IDX_COMICS_ID = 2;
        public static final int IDX_NUMBER = 3;
        public static final int IDX_DATA = 4;
        public static final int IDX_PRICE = 5;
        public static final int IDX_FLAGS = 6;
        public static final int IDX_NOTES = 7;

        public static final String[] COLUMNS = { COL_ID, COL_USER, COL_COMICS_ID,
                COL_NUMBER, COL_DATA, COL_PRICE, COL_FLAGS, COL_NOTES };

        //la colonna _id non serve a nulla ma è necessaria per eventuali usi di classi come CursorAdapter, etc.
        static final String SQL_CREATE = "create table " + NAME +
                " (_id INTEGER NOT NULL, " +
                "user TEXT NOT NULL, comics_id INTEGER NOT NULL, number INTEGER NOT NULL, date TEXT, " +
                "price REAL, flags INTEGER, notes TEXT, PRIMARY KEY (_id, user, comics_id, number))";

        public static ContentValues getContentValues(Release release, String user) {
            ContentValues cv = new ContentValues();
            cv.put(COL_ID, 0L); //mantengo compatibilità
            cv.put(COL_USER, user);
            cv.put(COL_COMICS_ID, release.getComicsId());
            cv.put(COL_NUMBER, release.getNumber());
            cv.put(COL_DATA, Utils.formatDbRelease(release.getDate()));
            cv.put(COL_PRICE, release.getPrice());
            cv.put(COL_FLAGS, release.getFlags());
            cv.put(COL_NOTES, release.getNotes());
            return cv;
        }

    }

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ComicsTable.SQL_CREATE);
        db.execSQL(ReleasesTable.SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //da usare per aggiornare il database ad una nuova versione
    }
}
