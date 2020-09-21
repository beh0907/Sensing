package com.coretec.sensing.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by CoreJin on 2017-03-29.
 */

public class Const {
    //로깅 폴더 경로
    public static final String LoggingPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;

    //로깅 폴더 경로
    public static final String DBPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator + "DB" + File.separator;

    public static final int NONE = 0;
    public static final int DRAG = 1;
    public static final int ZOOM = 2;

    //사무실 내부 도면
//    public static final float MAP_WIDTH = 2848f;
//    public static final float MAP_HEIGHT = 4574f;
//
//    public static final double PIXEL_PER_METER = 10.68f / MAP_HEIGHT;
//    public static final double METER_PER_PIXEL = MAP_HEIGHT / 10.68f;
//
//    public static final float LEFT_BLANK_PIXEL = 0;
//    public static final float RIGHT_BLANK_PIXEL = 0;
//    public static final float TOP_BLANK_PIXEL = 0;
//    public static final float BOTTOM_BLANK_PIXEL = 0;
//
//    public static final double LEFT_BLANK_METER = PIXEL_PER_METER * LEFT_BLANK_PIXEL;
//    public static final double RIGHT_BLANK_METER = PIXEL_PER_METER * RIGHT_BLANK_PIXEL;
//    public static final double TOP_BLANK_METER = PIXEL_PER_METER * TOP_BLANK_PIXEL;
//    public static final double BOTTOM_BLANK_METER = PIXEL_PER_METER * BOTTOM_BLANK_PIXEL;

    //11동 지하주차장 도면
//    public static final float MAP_WIDTH = 4000f;
//    public static final float MAP_HEIGHT = 3000f;
//
//    public static final float MAP_REAL_WIDTH = 3483f;
//    public static final float MAP_REAL_HEIGHT = 1485f;
//
//    public static final double PIXEL_PER_METER_WIDTH = 80.7f / MAP_REAL_WIDTH;
//    public static final double METER_PER_PIXEL_WIDTH = MAP_REAL_WIDTH / 80.7f;
//
//    public static final double PIXEL_PER_METER_HEIGHT = 34.4f / MAP_REAL_HEIGHT;
//    public static final double METER_PER_PIXEL_HEIGHT = MAP_REAL_HEIGHT / 34.4f;
//
//    public static final float LEFT_BLANK_PIXEL = 317f;
//    public static final float RIGHT_BLANK_PIXEL = 200f;
//    public static final float TOP_BLANK_PIXEL = 695f;
//    public static final float BOTTOM_BLANK_PIXEL = 820f;


    //평택역 도면 (공백 제거)

//    가로 21.5 pixel/m
//    세로 22.4 pixel/m
    public static final float MAP_WIDTH = 2411f;
    public static final float MAP_HEIGHT = 2040f;

    public static final float MAP_REAL_WIDTH = 2411f;
    public static final float MAP_REAL_HEIGHT = 2040f;

    public static final double PIXEL_PER_METER_WIDTH = 0.0465116279069767d;
    public static final double METER_PER_PIXEL_WIDTH = 21.5f;

    public static final double PIXEL_PER_METER_HEIGHT = 0.0446428571428571d;
    public static final double METER_PER_PIXEL_HEIGHT = 22.4f;

    public static final float LEFT_BLANK_PIXEL = 0f;
    public static final float RIGHT_BLANK_PIXEL = 0f;
    public static final float TOP_BLANK_PIXEL = 0f;
    public static final float BOTTOM_BLANK_PIXEL = 0f;

    //공백 영역 Meter로 변경
    public static final double LEFT_BLANK_METER = PIXEL_PER_METER_WIDTH * LEFT_BLANK_PIXEL;
    public static final double RIGHT_BLANK_METER = PIXEL_PER_METER_WIDTH * RIGHT_BLANK_PIXEL;
    public static final double TOP_BLANK_METER = PIXEL_PER_METER_HEIGHT * TOP_BLANK_PIXEL;
    public static final double BOTTOM_BLANK_METER = PIXEL_PER_METER_HEIGHT * BOTTOM_BLANK_PIXEL;
}
