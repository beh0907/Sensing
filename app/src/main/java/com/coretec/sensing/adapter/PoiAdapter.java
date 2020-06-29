/*
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coretec.sensing.adapter;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.coretec.sensing.R;
import com.coretec.sensing.listener.RecyclerViewClickListener;
import com.coretec.sensing.model.Poi;

import java.util.ArrayList;

/**
 * Displays the ssid and bssid from a list of {@link ScanResult}s including a header at the top of
 * the {@link RecyclerView} to label the data.
 */
public class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.ViewHolder> {

    private ArrayList<Poi> poiArrayList;
    private RecyclerViewClickListener recyclerViewClickListener;

    public PoiAdapter(ArrayList<Poi> poiArrayList, RecyclerViewClickListener recyclerViewClickListener) {
        this.poiArrayList = poiArrayList;
        this.recyclerViewClickListener = recyclerViewClickListener;
    }


    public void refreshPointList(ArrayList<Poi> locationList) {
        this.poiArrayList = locationList;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Poi poi = poiArrayList.get(position);

        viewHolder.txtSeq.setText((position + 1) + "");
        viewHolder.txtX.setText(String.format("%.4f m", poi.getPoint().getX()));
        viewHolder.txtY.setText(String.format("%.4f m", poi.getPoint().getY()));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Returns size of list plus the header item (adds extra item).
    @Override
    public int getItemCount() {
        return poiArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView txtSeq;
        TextView txtX;
        TextView txtY;

        ViewHolder(View view) {
            super(view);
            txtSeq = view.findViewById(R.id.txtSeq);
            txtX = view.findViewById(R.id.txtX);
            txtY = view.findViewById(R.id.txtY);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            recyclerViewClickListener.onClick(v, getAbsoluteAdapterPosition());
        }
    }
}
