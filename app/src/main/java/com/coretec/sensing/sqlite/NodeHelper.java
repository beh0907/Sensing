package com.coretec.sensing.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.coretec.sensing.model.Node;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.utils.FilePath;

import java.util.ArrayList;

public class NodeHelper {
    private static final String TABLE_NODE = "node";

    private static final String KEY_SEQ = "seq";
    private static final String KEY_MAP_X = "point_x";
    private static final String KEY_MAP_Y = "point_y";

    private SQLiteDatabase database;

    public NodeHelper() {
        this.database = SQLiteDatabase.openDatabase(FilePath.DB_PATH + FilePath.DB_NAME, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public ArrayList<Node> selectAllNodeList() {
        ArrayList<Node> nodeArrayList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_NODE + " ORDER by " + KEY_SEQ + " asc";

        Cursor cursor = database.rawQuery(query, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                int seq = cursor.getInt(cursor.getColumnIndex(KEY_SEQ));
                int mapX = cursor.getInt(cursor.getColumnIndex(KEY_MAP_X));
                int mapY = cursor.getInt(cursor.getColumnIndex(KEY_MAP_Y));

                Node node = new Node(seq, new Point(mapX, mapY));

                nodeArrayList.add(node);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return nodeArrayList;
    }

    public void insertNodeAll(ArrayList<Node> nodeArrayList) {

        for (Node node : nodeArrayList) {
            ContentValues values = new ContentValues();

            values.put(KEY_SEQ, node.getSeq());
            values.put(KEY_MAP_X, node.getPoint().getX());
            values.put(KEY_MAP_Y, node.getPoint().getY());

            database.insert(TABLE_NODE, "", values);
        }
    }

    public void deleteAll() {
        database.execSQL("DELETE FROM " + TABLE_NODE + ";");
    }
}