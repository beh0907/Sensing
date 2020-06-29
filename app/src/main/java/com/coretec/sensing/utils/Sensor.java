package com.coretec.sensing.utils;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Sensor implements SensorEventListener {
    private boolean isStart = false;
    private Context context;

    private static SensorManager sensorManager;

    private static android.hardware.Sensor accelSensor;
    private static android.hardware.Sensor magneticSensor;
    private static android.hardware.Sensor gyroSensor;
    private static android.hardware.Sensor pressureSensor;
    private static android.hardware.Sensor temperatureSensor;
    private static android.hardware.Sensor humiditySensor;

    private float[] accelerometer = new float[3];
    private float[] magnetic = new float[3];
    private float[] gyro = new float[3];
    private float pressure;
    private float altitude;
    private float temperature;
    private float humidity;

    public Sensor(Context context) {
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        pressureSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_PRESSURE);
        temperatureSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE);
        humiditySensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY);
        this.context = context;
    }

    public boolean isStart() {
        return isStart;
    }

    public void start() {
        sensorManager.registerListener(this, accelSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, pressureSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, temperatureSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, humiditySensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isStart = true;
    }

    public void stop() {
        isStart = false;
    }

    public void end() {
        sensorManager.unregisterListener(this);
        isStart = false;
    }

    public float[] getAccelerometer() {
        return accelerometer;
    }

    public float[] getMagnetic() {
        return magnetic;
    }

    public float[] getGyro() {
        return gyro;
    }

    public float getPressure() {
        return pressure;
    }

    public float getAltitude() {
        return altitude;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {

            if (!isStart)
                return;

            switch (event.sensor.getType()) {
                case android.hardware.Sensor.TYPE_ACCELEROMETER :
                    accelerometer = event.values;
                    break;
                case android.hardware.Sensor.TYPE_MAGNETIC_FIELD :
                    magnetic = event.values;
                    break;
                case android.hardware.Sensor.TYPE_GYROSCOPE :
                    gyro = event.values;
                    break;
                case android.hardware.Sensor.TYPE_PRESSURE :
                    float temp = event.values[0];

                    //hPa단위
                    pressure = (float) (Math.round(temp * 100) / 100.0); //소수점 2자리 반올림

                    //기압을 바탕으로 고도 계산 m단위
                    float temp2 = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
                    altitude = (float) (Math.round(temp2 * 100) / 100.0);
                    break;
                case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE :
                    temperature = event.values[0];
                    break;
                case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY :
                    humidity = event.values[0];
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
    }
}