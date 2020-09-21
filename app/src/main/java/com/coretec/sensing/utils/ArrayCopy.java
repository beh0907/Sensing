package com.coretec.sensing.utils;

import android.net.wifi.rtt.RangingResult;

import java.util.HashMap;
import java.util.Map;

public class ArrayCopy {
    public static HashMap<String, RangingResult> deepCopyHashMap(HashMap<String, RangingResult> originMap, int removeInterval) {
        HashMap<String, RangingResult> entryMap = new HashMap<>();

        for (Map.Entry<String, RangingResult> entry : originMap.entrySet()) {
            RangingResult rangingResult = entry.getValue();

            if (rangingResult.getStatus() == RangingResult.STATUS_FAIL)
                continue;

            if (DateUtils.isRemoveResult(rangingResult, removeInterval))
                continue;

            entryMap.put(entry.getKey(), entry.getValue());
        }

        return entryMap;
    }

}
