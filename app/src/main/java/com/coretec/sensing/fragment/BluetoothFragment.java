package com.coretec.sensing.fragment;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.coretec.sensing.R;
import com.coretec.sensing.activity.LoggingActivity;
import com.coretec.sensing.adapter.BluetoothListAdapter;
import com.coretec.sensing.databinding.FragmentBluetoothBinding;
import com.coretec.sensing.listener.RecyclerViewClickListener;
import com.coretec.sensing.model.Bluetooth;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothFragment extends Fragment {

    private LoggingActivity loggingActivity;

    private FragmentBluetoothBinding bluetoothBinding;

    private ArrayList<Bluetooth> bluetoothArrayList;
    private BluetoothListAdapter bluetoothListAdapter;

    private HashMap<String, android.bluetooth.le.ScanResult> bluetoothListHashMap;
    private HashMap<String, android.bluetooth.le.ScanResult> bluetoothLoggingHashMap;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private String[] bluetoothFilterBssid;

    private boolean isLogging = false;

    private ScanCallback leScanCallback;

    private CsvManager csvManager;

    private static TimerTask bluetoothTimer;

    //프래그먼트에 쓸 객체 리시브
    //프래그먼트에 쓸 객체는 bundle로 arguments 저장을 해야 함
    public static BluetoothFragment newInstance() {
        BluetoothFragment frag = new BluetoothFragment();
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
        bluetoothBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_bluetooth, container, false);
        View view = bluetoothBinding.getRoot();
        view.setTag(1);

        initList();
        initBluetooth();

        loggingActivity = ((LoggingActivity) getActivity());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    private void initList() {
        bluetoothArrayList = new ArrayList<>();
        bluetoothListHashMap = new HashMap<>();
        bluetoothLoggingHashMap = new HashMap<>();

        bluetoothListAdapter = new BluetoothListAdapter(bluetoothArrayList, new RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        bluetoothBinding.listBluetooth.setLayoutManager(linearLayoutManager);
        bluetoothBinding.listBluetooth.setAdapter(bluetoothListAdapter);
        bluetoothListAdapter.notifyDataSetChanged();
    }

    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                super.onScanResult(callbackType, result);
                String address = result.getDevice().getAddress();

                if (address == null) {
                    address = "";
                }

                for (String filterAddress : bluetoothFilterBssid) {
                    if (address.startsWith(filterAddress)) {
                        bluetoothListHashMap.put(result.getDevice().getAddress(), result);
                        bluetoothLoggingHashMap.put(result.getDevice().getAddress(), result);
                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    public void setBluetoothFilterBssid(String[] bluetoothFilterBssid) {
        this.bluetoothFilterBssid = bluetoothFilterBssid;
        bluetoothListHashMap.clear();
        bluetoothLoggingHashMap.clear();
        refreshBluetoothList();
    }

    private void refreshBluetoothList() {
        if (bluetoothArrayList.size() != 0) {
            bluetoothArrayList.clear();
        }


//        long runTime = loggingActivity.getRuntime();
//        int ptNum = loggingActivity.getPtNum();
//        int Status = loggingActivity.getStatus();
//
//        for (ScanResult result : bluetoothListHashMap.values()) {
//            Bluetooth bluetooth = new Bluetooth(currentTime, result.getDevice().getName(), result.getDevice().getAddress(), String.valueOf(result.getRssi()));
//            bluetoothArrayList.add(bluetooth);
//
//            if (isLogging && csvManager != null)
//                csvManager.Write(currentTime + "," + runTime + "," + ptNum + "," + Status + "," + bluetooth.getSsid() + "," + bluetooth.getBssid() + "," + bluetooth.getRssi());
//        }

        String currentTime = DateUtils.getCurrentDateTime();

        for (ScanResult result : bluetoothListHashMap.values()) {
            Bluetooth bluetooth = new Bluetooth(currentTime, result.getDevice().getName(), result.getDevice().getAddress(), String.valueOf(result.getRssi()));
            bluetoothArrayList.add(bluetooth);
        }

        long runTime = loggingActivity.getRuntime();
        int ptNum = loggingActivity.getPtNum();
        int Status = loggingActivity.getStatus();

        for (ScanResult result : bluetoothLoggingHashMap.values()) {
            currentTime = DateUtils.getTimeStampToDateTime(result.getTimestampNanos());

            if (isLogging && csvManager != null)
                csvManager.Write(currentTime + "," + runTime + "," + ptNum + "," + Status + "," + result.getDevice().getName() + "," + result.getDevice().getAddress() + "," + result.getRssi());
        }

        bluetoothLoggingHashMap.clear();
        bluetoothListAdapter.refreshList(bluetoothArrayList);
    }

    public void createCsvFile(String fileName) {
        csvManager = new CsvManager( fileName + "_Bluetooth.csv");
        csvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,RSSI");
    }

    public void bluetoothStartScanning(int delay) {
        bluetoothStopScanning();

        bluetoothLeScanner.startScan(leScanCallback);

        bluetoothTimer = new TimerTask() {
            public void run() {
                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        bluetoothFilterBssid = loggingActivity.getBleFilterBssid();
                        refreshBluetoothList();
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(bluetoothTimer, 0, delay);
    }

    public void bluetoothStopScanning() {
        if (bluetoothTimer != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            bluetoothTimer.cancel();
            bluetoothTimer = null;
        }
    }
}
