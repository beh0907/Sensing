package com.coretec.sensing.utils;

import android.content.Context;
import android.content.SharedPreferences;

//Preference 상태정보 체크
public class PrefManager {
    // Shared preferences file name
    private static final String PREF_NAME = "rtt";
    private static final String PREF_NAME2 = "rtt2";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static final String DATABASE = "database";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    // shared pref mode

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, 0);
        editor = pref.edit();
    }

    //앱 최초 실행 여부 체크
    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    //앱 최초 실행 여부 설정
    //앱 데이터 삭제시 최초 실행으로 간주
    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.apply();
    }

    //로컬DB 파일 다운로드 여부 설정
    public void setDownloadDB(boolean downloadDB) {
        editor.putBoolean(DATABASE, downloadDB);
        editor.apply();
    }

    //로컬DB 파일 다운로드 여부 체크
    public boolean isDownloadDB() {
        return pref.getBoolean(DATABASE, false);
    }
}