/*
 * Copyright (C)  2011  Álvaro Tanarro Santamaría.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atanarro.contentprovidersample;

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
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Class that extends ContentProvider to manage a collection of songs
 * 
 * @author atanarro
 * 
 */
public class SongsProvider extends ContentProvider {
	public static final String PROVIDER_NAME = "net.atanarro.provider.Songs";

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ PROVIDER_NAME + "/songs");

	private static final int SONGS = 1;
	private static final int SONG_ID = 2;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "songs", SONGS);
		uriMatcher.addURI(PROVIDER_NAME, "songs/#", SONG_ID);
	}

	// database stuff
	private SQLiteDatabase songsDB;

	public static final String _ID = "_id";
	public static final String TITLE = "title";
	public static final String AUTHOR = "author";

	private static final String DATABASE_NAME = "Songs";
	private static final String DATABASE_TABLE = "titles";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + _ID
			+ " integer primary key autoincrement, " + TITLE
			+ " text not null, " + AUTHOR + " text not null);";

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("Content provider database",
					"Upgrading database from version " + oldVersion + " to "
							+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		// get all songs
		case SONGS:
			return "vnd.android.cursor.dir/vnd.atanarro.songs";
			// get a particular song
		case SONG_ID:
			return "vnd.android.cursor.item/vnd.atanarro.songs";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		Context context = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		songsDB = dbHelper.getWritableDatabase();
		return (songsDB == null) ? false : true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);

		if (uriMatcher.match(uri) == SONG_ID)
			// if getting a particular song
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));

		if (sortOrder == null || sortOrder == "")
			sortOrder = TITLE;

		Cursor c = sqlBuilder.query(songsDB, projection, selection,
				selectionArgs, null, null, sortOrder);

		// register to watch a content URI for changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// add a new song
		long rowID = songsDB.insert(DATABASE_TABLE, "", values);

		// if added successfully
		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case SONGS:
			count = songsDB.delete(DATABASE_TABLE, selection, selectionArgs);
			break;
		case SONG_ID:
			String id = uri.getPathSegments().get(1);
			count = songsDB.delete(DATABASE_TABLE, _ID
					+ " = "
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case SONGS:
			count = songsDB.update(DATABASE_TABLE, values, selection,
					selectionArgs);
			break;
		case SONG_ID:
			count = songsDB.update(DATABASE_TABLE, values, _ID
					+ " = "
					+ uri.getPathSegments().get(1)
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
