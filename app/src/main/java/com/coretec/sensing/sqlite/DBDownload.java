package com.coretec.sensing.sqlite;

import android.content.Context;
import android.content.res.AssetManager;

import com.coretec.sensing.utils.PrefManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.coretec.sensing.utils.FilePath.DB_NAME;
import static com.coretec.sensing.utils.FilePath.DB_PATH;

public class DBDownload {
    public static void copyDB(PrefManager pref, Context context, String data) { //에셋폴더에 저장된 DB파일 프로젝트 폴더에 복사
        File folder = new File(DB_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        AssetManager aman = context.getResources().getAssets();
        File ofile = new File(DB_PATH + DB_NAME);
        ofile.delete();
        InputStream in;
        FileOutputStream out;
        long filesize;

        try {
            in = aman.open(DB_NAME, AssetManager.ACCESS_BUFFER);
            filesize = in.available();

            if (ofile.length() <= 0) {
                byte[] tmpbyte = new byte[(int) filesize];
                in.read(tmpbyte);
                in.close();
                ofile.createNewFile();
                out = new FileOutputStream(ofile);
                out.write(tmpbyte);
                out.close();
                pref.setDownloadDB(true, data);
            } else {
                System.out.println("DB있음!!!");
            }
        } catch (IOException e) {
            System.out.println("DB생성 오류 [" + e + "]");
            pref.setDownloadDB(false, data);
        }

    }
}
