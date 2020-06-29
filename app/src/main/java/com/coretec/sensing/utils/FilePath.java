package com.coretec.sensing.utils;

import com.coretec.sensing.BuildConfig;

/**
 * Created by CoreJin on 2016-12-16.
 */

public class FilePath {
    public static final String DB_PATH = "/data/data/" + BuildConfig.APPLICATION_ID + "/databases/";
    public static final String DB_NAME = "rtt.db";

    public static final String FILE_ROOT = "/storage/emulated/0/Android/data/" + BuildConfig.APPLICATION_ID + "/files/";

    public static final String SERVER_LINK = "http://devcoretec.iptime.org:82/rtt/";
}
