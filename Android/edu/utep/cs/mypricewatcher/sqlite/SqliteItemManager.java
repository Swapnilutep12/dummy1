package edu.utep.cs.mypricewatcher.sqlite;

import android.content.Context;

/** An item manager that stores items in a sqlite database. */
public class SqliteItemManager extends pricewatcher.model.SqliteItemManager {

    private static SqliteItemManager theInstance;

	private SqliteItemManager(Context context) {
		super(new ItemDatabaseHelper(context));
	}

	public static SqliteItemManager getInstance(Context context) {
		if (theInstance == null) {
			theInstance = new SqliteItemManager(context);
		}
		return theInstance;
	}
}
