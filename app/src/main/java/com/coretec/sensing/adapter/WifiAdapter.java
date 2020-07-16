package com.coretec.sensing.adapter;

import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.coretec.sensing.R;

import java.util.ArrayList;
import java.util.HashMap;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {

    private ArrayList<ScanResult> wifiAccessPoints;
    private ArrayList<ScanResult> wifiAccessPointsWithRtt;

    private HashMap<String, RangingResult> wifiAccessPointsWithRttInfo;
    private HashMap<String, Boolean> unCheckedMap;

    public WifiAdapter(ArrayList<ScanResult> wifiAccessPoints, HashMap<String, ScanResult> wifiAccessPointsWithRtt) {
        this.wifiAccessPoints = wifiAccessPoints;
        this.wifiAccessPointsWithRtt = new ArrayList<>(wifiAccessPointsWithRtt.values());
        wifiAccessPointsWithRttInfo = new HashMap<>();

        unCheckedMap = new HashMap<>();
    }

    public ArrayList<ScanResult> getWifiAccessPointsWithRtt() {
        ArrayList<ScanResult> temp = new ArrayList<>(wifiAccessPointsWithRtt);

        for (int i = 0; i < temp.size(); i++) {
            ScanResult scanResult = temp.get(i);
            if (unCheckedMap.get(scanResult.BSSID) != null) {
                temp.remove(scanResult);
                --i;
            }
        }

        return temp;
    }

    public void swapData(ArrayList<ScanResult> wifiAccessPoints, HashMap<String, ScanResult> wifiAccessPointsWithRtt, HashMap<String, RangingResult> wifiAccessPointsWithRttInfo) {
        this.wifiAccessPoints = wifiAccessPoints;
        this.wifiAccessPointsWithRtt = new ArrayList(wifiAccessPointsWithRtt.values());
        this.wifiAccessPointsWithRttInfo = wifiAccessPointsWithRttInfo;

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        ScanResult currentScanResult;

        if (position < wifiAccessPointsWithRtt.size()) {
            currentScanResult = wifiAccessPointsWithRtt.get(position);

            viewHolder.txtLevel.setText(currentScanResult.level + "");
            viewHolder.checkLogging.setVisibility(View.VISIBLE);

            boolean isInfo = false;

            for (RangingResult rangingResult : wifiAccessPointsWithRttInfo.values()) {
                //rangingResult.getStatus()가 0 일경우 정상 통신이며 1이 들어올 경우 통신 실패로 인해 값이 null이 나와 에러 발생
                if (currentScanResult.BSSID.equals(rangingResult.getMacAddress().toString()) && rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                    viewHolder.txtRtt.setText((rangingResult.getDistanceMm() / 1000f) + "m");
                    viewHolder.txtRssi.setText(rangingResult.getRssi() + "");
                    isInfo = true;
                    break;
                }
            }

            if (!isInfo)
                viewHolder.txtRtt.setText("O");

            if (unCheckedMap.get(currentScanResult.BSSID) == null) {
                viewHolder.checkLogging.setChecked(true);
            } else {
                viewHolder.checkLogging.setChecked(false);
            }


        } else {
            currentScanResult = wifiAccessPoints.get(position - wifiAccessPointsWithRtt.size());
            viewHolder.txtLevel.setText(currentScanResult.level + "");
            viewHolder.checkLogging.setVisibility(View.INVISIBLE);
            viewHolder.txtRssi.setText("");
            viewHolder.txtRtt.setText("X");
        }
        viewHolder.txtSsid.setText(currentScanResult.SSID);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Returns size of list plus the header item (adds extra item).
    @Override
    public int getItemCount() {
        return wifiAccessPoints.size() + wifiAccessPointsWithRtt.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        TextView txtSsid;
        TextView txtLevel;
        TextView txtRssi;
        TextView txtRtt;
        CheckBox checkLogging;

        ViewHolder(View view) {
            super(view);
            txtSsid = view.findViewById(R.id.txtSsid);
            txtLevel = view.findViewById(R.id.txtLevel);
            txtRssi = view.findViewById(R.id.txtRssi);
            txtRtt = view.findViewById(R.id.txtRtt);
            checkLogging = view.findViewById(R.id.checkLogging);

            checkLogging.setOnCheckedChangeListener(this);
            view.setOnClickListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                unCheckedMap.put(wifiAccessPointsWithRtt.get(getAbsoluteAdapterPosition()).BSSID, true);
            } else {
                unCheckedMap.remove(wifiAccessPointsWithRtt.get(getAbsoluteAdapterPosition()).BSSID);
            }
        }

        @Override
        public void onClick(View v) {
            if (getAbsoluteAdapterPosition() < wifiAccessPointsWithRtt.size()) {
                checkLogging.setChecked(!checkLogging.isChecked());
            }
        }
    }
}