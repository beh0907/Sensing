package com.coretec.sensing.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.coretec.sensing.R;
import com.coretec.sensing.activity.LoggingActivity;
import com.coretec.sensing.databinding.FragmentSensorBinding;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;
import com.coretec.sensing.utils.GpsTracker;
import com.coretec.sensing.utils.Sensor;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class SensorFragment extends Fragment {

    private static TimerTask sensorTimer;
    private LoggingActivity loggingActivity;

    //    private ArrayList<SensorData> sensorDataArrayList;
    private FragmentSensorBinding sensorBinding;
    private GpsTracker gpsTracker;
    private Sensor sensor;
    private CsvManager csvManager;

    private boolean isLogging = false;

    private TelephonyManager telephonyManager;

    //프래그먼트에 쓸 객체 리시브
    //프래그먼트에 쓸 객체는 bundle로 arguments 저장을 해야 함
    public static SensorFragment newInstance() {
        SensorFragment frag = new SensorFragment();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource file
        sensorBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_sensor, container, false);
        View view = sensorBinding.getRoot();
        view.setTag(2);

        loggingActivity = ((LoggingActivity) getActivity());
        sensor = new Sensor(getContext());

        telephonyManager = (TelephonyManager) getContext().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void createCsvFile(String fileName) {
        csvManager = new CsvManager("/" + fileName + "_Sensor.csv");
        csvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,acceleration X,acceleration Y,acceleration Z,Geomagnetic X,Geomagnetic Y,Geomagnetic Z,gyro X,gyro Y,gyro Z,Pressure(hPa),Altitude(m),Temperature,Humidity,GPSLatitude,GPSLongitude,GPSAltitude");
    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    public void startSensor(int delay) {
        gpsTracker = new GpsTracker(getContext(), delay);

        stopSensor();

        //센서 객체 초기화
        sensor.start();

        sensorTimer = new TimerTask() {
            @SuppressLint("MissingPermission")
            public void run() {
                String currentDateTime = DateUtils.getCurrentDateTime();
                float[] accelerometer = sensor.getAccelerometer();
                float[] magnetic = sensor.getMagnetic();
                float[] gyro = sensor.getGyro();
                float pressure = sensor.getPressure();
                float altitude = sensor.getAltitude();
                float temperature = sensor.getTemperature();
                float humidity = sensor.getHumidity();
                long runtime = loggingActivity.getRuntime();
                int ptNum = loggingActivity.getPtNum();
                int Status = loggingActivity.getStatus();

                double gpsLatitude = gpsTracker.getLatitude();
                double gpsLongitude = gpsTracker.getLongitude();
                double gpsAltitude = gpsTracker.getAltitude();


                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {

                        if (isLogging && csvManager != null) {
                            csvManager.Write(currentDateTime + "," + runtime + "," + ptNum + "," + Status + "," + accelerometer[0] + "," + accelerometer[1] + "," + accelerometer[2] + "," + magnetic[0] + "," + magnetic[1] + "," + magnetic[2] + "," + gyro[0] + "," + gyro[1] + "," + gyro[2] + "," + pressure + "," + altitude + "," + temperature + "," + humidity + "," + gpsLatitude + "," + gpsLongitude + "," + gpsAltitude);

                            sensorBinding.txtAccelerometerX.setText(String.format("%.6f", accelerometer[0]));
                            sensorBinding.txtAccelerometerY.setText(String.format("%.6f", accelerometer[1]));
                            sensorBinding.txtAccelerometerZ.setText(String.format("%.6f", accelerometer[2]));

                            sensorBinding.txtMagneticX.setText(String.format("%.6f", magnetic[0]));
                            sensorBinding.txtMagneticY.setText(String.format("%.6f", magnetic[1]));
                            sensorBinding.txtMagneticZ.setText(String.format("%.6f", magnetic[2]));

                            sensorBinding.txtGyroX.setText(String.format("%.6f", gyro[0]));
                            sensorBinding.txtGyroY.setText(String.format("%.6f", gyro[1]));
                            sensorBinding.txtGyroZ.setText(String.format("%.6f", gyro[2]));

                            sensorBinding.txtGpsLatitude.setText(String.format("%.6f", gpsLatitude));
                            sensorBinding.txtGpsLongitude.setText(String.format("%.6f", gpsLongitude));
                            sensorBinding.txtGpsAltitude.setText(String.format("%.6f", gpsAltitude));

                            sensorBinding.txtPressure.setText(pressure + " hPa");
                            sensorBinding.txtAltitude.setText(altitude + " m");

                            sensorBinding.txtTemperature.setText(temperature + " °C");
                            sensorBinding.txtHumidity.setText(humidity + " %");
                        }
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(sensorTimer, 0, delay);
    }

    public void stopSensor() {
        if (sensor.isStart()) {
            gpsTracker.stop();
            sensor.stop();
            sensorTimer.cancel();
            sensorTimer = null;
        }
    }

    public void endSensor() {
        if (sensor.isStart()) {
            gpsTracker.stop();
            sensor.end();
            sensorTimer.cancel();
            sensorTimer = null;
        }
    }
}
