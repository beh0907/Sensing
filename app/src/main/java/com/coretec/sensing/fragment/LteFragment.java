package com.coretec.sensing.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coretec.sensing.R;
import com.coretec.sensing.activity.LoggingActivity;
import com.coretec.sensing.databinding.FragmentLteBinding;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

public class LteFragment extends Fragment {

    private static TimerTask lteTimer;
    private LoggingActivity loggingActivity;

    private FragmentLteBinding sensorBinding;

    private CsvManager csvCellIdentityLteManager;
    private CsvManager csvCellSignalStrengthLteManager;

    private boolean isLogging = false;

    private TelephonyManager telephonyManager;

    //프래그먼트에 쓸 객체 리시브
    //프래그먼트에 쓸 객체는 bundle로 arguments 저장을 해야 함
    public static LteFragment newInstance() {
        LteFragment frag = new LteFragment();
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
        sensorBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_lte, container, false);
        View view = sensorBinding.getRoot();
        view.setTag(3);

        loggingActivity = ((LoggingActivity) getActivity());
        telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void createCsvFile(String fileName) {
        //LTE
        csvCellIdentityLteManager = new CsvManager(fileName + "_CellIdentityLte.csv");
        csvCellIdentityLteManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,BandWidth,Ci,Earfcn,Mcc,Mnc,NetworkOperator,Pci,Tac");
        csvCellSignalStrengthLteManager = new CsvManager(fileName + "_CellSignalStrengthLte.csv");

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            csvCellSignalStrengthLteManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,AsuLevel,Cqi,dBm,Level,Rsrp,Rsrq,Rssi,Rssnr,TimingAdvance");
        } else {
            csvCellSignalStrengthLteManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,AsuLevel,Cqi,dBm,Level,Rsrp,Rsrq,Rssnr,TimingAdvance");
        }

    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    public void startScanning(int delay) {
        stopScanning();

        lteTimer = new TimerTask() {
            @SuppressLint("MissingPermission")
            public void run() {
                String currentDateTime = DateUtils.getCurrentDateTime();
                long runtime = loggingActivity.getRuntime();
                int ptNum = loggingActivity.getPtNum();


                requireActivity().runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    public void run() {

                        if (isLogging && csvCellIdentityLteManager != null && csvCellSignalStrengthLteManager != null) {
                            try {
                                List<CellInfo> list = telephonyManager.getAllCellInfo();

                                if (list != null) {
                                    for (CellInfo info : list) {
                                        if (info instanceof CellInfoLte) {
                                            CellInfoLte cellInfoLte = (CellInfoLte) info;

                                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                                            CellIdentityLte identityLte = cellInfoLte.getCellIdentity();

                                            sensorBinding.txtCi.setText(identityLte.getCi() + "");
                                            sensorBinding.txtPci.setText(identityLte.getPci() + "");
                                            sensorBinding.txtRsrp.setText(cellSignalStrengthLte.getRsrp() + "");

                                            csvCellIdentityLteManager.Write(currentDateTime + "," + runtime + "," + ptNum + "," + identityLte.getBandwidth() + "," + identityLte.getCi() + "," + identityLte.getEarfcn() + "," + identityLte.getMccString() + "," + identityLte.getMncString() + "," + identityLte.getMobileNetworkOperator() + "," + identityLte.getPci() + "," + identityLte.getTac());

                                            if (android.os.Build.VERSION.SDK_INT >= 29) {
                                                csvCellSignalStrengthLteManager.Write(currentDateTime + "," + runtime + "," + ptNum + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getCqi() + "," + cellSignalStrengthLte.getDbm() + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getRsrp() + "," + cellSignalStrengthLte.getRsrq() + "," + cellSignalStrengthLte.getRssi() + "," + cellSignalStrengthLte.getRssnr() + "," + cellSignalStrengthLte.getTimingAdvance());
                                            } else {
                                                csvCellSignalStrengthLteManager.Write(currentDateTime + "," + runtime + "," + ptNum + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getCqi() + "," + cellSignalStrengthLte.getDbm() + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getRsrp() + "," + cellSignalStrengthLte.getRsrq() + "," + +cellSignalStrengthLte.getRssnr() + "," + cellSignalStrengthLte.getTimingAdvance());
                                            }
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        };
        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(lteTimer, 0, delay);
    }

    public void stopScanning() {
        if (lteTimer != null) {
            lteTimer.cancel();
            lteTimer = null;
        }
    }

    public void endScanning() {
        if (lteTimer != null) {
            lteTimer.cancel();
            lteTimer = null;
        }
    }

    public void csvClose() {
        if (csvCellIdentityLteManager != null) {
            csvCellIdentityLteManager.close();
            csvCellIdentityLteManager = null;
        }

        if (csvCellSignalStrengthLteManager != null) {
            csvCellSignalStrengthLteManager.close();
            csvCellSignalStrengthLteManager = null;
        }
    }
}
