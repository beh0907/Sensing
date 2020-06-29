package com.coretec.sensing.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorData implements Serializable {
    private String dateTime;
    private float[] accelerometer;
    private float[] magnetic;
    private float[] gyro;
}
