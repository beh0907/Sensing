package com.coretec.sensing.utils;


import android.net.wifi.rtt.RangingResult;
import android.os.SystemClock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String getNextDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Calendar cal = Calendar.getInstance();
        Date d = null;
        try {
            d = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        cal.setTime(d);
        cal.add(Calendar.DATE, 1);
        date = sdf.format(cal.getTime());
        return date;
    }

    public static String getPrevDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Calendar cal = Calendar.getInstance();
        Date d = null;
        try {
            d = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        cal.setTime(d);
        cal.add(Calendar.DATE, -1);
        date = sdf.format(cal.getTime());
        return date;
    }

    public static String getCurrentDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String currentDate = df.format(new Date());
        return currentDate;
    }

    public static String getCurrentDateStartTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String currentDateTime = df.format(new Date()) + " 00:00:00";

        return currentDateTime;
    }

//    public static String getCurrentDateTime() {
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS", Locale.KOREA);
//        String currentDateTime = df.format(new Date());
//
//        return currentDateTime;
//    }

    public static String getCurrentDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd,kkmm,ssSSS", Locale.KOREA);
        String currentDateTime = df.format(new Date());

        return currentDateTime;
    }

    public static String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat("kk:mm:ss.SSS", Locale.KOREA);
        String currentDateTime = df.format(new Date());

        return currentDateTime;
    }

    public static String getCurrentCsvFileName() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd kk-mm-ss.SSS", Locale.KOREA);
        String currentDateTime = df.format(new Date());

        return currentDateTime;
    }

    public static String getTimeStampToDateTime(long timeStamp) {
        long rxTimestampMillis = System.currentTimeMillis() -
                SystemClock.elapsedRealtime() +
                timeStamp / 1000000;

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd,kkmm,ssSSS", Locale.KOREA);
        Date date = new Date(rxTimestampMillis);

        return df.format(date);
    }

    public static boolean isRemoveResult(RangingResult rangingResult, int stdTimeMillis) {
        return System.currentTimeMillis() >= rangingResult.getRangingTimestampMillis() + stdTimeMillis;
    }
}