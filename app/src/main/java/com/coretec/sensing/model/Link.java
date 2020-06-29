package com.coretec.sensing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Link {
    private int seq;
    private int node_start;
    private int node_end;
    private int weight_p;
    private double weight_m;
}
