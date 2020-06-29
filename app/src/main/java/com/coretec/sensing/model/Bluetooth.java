package com.coretec.sensing.model;


import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bluetooth implements Serializable {
    private String dateTime;
    private String ssid;
    private String bssid;
    private String rssi;
}
