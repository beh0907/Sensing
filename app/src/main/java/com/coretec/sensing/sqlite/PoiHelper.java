package com.coretec.sensing.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.coretec.sensing.model.Poi;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.utils.FilePath;

import java.util.ArrayList;

public class PoiHelper {
    private static final String TABLE_POI = "poi";

    private static final String KEY_SEQ = "seq";
    private static final String KEY_NAME = "name";
    private static final String KEY_MAP_X = "point_x";
    private static final String KEY_MAP_Y = "point_y";

    private SQLiteDatabase database;

    public PoiHelper() {
        this.database = SQLiteDatabase.openDatabase(FilePath.DB_PATH + FilePath.DB_NAME, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public ArrayList<Poi> selectAllPoiList() {
        ArrayList<Poi> poiArrayList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_POI + " ORDER by " + KEY_SEQ + " asc";

        Cursor cursor = database.rawQuery(query, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                int seq = cursor.getInt(cursor.getColumnIndex(KEY_SEQ));
                String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                int mapX = cursor.getInt(cursor.getColumnIndex(KEY_MAP_X));
                int mapY = cursor.getInt(cursor.getColumnIndex(KEY_MAP_Y));

                Poi poi = new Poi(seq, name, new Point(mapX, mapY));

                poiArrayList.add(poi);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return poiArrayList;
    }

    public void insertPoiAll(ArrayList<Poi> poiArrayList) {
        for (Poi poi : poiArrayList) {
            ContentValues values = new ContentValues();

            values.put(KEY_SEQ, poi.getSeq());
            values.put(KEY_NAME, poi.getName());
            values.put(KEY_MAP_X, poi.getPoint().getX());
            values.put(KEY_MAP_Y, poi.getPoint().getY());

            database.insert(TABLE_POI, "", values);
        }
    }

    public void deleteAll() {
        database.execSQL("DELETE FROM " + TABLE_POI + ";");
    }
}