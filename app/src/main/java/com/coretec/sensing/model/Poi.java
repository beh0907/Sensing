package com.coretec.sensing.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Poi implements Serializable {
    private int seq;
    private String name;
    private Point point;
}
