package com.coretec.sensing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Ap {
    private int seq;
    private String name;
    private String macAddress;
    private Point point;
}
