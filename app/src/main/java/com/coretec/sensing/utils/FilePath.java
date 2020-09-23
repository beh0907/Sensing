package com.coretec.sensing.utils;

import com.coretec.sensing.BuildConfig;

/**
 * Created by CoreJin on 2016-12-16.
 */

public class FilePath {
    public static final String DB_PATH = "/data/data/" + BuildConfig.APPLICATION_ID + "/databases/";
    public static String DB_NAME = "rtt3.db";

    public static void setDbName(String dbName) {
        DB_NAME = dbName;
    }
}
