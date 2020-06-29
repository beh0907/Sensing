package com.coretec.sensing.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rtt implements Serializable {
    private String dateTime;
    private String bssid;
    private String ssid;
    private int status;
    private int distanceMm;
    private int distanceStdDevMm;
    private int rssi;
    private long timeStamp;
    private int numAttemptedMeasurements;
    private int numSuccessfulMeasurements;
}
