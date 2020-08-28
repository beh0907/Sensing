package com.coretec.sensing.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.coretec.sensing.R;
import com.coretec.sensing.adapter.FragmentAdapter;
import com.coretec.sensing.databinding.ActivityLoggingBinding;
import com.coretec.sensing.databinding.ContentLoggingBinding;
import com.coretec.sensing.dialog.AlarmDialog;
import com.coretec.sensing.fragment.BluetoothFragment;
import com.coretec.sensing.fragment.LteFragment;
import com.coretec.sensing.fragment.RttFragment;
import com.coretec.sensing.fragment.SensorFragment;
import com.coretec.sensing.sqlite.DBDownload;
import com.coretec.sensing.utils.PrefManager;
import com.coretec.sensing.utils.TabModel;
import com.google.android.material.navigation.NavigationView;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import devlight.io.library.ntb.NavigationTabBar;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class LoggingActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    private ActivityLoggingBinding activityBinding;
    private ContentLoggingBinding contentBinding;

    private RttFragment rttFragment;
    private BluetoothFragment bluetoothFragment;
    private SensorFragment sensorFragment;
    private LteFragment lteFragment;

    private boolean isCreateFile = false;
    private boolean isScan = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (activityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    activityBinding.drawerLayout.closeDrawer(activityBinding.navView);
                } else {
                    activityBinding.drawerLayout.openDrawer(activityBinding.navView);
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        activityBinding.drawerLayout.closeDrawer(GravityCompat.START);
        switch (id) {
            case R.id.nav_logging:
                return false;

            case R.id.nav_map:
                Intent bookmark = new Intent(this, MapActivity.class);
                startActivity(bookmark);
                return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nothing, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (activityBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            activityBinding.drawerLayout.closeDrawer(activityBinding.navView);
            return;
        }

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                downloadDB();
                init();
                initFragment();
            }//권한습득 성공

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                finish();
            }
        };

        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setRationaleMessage("앱을 사용하기 위해 권한설정이 필요합니다.")
                .setDeniedMessage("앱 사용을 위해 권한을 설정해주세요.\n[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                .setPermissions(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH)
                .check();//권한습득
    }

    private void downloadDB() {
        PrefManager pref = new PrefManager(this);
        if (!pref.isDownloadDB())
            DBDownload.copyDB(pref, this);
    }

    @SuppressLint("WrongConstant")
    private void init() {
        //데이터 바인딩 초기화
        activityBinding = DataBindingUtil.setContentView(this, R.layout.activity_logging);
        activityBinding.setActivity(this);
        contentBinding = activityBinding.includeContent;

        //네비게이션 메뉴 초기화
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, activityBinding.drawerLayout, activityBinding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        activityBinding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        toggle.syncState();

        //액션바 초기화
        setSupportActionBar(activityBinding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setElevation(0f);

        contentBinding.btnScan.setOnClickListener(this);
        contentBinding.btnStart.setOnClickListener(this);
        contentBinding.btnMove.setOnClickListener(this);
        contentBinding.btnEnd.setOnClickListener(this);
        activityBinding.navView.setNavigationItemSelectedListener(this);

        contentBinding.editBleName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                bluetoothFragment.setBluetoothFilterBssid(s.toString().split(","));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            Toast.makeText(this, "RTT를 지원하지 않아 일부 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
//            finish();
        }
    }

    private void initFragment() {
        ArrayList<NavigationTabBar.Model> models = new ArrayList<>();
        ArrayList<Fragment> listFragment = new ArrayList<>();

        models.add(TabModel.createTabModel(getResources().getDrawable(R.drawable.ic_wifi), "#81a9d8", "RTT"));
        rttFragment = RttFragment.newInstance();
        listFragment.add(rttFragment);

        models.add(TabModel.createTabModel(getResources().getDrawable(R.drawable.ic_bluetooth), "#81a9d8", "블루투스"));
        bluetoothFragment = BluetoothFragment.newInstance();
        listFragment.add(bluetoothFragment);

        models.add(TabModel.createTabModel(getResources().getDrawable(R.drawable.ic_sensor), "#81a9d8", "센서"));
        sensorFragment = SensorFragment.newInstance();
        listFragment.add(sensorFragment);

        models.add(TabModel.createTabModel(getResources().getDrawable(R.drawable.ic_lte), "#81a9d8", "LTE"));
        lteFragment = LteFragment.newInstance();
        listFragment.add(lteFragment);

        FragmentAdapter introAdapter = new FragmentAdapter(getSupportFragmentManager(), listFragment);

        contentBinding.viewPagerTab.setAdapter(introAdapter);
        contentBinding.viewPagerTab.setOffscreenPageLimit(4);

        contentBinding.naviTabBar.setModels(models);
        contentBinding.naviTabBar.setViewPager(contentBinding.viewPagerTab);
    }

    @Override
    public void onClick(View v) {
        int resId = v.getId();

        switch (resId) {
            case R.id.btnScan:
                scan();
                break;

            case R.id.btnStart:
                if (contentBinding.btnStart.isChecked())
                    start();
                else
                    pause();

                break;

            case R.id.btnMove:
                if (contentBinding.btnMove.isChecked())
                    move();
                else
                    stop();
                break;

            case R.id.btnEnd:
                end();
                break;
        }
    }

    private void scan() {
        isScan = true;
        setLogging(false);

        int scanDelay = Integer.parseInt(contentBinding.editScanTime.getText().toString());
        int loggingDelay = Integer.parseInt(contentBinding.editLoggingTime.getText().toString());

//        rttFragment.wifiStartScanning(scanDelay);
//        rttFragment.rttStartScanning(loggingDelay);
        rttFragment.findAccessPoints();
        bluetoothFragment.bluetoothStartScanning(loggingDelay);
        sensorFragment.sensorStartScanning(loggingDelay);
        lteFragment.lteStartScanning(loggingDelay);
    }

    private void start() {
        if (!isLocationCheck())
            return;

        if (!isCreateFile) {
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;
            String fileName = contentBinding.editFileName.getText().toString();

            File fileWifi = new File(filePath, fileName + "_WIFI.csv");
            File fileRtt = new File(filePath, fileName + "_RTT.csv");
            File fileBluetooth = new File(filePath, fileName + "_Bluetooth.csv");
            File fileSensor = new File(filePath, fileName + "_Sensor.csv");
            File fileIdentityLte = new File(filePath, fileName + "_CellIdentityLte.csv");
            File fileStrengthLte = new File(filePath, fileName + "_CellSignalStrengthLte.csv");

            if (fileWifi.exists() || fileRtt.exists() || fileBluetooth.exists() || fileSensor.exists() || fileIdentityLte.exists() || fileStrengthLte.exists()) {
                end();
                AlarmDialog.showDialog(this, "파일명이 중복되어 스캔할 수 없습니다.\n파일을 삭제하거나 작성 파일명을 수정해주세요.");
                return;
            }

            rttFragment.createWifiCsvFile(fileName);
            rttFragment.createRttCsvFile(fileName);
            bluetoothFragment.createCsvFile(fileName);
            sensorFragment.createCsvFile(fileName);
            lteFragment.createCsvFile(fileName);
        }

        int scanDelay = Integer.parseInt(contentBinding.editScanTime.getText().toString());
        int loggingDelay = Integer.parseInt(contentBinding.editLoggingTime.getText().toString());

        setLogging(true);
        startTimer();

        if (isScan) {
            rttFragment.rttStopScanning();
            rttFragment.wifiStopScanning();
            bluetoothFragment.bluetoothStopScanning();
            sensorFragment.sensorStopScanning();
            lteFragment.lteStopScanning();
            isScan = false;
        }

        rttFragment.wifiStartScanning(scanDelay);
        rttFragment.rttStartScanning(loggingDelay);
        bluetoothFragment.bluetoothStartScanning(loggingDelay);
        sensorFragment.sensorStartScanning(loggingDelay);
        lteFragment.lteStartScanning(loggingDelay);

        contentBinding.editFileName.setEnabled(false);
        contentBinding.editBleName.setEnabled(false);
        contentBinding.btnScan.setEnabled(false);
        contentBinding.editPtnum.setEnabled(false);
        contentBinding.editStatus.setEnabled(false);

        isCreateFile = true;
    }

    private void pause() {

        contentBinding.editPtnum.setEnabled(true);
        contentBinding.editStatus.setEnabled(true);

        int ptNum = Integer.parseInt(contentBinding.editPtnum.getText().toString()) + 1;
        contentBinding.editPtnum.setText(ptNum + "");

        stopScan();
    }

    private void move() {

        contentBinding.editPtnum.setEnabled(false);
        contentBinding.editStatus.setEnabled(false);

        contentBinding.editStatus.setText("1");

        int loggingDelay = Integer.parseInt(contentBinding.editLoggingTime.getText().toString());

        setLogging(true);
        startTimer();

        rttFragment.rttStartScanning(loggingDelay);
        bluetoothFragment.bluetoothStartScanning(loggingDelay);
        sensorFragment.sensorStartScanning(loggingDelay);
        lteFragment.lteStartScanning(loggingDelay);
    }

    private void stop() {
        contentBinding.editPtnum.setEnabled(true);
        contentBinding.editStatus.setEnabled(true);

        contentBinding.editStatus.setText("0");
        stopScan();
    }

    public void end() {
        contentBinding.editPtnum.setEnabled(true);
        contentBinding.editStatus.setEnabled(true);

        isCreateFile = false;
        isScan = false;
        contentBinding.editPtnum.setText("0");
        contentBinding.editStatus.setText("0");
        contentBinding.btnStart.setText("START");
        contentBinding.btnMove.setText("MOVE");
        contentBinding.btnStart.setChecked(false);
        contentBinding.btnMove.setChecked(false);

        contentBinding.editFileName.setEnabled(true);
        contentBinding.editBleName.setEnabled(true);
        contentBinding.btnScan.setEnabled(true);

        stopScan();
    }

    public void stopScan() {
        try {
            setLogging(false);
            stopTimer();
            rttFragment.wifiStopScanning();
            rttFragment.rttStopScanning();
            bluetoothFragment.bluetoothStopScanning();
            sensorFragment.sensorStopScanning();
            lteFragment.lteStopScanning();
        } catch (Exception e) {

        }
    }

    private void startTimer() {
        contentBinding.timerRanging.setBase(SystemClock.elapsedRealtime());
        contentBinding.timerRanging.start();
    }

    private void stopTimer() {
        contentBinding.timerRanging.setBase(SystemClock.elapsedRealtime());
        contentBinding.timerRanging.stop();
    }

    public long getRuntime() {
        return contentBinding.timerRanging.getTimeElapsed();
    }

    public int getPtNum() {
        return Integer.parseInt(contentBinding.editPtnum.getText().toString());
    }

    public int getStatus() {
        return Integer.parseInt(contentBinding.editStatus.getText().toString());
    }

    public String[] getBleFilterBssid() {
        return contentBinding.editBleName.getText().toString().split(",");
    }

    public void setLogging(boolean isLogging) {
        rttFragment.setWifiLogging(isLogging);
        rttFragment.setRttLogging(isLogging);
        bluetoothFragment.setLogging(isLogging);
        sensorFragment.setLogging(isLogging);
        lteFragment.setLogging(isLogging);
    }

    @SuppressLint("WrongConstant")
    private boolean isLocationCheck() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "스캔 기능을 사용하기 위해 GPS 기능을 On시켜 주시기 바랍니다.", Toast.LENGTH_SHORT).show();

            // GPS 설정 화면으로 이동
            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(gpsOptionsIntent);

            return false;
        }

        return true;
    }
}
