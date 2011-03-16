package net.morettoni.a;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.text.TextUtils;

public class TerremotoProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri
            .parse("content://net.morettoni.terremoto.provider.terremoto/terremoti");
    private static final UriMatcher uriMatcher;
    private static final int TERREMOTI = 1;
    private static final int TERREMOTI_ID = 2;

    private SQLiteDatabase terremotiDB;

    private static final String DB_NAME = "terremoti.db";
    private static final int DB_VERSION = 2;
    private static final String TERREMOTO_TABLE = "terremoti";

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("net.morettoni.terremoto.provider.terremoto",
                "terremoti", TERREMOTI);
        uriMatcher.addURI("net.morettoni.terremoto.provider.terremoto",
                "terremoti/#", TERREMOTI_ID);
    }

    public static final String KEY_ID = "_id";
    public static final String KEY_DATA = "data";
    public static final String KEY_LAT = "latitude";
    public static final String KEY_LNG = "longitude";
    public static final String KEY_MAG = "magnitude";
    public static final String KEY_WHERE = "luogo";
    public static final String KEY_DEEP = "profondita";

    public static final int ID_COLUMN = 0;
    public static final int DATA_COLUMN = 1;
    public static final int LATITUDE_COLUMN = 2;
    public static final int LONGITUDE_COLUMN = 3;
    public static final int MAGNITUDE_COLUMN = 4;
    public static final int WHERE_COLUMN = 5;
    public static final int DEEP_COLUMN = 6;

    // Helper class for opening, creating, and managing database version control
    private static class TerremotoDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_CREATE = "CREATE TABLE "
                + TERREMOTO_TABLE + " (" + KEY_ID + " INTEGER PRIMARY KEY, "
                + KEY_DATA + " INTEGER, " + KEY_LAT + " FLOAT, " + KEY_LNG
                + " FLOAT, " + KEY_MAG + " FLOAT, " + KEY_WHERE + " TEXT, "
                + KEY_DEEP + " FLOAT);";

        public TerremotoDatabaseHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TERREMOTO_TABLE);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        TerremotoDatabaseHelper dbHelper = new TerremotoDatabaseHelper(context,
                DB_NAME, null, DB_VERSION);
        terremotiDB = dbHelper.getWritableDatabase();
        return (terremotiDB == null) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sort) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(TERREMOTO_TABLE);

        switch (uriMatcher.match(uri)) {
        case TERREMOTI_ID:
            qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
            break;
        default:
            break;
        }

        String orderBy;
        if (TextUtils.isEmpty(sort)) {
            orderBy = KEY_DATA + " DESC";
        } else {
            orderBy = sort;
        }

        Cursor c = qb.query(terremotiDB, projection, selection, selectionArgs,
                null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri _uri, ContentValues _initialValues) {
        long rowID = terremotiDB.insert(TERREMOTO_TABLE, "?", _initialValues);

        // Return a URI to the newly inserted row on success.
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        }
        throw new SQLException("Failed to insert row into " + _uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;

        switch (uriMatcher.match(uri)) {
        case TERREMOTI:
            count = terremotiDB.delete(TERREMOTO_TABLE, where, whereArgs);
            break;

        case TERREMOTI_ID:
            String segment = uri.getPathSegments().get(1);
            count = terremotiDB.delete(TERREMOTO_TABLE,
                    KEY_ID
                            + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
        case TERREMOTI:
            count = terremotiDB.update(TERREMOTO_TABLE, values, where,
                    whereArgs);
            break;

        case TERREMOTI_ID:
            String segment = uri.getPathSegments().get(1);
            count = terremotiDB.update(TERREMOTO_TABLE, values,
                    KEY_ID
                            + "="
                            + segment
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case TERREMOTI:
            return "vnd.android.cursor.dir/vnd.morettoni.terremoto";
        case TERREMOTI_ID:
            return "vnd.android.cursor.item/vnd.morettoni.terremoto";
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
