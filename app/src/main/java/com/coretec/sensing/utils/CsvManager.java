package com.coretec.sensing.utils;

import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvManager {
    private String delimiter = ",";
    private String fileName = "AnalysisData.csv";
    private FileWriter mFileWriter;
    private CSVWriter writer;

    private String baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;

    public CsvManager() {
        // Do nothing
        createFolder();
    }

    public CsvManager(String fileName) {
        createFolder();
        this.fileName = fileName;
    }

    public CsvManager(String fileName, String delimiter) {
        createFolder();
        this.fileName = fileName;
        this.delimiter = delimiter;
    }

    private void createFolder() {
        File folder = new File(baseDir);
        if (!folder.exists())
            folder.mkdirs();
    }

    public void Write(String data) {
        String[] data_list = {data};
        this.Write(data_list);
    }

    public void Write(String[] data_list) {
        try {
            if (writer == null) {
                String filePath = baseDir + this.fileName;

                File file = new File(filePath);

                if (file.exists() && !file.isDirectory()) {
                    mFileWriter = new FileWriter(filePath, true);
                    writer = new CSVWriter(mFileWriter);
                } else {
                    mFileWriter = new FileWriter(filePath);
                    writer = new CSVWriter(mFileWriter);
                }
            }

            for (String data : data_list) {
                writer.writeNext(data.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void WriteAll(List<String[]> all) {
        try {

            String filePath = baseDir + this.fileName;

            File file = new File(filePath);

            if (file.exists() && !file.isDirectory()) {
                mFileWriter = new FileWriter(filePath, true);
                writer = new CSVWriter(mFileWriter);
            } else {
                mFileWriter = new FileWriter(filePath);
                writer = new CSVWriter(mFileWriter);
            }

            writer.writeAll(all);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
