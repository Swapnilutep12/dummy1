package edu.utep.cs.mypricewatcher.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import pricewatcher.model.SqliteDatabaseHelperable;
import pricewatcher.model.SqliteItem;

public class ItemDatabaseHelper extends SQLiteOpenHelper implements SqliteDatabaseHelperable {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "itemDB";
    private static final String ITEM_TABLE = "items";

    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_GROUP = "group_name";
    private static final String KEY_URL = "url";
    private static final String KEY_TIME = "time";
    private static final String KEY_PRICE = "price";
    private static final String KEY_PRICE_INIT = "price_init";


    public ItemDatabaseHelper(Context context){
        super (context, DB_NAME, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + ITEM_TABLE + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_NAME + " TEXT, "
                + KEY_GROUP + " TEXT, "
                + KEY_URL + " TEXT, "
                + KEY_TIME + " INTEGER, "
                + KEY_PRICE_INIT + " REAL, "
                + KEY_PRICE + " REAL" + ")";

        db.execSQL(sql);
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + ITEM_TABLE);
        onCreate(database);
    }

    /** Add a new item and return the id of the added item or -1 upon an error. */
    @Override
    public int addItem(SqliteItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, item.name());
        values.put(KEY_GROUP, item.group());
        values.put(KEY_URL, item.url());
        values.put(KEY_PRICE, item.currentPrice());
        values.put(KEY_PRICE_INIT, item.initialPrice());
        values.put(KEY_TIME, System.currentTimeMillis());
        long id = db.insert(ITEM_TABLE, null, values);
        db.close();
        return (int) id;
    }

    /** Remove the item that has the given id. */
    public boolean removeItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int cnt = db.delete(ITEM_TABLE,
                KEY_ID + " = ?",
                new String[] { Integer.toString(id) } );
        db.close();
        return cnt > 0;
    }

    @Override
    public boolean updateItem(SqliteItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, item.name());
        values.put(KEY_GROUP, item.group());
        values.put(KEY_URL, item.url());
        values.put(KEY_PRICE, item.currentPrice());
        values.put(KEY_PRICE_INIT, item.initialPrice());
        values.put(KEY_TIME, System.currentTimeMillis());
        int rows = db.update(ITEM_TABLE,
                values,
                KEY_ID + " = ?",
                new String[] { Integer.toString(item.id()) } );
        db.close();
        return rows > 0;
    }

    public List<SqliteItem> items() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(ITEM_TABLE,
                null, null, null, null, null, null);
        List<SqliteItem> result = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                String group = cursor.getString(cursor.getColumnIndex(KEY_GROUP));
                String url = cursor.getString(cursor.getColumnIndex(KEY_URL));
                long time = cursor.getLong(cursor.getColumnIndex(KEY_TIME));
                float initPrice = cursor.getFloat(cursor.getColumnIndex(KEY_PRICE_INIT));
                float price = cursor.getFloat(cursor.getColumnIndex(KEY_PRICE));
                result.add(new SqliteItem(id, name, group, url, time, initPrice, price));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }
}
