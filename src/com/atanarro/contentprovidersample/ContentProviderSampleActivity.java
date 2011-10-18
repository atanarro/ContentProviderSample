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

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Class to test de ContentProvider
 * 
 * @author atanarro
 *
 */
public class ContentProviderSampleActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// add a song
		ContentValues values = new ContentValues();
		values.put(SongsProvider.TITLE, "Walking Out the Door");
		values.put(SongsProvider.AUTHOR, "John Doe");
		Uri uri = getContentResolver()
				.insert(SongsProvider.CONTENT_URI, values);

		// add another song
		values.clear();
		values.put(SongsProvider.TITLE, "Should we fight back");
		values.put(SongsProvider.AUTHOR, "The Parlotones");
		uri = getContentResolver().insert(SongsProvider.CONTENT_URI, values);

		// from external packages
		values.clear();
		values.put("title", "Baby");
		values.put("author", "Alice Smith & Aloe Blacc");
		uri = getContentResolver().insert(
				Uri.parse("content://net.atanarro.provider.Songs/songs"),
				values);

		// test
		Uri allTitles = Uri
				.parse("content://net.atanarro.provider.Songs/songs");
		Cursor c = managedQuery(allTitles, null, null, null, "title desc");
		if (c.moveToFirst()) {
			do {
				Toast.makeText(
						this,
						c.getString(c.getColumnIndex(SongsProvider.AUTHOR))
						+ ", \""
						+ c.getString(c.getColumnIndex(SongsProvider.TITLE)) + "\"",
						Toast.LENGTH_LONG).show();
			} while (c.moveToNext());
		}

		// update
		ContentValues editedValues = new ContentValues();
		editedValues.put(SongsProvider.TITLE, "Should We Fight Back?");

		getContentResolver().update(
				Uri.parse("content://net.atanarro.provider.Songs/songs/2"),
				editedValues, null, null);

		// delete
		getContentResolver().delete(
				Uri.parse("content://net.atanarro.provider.Songs/songs/2"),
				null, null);
		
		// delete all
		getContentResolver().delete(
				Uri.parse("content://net.atanarro.provider.Songs/songs"), null,
				null);

	}

}