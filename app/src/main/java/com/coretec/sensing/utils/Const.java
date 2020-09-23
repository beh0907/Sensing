package com.coretec.sensing.utils;

import android.os.Environment;

import java.io.File;

public class Const {
    //로깅 폴더 경로
    public static final String LoggingPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;

    //DB 폴더 경로
    public static final String DBPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator + "PYEONGTAEK_DB" + File.separator;
    public static final String DB2Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator + "11_DB" + File.separator;

    //맵 폴더 경로
    public static final String MAPPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator + "MAP" + File.separator;

    public static final int NONE = 0;
    public static final int DRAG = 1;
    public static final int ZOOM = 2;


    public static void setMapParam(float width, float height, double fixelPerMeterX, double fixelPerMeterY) {
        MAP_WIDTH = width;
        MAP_HEIGHT = height;

        PIXEL_PER_METER_WIDTH = fixelPerMeterX;
        METER_PER_PIXEL_WIDTH = 1d / fixelPerMeterX;

        PIXEL_PER_METER_HEIGHT = fixelPerMeterY;
        METER_PER_PIXEL_HEIGHT = 1d / fixelPerMeterY;
    }


    public static void setMapParam(float width, float height, double fixelPerMeterX, double fixelPerMeterY, float blankLeft, float blankRight, float blankTop, float blankBottom) {
        MAP_WIDTH = width;
        MAP_HEIGHT = height;

        LEFT_BLANK_PIXEL = blankLeft;
        RIGHT_BLANK_PIXEL = blankRight;
        TOP_BLANK_PIXEL = blankTop;
        BOTTOM_BLANK_PIXEL = blankBottom;

        MAP_REAL_WIDTH = MAP_WIDTH - LEFT_BLANK_PIXEL - RIGHT_BLANK_PIXEL;
        MAP_REAL_HEIGHT = MAP_HEIGHT - TOP_BLANK_PIXEL - BOTTOM_BLANK_PIXEL;

        PIXEL_PER_METER_WIDTH = MAP_REAL_WIDTH / fixelPerMeterX;
        METER_PER_PIXEL_WIDTH = fixelPerMeterX / MAP_REAL_WIDTH;

        PIXEL_PER_METER_HEIGHT = MAP_REAL_HEIGHT / fixelPerMeterY;
        METER_PER_PIXEL_HEIGHT = fixelPerMeterY / MAP_REAL_HEIGHT;
    }


    //11동 지하주차장 도면
//    public static float MAP_WIDTH = 4000f;
//    public static float MAP_HEIGHT = 3000f;
//
//    public static double PIXEL_PER_METER_WIDTH = MAP_REAL_WIDTH / 80.7f;
//    public static double METER_PER_PIXEL_WIDTH = 80.7f / MAP_REAL_WIDTH;
//
//    public static double PIXEL_PER_METER_HEIGHT = MAP_REAL_HEIGHT / 34.4f;
//    public static double METER_PER_PIXEL_HEIGHT = 34.4f / MAP_REAL_HEIGHT;

//    public static float LEFT_BLANK_PIXEL = 317f;
//    public static float RIGHT_BLANK_PIXEL = 200f;
//    public static float TOP_BLANK_PIXEL = 695f;
//    public static float BOTTOM_BLANK_PIXEL = 820f;
////
//    public static float MAP_REAL_WIDTH = 3483f;
//    public static float MAP_REAL_HEIGHT = 1485f;

    //평택역 도면 (공백 제거)
//    가로 21.5 pixel/m
//    세로 22.4 pixel/m
//    public static float MAP_WIDTH = 2411f;
//    public static float MAP_HEIGHT = 2040f;
//
//    public static double PIXEL_PER_METER_WIDTH = 21.5f;
//    public static double METER_PER_PIXEL_WIDTH = 0.0465116279069767d;
//
//    public static double PIXEL_PER_METER_HEIGHT = 22.4f;
//    public static double METER_PER_PIXEL_HEIGHT = 0.0446428571428571d;


    public static float MAP_WIDTH = 0;
    public static float MAP_HEIGHT = 0;

    public static double PIXEL_PER_METER_WIDTH = 0d;
    public static double METER_PER_PIXEL_WIDTH = 0d;

    public static double PIXEL_PER_METER_HEIGHT = 0d;
    public static double METER_PER_PIXEL_HEIGHT = 0d;

    public static float LEFT_BLANK_PIXEL = 0f;
    public static float RIGHT_BLANK_PIXEL = 0f;
    public static float TOP_BLANK_PIXEL = 0f;
    public static float BOTTOM_BLANK_PIXEL = 0f;

    //공백이 있을 경우 결정 되는 값
    public static float MAP_REAL_WIDTH = 0;
    public static float MAP_REAL_HEIGHT = 0;

    //공백 영역 Meter로 변경
    public static double LEFT_BLANK_METER = METER_PER_PIXEL_WIDTH * LEFT_BLANK_PIXEL;
    public static double RIGHT_BLANK_METER = METER_PER_PIXEL_WIDTH * RIGHT_BLANK_PIXEL;
    public static double TOP_BLANK_METER = METER_PER_PIXEL_HEIGHT * TOP_BLANK_PIXEL;
    public static double BOTTOM_BLANK_METER = METER_PER_PIXEL_HEIGHT * BOTTOM_BLANK_PIXEL;
}
