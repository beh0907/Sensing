package com.coretec.sensing.utils;

import android.os.Environment;
import android.util.Log;

import com.coretec.sensing.model.Bluetooth;
import com.coretec.sensing.model.SensorData;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvManager {
    private String delimiter = ",";
    private String fileName = "AnalysisData.csv";
    private FileWriter mFileWriter;
    private CSVWriter writer;

    private String baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();

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

            String filePath = baseDir + this.fileName;

            File file = new File(filePath);

            if(file.exists() && !file.isDirectory()){
                mFileWriter = new FileWriter(filePath , true);
                writer = new CSVWriter(mFileWriter);
            }
            else {
                mFileWriter = new FileWriter(filePath);
                writer = new CSVWriter(mFileWriter);
            }

            for (String data : data_list) {
                writer.writeNext(data.split(","));
                writer.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }








    static public void sensorDataCsvSave(ArrayList<SensorData> sensorDataArrayList) {
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Download/Sensor_" + DateUtils.getCurrentCsvFileName() + ".csv";

        try (FileOutputStream fos = new FileOutputStream(fileName);
             OutputStreamWriter osw = new OutputStreamWriter(fos,
                     StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {

            List<String[]> entries = new ArrayList<>();
            String[] entry;

            entries.add(new String[]{"DATETIME", "acceleration X", "acceleration Y", "acceleration Z", "Geomagnetic X", "Geomagnetic Y", "Geomagnetic Z", "gyro X", "gyro Y", "gyro Z"});

            for (SensorData sensorData : sensorDataArrayList) {
                entry = new String[10];

                entry[0] = sensorData.getDateTime();
                entry[1] = Float.toString(sensorData.getAccelerometer()[0]);
                entry[2] = Float.toString(sensorData.getAccelerometer()[1]);
                entry[3] = Float.toString(sensorData.getAccelerometer()[2]);
                entry[4] = Float.toString(sensorData.getMagnetic()[0]);
                entry[5] = Float.toString(sensorData.getMagnetic()[1]);
                entry[5] = Float.toString(sensorData.getMagnetic()[1]);
                entry[6] = Float.toString(sensorData.getMagnetic()[2]);
                entry[7] = Float.toString(sensorData.getGyro()[0]);
                entry[8] = Float.toString(sensorData.getGyro()[1]);
                entry[9] = Float.toString(sensorData.getGyro()[2]);

                entries.add(entry);
            }

            writer.writeAll(entries);

            sensorDataArrayList.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void wifiCsvSave(ArrayList<String> rttArrayList) {
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Download/Wifi_" + DateUtils.getCurrentCsvFileName() + ".csv";

        try (FileOutputStream fos = new FileOutputStream(fileName);
             OutputStreamWriter osw = new OutputStreamWriter(fos,
                     StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {

            List<String[]> entries = new ArrayList<>();

            entries.add(new String[]{"SSID","BSSID","centerFreq0","centerFreq1","channelWidth","frequency","level","time"});

            for (String rtt : rttArrayList) {
                entries.add(rtt.split(","));
            }

            writer.writeAll(entries);

            rttArrayList.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void rttCsvSave(ArrayList<String> rttArrayList) {
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Download/Rtt_" + DateUtils.getCurrentCsvFileName() + ".csv";

        try (FileOutputStream fos = new FileOutputStream(fileName);
             OutputStreamWriter osw = new OutputStreamWriter(fos,
                     StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {

            List<String[]> entries = new ArrayList<>();

            entries.add(new String[]{"SSID","BSSID","Status","DistanceMm","DistanceStdDevMm","Rssi","Time","NumAttemptedMeasurements","NumSuccessfulMeasurements"});

            for (String rtt : rttArrayList) {
                entries.add(rtt.split(","));
            }

            writer.writeAll(entries);

            rttArrayList.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void bluetoothCsvSave(ArrayList<Bluetooth> bluetoothArrayList) {
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Download/Bluetooth_" + DateUtils.getCurrentCsvFileName() + ".csv";

        try (FileOutputStream fos = new FileOutputStream(fileName);
             OutputStreamWriter osw = new OutputStreamWriter(fos,
                     StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {

            List<String[]> entries = new ArrayList<>();
            String[] entry;

            entries.add(new String[]{"DATETIME", "SSID", "BSSID", "RSSI"});

            for (Bluetooth bluetooth : bluetoothArrayList) {
                entry = new String[4];

                entry[0] = bluetooth.getDateTime();
                entry[1] = bluetooth.getSsid();
                entry[2] = bluetooth.getBssid();
                entry[3] = bluetooth.getRssi();

                entries.add(entry);
            }

            writer.writeAll(entries);

            bluetoothArrayList.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
