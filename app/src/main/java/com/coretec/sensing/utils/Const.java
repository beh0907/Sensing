package com.coretec.sensing.utils;

/**
 * Created by CoreJin on 2017-03-29.
 */

public class Const {
    public static final int NONE = 0;
    public static final int DRAG = 1;
    public static final int ZOOM = 2;

    public static final int MEGA_BYTE = 1048576;

//    public static final double PIXEL_PER_METER = 81.45f / 3486f;
//    public static final double METER_PER_PIXEL = 3486f / 81.45f;
//
//    public static final double LEFT_BLANK_PIXEL = 316f;
//    public static final double BOTTOM_BLANK_PIXEL = 839f;

    public static final double PIXEL_PER_METER = 10.68f / 4574f;
    public static final double METER_PER_PIXEL = 4574f / 10.68f;

    public static final double LEFT_BLANK_PIXEL = 0;
    public static final double BOTTOM_BLANK_PIXEL = 0;

    public static final double LEFT_BLANK_METER = PIXEL_PER_METER * LEFT_BLANK_PIXEL;
    public static final double BOTTOM_BLANK_METER = PIXEL_PER_METER * BOTTOM_BLANK_PIXEL;
}
