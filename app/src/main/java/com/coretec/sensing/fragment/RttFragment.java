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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coretec.sensing.R;
import com.coretec.sensing.activity.LoggingActivity;
import com.coretec.sensing.adapter.WifiAdapter;
import com.coretec.sensing.databinding.FragmentRttBinding;
import com.coretec.sensing.dialog.LoadingDialog;
import com.coretec.sensing.model.Rtt;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RttFragment extends Fragment {
    final Handler rttRequestDelayer = new Handler();
    final Handler wifiRequestDelayer = new Handler();

    private LoggingActivity loggingActivity;

    private FragmentRttBinding rttBinding;

//    private ArrayList<String> wifiArrayListData;
//    private ArrayList<String> rttArrayListData;

    private HashMap<String, RangingResult> accessPointsSupporting80211mcInfo;
    private ArrayList<ScanResult> accessPointsSupporting80211mc;
    private ArrayList<ScanResult> accessPoints;

    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;

    private WifiRttManager wifiRttManager;
    private RttRangingResultCallback rttRangingResultCallback;

    private WifiAdapter wifiAdapter;

    private CsvManager wifiCsvManager;
    private CsvManager rttCsvManager;

    private boolean wifiScanning = false;
    private boolean rttScanning = false;

    private boolean isWifiLogging = false;
    private boolean isRttLogging = false;

    private Map<String, Rtt> buffer;

    private static TimerTask wifiTimer;
    private static TimerTask rttTimer;

    private long wifiScanMillisecondDelay;
    private long rttScanMillisecondDelay;

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

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loggingActivity = ((LoggingActivity) getActivity());
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
        accessPointsSupporting80211mc = new ArrayList<>();
        accessPointsSupporting80211mcInfo = new HashMap<>();

        wifiAdapter = new WifiAdapter(accessPoints, accessPointsSupporting80211mc);
        rttBinding.listWifi.setAdapter(wifiAdapter);

        wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();

        wifiRttManager = (WifiRttManager) getActivity().getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

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

    public void findAccessPoints() {
        boolean isScan = wifiManager.startScan();

        if (isScan) {
            if (!isRttLogging)
                Toast.makeText(getContext(), "와이파이를 조회하고 있습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "스캔 한도가 초과하여 스캔에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void createWifiCsvFile(String fileName) {
        wifiCsvManager = new CsvManager(fileName + "_WIFI.csv");
        wifiCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,SSID,BSSID,centerFreq0,centerFreq1,channelWidth,frequency,level");
    }

    public void createRttCsvFile(String fileName) {
        rttCsvManager = new CsvManager(fileName + "_RTT.csv");
        rttCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,SSID,BSSID,RttStatus,Distance(Mm),DistanceStdDev(Mm),Rssi,timestamp,NumAttemptedMeasurements,NumSuccessfulMeasurements");
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
                requireActivity().runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
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
                requireActivity().runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
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

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT))
            return;

        //체크된 RTT지원 항목만 얻음
        ArrayList<ScanResult> results = wifiAdapter.getWifiAccessPointsWithRtt();

        //RTT 리스트가 1개 이상 있을경우 콜백 이벤트 등록
        if (results.size() > 0) {
            RangingRequest rangingRequest =
                    new RangingRequest.Builder().addAccessPoints(results).build();
//                    new RangingRequest.Builder().addAccessPoints(accessPointsSupporting80211mc).build();


            wifiRttManager.startRanging(rangingRequest, getActivity().getApplication().getMainExecutor(), rttRangingResultCallback);
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

            boolean isScannedResult = false;

            for (int i = 0; i < accessPointsSupporting80211mc.size(); i++) {
                ScanResult rttResult = accessPointsSupporting80211mc.get(i);
                if (rttResult.BSSID.equals(scanResult.BSSID)) {
                    isScannedResult = true;
                    accessPointsSupporting80211mc.set(i, scanResult);
                    break;
                }
            }

            if (!isScannedResult) {
                if (scanResult.is80211mcResponder()) {
                    accessPointsSupporting80211mc.add(scanResult);
                }

                if (accessPointsSupporting80211mc.size() >= RangingRequest.getMaxPeers()) {
                    break;
                }
            }
        }
    }

    private void recordCsvBuffer() {

        ArrayList<ScanResult> tempList = new ArrayList<>();

        tempList.addAll(accessPointsSupporting80211mc);
        tempList.addAll(accessPoints);

        String dateTime = DateUtils.getCurrentDateTime();

        long runTime = loggingActivity.getRuntime();
        int ptNum = loggingActivity.getPtNum();

        buffer.clear();

        for (int i = 0; i < tempList.size(); i++) {
            ScanResult scanResult = tempList.get(i);

            String writeData = dateTime;
            writeData += "," + runTime;
            writeData += "," + ptNum;
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

                findAccessPoints(newList);
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
            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)

            //if (list.size() == 1) {
            if (!rttScanning)
                return;

            String dateTime = DateUtils.getCurrentDateTime();

            long runTime = loggingActivity.getRuntime();
            int ptNum = loggingActivity.getPtNum();

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

                    } else {
                        if (rtt != null) {
                            String data = dateTime;
                            data += "," + runTime;
                            data += "," + ptNum;
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
        }
    }

    public void csvClose() {
        if (wifiCsvManager != null) {
            wifiCsvManager.close();
            wifiCsvManager = null;
        }

        if (rttCsvManager != null) {
            rttCsvManager.close();
            rttCsvManager = null;
        }
    }
}
