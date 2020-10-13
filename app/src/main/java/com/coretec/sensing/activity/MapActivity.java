package com.coretec.sensing.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;

import com.coretec.sensing.R;
import com.coretec.sensing.databinding.ActivityMapBinding;
import com.coretec.sensing.databinding.ContentMapBinding;
import com.coretec.sensing.dialog.AlarmDialog;
import com.coretec.sensing.dialog.LoadingDialog;
import com.coretec.sensing.listener.OnTouchMapListener;
import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Link;
import com.coretec.sensing.model.Node;
import com.coretec.sensing.model.Poi;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.model.Rtt;
import com.coretec.sensing.sqlite.ApHelper;
import com.coretec.sensing.sqlite.DBDownload;
import com.coretec.sensing.sqlite.LinkHelper;
import com.coretec.sensing.sqlite.NodeHelper;
import com.coretec.sensing.sqlite.PoiHelper;
import com.coretec.sensing.utils.ArrayCopy;
import com.coretec.sensing.utils.Calculation;
import com.coretec.sensing.utils.Const;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.CsvUtil;
import com.coretec.sensing.utils.DateUtils;
import com.coretec.sensing.utils.FilePath;
import com.coretec.sensing.utils.GpsTracker;
import com.coretec.sensing.utils.ImageUtils;
import com.coretec.sensing.utils.PrefManager;
import com.coretec.sensing.utils.Sensor;
import com.coretec.sensing.utils.Sort;
import com.coretec.sensing.view.MoveImageView;
import com.github.dakusui.combinatoradix.Combinator;
import com.google.android.material.navigation.NavigationView;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import lombok.SneakyThrows;
import no.wtw.android.dijkstra.DijkstraAlgorithm;
import no.wtw.android.dijkstra.model.Edge;
import no.wtw.android.dijkstra.model.Graph;
import no.wtw.android.dijkstra.model.Vertex;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.coretec.sensing.utils.Const.BOTTOM_BLANK_PIXEL;
import static com.coretec.sensing.utils.Const.DBPath;
import static com.coretec.sensing.utils.Const.LEFT_BLANK_PIXEL;
import static com.coretec.sensing.utils.Const.LoggingPath;
import static com.coretec.sensing.utils.Const.MAP_HEIGHT;
import static com.coretec.sensing.utils.Const.METER_PER_PIXEL_HEIGHT;
import static com.coretec.sensing.utils.Const.METER_PER_PIXEL_WIDTH;
import static com.coretec.sensing.utils.Const.PIXEL_PER_METER_HEIGHT;
import static com.coretec.sensing.utils.Const.PIXEL_PER_METER_WIDTH;

public class MapActivity extends AppCompatActivity implements OnTouchMapListener, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private ActivityMapBinding activityBinding;
    private ContentMapBinding contentBinding;

    //내부DB 접근 객체
    private PoiHelper poiHelper;
    private ApHelper apHelper;
    private NodeHelper nodeHelper;
    private LinkHelper linkHelper;

    //내부DB 정보
    private ArrayList<Poi> poiArrayList;
    private HashMap<String, Ap> apHashMap;
    private ArrayList<Node> nodeArrayList;
    private ArrayList<Link> linkArrayList;

    //노드링크 경로 정보
    private Graph pathGraph;
    private LinkedList<Vertex> path;
    private int pathDistance;

    //경로 조회 상태정보
    private boolean isPlaying = false;

    //setting 다이얼로그에서 설정
    //////////////////////////////////////////////////////////////////////////////
    //오래된 데이터를 거르기 위한 인터벌 값을 설정
    private int removeInterval = 2000;
    //링크과 연계하여 보정 결과 표출 알고리즘 설정
    private int useAlgorithm = 0;
    //RTT 조회 시 일괄로 요청하는 객체 개수
    private int useScanCount = 1;
    //RTT 요청 시 총 요청 객체 개수
    private int apScanCount = 15;
    //DBSCAN 사용 시 반영되는 반지름 거리(m)
    private double dbScanDistance = 0.5f;
    //조합 개수 기준
    private int combination = 4;
    //DBSCAN 거리 내 AP 인접 수 필터 기준
    private int dbScanFilter = 4;
    //임계치 값
    private double stdReliability = 2.5;
    //오차 거리 값 (위치 값이 튀는 경우를 방지하기 위한 기능)
    private double errorDistance = 5;
    //표출 알고리즘
    private boolean isOutputMedian = true, isOutputDbScan = true, isOutputReliability = true, isOutputCorrection = true;
    //////////////////////////////////////////////////////////////////////////////

    //로깅 데이터 설정
    //////////////////////////////////////////////////////////////////////////////
    private boolean isLoggingWifi = true;
    private boolean isLoggingRtt = true;
    private boolean isLoggingBluetooth = true;
    private boolean isLoggingSensor = true;
    private boolean isLoggingLte = true;

    //WIFI 정보 요청 인터벌
    private int wifiInterval = 30000;
    //RTT 정보 요청 인터벌
    private int rttInterval = 1000;
    //위치정보 업데이트 인터벌
    private int locationInterval = 1000;
    //Bluetooth 정보 요청 인터벌
    private int blueToothInterval = 1000;
    //Sensor 정보 요청 인터벌
    private int sensorInterval = 100;
    //LTE 정보 요청 인터벌
    private int lteInterval = 1000;

    //현재 로깅 중인지 체크
    private boolean isLogging = false;
    //파일 저장 중인지 체크
    private boolean isCreateFile = false;
    //////////////////////////////////////////////////////////////////////////////

    //CSV 로깅 객체
    //////////////////////////////////////////////////////////////////////////////
    private CsvManager wifiCsvManager;
    private CsvManager rttCsvManager;
    private CsvManager myLocationCsvManager;
    private CsvManager bluetoothCsvManager;
    private CsvManager sensorCsvManager;
    private CsvManager cellIdentityLteCsvManager;
    private CsvManager cellSignalStrengthLteCsvManager;
    private CsvManager algorithmSettingCsvManager;
    //////////////////////////////////////////////////////////////////////////////

    //PT NUMBER 컨트롤
    private int ptNum = 1;

    //RTT를 1개씩 조회할 경우 기본 객체에 MAC주소만 수정하여 재활용함
    private ScanResult searchResult;

    //내 위치 표출을 위한 이미지 객체
    //////////////////////////////////////////////////////////////////////////////
    private MoveImageView myMedianLocationView;
    private MoveImageView myDbScanLocationView;
    private MoveImageView myReliabilityLocationView;
    private MoveImageView myNearLocationView;
    //////////////////////////////////////////////////////////////////////////////

    //poi 이미지 객체 관리 리스트
    private ArrayList<MoveImageView> listPoiImage = new ArrayList<>();
    //AP 이미지 객체 관리 리스트
    private ArrayList<MoveImageView> listApImage = new ArrayList<>();

    //내 위치 및 링크 연계 조정위치 객체
    private Point myLocation;
    private Point medianLocation;
    private Point dbScanLocation;
    private Point reliabilityLocation;
    private Point correntionLocation;
    private Point nearLocation;

    //////////////////////////////////////////////////////////////////////////////
    //스캔 타이머
    private static TimerTask wifiTimer;
    private static TimerTask rttTimer;
    private static TimerTask locationTimer;
    private static TimerTask bluetoothTimer;
    private static TimerTask sensorTimer;
    private static TimerTask lteTimer;
    //////////////////////////////////////////////////////////////////////////////

    //데이터 수신을 위한 객체 설정
    //////////////////////////////////////////////////////////////////////////////
    //와이파이
    private WifiManager wifiManager;
    private WifiScanCallback wifiScanCallback;
    //RTT
    private WifiRttManager wifiRttManager;
    private RttRangingResultCallback rttRangingResultCallback;
    //GPS
    private GpsTracker gpsTracker;
    //센서
    private Sensor sensor;
    //LTE
    private TelephonyManager telephonyManager;
    //블루투스
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback leScanCallback;
    private HashMap<String, android.bluetooth.le.ScanResult> bluetoothLoggingHashMap;
    //////////////////////////////////////////////////////////////////////////////

    //조회된 와이파이 RTT 데이터를 담은 리스트
    private ArrayList<ScanResult> accessPoints;
    private HashMap<String, ScanResult> accessPointsSupporting80211mc;
    private HashMap<String, RangingResult> accessPointsSupporting80211mcInfo;

    //wifi, rtt 작성용 버퍼
    private Map<String, Rtt> buffer;

    //AP위치 수정 기능 활성화 여부
    private boolean isEdit = false;
    //정보 조회 기능 활성화 여부
    private boolean isInfo = false;
    private Menu menu;

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        activityBinding.drawerLayout.closeDrawer(GravityCompat.START);
        switch (id) {
            case R.id.nav_logging:
                Intent intent = new Intent(this, LoggingActivity.class);
                startActivity(intent);
                finish();
                return false;

            case R.id.nav_map:
                return false;

            case R.id.nav_map2:
                intent = new Intent(this, Map2Activity.class);
                startActivity(intent);
                finish();
                return false;

            case R.id.nav_setting_paramter:
                showSettingParameterDialog(MapActivity.this);
                return false;

            case R.id.nav_setting_logging:
                showSettingLoggingDialog(MapActivity.this);
                return false;
        }
        return true;
    }

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

            case R.id.item_edit:
                isEdit = !isEdit;

                if (isEdit) {
                    item.setIcon(R.drawable.ic_edit_select);
                    menu.findItem(R.id.item_info).setIcon(R.drawable.ic_info);
                    isInfo = false;
                } else {
                    item.setIcon(R.drawable.ic_edit);

                    CsvUtil.writeApCsv(new ArrayList<>(apHashMap.values()), DBPath);
                    CsvUtil.writePoiCsv(poiArrayList, DBPath);
                    CsvUtil.writeNodeCsv(nodeArrayList, DBPath);
                    CsvUtil.writeLinkCsv(linkArrayList, DBPath);
                }
                break;

            case R.id.item_info:
                isInfo = !isInfo;

                if (isInfo) {
                    item.setIcon(R.drawable.ic_info_select);
                    menu.findItem(R.id.item_edit).setIcon(R.drawable.ic_edit);
                    isEdit = false;
                } else {
                    item.setIcon(R.drawable.ic_info);
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_edit, menu);

        this.menu = menu;
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
    public void onResume() {
        super.onResume();

        if (wifiScanCallback != null)
            registerReceiver(wifiScanCallback, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (wifiScanCallback != null)
            unregisterReceiver(wifiScanCallback);
    }

    private void downloadDB() {
        FilePath.setDbName("rtt3.db");

        PrefManager pref = new PrefManager(this);
        if (!pref.isDownloadDB("database"))
            DBDownload.copyDB(pref, this, "database");
    }

    public void showSettingParameterDialog(@NonNull Context context) {
        Activity activity = (Activity) context;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_setting, null);

        final RadioButton radioMedian = dialogView.findViewById(R.id.radioMedian);
        final RadioButton radioDbScan = dialogView.findViewById(R.id.radioDbScan);
        final RadioButton radioReliability = dialogView.findViewById(R.id.radioReliability);
        final RadioButton radioOne = dialogView.findViewById(R.id.radioOne);
        final RadioButton radioTen = dialogView.findViewById(R.id.radioTen);
        final NumberPicker pickerCombination = dialogView.findViewById(R.id.pickerCombination);
        final NumberPicker pickerFilter = dialogView.findViewById(R.id.pickerFilter);
        final EditText editRadius = dialogView.findViewById(R.id.editRadius);
        final EditText editApCount = dialogView.findViewById(R.id.editApCount);
        final EditText editReliability = dialogView.findViewById(R.id.editReliability);
        final EditText editRemoveInterval = dialogView.findViewById(R.id.editRemoveInterval);
        final EditText editErrorDistance = dialogView.findViewById(R.id.editErrorDistance);
        final CheckBox checkMedian = dialogView.findViewById(R.id.checkMedian);
        final CheckBox checkDbScan = dialogView.findViewById(R.id.checkDbScan);
        final CheckBox checkReliability = dialogView.findViewById(R.id.checkReliability);
        final CheckBox checkCorrection = dialogView.findViewById(R.id.checkCorrection);

        pickerCombination.setMinValue(1);
        pickerCombination.setMaxValue(10);
        pickerCombination.setValue(combination);
        pickerCombination.setWrapSelectorWheel(false);
        pickerCombination.setOnLongPressUpdateInterval(100);

        pickerFilter.setMinValue(1);
        pickerFilter.setMaxValue(100);
        pickerFilter.setValue(dbScanFilter);
        pickerFilter.setWrapSelectorWheel(false);
        pickerFilter.setOnLongPressUpdateInterval(100);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("알고리즘 설정");
        builder.setView(dialogView);
        builder.setCancelable(false);

        editRadius.setText(dbScanDistance + "");
        editApCount.setText(apScanCount + "");
        editReliability.setText(stdReliability + "");
        editRemoveInterval.setText(removeInterval + "");
        editErrorDistance.setText(errorDistance + "");

        if (useScanCount == 1)
            radioOne.setChecked(true);
        else
            radioTen.setChecked(true);

        if (useAlgorithm == 0)
            radioMedian.setChecked(true);
        else if (useAlgorithm == 1)
            radioDbScan.setChecked(true);
        else
            radioReliability.setChecked(true);

        checkMedian.setChecked(isOutputMedian);
        checkDbScan.setChecked(isOutputDbScan);
        checkReliability.setChecked(isOutputReliability);
        checkCorrection.setChecked(isOutputCorrection);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //combination 설정
                combination = pickerCombination.getValue();

                //dbScanFilter 설정
                dbScanFilter = pickerFilter.getValue();

                //Link와 연계되는 좌표 조회
                if (radioMedian.isChecked())
                    useAlgorithm = 0;
                else if (radioDbScan.isChecked())
                    useAlgorithm = 1;
                else
                    useAlgorithm = 2;

                //오래된 데이터를 거르기 위한 기준 시간 설정
                removeInterval = Integer.parseInt(editRemoveInterval.getText().toString());

                //DBSCAN을 사용할 경우 거리값 설정
                dbScanDistance = Double.parseDouble(editRadius.getText().toString());

                //10개씩 혹은 1개씩 RTT 조회
                useScanCount = radioOne.isChecked() ? 1 : 10;

                //총 요청 개수
                apScanCount = Integer.parseInt(editApCount.getText().toString());

                //RTT 통신 주기(ms) 500 or 1000ms
                stdReliability = Double.parseDouble(editReliability.getText().toString());

                //오차 거리 값(m)
                errorDistance = Double.parseDouble(editErrorDistance.getText().toString());

                isOutputMedian = checkMedian.isChecked();
                isOutputDbScan = checkDbScan.isChecked();
                isOutputReliability = checkReliability.isChecked();
                isOutputCorrection = checkCorrection.isChecked();

                dialog.cancel();
            }
        });

        builder.setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void showSettingLoggingDialog(@NonNull Context context) {
        Activity activity = (Activity) context;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_logging, null);

        final CheckBox checkWifi = dialogView.findViewById(R.id.checkWifi);
        final CheckBox checkRtt = dialogView.findViewById(R.id.checkRtt);
        final CheckBox checkBluetooth = dialogView.findViewById(R.id.checkBluetooth);
        final CheckBox checkSensor = dialogView.findViewById(R.id.checkSensor);
        final CheckBox checkLte = dialogView.findViewById(R.id.checkLte);

        final EditText editWifiInterval = dialogView.findViewById(R.id.editWifiInterval);
        final EditText editRttInterval = dialogView.findViewById(R.id.editRttInterval);
        final EditText editLocationInterval = dialogView.findViewById(R.id.editLocationInterval);
        final EditText editBluetoothInterval = dialogView.findViewById(R.id.editBluetoothInterval);
        final EditText editSensorInterval = dialogView.findViewById(R.id.editSensorInterval);
        final EditText editLteInterval = dialogView.findViewById(R.id.editLteInterval);

        checkWifi.setChecked(isLoggingWifi);
        checkRtt.setChecked(isLoggingRtt);
        checkBluetooth.setChecked(isLoggingBluetooth);
        checkSensor.setChecked(isLoggingSensor);
        checkLte.setChecked(isLoggingLte);

        editWifiInterval.setText(wifiInterval + "");
        editRttInterval.setText(rttInterval + "");
        editLocationInterval.setText(locationInterval + "");
        editBluetoothInterval.setText(blueToothInterval + "");
        editSensorInterval.setText(sensorInterval + "");
        editLteInterval.setText(lteInterval + "");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("로깅 설정");
        builder.setView(dialogView);
        builder.setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //와이파이 로깅 여부 설정
                isLoggingWifi = checkWifi.isChecked();
                //RTT 로깅 여부 설정
                isLoggingRtt = checkRtt.isChecked();
                //블루투스 로깅 여부 설정
                isLoggingBluetooth = checkBluetooth.isChecked();
                //센서 로깅 여부 설정
                isLoggingSensor = checkSensor.isChecked();
                //LTE 로깅 여부 설정
                isLoggingLte = checkLte.isChecked();

                //와이파이 주기 설정
                wifiInterval = Integer.parseInt(editWifiInterval.getText().toString());

                //RTT 주기 설정
                rttInterval = Integer.parseInt(editLocationInterval.getText().toString());

                //위치정보 업데이트 주기 설정
                locationInterval = Integer.parseInt(editRttInterval.getText().toString());

                //블루투스 주기 설정
                blueToothInterval = Integer.parseInt(editBluetoothInterval.getText().toString());

                //센서 주기 설정
                sensorInterval = Integer.parseInt(editSensorInterval.getText().toString());

                //LTE 주기 설정
                lteInterval = Integer.parseInt(editLteInterval.getText().toString());

                dialog.cancel();
            }
        });

        builder.setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        end();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionListener permissionlistener = new PermissionListener() {
            @SneakyThrows
            @Override
            public void onPermissionGranted() {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
                    Toast.makeText(MapActivity.this, "RTT를 지원하지 않아 일부 기능 사용 시 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
//            finish();
                }

                downloadDB();

                init();

                initPath();
                loadMapImage();
                setBackgroundPosition();

                initRtt();
                initBluetooth();
                initSensor();
                initLte();

                for (Ap ap : apHashMap.values()) {
                    int[] point = contentBinding.imgMap.pointMeterToPixel(new double[]{ap.getPoint().getX(), ap.getPoint().getY()});
                    addAp(point[0], point[1], ap.getMacAddress());
                }
            }//권한습득 성공

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                finish();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TedPermission.with(this)
                    .setPermissionListener(permissionlistener)
                    .setRationaleMessage("앱을 사용하기 위해 권한설정이 필요합니다.")
                    .setDeniedMessage("앱 사용을 위해 권한을 설정해주세요.\n[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                    .setPermissions(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH, ACCESS_WIFI_STATE, ACCESS_BACKGROUND_LOCATION)
                    .check();//권한습득
        } else {
            TedPermission.with(this)
                    .setPermissionListener(permissionlistener)
                    .setRationaleMessage("앱을 사용하기 위해 권한설정이 필요합니다.")
                    .setDeniedMessage("앱 사용을 위해 권한을 설정해주세요.\n[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                    .setPermissions(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH, ACCESS_WIFI_STATE)
                    .check();//권한습득
        }
    }

    private void init() {
        //데이터 바인딩 초기화
        activityBinding = DataBindingUtil.setContentView(this, R.layout.activity_map);
        activityBinding.setActivity(this);
        contentBinding = activityBinding.includeContent;

        //네비게이션 메뉴 초기화
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, activityBinding.drawerLayout, activityBinding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        activityBinding.drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
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

        contentBinding.btnScan.setOnClickListener(this::onClick);
        contentBinding.btnStart.setOnClickListener(this::onClick);
        contentBinding.btnNext.setOnClickListener(this::onClick);
        contentBinding.btnEnd.setOnClickListener(this::onClick);

        contentBinding.imgMap.setOnTouchMapView(this);
        contentBinding.btnFind.setOnClickListener(this::onClick);
        contentBinding.btnPlay.setOnClickListener(this::onClick);
        contentBinding.btnInit.setOnClickListener(this::onClick);
        activityBinding.navView.setNavigationItemSelectedListener(this);

//        contentBinding.imgMarker.removeAllViews();

        poiHelper = new PoiHelper();
        apHelper = new ApHelper();
        nodeHelper = new NodeHelper();
        linkHelper = new LinkHelper();


        File poiFile = new File(DBPath, "POI.csv");
        File apFile = new File(DBPath, "AP.csv");
        File linkFile = new File(DBPath, "LINK.csv");
        File nodeFile = new File(DBPath, "NODE.csv");

        if (apFile.exists()) {
            apHelper.deleteAll();
            apHelper.insertApAll(CsvUtil.readApCsv(apFile.getAbsolutePath()));
        }

        if (poiFile.exists()) {
            poiHelper.deleteAll();
            poiHelper.insertPoiAll(CsvUtil.readPoiCsv(poiFile.getAbsolutePath()));
        }

        if (nodeFile.exists()) {
            nodeHelper.deleteAll();
            nodeHelper.insertNodeAll(CsvUtil.readNodeCsv(nodeFile.getAbsolutePath()));

        }

        if (linkFile.exists()) {
            linkHelper.deleteAll();
            linkHelper.insertLinkAll(CsvUtil.readLinkCsv(linkFile.getAbsolutePath()));
        }
    }

    private void initRtt() {
        accessPoints = new ArrayList<>();
        accessPointsSupporting80211mc = new HashMap<>();
        accessPointsSupporting80211mcInfo = new HashMap<>();
        buffer = new HashMap<>();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanCallback = new WifiScanCallback();

        wifiRttManager = (WifiRttManager) getApplicationContext().getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        rttRangingResultCallback = new RttRangingResultCallback();

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLoggingHashMap = new HashMap<>();

        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                super.onScanResult(callbackType, result);
                bluetoothLoggingHashMap.put(result.getDevice().getAddress(), result);
            }

            @Override
            public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
                super.onBatchScanResults(results);
                for (android.bluetooth.le.ScanResult result : results)
                    bluetoothLoggingHashMap.put(result.getDevice().getAddress(), result);
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

    private void initSensor() {
        sensor = new Sensor(this);
    }

    private void initLte() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    private void initPath() {
        poiArrayList = poiHelper.selectAllPoiList();
        apHashMap = apHelper.selectAllApList();
        nodeArrayList = nodeHelper.selectAllNodeList();
        linkArrayList = linkHelper.selectAllLinkList();

        pathGraph = new Graph(initDijkstra());
    }

    private ArrayList<Edge> initDijkstra() {
        ArrayList<Edge> edgeArrayList = new ArrayList<>();
        for (Link link : linkArrayList) {
            edgeArrayList.add(new Edge(new Vertex<>(link.getNode_start()), new Vertex<>(link.getNode_end()), link.getWeight_p()));

            //양방향을 위해 역방향 추가
            edgeArrayList.add(new Edge(new Vertex<>(link.getNode_end()), new Vertex<>(link.getNode_start()), link.getWeight_p()));
        }

        return edgeArrayList;
    }

    //지도 이미지 로딩
    private void loadMapImage() {
        Resources resource = getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
//        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Const.setMapParam(2411f, 2040f, 21.5d, 22.4d);
        Bitmap result = BitmapFactory.decodeResource(resource, R.drawable.bg_map3, options);

        contentBinding.imgMap.setImageBitmap(result);
//        contentBinding.imgMap.initPath();
    }

//    //지도 이미지 로딩
//    private void loadMapImage() {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false;
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//
//        if (!CsvUtil.readMapSettingCsv(MAPPath + "setting.csv")) {
//            Toast.makeText(this,"설정 데이터가 사용이 불가능합니다.", Toast.LENGTH_SHORT).show();
//            finish();
//        }
//
//        Bitmap result = BitmapFactory.decodeFile(MAPPath + "bg_map.png", options);
//        contentBinding.imgMap.setImageBitmap(result);
//    }

    private void setBackgroundPosition() {
        int width = (int) (contentBinding.imgMap.getDrawable().getIntrinsicWidth() / 2f);
        int height = (int) (contentBinding.imgMap.getDrawable().getIntrinsicHeight() / 2f);

        contentBinding.imgMap.initPosition(width, height);
    }

    @SneakyThrows
    private void searchPath(Poi poiStart, Poi poiEnd) {
        DijkstraAlgorithm dijkstraAlgorithm = new DijkstraAlgorithm(pathGraph).execute(new Vertex<>(poiStart.getSeq()));

        contentBinding.btnFind.setVisibility(View.GONE);
        contentBinding.layoutInfoNavi.setVisibility(View.VISIBLE);

        removeAlMarker();
        contentBinding.imgMap.initPath();

        pathDistance = (int) (dijkstraAlgorithm.getDistance(new Vertex<>(poiEnd.getSeq())) * METER_PER_PIXEL_WIDTH);
        path = dijkstraAlgorithm.getPath(new Vertex<>(poiEnd.getSeq()));

        contentBinding.txtNaviLen.setText(pathDistance + "m");
        contentBinding.txtNaviTime.setText((pathDistance / 66 + 1) + "분");

        contentBinding.txtStart.setText("현재 위치");
        contentBinding.txtEnd.setText(poiArrayList.get(poiEnd.getSeq() - 1).getName());

        for (int i = 0; i < path.size(); i++) {
            Vertex vertex = path.get(i);
            Node node = nodeArrayList.get((Integer) vertex.getPayload() - 1);
            contentBinding.imgMap.addPath((float) node.getPoint().getX(), (float) node.getPoint().getY());

            if (i == 0)
                addPoint((int) node.getPoint().getX(), (int) node.getPoint().getY(), R.drawable.ic_departure);

            if (i == path.size() - 1)
                addPoint((int) node.getPoint().getX(), (int) node.getPoint().getY(), R.drawable.ic_destination);
        }
    }

    //지도와 관련된 함수들
    //===========================================================================================
    //지정된 좌표 상에 이미지 표출
    private void addPoint(int posX, int posY, int resId) {
        if (resId == 0)
            return;

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);

        MoveImageView imgDonut = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, posX, posY, this);
        imgDonut.setImageBitmap(bitmap);
        imgDonut.setLayoutParams(layoutParams);
        imgDonut.setScaleType(ImageView.ScaleType.MATRIX);

        listPoiImage.add(imgDonut);
        contentBinding.imgMarker.addView(imgDonut);
    }

    private void addAp(int posX, int posY, String macAddress) {
        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker);

        MoveImageView imgDonut = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, posX, posY, this, macAddress);
        imgDonut.setImageBitmap(bitmap);
        imgDonut.setLayoutParams(layoutParams);
        imgDonut.setScaleType(ImageView.ScaleType.MATRIX);

        listApImage.add(imgDonut);
        contentBinding.imgMarker.addView(imgDonut);
    }

    public void setMedianLocation(Point point) {
        if (medianLocation == null)
            medianLocation = point;
        else {
            if (errorDistance >= Calculation.getPoint2PointDistance(medianLocation, point))
                medianLocation = point;
        }
    }

    public void setDbScanLocation(Point point) {
        if (dbScanLocation == null)
            dbScanLocation = point;
        else {
            if (errorDistance >= Calculation.getPoint2PointDistance(dbScanLocation, point))
                dbScanLocation = point;
        }
    }

    public void setReliabilityLocation(Point point) {
        if (reliabilityLocation == null)
            reliabilityLocation = point;
        else {
            if (errorDistance >= Calculation.getPoint2PointDistance(reliabilityLocation, point))
                reliabilityLocation = point;
        }
    }


    //지정된 좌표 상에 이미지 표출
    private void showMedianLocationView() {
        if (!isOutputMedian)
            return;

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location);

        if (myMedianLocationView != null)
            contentBinding.imgMarker.removeView(myMedianLocationView);

        myMedianLocationView = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, (int) (medianLocation.getX() * PIXEL_PER_METER_WIDTH + LEFT_BLANK_PIXEL), (int) (MAP_HEIGHT - (medianLocation.getY() * PIXEL_PER_METER_HEIGHT) - BOTTOM_BLANK_PIXEL), MapActivity.this);

        myMedianLocationView.setImageBitmap(bitmap);
        myMedianLocationView.setLayoutParams(layoutParams);
        myMedianLocationView.setScaleType(ImageView.ScaleType.MATRIX);

        contentBinding.imgMarker.addView(myMedianLocationView);
    }

    //지정된 좌표 상에 이미지 표출
    private void showDbScanLocationView() {
        if (!isOutputDbScan)
            return;

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location2);

        if (myDbScanLocationView != null)
            contentBinding.imgMarker.removeView(myDbScanLocationView);

        myDbScanLocationView = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, (int) (dbScanLocation.getX() * PIXEL_PER_METER_WIDTH + LEFT_BLANK_PIXEL), (int) (MAP_HEIGHT - (dbScanLocation.getY() * PIXEL_PER_METER_HEIGHT) - BOTTOM_BLANK_PIXEL), MapActivity.this);

        myDbScanLocationView.setImageBitmap(bitmap);
        myDbScanLocationView.setLayoutParams(layoutParams);
        myDbScanLocationView.setScaleType(ImageView.ScaleType.MATRIX);

        contentBinding.imgMarker.addView(myDbScanLocationView);
    }

    //지정된 좌표 상에 이미지 표출
    private void showReliabilityLocationView() {
        if (!isOutputReliability)
            return;

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location3);

        if (myReliabilityLocationView != null)
            contentBinding.imgMarker.removeView(myReliabilityLocationView);

        myReliabilityLocationView = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, (int) (reliabilityLocation.getX() * PIXEL_PER_METER_WIDTH + LEFT_BLANK_PIXEL), (int) (MAP_HEIGHT - (reliabilityLocation.getY() * PIXEL_PER_METER_HEIGHT) - BOTTOM_BLANK_PIXEL), MapActivity.this);

        myReliabilityLocationView.setImageBitmap(bitmap);
        myReliabilityLocationView.setLayoutParams(layoutParams);
        myReliabilityLocationView.setScaleType(ImageView.ScaleType.MATRIX);

        contentBinding.imgMarker.addView(myReliabilityLocationView);
    }

    //지정된 좌표 상에 이미지 표출
    private void showCorrectionLocationView() {
        if (!isOutputCorrection)
            return;

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location4);

        if (myNearLocationView != null)
            contentBinding.imgMarker.removeView(myNearLocationView);

        myNearLocationView = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, (int) (correntionLocation.getX() * PIXEL_PER_METER_WIDTH + LEFT_BLANK_PIXEL), (int) (MAP_HEIGHT - (correntionLocation.getY() * PIXEL_PER_METER_HEIGHT) - BOTTOM_BLANK_PIXEL), MapActivity.this);

        myNearLocationView.setImageBitmap(bitmap);
        myNearLocationView.setLayoutParams(layoutParams);
        myNearLocationView.setScaleType(ImageView.ScaleType.MATRIX);

        contentBinding.imgMarker.addView(myNearLocationView);
    }

    private void removeAlMarker() {
        for (MoveImageView moveImageView : listPoiImage) {
            contentBinding.imgMarker.removeView(moveImageView);
            listPoiImage.remove(moveImageView);
        }
    }

    //지도 컨트롤시(이동/확대/축소) 이미지 위치 조정
    @Override
    public void onTouchMap() {
        for (MoveImageView imageView : listPoiImage) {
            imageView.initPosition();
        }

        for (MoveImageView imageView : listApImage) {
            imageView.initPosition();
        }

        if (myMedianLocationView != null)
            myMedianLocationView.initPosition();

        if (myDbScanLocationView != null)
            myDbScanLocationView.initPosition();

        if (myReliabilityLocationView != null)
            myReliabilityLocationView.initPosition();

        if (myNearLocationView != null)
            myNearLocationView.initPosition();
    }

    public void searchAp(Ap ap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_ap, null);

        final EditText editApName = dialogView.findViewById(R.id.editApName);
        final EditText MacAddressEditText = dialogView.findViewById(R.id.editApMac);
        final EditText txtPointX = dialogView.findViewById(R.id.txtPointX);
        final EditText txtPointY = dialogView.findViewById(R.id.txtPointY);

        editApName.setText(ap.getName());
        MacAddressEditText.setText(ap.getMacAddress());
        txtPointX.setText(ap.getPoint().getX() + "");
        txtPointY.setText(ap.getPoint().getY() + "");

        editApName.setEnabled(false);
        MacAddressEditText.setEnabled(false);

        builder.setTitle("AP 정보");
        builder.setView(dialogView);
        builder.setCancelable(false);

        builder.setNegativeButton("닫기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void insertAp(float[] pixelPoint, float[] meterPoint) {
        if (!isEdit)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_ap, null);

        final EditText editApName = dialogView.findViewById(R.id.editApName);
        final EditText MacAddressEditText = dialogView.findViewById(R.id.editApMac);
        final EditText txtPointX = dialogView.findViewById(R.id.txtPointX);
        final EditText txtPointY = dialogView.findViewById(R.id.txtPointY);

        txtPointX.setText(meterPoint[0] + "");
        txtPointY.setText(meterPoint[1] + "");

        builder.setTitle("AP 추가");
        builder.setView(dialogView);
        builder.setCancelable(false);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (editApName.getText().length() == 0 || MacAddressEditText.getText().length() == 0)
                    return;

                Ap ap = new Ap(listApImage.size() + 1, editApName.getText().toString(), MacAddressEditText.getText().toString(), new Point(meterPoint[0], meterPoint[1]));

                addAp((int) pixelPoint[0], (int) pixelPoint[1], ap.getMacAddress());
                apHelper.insertAp(ap);
                apHashMap.put(ap.getMacAddress(), ap);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void deleteAp(MoveImageView imageView) {
        if (!isEdit)
            return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("선택한 마커를 삭제하시겠습니까?");

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int position = listApImage.indexOf(imageView);

                apHelper.deleteAp(imageView.getMacAddress());
                apHashMap.remove(imageView.getMacAddress());

                listApImage.remove(position);
                contentBinding.imgMarker.removeView(imageView);
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    //터치 좌표를 기준으로 AP 검색
    public MoveImageView getApPosition(float[] src, float scale) {
        if (!isEdit && !isInfo)
            return null;

        for (MoveImageView imageView : listApImage) {
            int[] posLocation = imageView.getPosLocation();

            if (posLocation[0] > src[0] - ImageUtils.getDp(this, 20 / scale) && posLocation[0] < src[0] + ImageUtils.getDp(this, 20 / scale) && posLocation[1] > src[1] && posLocation[1] < src[1] + ImageUtils.getDp(this, 40 / scale)) {
                if (isEdit) {
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker);
                    imageView.setImageBitmap(bitmap);
                    return imageView;
                }

                if (isInfo) {
                    searchAp(apHashMap.get(imageView.getMacAddress()));
                    return null;
                }
            }
        }
        return null;
    }

    public void setApPosition(MoveImageView imageView, float[] pixelPoint) {
        int position = listApImage.indexOf(imageView);

        imageView.setPosLocation(pixelPoint);
        listApImage.set(position, imageView);
    }

    public void setPointDB(MoveImageView imageView, float[] meterPoint) {
//        Log.d("저장 포지션", imageView.getMacAddress() + " - (" + meterPoint[0] + "," + meterPoint[1] + ")");
        apHelper.updateApPoint(imageView.getMacAddress(), meterPoint);
        apHashMap.get(imageView.getMacAddress()).setPoint(new Point(meterPoint[0], meterPoint[1]));
    }
//    ===========================================================================================

    @Override
    public void onClick(View v) {
        int resId = v.getId();

        switch (resId) {
            case R.id.btnFind:
                Intent intent = new Intent(MapActivity.this, LocationActivity.class);
                startActivityForResult(intent, 0);
//                searchPath();
                break;

            case R.id.btnPlay:
                isPlaying = !isPlaying;

                if (isPlaying) {
                    contentBinding.seekBar.setMax(path.size());
                    contentBinding.seekBar.setProgress(0);

                    contentBinding.layoutNavigation.setVisibility(View.VISIBLE);
                    contentBinding.layoutStop.setVisibility(View.GONE);

                    contentBinding.btnPlay.setImageResource(R.drawable.ic_pause);
                } else {
                    contentBinding.layoutNavigation.setVisibility(View.GONE);
                    contentBinding.layoutStop.setVisibility(View.VISIBLE);

                    contentBinding.btnPlay.setImageResource(R.drawable.ic_start);
                }
                break;

            case R.id.btnInit:
                isPlaying = false;

                contentBinding.btnFind.setVisibility(View.VISIBLE);
                contentBinding.layoutInfoNavi.setVisibility(View.GONE);

                removeAlMarker();
                setBackgroundPosition();
                contentBinding.imgMap.initPath();
                break;

            case R.id.btnScan:
                scanWifi();
                break;

            case R.id.btnStart:
                if (contentBinding.btnStart.isChecked())
                    start();
                else
                    pause();
                break;

            case R.id.btnNext:
                contentBinding.txtPtNum.setText(++ptNum + "");
                break;

            case R.id.btnEnd:
                end();
                break;
        }
    }

    private void start() {
        if (searchResult == null && isLoggingRtt) {
            AlarmDialog.showDialog(this, "스캔 버튼을 눌러 1개 이상의 RTT 지원 AP를 찾아주세요.");
            contentBinding.btnStart.setChecked(false);
            return;
        }


        if (!isCreateFile) {
            String fileName = contentBinding.editFileName.getText().toString();

            File fileWifi = new File(LoggingPath, fileName + "_WIFI.csv");
            File fileRtt = new File(LoggingPath, fileName + "_RTT.csv");
            File fileBluetooth = new File(LoggingPath, fileName + "_Bluetooth.csv");
            File fileSensor = new File(LoggingPath, fileName + "_Sensor.csv");
            File fileIdentityLte = new File(LoggingPath, fileName + "_CellIdentityLte.csv");
            File fileStrengthLte = new File(LoggingPath, fileName + "_CellSignalStrengthLte.csv");
            File fileAlgorithmSetting = new File(LoggingPath, fileName + "_AlgorithmSetting.csv");

            if (fileWifi.exists() || fileRtt.exists() || fileBluetooth.exists() || fileSensor.exists() || fileIdentityLte.exists() || fileStrengthLte.exists() || fileAlgorithmSetting.exists()) {
                contentBinding.btnStart.setChecked(false);
                AlarmDialog.showDialog(this, "파일명이 중복되어 스캔할 수 없습니다.\n파일을 삭제하거나 작성 파일명을 수정해주세요.");
                return;
            }
            if (isLoggingWifi) {
                wifiCsvManager = new CsvManager(fileName + "_WIFI.csv");
                wifiCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,centerFreq0,centerFreq1,channelWidth,frequency,level");
            }

            if (isLoggingRtt) {
                rttCsvManager = new CsvManager(fileName + "_RTT.csv");
                rttCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,RttStatus,Distance(Mm),DistanceStdDev(Mm),Rssi,timestamp,NumAttemptedMeasurements,NumSuccessfulMeasurements");

                myLocationCsvManager = new CsvManager(fileName + "_MyLocation.csv");
                myLocationCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,AP1 MAC,AP1 RANGE,AP2 MAC,AP2 RANGE,AP3 MAC,AP3 RANGE,AP4 MAC,AP4 RANGE,AP5 MAC,AP5 RANGE,AP6 MAC,AP6 RANGE,AP7 MAC,AP7 RANGE,AP8 MAC,AP8 RANGE,AP9 MAC,AP9 RANGE,AP10 MAC,AP10 RANGE,Median (X),Median (Y),DBscan (X),DBscan (Y),Reliability (X),Reliability (Y),Link (X),Link (Y)");

            }

            if (isLoggingBluetooth) {
                bluetoothCsvManager = new CsvManager(fileName + "_Bluetooth.csv");
                bluetoothCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,RSSI");
            }

            if (isLoggingSensor) {
                sensorCsvManager = new CsvManager(fileName + "_Sensor.csv");
                sensorCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,acceleration X,acceleration Y,acceleration Z,Geomagnetic X,Geomagnetic Y,Geomagnetic Z,gyro X,gyro Y,gyro Z,Pressure(hPa),Altitude(m),Temperature,Humidity,GPSLatitude,GPSLongitude,GPSAltitude");
            }

            if (isLoggingLte) {
                cellIdentityLteCsvManager = new CsvManager(fileName + "_CellIdentityLte.csv");
                cellIdentityLteCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,BandWidth,Ci,Earfcn,Mcc,Mnc,NetworkOperator,Pci,Tac");
                cellSignalStrengthLteCsvManager = new CsvManager(fileName + "_CellSignalStrengthLte.csv");

                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    cellSignalStrengthLteCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,AsuLevel,Cqi,dBm,Level,Rsrp,Rsrq,Rssi,Rssnr,TimingAdvance");
                } else {
                    cellSignalStrengthLteCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,AsuLevel,Cqi,dBm,Level,Rsrp,Rsrq,Rssnr,TimingAdvance");
                }
            }

            String temp = "";

            switch (useAlgorithm) {
                case 0:
                    temp = "Median";
                    break;
                case 1:
                    temp = "DBSCAN";
                    break;
                case 2:
                    temp = "Reliability";
                    break;
            }

            algorithmSettingCsvManager = new CsvManager(fileName + "_AlgorithmSetting.csv");
            algorithmSettingCsvManager.Write("RTT 정보 소멸 시간(ms),일괄 통신 요청 개수,AP 총 요청 개수,DB SCAN 반경(M),알고리즘 조합 개수,DB SCAN 필터링 기준,Reliability 임계치(m), 측위 오차 거리(m), 링크 연계 알고리즘");
            algorithmSettingCsvManager.Write(removeInterval + "," + useScanCount + "," + apScanCount + "," + dbScanDistance + "," + combination + "," + dbScanFilter + "," + stdReliability + "," + errorDistance + "," + temp);
        }
        startTimer();

        if (isLoggingWifi)
            wifiStartScanning(wifiInterval);

        if (isLoggingRtt) {
            rttStartScanning(rttInterval);
            locationStartUpdating(locationInterval);
        }

        if (isLoggingBluetooth)
            bluetoothStartScanning(blueToothInterval);

        if (isLoggingSensor)
            sensorStartScanning(sensorInterval);

        if (isLoggingLte)
            lteStartScanning(lteInterval);

        isCreateFile = true;
        isLogging = true;

        contentBinding.editFileName.setEnabled(false);
        contentBinding.btnScan.setEnabled(false);
    }

    private void pause() {
        contentBinding.txtPtNum.setText(++ptNum + "");

        accessPointsSupporting80211mcInfo.clear();

        isLogging = false;
        stopTimer();
        stopScanning();
    }

    private void end() {
        ptNum = 1;
        contentBinding.txtPtNum.setText(ptNum + "");

        isCreateFile = false;
        isLogging = false;
        stopTimer();
        stopScanning();
        stopLogging();

        contentBinding.editFileName.setEnabled(true);
        contentBinding.btnScan.setEnabled(true);

        contentBinding.btnStart.setChecked(false);
    }

    private void startTimer() {
        contentBinding.timerRanging.setBase(SystemClock.elapsedRealtime());
        contentBinding.timerRanging.start();
    }

    private void stopTimer() {
        contentBinding.timerRanging.setBase(SystemClock.elapsedRealtime());
        contentBinding.timerRanging.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == 0) {
            Poi poiStart = (Poi) data.getSerializableExtra("poiStart");
            Poi poiEnd = (Poi) data.getSerializableExtra("poiEnd");

//            Log.d("시작 POI", poiStart.toString());
//            Log.d("도착 POI", poiEnd.toString());

            searchPath(poiStart, poiEnd);
        }
    }

    //2분 4회의 wifi 스캔 제한이 있기 떄문에
    //30초에 한번식 강제적으로 스캔 시도
    private void wifiStartScanning(int delay) {
        wifiTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    public void run() {
                        scanWifi();
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(wifiTimer, 0, delay);
    }

    private void rttStartScanning(int delay) {
        rttTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("MissingPermission")
                    public void run() {
//                        HashMap<String, RangingResult> rawResultMap = new HashMap<>(accessPointsSupporting80211mcInfo);
                        HashMap<String, RangingResult> rawResultMap = ArrayCopy.deepCopyHashMap(accessPointsSupporting80211mcInfo, removeInterval);

                        //체크된 RTT지원 항목만 얻음
                        ArrayList<ScanResult> results = new ArrayList<>();
                        RangingRequest rangingRequest;

//                        accessPointsSupporting80211mcInfo.clear();

                        if (rawResultMap.size() >= combination) {
                            rttRangingResultCallback.getMyLocation(new ArrayList(rawResultMap.values()));

                            HashMap<String, Double> distanceMap = new HashMap<>();

                            for (Ap ap : apHashMap.values())
                                distanceMap.put(ap.getMacAddress(), Calculation.getPoint2PointDistance(myLocation, ap.getPoint()));

                            List<String> keySetList = new ArrayList<>(distanceMap.keySet());

                            // 오름차순
                            Collections.sort(keySetList, (o1, o2) -> (distanceMap.get(o1).compareTo(distanceMap.get(o2))));

                            int requsetSize = Math.min(apScanCount, keySetList.size());

                            if (useScanCount == 10) {
                                for (int i = 0; i < requsetSize; i++) {
                                    results.add(accessPointsSupporting80211mc.get(keySetList.get(i)));
                                }
                                rangingRequest = new RangingRequest.Builder().addAccessPoints(results).build();
                                wifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), rttRangingResultCallback);
                            } else {
                                for (int i = 0; i < requsetSize; i++) {
                                    searchResult.BSSID = keySetList.get(i);

                                    results.add(searchResult);

                                    rangingRequest = new RangingRequest.Builder().addAccessPoints(results).build();

                                    wifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), rttRangingResultCallback);

                                    results.clear();
                                }
                            }
                        } else {

                            Log.d("위치 연산1", "컷 " + accessPointsSupporting80211mcInfo.size() + " - " + rawResultMap.size());
                            Log.d("위치 연산2 로우", accessPointsSupporting80211mcInfo.toString());
                            Log.d("위치 연산3 필터링", rawResultMap.toString());

                            if (useScanCount == 10) {
                                for (ScanResult scanResult : accessPointsSupporting80211mc.values()) {
                                    results.add(scanResult);

                                    if (results.size() == RangingRequest.getMaxPeers()) {
                                        rangingRequest = new RangingRequest.Builder().addAccessPoints(results).build();
                                        wifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), rttRangingResultCallback);

                                        results.clear();
                                    }
                                }
                            } else {
                                for (Ap ap : apHashMap.values()) {
                                    searchResult.BSSID = ap.getMacAddress();

                                    results.add(searchResult);

                                    rangingRequest = new RangingRequest.Builder().addAccessPoints(results).build();

                                    wifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), rttRangingResultCallback);

                                    results.clear();
                                }
                            }
                        }
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(rttTimer, 0, delay);
    }

    private void locationStartUpdating(int delay) {
        locationTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("MissingPermission")
                    public void run() {
                        showMedianLocationView();
                        showDbScanLocationView();
                        showReliabilityLocationView();
                        showCorrectionLocationView();
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(locationTimer, 0, delay);
    }

    public void bluetoothStartScanning(int delay) {
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        bluetoothLeScanner.startScan(null, scanSettings, leScanCallback);
//        bluetoothLeScanner.startScan(leScanCallback);

        bluetoothTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        for (android.bluetooth.le.ScanResult result : bluetoothLoggingHashMap.values()) {
                            String currentTime = DateUtils.getTimeStampToDateTime(result.getTimestampNanos());

                            if (bluetoothCsvManager != null)
                                bluetoothCsvManager.Write(currentTime + "," + contentBinding.timerRanging.getTimeElapsed() + "," + ptNum + "," + "-" + "," + result.getDevice().getName() + "," + result.getDevice().getAddress() + "," + result.getRssi());
                        }

                        bluetoothLoggingHashMap.clear();
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(bluetoothTimer, 0, delay);
    }

    public void sensorStartScanning(int delay) {
        gpsTracker = new GpsTracker(this, delay);

        //센서 객체 초기화
        sensor.start();

        sensorTimer = new TimerTask() {
            @SuppressLint("MissingPermission")
            public void run() {
                String currentDateTime = DateUtils.getCurrentDateTime();
                float[] accelerometer = sensor.getAccelerometer();
                float[] magnetic = sensor.getMagnetic();
                float[] gyro = sensor.getGyro();
                float pressure = sensor.getPressure();
                float altitude = sensor.getAltitude();
                float temperature = sensor.getTemperature();
                float humidity = sensor.getHumidity();
                double gpsLatitude = gpsTracker.getLatitude();
                double gpsLongitude = gpsTracker.getLongitude();
                double gpsAltitude = gpsTracker.getAltitude();


                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        if (sensorCsvManager != null)
                            sensorCsvManager.Write(currentDateTime + "," + contentBinding.timerRanging.getTimeElapsed() + "," + ptNum + "," + "-" + "," + accelerometer[0] + "," + accelerometer[1] + "," + accelerometer[2] + "," + magnetic[0] + "," + magnetic[1] + "," + magnetic[2] + "," + gyro[0] + "," + gyro[1] + "," + gyro[2] + "," + pressure + "," + altitude + "," + temperature + "," + humidity + "," + gpsLatitude + "," + gpsLongitude + "," + gpsAltitude);
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(sensorTimer, 0, delay);
    }

    public void lteStartScanning(int delay) {
        lteTimer = new TimerTask() {
            @SuppressLint("MissingPermission")
            public void run() {
                String currentDateTime = DateUtils.getCurrentDateTime();

                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    public void run() {
                        try {
                            List<CellInfo> list = telephonyManager.getAllCellInfo();

                            if (list != null) {
                                for (CellInfo info : list) {
                                    if (info instanceof CellInfoLte) {
                                        CellInfoLte cellInfoLte = (CellInfoLte) info;

                                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                                        CellIdentityLte identityLte = cellInfoLte.getCellIdentity();

                                        if (cellIdentityLteCsvManager != null)
                                            cellIdentityLteCsvManager.Write(currentDateTime + "," + contentBinding.timerRanging.getTimeElapsed() + "," + ptNum + "," + "-" + "," + identityLte.getBandwidth() + "," + identityLte.getCi() + "," + identityLte.getEarfcn() + "," + identityLte.getMccString() + "," + identityLte.getMncString() + "," + identityLte.getMobileNetworkOperator() + "," + identityLte.getPci() + "," + identityLte.getTac());

                                        if (cellSignalStrengthLteCsvManager == null)
                                            break;

                                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                                            cellSignalStrengthLteCsvManager.Write(currentDateTime + "," + contentBinding.timerRanging.getTimeElapsed() + "," + ptNum + "," + "-" + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getCqi() + "," + cellSignalStrengthLte.getDbm() + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getRsrp() + "," + cellSignalStrengthLte.getRsrq() + "," + cellSignalStrengthLte.getRssi() + "," + cellSignalStrengthLte.getRssnr() + "," + cellSignalStrengthLte.getTimingAdvance());
                                        } else {
                                            cellSignalStrengthLteCsvManager.Write(currentDateTime + "," + contentBinding.timerRanging.getTimeElapsed() + "," + ptNum + "," + "-" + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getCqi() + "," + cellSignalStrengthLte.getDbm() + "," + cellSignalStrengthLte.getAsuLevel() + "," + cellSignalStrengthLte.getRsrp() + "," + cellSignalStrengthLte.getRsrq() + "," + +cellSignalStrengthLte.getRssnr() + "," + cellSignalStrengthLte.getTimingAdvance());
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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
        if (wifiTimer != null) {
            wifiTimer.cancel();
            wifiTimer = null;
        }

        if (rttTimer != null) {
            rttTimer.cancel();
            rttTimer = null;
        }

        if (bluetoothTimer != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            bluetoothTimer.cancel();
            bluetoothTimer = null;
        }

        if (sensor.isStart()) {
            gpsTracker.stop();
            sensor.stop();
            sensorTimer.cancel();
            sensorTimer = null;
        }

        if (lteTimer != null) {
            lteTimer.cancel();
            lteTimer = null;
        }
    }

    public void stopLogging() {
        if (wifiCsvManager != null) {
            wifiCsvManager.close();
            wifiCsvManager = null;
        }

        if (rttCsvManager != null) {
            rttCsvManager.close();
            rttCsvManager = null;
        }

        if (myLocationCsvManager != null) {
            myLocationCsvManager.close();
            myLocationCsvManager = null;
        }

        if (bluetoothCsvManager != null) {
            bluetoothCsvManager.close();
            bluetoothCsvManager = null;
        }

        if (sensorCsvManager != null) {
            sensorCsvManager.close();
            sensorCsvManager = null;
        }

        if (cellIdentityLteCsvManager != null) {
            cellIdentityLteCsvManager.close();
            cellIdentityLteCsvManager = null;
        }

        if (cellSignalStrengthLteCsvManager != null) {
            cellSignalStrengthLteCsvManager.close();
            cellSignalStrengthLteCsvManager = null;
        }

        if (algorithmSettingCsvManager != null) {
            algorithmSettingCsvManager.close();
            algorithmSettingCsvManager = null;
        }
    }


    public void scanWifi() {
        wifiManager.startScan();

        if (!isLogging)
            LoadingDialog.showDialog(MapActivity.this, "AP를 스캔 중입니다...");
    }

    private void findAccessPoints(@NonNull List<ScanResult> originalList) {
        accessPoints.clear();

        for (ScanResult scanResult : originalList) {
            if (!scanResult.is80211mcResponder()) {
                accessPoints.add(scanResult);
            }
        }
    }

    private void find80211mcSupportedAccessPoints
            (@NonNull List<ScanResult> originalList) {
        for (ScanResult scanResult : originalList) {

            if (!scanResult.is80211mcResponder())
                continue;

            if (searchResult == null)
                searchResult = scanResult;

            if (myLocation == null && accessPointsSupporting80211mc.size() >= RangingRequest.getMaxPeers())
                return;

            accessPointsSupporting80211mc.put(scanResult.BSSID, scanResult);
        }

        //RTT 통신의 한계 AP인 10개가 넘어갈 경우 실제 좌표상의 실제 거리와 비교함
        //내림차순으로 10위가 넘어가는 AP는 삭제
        if (accessPointsSupporting80211mc.size() > RangingRequest.getMaxPeers()) {

            HashMap<String, Double> distanceList = new HashMap<>();

            for (ScanResult scanResult : accessPointsSupporting80211mc.values()) {
                Ap ap = apHashMap.get(scanResult.BSSID);

                distanceList.put(ap.getMacAddress(), Calculation.getPoint2PointDistance(myLocation, ap.getPoint()));
            }

            // 내림차순
            ArrayList<String> keySetList = new ArrayList<String>(distanceList.keySet());
            Collections.sort(keySetList, (o1, o2) -> (distanceList.get(o2).compareTo(distanceList.get(o1))));


            //내림차순 후
            for (int i = accessPointsSupporting80211mc.size() - 1; i >= RangingRequest.getMaxPeers(); i--) {
                accessPointsSupporting80211mc.remove(keySetList.get(i));
            }
        }
    }

    private class WifiScanCallback extends BroadcastReceiver {
        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            LoadingDialog.hideDialog();

            List<ScanResult> scanResults = wifiManager.getScanResults();

            //RTT를 지원하지 않는 WIFI 객체 리스트 저장
            findAccessPoints(scanResults);

            //RTT를 지원하는 WIFI 객체 리스트 저장
            find80211mcSupportedAccessPoints(scanResults);

            if (accessPointsSupporting80211mc.size() < useScanCount) {
                //RTT AP를 하나도 찾기 못할 경우 무한 조회
//                scanWifi();
            } else {
                ArrayList<ScanResult> tempList = new ArrayList<>();

                tempList.addAll(accessPointsSupporting80211mc.values());
                tempList.addAll(accessPoints);

                String dateTime = DateUtils.getCurrentDateTime();

//                buffer.clear();

                for (int i = 0; i < tempList.size(); i++) {
                    ScanResult scanResult = tempList.get(i);

                    String writeData = dateTime;
                    writeData += "," + contentBinding.timerRanging.getTimeElapsed();
                    writeData += "," + ptNum;
                    writeData += "," + "-";
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

                    if (wifiCsvManager != null)
                        wifiCsvManager.Write(writeData);
                }
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

            for (RangingResult rangingResult : list) {

                String dateTime = DateUtils.getCurrentDateTime();
                String key = rangingResult.getMacAddress().toString();
                Rtt rtt = buffer.get(key);
                String data = "";

                data += dateTime;
                data += "," + contentBinding.timerRanging.getTimeElapsed();
                data += "," + ptNum;
                data += "," + "-";

                //와이파이 스캔 시 해당 기기가 스캔되지 않았을 경우 RTT값을 가져올 수 없어 빈 값을 넣음
                if (rtt != null) {
                    data += "," + rtt.getSsid();
                } else {
                    data += "," + apHashMap.get(key).getName();
                }

                data += "," + key;
                data += "," + rangingResult.getStatus();

                if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                    data += "," + rangingResult.getDistanceMm();
                    data += "," + rangingResult.getDistanceStdDevMm();
                    data += "," + rangingResult.getRssi();
                    data += "," + rangingResult.getRangingTimestampMillis();
                    data += "," + rangingResult.getNumAttemptedMeasurements();
                    data += "," + rangingResult.getNumSuccessfulMeasurements();
                }

                if (rttCsvManager != null)
                    rttCsvManager.Write(data);

                accessPointsSupporting80211mcInfo.put(rangingResult.getMacAddress().toString(), rangingResult);
            }
        }

        public void getMyLocation(List<RangingResult> list) {
            ArrayList<Double> locationXList = new ArrayList<>();
            ArrayList<Double> locationYList = new ArrayList<>();

            //오름차순
            Sort.Ascending ascending = new Sort.Ascending();

            HashMap<String, Integer> distanceMap = new HashMap<>();

            for (int i = list.size() - 1; i >= 0; i--) {
                RangingResult rangingResult = list.get(i);
                distanceMap.put(rangingResult.getMacAddress().toString(), rangingResult.getDistanceMm());
            }

            List<String> keySetList = new ArrayList<>(distanceMap.keySet());

            // 오름차순
            Collections.sort(keySetList, (o1, o2) -> (distanceMap.get(o1).compareTo(distanceMap.get(o2))));

            int requsetSize = Math.min(10, keySetList.size());
            for (int i = requsetSize; i < keySetList.size(); i++) {
                for (RangingResult rangingResult : list) {
                    if (keySetList.get(i).equals(rangingResult.getMacAddress().toString())) {
                        list.remove(rangingResult);
                        break;
                    }
                }
            }

            Combinator<RangingResult> groupRttList = new Combinator<>(list, combination);

            for (List<RangingResult> each : groupRttList) {
                double[][] myLocation = Calculation.getMyLocation(each, apHashMap);

                if (myLocation != null) {
                    locationXList.add(myLocation[0][0]);
                    locationYList.add(myLocation[1][0]);
                }
            }

            if (locationXList.size() == 0 || locationYList.size() == 0) {

                Log.d("위치 연산2", "컷");
                return;
            }

            ArrayList<Double> locationDbscanXList = new ArrayList<>(locationXList);
            ArrayList<Double> locationDbscanYList = new ArrayList<>(locationYList);

            //====================================================================================================================================================
            //기존 그룹군의 중위값으로 위치 측위
            Collections.sort(locationXList, ascending);
            Collections.sort(locationYList, ascending);

            Point medianLocation = new Point(locationXList.get(locationXList.size() / 2), locationYList.get(locationYList.size() / 2));
            //====================================================================================================================================================

            //====================================================================================================================================================
            //median 알고리즘을 통해 구한 예비위치를 기반으로 그룹별 신뢰도를 감안하여 최종 위치정보 수집
            ArrayList<Double> locationReliabilityXList = new ArrayList<>();
            ArrayList<Double> locationReliabilityYList = new ArrayList<>();

            for (List<RangingResult> each : groupRttList) {
                double reliability = 0;
                int count = 0;

                for (RangingResult rangingResult : each) {
                    if (rangingResult.getStatus() == RangingResult.STATUS_FAIL)
                        continue;

                    Ap ap = apHashMap.get(rangingResult.getMacAddress().toString());
                    //거리 정보는 mm단위로 제공되기 때문에 m단위로 변환하여 계산
                    reliability += Math.abs((rangingResult.getDistanceMm() / 1000f) - Calculation.getPoint2PointDistance(medianLocation, ap.getPoint()));
                    ++count;
                }

                reliability /= count;

                if (stdReliability >= reliability) {
                    double[][] myLocation = Calculation.getMyLocation(each, apHashMap);

                    if (myLocation != null) {
                        locationReliabilityXList.add(myLocation[0][0]);
                        locationReliabilityYList.add(myLocation[1][0]);
                    }
                }
            }

            Collections.sort(locationReliabilityXList, ascending);
            Collections.sort(locationReliabilityYList, ascending);

            Point reliabilityLocation = new Point();

            if (locationReliabilityXList.size() > 0)
                reliabilityLocation = new Point(locationReliabilityXList.get(locationReliabilityXList.size() / 2), locationReliabilityYList.get(locationReliabilityYList.size() / 2));
            //====================================================================================================================================================

            //====================================================================================================================================================
            //기존 그룹군 데이터를 기반으로 DBSCAN 알고리즘 반영
            ArrayList<Double> dbScanXList = new ArrayList<>();
            ArrayList<Double> dbScanYList = new ArrayList<>();

            for (int i = 0; i < locationDbscanXList.size(); i++) {
                ArrayList<Double> tempXList = new ArrayList<>();
                ArrayList<Double> tempYList = new ArrayList<>();

                Point criteriaPoint = new Point(locationDbscanXList.get(i), locationDbscanYList.get(i));

                tempXList.add(criteriaPoint.getX());
                tempYList.add(criteriaPoint.getY());

                int count = 1;

                for (int j = 0; j < locationDbscanXList.size(); j++) {
                    if (i == j)
                        continue;

                    Point point = new Point(locationDbscanXList.get(j), locationDbscanYList.get(j));

                    double distance = Calculation.getPoint2PointDistance(criteriaPoint, point);

                    //설정한 거리값 이내로 근접한 포인트 저장
                    if (dbScanDistance >= distance) {
                        tempXList.add(point.getX());
                        tempYList.add(point.getY());
                        ++count;
                    }
                }

                if (count >= dbScanFilter) {
                    dbScanXList.addAll(tempXList);
                    dbScanYList.addAll(tempYList);
                }
            }

            Point dbscanLocation = new Point();

            Collections.sort(dbScanXList, ascending);
            Collections.sort(dbScanXList, ascending);

            if (dbScanXList.size() > 0 && dbScanYList.size() > 0)
                dbscanLocation = new Point(dbScanXList.get(dbScanXList.size() / 2), dbScanYList.get(dbScanYList.size() / 2));
            //====================================================================================================================================================

            switch (useAlgorithm) {
                case 0:
                    myLocation = medianLocation;
                    break;
                case 1:
                    myLocation = dbscanLocation;
                    break;
                case 2:
                    myLocation = reliabilityLocation;
                    break;
            }

            setMedianLocation(medianLocation);
            setDbScanLocation(dbscanLocation);
            setReliabilityLocation(reliabilityLocation);
            getNearestLink(nodeArrayList, linkArrayList, myLocation);

            String apData = "";
            int count = 0;

            for (RangingResult rangingResult : list) {
                if (rangingResult.getStatus() == RangingResult.STATUS_FAIL) {
                    ++count;
                    continue;
                }

                apData += rangingResult.getMacAddress().toString() + "," + rangingResult.getDistanceMm() + ",";
            }

            for (int i = list.size() - count; i < 10; i++) {
                apData += ", ,";
            }

            if (myLocationCsvManager != null)
                myLocationCsvManager.Write(DateUtils.getCurrentDateTime() + "," + contentBinding.timerRanging.getTimeElapsed() + "," + ptNum + "," + apData + medianLocation.getX() + "," + medianLocation.getY() + "," + dbscanLocation.getX() + "," + dbscanLocation.getY() + "," + reliabilityLocation.getX() + "," + reliabilityLocation.getY() + "," + correntionLocation.getX() + "," + correntionLocation.getY());

//            list.clear();
//            accessPointsSupporting80211mcInfo.clear();
        }

        //a,b의 포인트 좌표를 픽셀에서 M단위로 수정해야함
        private Link getNearestLink(ArrayList<Node> nodeArrayList, ArrayList<Link> linkArrayList, Point point) {
            correntionLocation = new Point();
            double distance = Double.MAX_VALUE;
            Link nearestLink = null;

            for (Link link : linkArrayList) {
                Point start = nodeArrayList.get(link.getNode_start() - 1).getPoint();
                Point end = nodeArrayList.get(link.getNode_end() - 1).getPoint();

                double x1 = (start.getX() - LEFT_BLANK_PIXEL) * METER_PER_PIXEL_WIDTH;
                double y1 = (MAP_HEIGHT - start.getY() - BOTTOM_BLANK_PIXEL) * METER_PER_PIXEL_HEIGHT;
                double x2 = (end.getX() - LEFT_BLANK_PIXEL) * METER_PER_PIXEL_WIDTH;
                double y2 = (MAP_HEIGHT - end.getY() - BOTTOM_BLANK_PIXEL) * METER_PER_PIXEL_HEIGHT;
                double px = point.getX();
                double py = point.getY();

                double temp = getDist2LineSegment(px, py, x1, y1, x2, y2);

                //거리가 짧은 링크의 정보를 담아 갱신
                if (distance > temp) {
                    distance = temp;
                    nearestLink = link;
                    correntionLocation = nearLocation;
                }
            }

//            Log.d("가장 가까운 링크", nearestLink.toString());
//            Log.d("가장 가까운 링크 거리", distance + "m");

            return nearestLink;
        }

        private double getDist2LineSegment(double px, double py, double x1, double y1, double x2, double y2) {
            double xDelta = x2 - x1;
            double yDelta = y2 - y1;

            if ((xDelta == 0) && (yDelta == 0)) {
                throw new IllegalArgumentException("Segment start equals segment end");
            }

            double u = ((px - x1) * xDelta + (py - y1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

            if (u < 0) {
                nearLocation = new Point(x1, y1);
            } else if (u > 1) {
                nearLocation = new Point(x2, y2);
            } else {
                nearLocation = new Point(x1 + u * xDelta, y1 + u * yDelta);
            }

            return getPoint2PointDistance(nearLocation, new Point(px, py));
        }

        private double getPoint2PointDistance(Point p1, Point p2) {
            return Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX())
                    + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
        }
    }
}
