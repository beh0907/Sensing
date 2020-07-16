package com.coretec.sensing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Poi {
    private int seq;
    private String name;
    private Point point;
}
