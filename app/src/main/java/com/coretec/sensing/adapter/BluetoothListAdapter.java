package com.coretec.sensing.adapter;


import android.bluetooth.le.ScanResult;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coretec.sensing.R;
import com.coretec.sensing.listener.RecyclerViewClickListener;
import com.coretec.sensing.model.Bluetooth;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class BluetoothListAdapter extends RecyclerView.Adapter<BluetoothListAdapter.ViewHolder> {
    private RecyclerViewClickListener itemListener;
    private ArrayList<Bluetooth> bluetoothList;

    private ScanResult refreshResult;

    public BluetoothListAdapter(ArrayList<Bluetooth> bluetoothList, RecyclerViewClickListener itemListener) {
        this.bluetoothList = bluetoothList;
        this.itemListener = itemListener;
    }

    public void refreshList(ArrayList<Bluetooth> bluetoothList, ScanResult refreshResult) {
        this.bluetoothList = bluetoothList;
        this.refreshResult = refreshResult;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Bluetooth bluetooth = bluetoothList.get(position);

        if (bluetooth.getSsid() == null)
            holder.txtSSID.setText("N/A");
        else
            holder.txtSSID.setText(bluetooth.getSsid());

        holder.txtBSSID.setText(bluetooth.getBssid());
        holder.txtRSSI.setText(bluetooth.getRssi());

        if (refreshResult == null) {
            holder.layoutBackground.setBackgroundColor(Color.WHITE);
        } else {
            if (refreshResult.getDevice().getAddress().equals(bluetooth.getBssid())) {
                holder.layoutBackground.setBackgroundColor(Color.LTGRAY);
            } else {
                holder.layoutBackground.setBackgroundColor(Color.WHITE);
            }
        }



    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return bluetoothList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        TextView txtSSID;
        TextView txtBSSID;
        TextView txtRSSI;
        LinearLayout layoutBackground;

        ViewHolder(View view) {
            super(view);
            txtSSID = (TextView) view.findViewById(R.id.txtSSID);
            txtBSSID = (TextView) view.findViewById(R.id.txtBSSID);
            txtRSSI = (TextView) view.findViewById(R.id.txtRSSI);
            layoutBackground = (LinearLayout) view.findViewById(R.id.layoutBackground);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            itemListener.onClick(view, getPosition());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        }
    }
}
