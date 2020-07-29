package com.coretec.sensing.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.coretec.sensing.model.Link;
import com.coretec.sensing.model.Poi;
import com.coretec.sensing.utils.FilePath;

import java.util.ArrayList;

public class LinkHelper {
    private static final String TABLE_LINK = "link";

    private static final String KEY_SEQ = "seq";
    private static final String KEY_NODE_START = "node_start";
    private static final String KEY_NODE_END = "node_end";
    private static final String KEY_WEIGHT_P = "weight(p)";
    private static final String KEY_WEIGHT_M = "weight(m)";

    private SQLiteDatabase database;

    public LinkHelper() {
        this.database = SQLiteDatabase.openDatabase(FilePath.DB_PATH + FilePath.DB_NAME, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public ArrayList<Link> selectAllLinkList() {
        ArrayList<Link> linkArrayList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_LINK + " ORDER by " + KEY_SEQ + " asc";

        Cursor cursor = database.rawQuery(query, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                int seq = cursor.getInt(cursor.getColumnIndex(KEY_SEQ));
                int startNode = cursor.getInt(cursor.getColumnIndex(KEY_NODE_START));
                int endNode = cursor.getInt(cursor.getColumnIndex(KEY_NODE_END));
                int weightP = cursor.getInt(cursor.getColumnIndex(KEY_WEIGHT_P));
                double weightM = cursor.getDouble(cursor.getColumnIndex(KEY_WEIGHT_M));

                Link link = new Link(seq, startNode, endNode, weightP, weightM);

                linkArrayList.add(link);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return linkArrayList;
    }
}