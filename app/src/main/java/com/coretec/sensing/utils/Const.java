package com.coretec.sensing.utils;

/**
 * Created by CoreJin on 2017-03-29.
 */

public class Const {
    public static final int NONE = 0;
    public static final int DRAG = 1;
    public static final int ZOOM = 2;

    //사무실 내부 도면
    public static final float MAP_WIDTH = 2848f;
    public static final float MAP_HEIGHT = 4574f;

    public static final double PIXEL_PER_METER = 10.68f / MAP_HEIGHT;
    public static final double METER_PER_PIXEL = MAP_HEIGHT / 10.68f;

    public static final float LEFT_BLANK_PIXEL = 0;
    public static final float RIGHT_BLANK_PIXEL = 0;
    public static final float TOP_BLANK_PIXEL = 0;
    public static final float BOTTOM_BLANK_PIXEL = 0;

    public static final double LEFT_BLANK_METER = PIXEL_PER_METER * LEFT_BLANK_PIXEL;
    public static final double RIGHT_BLANK_METER = PIXEL_PER_METER * RIGHT_BLANK_PIXEL;
    public static final double TOP_BLANK_METER = PIXEL_PER_METER * TOP_BLANK_PIXEL;
    public static final double BOTTOM_BLANK_METER = PIXEL_PER_METER * BOTTOM_BLANK_PIXEL;


    //11동 지하주차장 도면
//    public static final float MAP_WIDTH = 4000f;
//    public static final float MAP_HEIGHT = 3000f;
//
//    public static final float MAP_REAL_WIDTH = 3483f;
//    public static final float MAP_REAL_HEIGHT = 1485f;
//
//    public static final double PIXEL_PER_METER = 80.7f / MAP_REAL_WIDTH;
//    public static final double METER_PER_PIXEL = MAP_REAL_WIDTH / 80.7f;
//
//    public static final float LEFT_BLANK_PIXEL = 317f;
//    public static final float RIGHT_BLANK_PIXEL = 200f;
//    public static final float TOP_BLANK_PIXEL = 695f;
//    public static final float BOTTOM_BLANK_PIXEL = 820f;
//
//    public static final double LEFT_BLANK_METER = PIXEL_PER_METER * LEFT_BLANK_PIXEL;
//    public static final double RIGHT_BLANK_METER = PIXEL_PER_METER * RIGHT_BLANK_PIXEL;
//    public static final double TOP_BLANK_METER = PIXEL_PER_METER * TOP_BLANK_PIXEL;
//    public static final double BOTTOM_BLANK_METER = PIXEL_PER_METER * BOTTOM_BLANK_PIXEL;


    //11동 지하주차장 도면 공백 제거
//    public static final float MAP_WIDTH = 3485f;
//    public static final float MAP_HEIGHT = 1487f;
//
//    public static final double PIXEL_PER_METER = 80.7f / MAP_WIDTH;
//    public static final double METER_PER_PIXEL = MAP_WIDTH / 80.7f;
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
}
