package com.coretec.sensing.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coretec.sensing.R;
import com.coretec.sensing.activity.LoggingActivity;
import com.coretec.sensing.adapter.WifiAdapter;
import com.coretec.sensing.databinding.FragmentRttBinding;
import com.coretec.sensing.dialog.LoadingDialog;
import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Rtt;
import com.coretec.sensing.sqlite.ApHelper;
import com.coretec.sensing.utils.Calculation;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;
import com.coretec.sensing.utils.Sort;
import com.github.dakusui.combinatoradix.Combinator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class RttFragment extends Fragment {
    private static TimerTask wifiTimer;
    private static TimerTask rttTimer;

    private LoggingActivity loggingActivity;
    private FragmentRttBinding rttBinding;

    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private WifiRttManager wifiRttManager;
    private WifiAdapter wifiAdapter;

    private RttRangingResultCallback rttRangingResultCallback;

    private ArrayList<ScanResult> accessPoints;
    private HashMap<String, ScanResult> accessPointsSupporting80211mc;
    private HashMap<String, RangingResult> accessPointsSupporting80211mcInfo;

    private CsvManager wifiCsvManager;
    private CsvManager rttCsvManager;
    private CsvManager rttSendCsvManager;

    private boolean wifiScanning = false;
    private boolean rttScanning = false;
    private boolean isWifiLogging = false;
    private boolean isRttLogging = false;

    private Map<String, Rtt> buffer;

    private long wifiScanMillisecondDelay;
    private long rttScanMillisecondDelay;


    private ApHelper apHelper = new ApHelper();
    private HashMap<String, Ap> apHashMap;

    //프래그먼트에 쓸 객체 리시브
    //프래그먼트에 쓸 객체는 bundle로 arguments 저장을 해야 함
    public static RttFragment newInstance() {
        RttFragment frag = new RttFragment();
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
        rttBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_rtt, container, false);
        View view = rttBinding.getRoot();
        view.setTag(0);

        initList();
        initRtt();

        loggingActivity = ((LoggingActivity) getActivity());

        apHashMap = apHelper.selectAllApList();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(
                wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(wifiScanReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wifiScanning = false;
        rttScanning = false;
    }

    private void initList() {
        rttBinding.listWifi.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rttBinding.listWifi.setLayoutManager(layoutManager);
    }

    private void initRtt() {
        accessPoints = new ArrayList<>();
        accessPointsSupporting80211mc = new HashMap<>();
        accessPointsSupporting80211mcInfo = new HashMap<>();

        wifiAdapter = new WifiAdapter(accessPoints, accessPointsSupporting80211mc);
        rttBinding.listWifi.setAdapter(wifiAdapter);

        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();

        wifiRttManager = (WifiRttManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        rttRangingResultCallback = new RttRangingResultCallback();

        buffer = new HashMap<>();

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    public void setWifiLogging(boolean wifiLogging) {
        isWifiLogging = wifiLogging;
    }

    public void setRttLogging(boolean rttLogging) {
        isRttLogging = rttLogging;
    }

    public void createWifiCsvFile(String fileName) {
        wifiCsvManager = new CsvManager(fileName + "_WIFI.csv");
        wifiCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,centerFreq0,centerFreq1,channelWidth,frequency,level");
    }

    public void createRttCsvFile(String fileName) {
        rttCsvManager = new CsvManager(fileName + "_RTT.csv");
        rttCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,RttStatus,Distance(Mm),DistanceStdDev(Mm),Rssi,timestamp,NumAttemptedMeasurements,NumSuccessfulMeasurements");

        rttSendCsvManager = new CsvManager(fileName + "_RTT_SEND.csv");
        rttSendCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),MacAddress");
    }

    public void wifiStartScanning(int delay) {
        wifiStopScanning();

        wifiScanning = true;
        wifiScanMillisecondDelay = delay;
        wifiScanDelayRequest();
    }

    public void rttStartScanning(int delay) {
        rttStopScanning();

        rttScanning = true;
        rttScanMillisecondDelay = delay;
        rttScanDelayRequest();
    }

    public void wifiStopScanning() {
        if (wifiTimer != null) {
            wifiScanning = false;
            wifiTimer.cancel();
            wifiTimer = null;
        }
    }

    public void rttStopScanning() {
        if (rttTimer != null) {
            rttScanning = false;
            rttTimer.cancel();
            rttTimer = null;
        }
    }


    //2분 4회의 wifi 스캔 제한이 있기 떄문에
    //30초에 한번식 강제적으로 스캔 시도
    private void wifiScanDelayRequest() {
        wifiTimer = new TimerTask() {
            public void run() {
                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        if (wifiScanning) {
                            findAccessPoints();
//                            wifiScanDelayRequest();
                        }
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(wifiTimer, 0, wifiScanMillisecondDelay);
    }

    private void rttScanDelayRequest() {
        rttTimer = new TimerTask() {
            public void run() {
                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        if (rttScanning) {
                            startRangingRequest();
                        }
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(rttTimer, 0, rttScanMillisecondDelay);
    }

    private void startRangingRequest() {
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT))
            return;

        //체크된 RTT지원 항목만 얻음
        ArrayList<ScanResult> results = wifiAdapter.getWifiAccessPointsWithRtt();

        //RTT 리스트가 1개 이상 있을경우 콜백 이벤트 등록
        if (results.size() > 0) {
            //1개씩 9번 로깅
//            for (int i = 0; i < results.size(); i++) {
//                ScanResult scanResult = results.get(i);
//
//                ArrayList<ScanResult> list = new ArrayList<>();
//                list.add(scanResult);
//
//                rttSendCsvManager.Write(DateUtils.getCurrentDateTime() + "," + loggingActivity.getRuntime() + "," + scanResult.BSSID);
//
//                RangingRequest rangingRequest =
//                        new RangingRequest.Builder().addAccessPoints(list).build();
//
//                wifiRttManager.startRanging(rangingRequest, getActivity().getApplication().getMainExecutor(), rttRangingResultCallback);
//            }

            //3개씩 3번 로깅
//            String macAddressList = "";
//            ArrayList<ScanResult> list = new ArrayList<>();
//
//            for (int i = 0; i < results.size(); i++) {
//                ScanResult scanResult = results.get(i);
//
//                list.add(scanResult);
//                macAddressList += scanResult.BSSID + " ";
//
//                if (list.size() == 3) {
//                    rttSendCsvManager.Write(DateUtils.getCurrentDateTime() + "," + loggingActivity.getRuntime() + "," + macAddressList);
//
//                    RangingRequest rangingRequest =
//                            new RangingRequest.Builder().addAccessPoints(list).build();
//
//                    wifiRttManager.startRanging(rangingRequest, getActivity().getApplication().getMainExecutor(), rttRangingResultCallback);
//
//                    macAddressList = "";
//                    list = new ArrayList<>();
//                }
//            }

            //전체를 한번에 로깅
            String macAddressList = "";

            for (ScanResult scanResult : results)
                macAddressList += scanResult.BSSID + " ";

            rttSendCsvManager.Write(DateUtils.getCurrentDateTime() + "," + loggingActivity.getRuntime() + "," + macAddressList);

            RangingRequest rangingRequest =
                    new RangingRequest.Builder().addAccessPoints(results).build();

            wifiRttManager.startRanging(rangingRequest, getActivity().getApplication().getMainExecutor(), rttRangingResultCallback);
        }
//        }
    }


    public void findAccessPoints() {
        boolean isScan = wifiManager.startScan();

        if (isScan) {
            if (!isRttLogging)
                Toast.makeText(getContext(), "와이파이를 조회하고 있습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "스캔 한도가 초과하여 스캔에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void findAccessPoints(@NonNull List<ScanResult> originalList) {
        accessPoints.clear();

        for (ScanResult scanResult : originalList) {
            if (!scanResult.is80211mcResponder()) {
                accessPoints.add(scanResult);
            }
        }
    }

    private void find80211mcSupportedAccessPoints(@NonNull List<ScanResult> originalList) {
        for (ScanResult scanResult : originalList) {
            if (accessPointsSupporting80211mc.size() >= RangingRequest.getMaxPeers()) {
                return;
            }

            if (scanResult.is80211mcResponder()) {
                accessPointsSupporting80211mc.put(scanResult.BSSID, scanResult);
            }
        }
    }

    private void recordCsvBuffer() {
        ArrayList<ScanResult> tempList = new ArrayList<>();

        tempList.addAll(accessPointsSupporting80211mc.values());
        tempList.addAll(accessPoints);

        String dateTime = DateUtils.getCurrentDateTime();

        long runTime = loggingActivity.getRuntime();
        int ptNum = loggingActivity.getPtNum();
        int Status = loggingActivity.getStatus();

        buffer.clear();

        for (int i = 0; i < tempList.size(); i++) {
            ScanResult scanResult = tempList.get(i);

            String writeData = dateTime;
            writeData += "," + runTime;
            writeData += "," + ptNum;
            writeData += "," + Status;
            writeData += "," + scanResult.SSID;
            writeData += "," + scanResult.BSSID;

            Rtt rtt = new Rtt();
            rtt.setSsid(scanResult.SSID);
            rtt.setBssid(scanResult.BSSID);

            //RTT 지원기기일 경우 SSID와 BSSID 저장
            if (scanResult.is80211mcResponder()) {
                buffer.put(scanResult.BSSID, rtt);
            }

            writeData += "," + scanResult.centerFreq0;
            writeData += "," + scanResult.centerFreq1;
            writeData += "," + scanResult.channelWidth;
            writeData += "," + scanResult.frequency;
            writeData += "," + scanResult.level;

//            wifiArrayListData.add(writeData);
            if (isWifiLogging && wifiCsvManager != null)
                wifiCsvManager.Write(writeData);
        }
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = wifiManager.getScanResults();

            if (scanResults != null) {

                List<ScanResult> newList = scanResults;
                for (ScanResult ap : accessPoints) {
                    boolean flag = false;
                    for (ScanResult sr : scanResults) {
                        if (ap.BSSID.equals(sr.BSSID)) {
                            flag = true;
                        }
                    }

                    if (!flag) {
                        newList.add(0, ap);
                        if (newList.size() >= RangingRequest.getMaxPeers()) {
                            break;
                        }
                    }
                }

                //RTT를 지원하지 않는 WIFI 객체 리스트 저장
                findAccessPoints(newList);

                //RTT를 지원하는 WIFI 객체 리스트 저장
                find80211mcSupportedAccessPoints(newList);

                recordCsvBuffer();

                wifiAdapter.swapData(accessPoints, accessPointsSupporting80211mc, accessPointsSupporting80211mcInfo);
                LoadingDialog.hideDialog();
            }
        }
    }

    private class RttRangingResultCallback extends RangingResultCallback {
        @Override
        public void onRangingFailure(int code) {
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
//            Log.d("와이파이 RTT 데이터 테스트 태그", list.toString());
            if (!rttScanning)
                return;

            String dateTime = DateUtils.getCurrentDateTime();

            long runTime = loggingActivity.getRuntime();
            int ptNum = loggingActivity.getPtNum();
            int Status = loggingActivity.getStatus();

            String myLocationLog = dateTime + "," + runTime + ",";

            for (int i = 0; i < list.size(); i++) {
                if (isRttLogging) {
                    RangingResult rangingResult = list.get(i);

                    String key = rangingResult.getMacAddress().toString();
                    Rtt rtt = buffer.get(key);

                    if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {

                        if (rtt != null) {
                            String data = dateTime;
                            data += "," + runTime;
                            data += "," + ptNum;
                            data += "," + Status;
                            data += "," + rtt.getSsid();
                            data += "," + rtt.getBssid();
                            data += "," + rangingResult.getStatus();
                            data += "," + rangingResult.getDistanceMm();
                            data += "," + rangingResult.getDistanceStdDevMm();
                            data += "," + rangingResult.getRssi();
                            data += "," + rangingResult.getRangingTimestampMillis();
                            data += "," + rangingResult.getNumAttemptedMeasurements();
                            data += "," + rangingResult.getNumSuccessfulMeasurements();

                            if (rttCsvManager != null)
                                rttCsvManager.Write(data);
                        }

                        Ap ap = apHashMap.get(rangingResult.getMacAddress().toString());
                        String name = ap.getName();
                        double pointX = ap.getPoint().getX();
                        double pointY = ap.getPoint().getY();
                        double distance = rangingResult.getDistanceMm() / 1000f;

                        myLocationLog += name + "," + pointX + "," + pointY + "," + distance + ",";
                    } else {
                        if (rtt != null) {
                            String data = dateTime;
                            data += "," + runTime;
                            data += "," + ptNum;
                            data += "," + Status;
                            data += "," + rtt.getSsid();
                            data += "," + rtt.getBssid();
                            data += "," + rangingResult.getStatus();

                            if (rttCsvManager != null)
                                rttCsvManager.Write(data);
                        }
                    }

                    accessPointsSupporting80211mcInfo.put(rangingResult.getMacAddress().toString(), rangingResult);
                }
            }

//            Log.d("와이파이 RTT 데이터 테스트 태그", accessPointsSupporting80211mcInfo.toString());
            wifiAdapter.swapData(accessPoints, accessPointsSupporting80211mc, accessPointsSupporting80211mcInfo);

//            double[][] myLocation = Calculation.getMyLocation(accessPointsSupporting80211mcInfo, apHashMap);
//
//            if (myLocation != null) {
//                myLocationLog += myLocation[0][0] + "," + myLocation[1][0];
//
////                Log.d("RTT 실내위치 측위", "RTT 스캔 데이터 : " + accessPointsSupporting80211mcInfo.toString());
////                Log.d("RTT 실내위치 측위", "AP 정보 : " + apHashMap.toString());
////                Log.d("RTT 실내위치 측위", "내 위치 : (" + myLocation[0][0] + "m," + myLocation[1][0] + "m)");
//                Log.d("RTT 실내위치 측위", myLocationLog);
//            }
            ArrayList<Double> locationXList = new ArrayList<>();
            ArrayList<Double> locationYList = new ArrayList<>();
            Sort.Ascending ascending = new Sort.Ascending();

            for (List<RangingResult> each : new Combinator<>(list, 4)) {
                double[][] myLocation = Calculation.getMyLocation(each, apHashMap);
                locationXList.add(myLocation[0][0]);
                locationYList.add(myLocation[1][0]);
            }
            Collections.sort(locationXList, ascending);
            Collections.sort(locationYList, ascending);

            myLocationLog += locationXList.get(locationXList.size() / 2) + "," + locationXList.get(locationYList.size() / 2);
            Log.d("RTT 실내위치 측위", myLocationLog);
        }
    }
}
