package com.coretec.sensing.activity;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.coretec.sensing.R;
import com.coretec.sensing.adapter.PoiAdapter;
import com.coretec.sensing.databinding.ActivityLocationBinding;
import com.coretec.sensing.databinding.ContentLocationBinding;
import com.coretec.sensing.listener.RecyclerViewClickListener;
import com.coretec.sensing.model.Poi;

import java.util.ArrayList;

public class LocationActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityLocationBinding activityBinding;
    private ContentLocationBinding contentBinding;

    private PoiAdapter poiAdapter;
    private ArrayList<Poi> poiArrayList;

    private Poi poiStart;
    private Poi poiEnd;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nothing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        initList();
    }

    private void init() {
        //데이터 바인딩 초기화
        activityBinding = DataBindingUtil.setContentView(this, R.layout.activity_location);
        activityBinding.setActivity(this);
        contentBinding = activityBinding.includeContent;

        //액션바 초기화
        setSupportActionBar(activityBinding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setElevation(0f);

        contentBinding.btnFind.setOnClickListener(this);

        contentBinding.inputStart.setSelected(true);
        contentBinding.inputEnd.setSelected(true);

        TypedValue outValue = new TypedValue();
        getApplicationContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true);
    }

    private void initList() {
        poiArrayList = new ArrayList<>();

        poiAdapter = new PoiAdapter(poiArrayList, new RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                Poi poi = poiArrayList.get(position);

                if (contentBinding.inputStart.isFocused()) {
                    contentBinding.inputEnd.requestFocus();

                    poiStart = poi;
                    contentBinding.inputStart.setText(poiStart.getName());
                } else {
                    poiEnd = poi;
                    contentBinding.inputEnd.setText(poiEnd.getName());
                }

            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        contentBinding.listLocation.setLayoutManager(linearLayoutManager);
        contentBinding.listLocation.setAdapter(poiAdapter);
    }

    @Override
    public void onClick(View v) {

    }
}
