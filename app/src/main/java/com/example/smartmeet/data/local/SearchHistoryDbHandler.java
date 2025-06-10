package com.example.smartmeet.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.smartmeet.data.model.Venue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryDbHandler {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private final Gson gson = new Gson();

    public SearchHistoryDbHandler(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public void insertSearchHistory(long timestamp, List<String> addresses, String amenity, List<Venue> venues) {
        ContentValues values = new ContentValues();
        values.put(SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP, timestamp);
        values.put(SearchHistoryContract.HistoryEntry.COLUMN_NAME_AMENITY, amenity);
        values.put(SearchHistoryContract.HistoryEntry.COLUMN_NAME_INPUT_ADDRESSES, gson.toJson(addresses));
        values.put(SearchHistoryContract.HistoryEntry.COLUMN_NAME_RESULT_VENUES, gson.toJson(venues));

        database.insert(SearchHistoryContract.HistoryEntry.TABLE_NAME, null, values);
    }

    public List<SearchHistory> getAllHistory() {
        List<SearchHistory> historyList = new ArrayList<>();
        Cursor cursor = database.query(
                SearchHistoryContract.HistoryEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP + " DESC"
        );

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry._ID));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP));
                String amenity = cursor.getString(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_AMENITY));
                String addressesJson = cursor.getString(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_INPUT_ADDRESSES));
                String venuesJson = cursor.getString(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_RESULT_VENUES));

                Type addressListType = new TypeToken<List<String>>() {}.getType();
                List<String> addresses = gson.fromJson(addressesJson, addressListType);

                Type venueListType = new TypeToken<List<Venue>>() {}.getType();
                List<Venue> venues = gson.fromJson(venuesJson, venueListType);

                historyList.add(new SearchHistory(id, timestamp, addresses, amenity, venues));
            }
        } finally {
            cursor.close();
        }
        return historyList;
    }

    public void deleteHistory(long timestamp) {
        database.delete(SearchHistoryContract.HistoryEntry.TABLE_NAME,
                SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP + " = ?",
                new String[]{String.valueOf(timestamp)});
    }

    public void deleteAllHistory() {
        database.delete(SearchHistoryContract.HistoryEntry.TABLE_NAME, null, null);
    }

    public List<SearchHistory> getRecentHistory(long afterTimestamp) {
        List<SearchHistory> historyList = new ArrayList<>();
        Cursor cursor = database.query(
                SearchHistoryContract.HistoryEntry.TABLE_NAME,
                null,
                SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP + " > ?",
                new String[]{String.valueOf(afterTimestamp)},
                null,
                null,
                SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP + " DESC"
        );

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry._ID));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_TIMESTAMP));
                String amenity = cursor.getString(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_AMENITY));
                String addressesJson = cursor.getString(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_INPUT_ADDRESSES));
                String venuesJson = cursor.getString(cursor.getColumnIndexOrThrow(SearchHistoryContract.HistoryEntry.COLUMN_NAME_RESULT_VENUES));

                Type addressListType = new TypeToken<List<String>>() {}.getType();
                List<String> addresses = gson.fromJson(addressesJson, addressListType);

                Type venueListType = new TypeToken<List<Venue>>() {}.getType();
                List<Venue> venues = gson.fromJson(venuesJson, venueListType);

                historyList.add(new SearchHistory(id, timestamp, addresses, amenity, venues));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return historyList;
    }
}