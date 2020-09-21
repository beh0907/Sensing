package com.coretec.sensing.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.utils.FilePath;

import java.util.ArrayList;
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
        String query = "SELECT * FROM " + TABLE_AP + " ORDER by " + KEY_SEQ + " asc";

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

    public void updateApPoint(int seq, float[] meterPoint) {
        ContentValues values = new ContentValues();

        values.put(KEY_MAP_X, meterPoint[0]);
        values.put(KEY_MAP_Y, meterPoint[1]);

        database.update(TABLE_AP, values, KEY_SEQ + "=" + seq, null);
    }

    public void insertApAll(ArrayList<Ap> apArrayList) {

        for (Ap ap : apArrayList) {
            ContentValues values = new ContentValues();

            values.put(KEY_SEQ, ap.getSeq());
            values.put(KEY_NAME, ap.getName());
            values.put(KEY_MAC_ADDRESS, ap.getMacAddress());
            values.put(KEY_MAP_X, ap.getPoint().getX());
            values.put(KEY_MAP_Y, ap.getPoint().getY());

            database.insert(TABLE_AP, "", values);
        }
    }

    public void insertAp(Ap ap) {
        ContentValues values = new ContentValues();

        values.put(KEY_SEQ, ap.getSeq());
        values.put(KEY_NAME, ap.getName());
        values.put(KEY_MAC_ADDRESS, ap.getMacAddress());
        values.put(KEY_MAP_X, ap.getPoint().getX());
        values.put(KEY_MAP_Y, ap.getPoint().getY());

        database.insert(TABLE_AP, "", values);
    }

    public void deleteAp(Ap ap) {
        database.delete(TABLE_AP, KEY_SEQ + "=?", new String[]{ap.getSeq() + ""});
    }

    public void deleteAll() {
        database.execSQL("DELETE FROM " + TABLE_AP + ";");
    }
}