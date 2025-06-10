package com.example.smartmeet.data.local;

import android.provider.BaseColumns;

public final class SearchHistoryContract {
    private SearchHistoryContract() {}

    public static class HistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "search_history";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_INPUT_ADDRESSES = "input_addresses";
        public static final String COLUMN_NAME_AMENITY = "amenity";
        public static final String COLUMN_NAME_RESULT_VENUES = "result_venues";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + HistoryEntry.TABLE_NAME + " (" +
                    HistoryEntry._ID + " INTEGER PRIMARY KEY," +
                    HistoryEntry.COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    HistoryEntry.COLUMN_NAME_INPUT_ADDRESSES + " TEXT," +
                    HistoryEntry.COLUMN_NAME_AMENITY + " TEXT," +
                    HistoryEntry.COLUMN_NAME_RESULT_VENUES + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + HistoryEntry.TABLE_NAME;
}