package com.coretec.sensing.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.utils.FilePath;

import java.util.HashMap;

public class ApHelper {
    private static final String TABLE_AP = "ap";

    private static final String KEY_SEQ = "seq";
    private static final String KEY_NAME = "name";
    private static final String KEY_MAC_ADDRESS = "macAddress";
    private static final String KEY_MAP_X = "point_x";
    private static final String KEY_MAP_Y = "point_y";

    private SQLiteDatabase database;

    public ApHelper() {
        this.database = SQLiteDatabase.openDatabase(FilePath.DB_PATH + FilePath.DB_NAME, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public HashMap<String, Ap> selectAllApList() {
        HashMap<String, Ap> apHashMap = new HashMap<>();
        String query = "SELECT * FROM " + TABLE_AP;

        Cursor cursor = database.rawQuery(query, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                int seq = cursor.getInt(cursor.getColumnIndex(KEY_SEQ));
                String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                String madAddress = cursor.getString(cursor.getColumnIndex(KEY_MAC_ADDRESS));
                double mapX = cursor.getDouble(cursor.getColumnIndex(KEY_MAP_X));
                double mapY = cursor.getDouble(cursor.getColumnIndex(KEY_MAP_Y));

                Ap ap = new Ap(seq, name, madAddress, new Point(mapX, mapY));

                apHashMap.put(madAddress, ap);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return apHashMap;
    }
}