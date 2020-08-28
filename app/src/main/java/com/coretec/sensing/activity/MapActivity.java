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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

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
import com.coretec.sensing.utils.Calculation;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;
import com.coretec.sensing.utils.GpsTracker;
import com.coretec.sensing.utils.PrefManager;
import com.coretec.sensing.utils.Sensor;
import com.coretec.sensing.utils.Sort;
import com.coretec.sensing.view.MoveImageView;
import com.github.dakusui.combinatoradix.Combinator;
import com.google.android.material.checkbox.MaterialCheckBox;
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
import static com.coretec.sensing.utils.Const.LEFT_BLANK_PIXEL;
import static com.coretec.sensing.utils.Const.MAP_HEIGHT;
import static com.coretec.sensing.utils.Const.METER_PER_PIXEL;
import static com.coretec.sensing.utils.Const.PIXEL_PER_METER;

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
    //RTT 조회 시 일괄로 요청하는 객체 개수
    private boolean isUseDbScan = false;
    //RTT 조회 시 일괄로 요청하는 객체 개수
    private int useScanCount = 1;
    //DBSCAN 사용 시 반영되는 반지름 거리(m)
    private double dbScanDistance = 0.5f;
    //조합 개수 기준
    private int combination = 4;
    //DBSCAN 거리 내 AP 인접 수 필터 기준
    private int dbScanFilter = 4;
    //RTT 정보 요청 인터벌
    private int rttInterval = 1000;
    //////////////////////////////////////////////////////////////////////////////

    //로깅 데이터 설정
    //////////////////////////////////////////////////////////////////////////////
    private boolean isLoggingWifi = true;
    private boolean isLoggingRtt = true;
    private boolean isLoggingBluetooth = true;
    private boolean isLoggingSensor = true;
    private boolean isLoggingLte = true;

    //현재 로깅 중인지 체크
    private boolean isLogging = false;
    //////////////////////////////////////////////////////////////////////////////

    //CSV 로깅 객체
    //////////////////////////////////////////////////////////////////////////////
    private CsvManager wifiCsvManager;
    private CsvManager rttCsvManager;
    private CsvManager bluetoothCsvManager;
    private CsvManager sensorCsvManager;
    private CsvManager cellIdentityLteCsvManager;
    private CsvManager cellSignalStrengthLteCsvManager;
    //////////////////////////////////////////////////////////////////////////////

    //PT NUMBER 컨트롤
    private int ptNum = 1;

    //RTT를 1개씩 조회할 경우 기본 객체에 MAC주소만 수정하여 재활용함
    private ScanResult searchResult;

    //내 위치 표출을 위한 이미지 객체
    //////////////////////////////////////////////////////////////////////////////
    private MoveImageView myLocationView;
    private MoveImageView myLocationView2;
    //////////////////////////////////////////////////////////////////////////////

    //poi, AP 등 이미지 객체 관리 리스트
    private ArrayList<MoveImageView> listPointImage = new ArrayList<>();

    //내 위치 및 링크 연계 조정위치 객체
    private Point myLocation;
    private Point myLocation2;
    private Point nearLocation;

    //////////////////////////////////////////////////////////////////////////////
    //스캔 타이머
    private static TimerTask wifiTimer;
    private static TimerTask rttTimer;
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

    //wifi, rtt 작성용 버퍼퍼
    private Map<String, Rtt> buffer;

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        activityBinding.drawerLayout.closeDrawer(GravityCompat.START);
        switch (id) {
            case R.id.nav_logging:
                Intent bookmark = new Intent(this, LoggingActivity.class);
                startActivity(bookmark);
                return false;

            case R.id.nav_map:
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
        }
        return false;
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
        PrefManager pref = new PrefManager(this);
//        if (!pref.isDownloadDB())
        DBDownload.copyDB(pref, this);
    }

    public void showSettingParameterDialog(@NonNull Context context) {
        Activity activity = (Activity) context;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_setting, null);

        final RadioButton radioOne = dialogView.findViewById(R.id.radioOne);
        final RadioButton radioTen = dialogView.findViewById(R.id.radioTen);
        final RadioButton radioDistance0_5m = dialogView.findViewById(R.id.radioDistance0_5m);
        final RadioButton radioDistance1m = dialogView.findViewById(R.id.radioDistance1m);
        final RadioButton radio500ms = dialogView.findViewById(R.id.radio500ms);
        final RadioButton radio1000ms = dialogView.findViewById(R.id.radio1000ms);
        final RadioButton radioMedian = dialogView.findViewById(R.id.radioMedian);
        final RadioButton radioDbScan = dialogView.findViewById(R.id.radioDbScan);
        final RadioButton radioTrust = dialogView.findViewById(R.id.radioTrust);
        final SeekBar seekBarCombination = dialogView.findViewById(R.id.seekBarCombination);
        final SeekBar seekBarFilter = dialogView.findViewById(R.id.seekBarFilter);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("위치 측위 설정");
        builder.setView(dialogView);
        builder.setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //combination 설정
                combination = seekBarCombination.getProgress();

                //dbScanFilter 설정
                dbScanFilter = seekBarFilter.getProgress();

                //DBSCAN을 사용할 경우 거리값 설정
                isUseDbScan = radioDbScan.isChecked();

                //DBSCAN을 사용할 경우 거리값 설정
                dbScanDistance = radioDistance0_5m.isChecked() ? 0.5f : 1;

                //10개씩 혹은 1개씩 RTT 조회
                useScanCount = radioTen.isChecked() ? 10 : 1;

                //RTT 통신 간격 500 or 1000ms
                rttInterval = radio500ms.isChecked() ? 500 : 1000;

                dialog.cancel();
            }
        });

        builder.show();
    }

    public void showSettingLoggingDialog(@NonNull Context context) {
        Activity activity = (Activity) context;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_logging, null);

        final MaterialCheckBox checkWifi = dialogView.findViewById(R.id.checkWifi);
        final MaterialCheckBox checkRtt = dialogView.findViewById(R.id.checkRtt);
        final MaterialCheckBox checkBluetooth = dialogView.findViewById(R.id.checkBluetooth);
        final MaterialCheckBox checkSensor = dialogView.findViewById(R.id.checkSensor);
        final MaterialCheckBox checkLte = dialogView.findViewById(R.id.checkLte);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("로깅 데이터 설정");
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

                dialog.cancel();
            }
        });

        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
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
                downloadDB();

                init();

                initRtt();
                initBluetooth();
                initSensor();
                initLte();

                initPath();
                loadMapImage();
                setBackgroundPosition();
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
                .setPermissions(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH, ACCESS_WIFI_STATE, ACCESS_BACKGROUND_LOCATION, BLUETOOTH)
                .check();//권한습득
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

        contentBinding.btnStart.setOnClickListener(this::onClick);
        contentBinding.btnNext.setOnClickListener(this::onClick);

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

        Bitmap result = BitmapFactory.decodeResource(resource, R.drawable.bg_map2, options);
        contentBinding.imgMap.setImageBitmap(result);
//        contentBinding.imgMap.initPath();
    }

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

        pathDistance = (int) (dijkstraAlgorithm.getDistance(new Vertex<>(poiEnd.getSeq())) * PIXEL_PER_METER);
        path = dijkstraAlgorithm.getPath(new Vertex<>(poiEnd.getSeq()));

        contentBinding.txtNaviLen.setText(pathDistance + "m");
        contentBinding.txtNaviTime.setText((pathDistance / 66 + 1) + "분");

        contentBinding.txtStart.setText("현재 위치");
        contentBinding.txtEnd.setText(poiArrayList.get(poiEnd.getSeq() - 1).getName());

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        for (int i = 0; i < path.size(); i++) {
            Vertex vertex = path.get(i);
            Node node = nodeArrayList.get((Integer) vertex.getPayload() - 1);
            contentBinding.imgMap.addPath((float) node.getPoint().getX(), (float) node.getPoint().getY());

            if (i == 0)
                addPoint(parentWidth, parentHeight, (int) node.getPoint().getX(), (int) node.getPoint().getY(), R.drawable.ic_departure);

            if (i == path.size() - 1)
                addPoint(parentWidth, parentHeight, (int) node.getPoint().getX(), (int) node.getPoint().getY(), R.drawable.ic_destination);
        }
    }

    //지정된 좌표 상에 이미지 표출
    private void addPoint(int parentWidth, int parentHeight, int posX, int posY, int resId) {
        if (resId == 0)
            return;

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);

        MoveImageView imgDonut = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, posX, posY, this);
        imgDonut.setImageBitmap(bitmap);
        imgDonut.setLayoutParams(layoutParams);
        imgDonut.setScaleType(ImageView.ScaleType.MATRIX);

        listPointImage.add(imgDonut);
        contentBinding.imgMarker.addView(imgDonut);
    }

    //지정된 좌표 상에 이미지 표출
    private void setMyLocationView(Point point) {
        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location);

        if (myLocationView != null)
            contentBinding.imgMarker.removeView(myLocationView);

        myLocationView = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, (int) (point.getX() * METER_PER_PIXEL + LEFT_BLANK_PIXEL), (int) (MAP_HEIGHT - (point.getY() * METER_PER_PIXEL) - BOTTOM_BLANK_PIXEL), MapActivity.this);

        myLocationView.setImageBitmap(bitmap);
        myLocationView.setLayoutParams(layoutParams);
        myLocationView.setScaleType(ImageView.ScaleType.MATRIX);

        contentBinding.imgMarker.addView(myLocationView);
    }

    //지정된 좌표 상에 이미지 표출
    private void setMyLocationView2(Point point) {
        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_my_location2);

        if (myLocationView2 != null)
            contentBinding.imgMarker.removeView(myLocationView2);

        myLocationView2 = new MoveImageView(contentBinding.imgMap.savedMatrix2, parentWidth, parentHeight, (int) (point.getX() * METER_PER_PIXEL + LEFT_BLANK_PIXEL), (int) (MAP_HEIGHT - (point.getY() * METER_PER_PIXEL) - BOTTOM_BLANK_PIXEL), MapActivity.this);

        myLocationView2.setImageBitmap(bitmap);
        myLocationView2.setLayoutParams(layoutParams);
        myLocationView2.setScaleType(ImageView.ScaleType.MATRIX);

        contentBinding.imgMarker.addView(myLocationView2);
    }

    private void removeAlMarker() {
        contentBinding.imgMarker.removeAllViews();
        listPointImage.clear();
    }

    //지도 컨트롤시(이동/확대/축소) 이미지 위치 조정
    @Override
    public void onTouchMap() {
        for (MoveImageView imageView : listPointImage) {
            imageView.initPosition();
        }
        if (myLocationView != null)
            myLocationView.initPosition();

        if (myLocationView2 != null)
            myLocationView2.initPosition();
    }

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

            case R.id.btnStart:
                if (contentBinding.btnStart.isChecked())
                    start();
                else
                    end();
                break;

            case R.id.btnNext:
                contentBinding.txtPtNum.setText(++ptNum + "");
                break;
        }
    }

    private void start() {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;
        String fileName = contentBinding.editFileName.getText().toString();

        File fileWifi = new File(filePath, fileName + "_WIFI.csv");
        File fileRtt = new File(filePath, fileName + "_RTT.csv");
        File fileBluetooth = new File(filePath, fileName + "_Bluetooth.csv");
        File fileSensor = new File(filePath, fileName + "_Sensor.csv");
        File fileIdentityLte = new File(filePath, fileName + "_CellIdentityLte.csv");
        File fileStrengthLte = new File(filePath, fileName + "_CellSignalStrengthLte.csv");

        if (fileWifi.exists() || fileRtt.exists() || fileBluetooth.exists() || fileSensor.exists() || fileIdentityLte.exists() || fileStrengthLte.exists()) {
            contentBinding.btnStart.setChecked(false);
            AlarmDialog.showDialog(this, "파일명이 중복되어 스캔할 수 없습니다.\n파일을 삭제하거나 작성 파일명을 수정해주세요.");
            return;
        }
        isLogging = true;

        ptNum = 1;
        contentBinding.txtPtNum.setText(ptNum + "");

        LoadingDialog.showDialog(MapActivity.this, "AP를 스캔 중입니다... (" + accessPointsSupporting80211mc.size() + "/" + useScanCount + ")");
        scanWifi();
    }

    private void end() {
        isLogging = false;

        stopTimer();

        rttStopScanning();
        wifiStopScanning();
        bluetoothStopScanning();
        sensorStopScanning();
        lteStopScanning();
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

            Log.d("시작 POI", poiStart.toString());
            Log.d("도착 POI", poiEnd.toString());

            searchPath(poiStart, poiEnd);
        }
    }

    //2분 4회의 wifi 스캔 제한이 있기 떄문에
    //30초에 한번식 강제적으로 스캔 시도
    private void wifiStartScanning(int delay) {
        wifiStopScanning();

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
        rttStopScanning();

        rttTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    public void run() {
                        if (accessPointsSupporting80211mcInfo.size() > 0)
                            rttRangingResultCallback.getMyLocation(new ArrayList(accessPointsSupporting80211mcInfo.values()));

                        //체크된 RTT지원 항목만 얻음
                        ArrayList<ScanResult> results = new ArrayList<>();

                        RangingRequest rangingRequest;

                        if (useScanCount == 10) {
                            results = new ArrayList<>(accessPointsSupporting80211mc.values());

                            rangingRequest = new RangingRequest.Builder().addAccessPoints(results).build();

                            wifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), rttRangingResultCallback);

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
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(rttTimer, 0, delay);
    }

    public void bluetoothStartScanning(int delay) {
        bluetoothStopScanning();

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(delay)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        bluetoothLeScanner.startScan(null, scanSettings, leScanCallback);

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
        timer.schedule(bluetoothTimer, 1000, delay);
    }

    public void sensorStartScanning(int delay) {
        sensorStopScanning();

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
        lteStopScanning();

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

    public void wifiStopScanning() {
        if (wifiTimer != null) {
            wifiTimer.cancel();
            wifiTimer = null;
        }

        if (wifiCsvManager != null) {
            wifiCsvManager.close();
            wifiCsvManager = null;
        }
    }

    public void rttStopScanning() {
        if (rttTimer != null) {
            rttTimer.cancel();
            rttTimer = null;
        }

        if (rttCsvManager != null) {
            rttCsvManager.close();
            rttCsvManager = null;
        }
    }

    public void bluetoothStopScanning() {
        if (bluetoothTimer != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            bluetoothTimer.cancel();
            bluetoothTimer = null;
        }

        if (bluetoothCsvManager != null) {
            bluetoothCsvManager.close();
            bluetoothCsvManager = null;
        }
    }

    public void sensorStopScanning() {
        if (sensor.isStart()) {
            gpsTracker.stop();
            sensor.stop();
            sensorTimer.cancel();
            sensorTimer = null;
        }

        if (sensorCsvManager != null) {
            sensorCsvManager.close();
            sensorCsvManager = null;
        }
    }

    public void lteStopScanning() {
        if (lteTimer != null) {
            lteTimer.cancel();
            lteTimer = null;
        }

        if (cellIdentityLteCsvManager != null) {
            cellIdentityLteCsvManager.close();
            cellIdentityLteCsvManager = null;
        }

        if (cellSignalStrengthLteCsvManager != null) {
            cellSignalStrengthLteCsvManager.close();
            cellSignalStrengthLteCsvManager = null;
        }
    }


    public void scanWifi() {
        wifiManager.startScan();
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
            List<ScanResult> scanResults = wifiManager.getScanResults();

//            Log.d("와이파이 데이터 테스트 태그", scanResults.toString());

            //RTT를 지원하지 않는 WIFI 객체 리스트 저장
            findAccessPoints(scanResults);

            //RTT를 지원하는 WIFI 객체 리스트 저장
            find80211mcSupportedAccessPoints(scanResults);

            if (accessPointsSupporting80211mc.size() < useScanCount) {
                LoadingDialog.updateMessage("AP를 스캔 중입니다... (" + accessPointsSupporting80211mc.size() + "/" + useScanCount + ")");
                scanWifi();
            } else {
                if (LoadingDialog.isShowDialog()) {
                    String fileName = contentBinding.editFileName.getText().toString();

                    if (isLoggingWifi) {
                        wifiCsvManager = new CsvManager(fileName + "_WIFI.csv");
                        wifiCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,centerFreq0,centerFreq1,channelWidth,frequency,level");

                        wifiStartScanning(rttInterval);
                    }

                    if (isLoggingRtt) {
                        rttCsvManager = new CsvManager(fileName + "_RTT.csv");
                        rttCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,RttStatus,Distance(Mm),DistanceStdDev(Mm),Rssi,timestamp,NumAttemptedMeasurements,NumSuccessfulMeasurements");

                        rttStartScanning(rttInterval);
                    }

                    if (isLoggingBluetooth) {
                        bluetoothCsvManager = new CsvManager(fileName + "_Bluetooth.csv");
                        bluetoothCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,SSID,BSSID,RSSI");

                        bluetoothStartScanning(rttInterval);
                    }

                    if (isLoggingSensor) {
                        sensorCsvManager = new CsvManager(fileName + "_Sensor.csv");
                        sensorCsvManager.Write("DATE,TIME,SEC,RUNTIME(ms),PTNUM,STATUS,acceleration X,acceleration Y,acceleration Z,Geomagnetic X,Geomagnetic Y,Geomagnetic Z,gyro X,gyro Y,gyro Z,Pressure(hPa),Altitude(m),Temperature,Humidity,GPSLatitude,GPSLongitude,GPSAltitude");

                        sensorStartScanning(rttInterval);
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

                        lteStartScanning(rttInterval);
                    }

                    startTimer();
                    LoadingDialog.hideDialog();
                }

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

//            wifiAdapter.swapData(accessPoints, accessPointsSupporting80211mc, accessPointsSupporting80211mcInfo);
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

                if (rtt != null) {
                    data += dateTime;
                    data += "," + contentBinding.timerRanging.getTimeElapsed();
                    data += "," + ptNum;
                    data += "," + "-";
                    data += "," + rtt.getSsid();
                    data += "," + rtt.getBssid();
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
                }

                accessPointsSupporting80211mcInfo.put(rangingResult.getMacAddress().toString(), rangingResult);
            }
        }

        public void getMyLocation(List<RangingResult> list) {
            ArrayList<Double> locationXList = new ArrayList<>();
            ArrayList<Double> locationYList = new ArrayList<>();

            Sort.Ascending ascending = new Sort.Ascending();

            if (list.size() < combination) {
                list.clear();
                return;
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
                return;
            }

            ArrayList<Double> locationXList2 = new ArrayList<>(locationXList);
            ArrayList<Double> locationYList2 = new ArrayList<>(locationYList);

            //====================================================================================================================================================
            //기존 그룹군의 중위값으로 위치 측위
            Collections.sort(locationXList, ascending);
            Collections.sort(locationYList, ascending);

            Point medianLocation = new Point(locationXList.get(locationXList.size() / 2), locationYList.get(locationYList.size() / 2));
            //====================================================================================================================================================

            //====================================================================================================================================================
            //기존 그룹군 데이터를 기반으로 DBSCAN 알고리즘 반영
            ArrayList<Double> dbScanXList = new ArrayList<>();
            ArrayList<Double> dbScanYList = new ArrayList<>();

            for (int i = 0; i < locationXList2.size(); i++) {
                ArrayList<Double> tempXList = new ArrayList<>();
                ArrayList<Double> tempYList = new ArrayList<>();

                Point criteriaPoint = new Point(locationXList2.get(i), locationYList2.get(i));

                tempXList.add(criteriaPoint.getX());
                tempYList.add(criteriaPoint.getY());

                int count = 1;

                for (int j = 0; j < locationXList2.size(); j++) {
                    if (i == j)
                        continue;

                    Point point = new Point(locationXList2.get(j), locationYList2.get(j));

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

            if (dbScanXList.size() == 0 || dbScanYList.size() == 0) {
                if (rttCsvManager != null)
                    rttCsvManager.Write(DateUtils.getCurrentDateTime() + "," + contentBinding.timerRanging.getTimeElapsed() + ",=에러=");
                return;
            }

            Collections.sort(dbScanXList, ascending);
            Collections.sort(dbScanXList, ascending);
            Point dbscanLocation = new Point(dbScanXList.get(dbScanXList.size() / 2), dbScanYList.get(dbScanYList.size() / 2));
            //====================================================================================================================================================


            if (isUseDbScan)
                myLocation = dbscanLocation;
            else
                myLocation = medianLocation;

            setMyLocationView(myLocation);
            getNearestLink(nodeArrayList, linkArrayList, myLocation);

            Log.d("RTT 실내위치 측위", myLocation.toString());
            Log.d("RTT 실내위치 측위 보정", myLocation2.toString());

            if (rttCsvManager != null)
                rttCsvManager.Write(DateUtils.getCurrentDateTime() + "," + contentBinding.timerRanging.getTimeElapsed() + ",,,,,,,,,,,," + medianLocation.getX() + "," + medianLocation.getY() + "," + dbscanLocation.getX() + "," + dbscanLocation.getY() + "," + dbscanLocation.getX() + "," + dbscanLocation.getY());

            contentBinding.txtTime.setText(DateUtils.getCurrentCsvFileName());
            contentBinding.txtLocation.setText(myLocation.getX() + "m - " + myLocation.getY() + "m");
            contentBinding.txtLocation2.setText(myLocation2.getX() + "m - " + myLocation2.getY() + "m");

            list.clear();
//            accessPointsSupporting80211mcInfo.clear();
        }

        //a,b의 포인트 좌표를 픽셀에서 M단위로 수정해야함
        private Link getNearestLink(ArrayList<Node> nodeArrayList, ArrayList<Link> linkArrayList, Point point) {
            double distance = Double.MAX_VALUE;
            Link nearestLink = null;

            for (Link link : linkArrayList) {
                Point start = nodeArrayList.get(link.getNode_start() - 1).getPoint();
                Point end = nodeArrayList.get(link.getNode_end() - 1).getPoint();

                double x1 = (start.getX() - LEFT_BLANK_PIXEL) * PIXEL_PER_METER;
                double y1 = (MAP_HEIGHT - start.getY() - BOTTOM_BLANK_PIXEL) * PIXEL_PER_METER;
                double x2 = (end.getX() - LEFT_BLANK_PIXEL) * PIXEL_PER_METER;
                double y2 = (MAP_HEIGHT - end.getY() - BOTTOM_BLANK_PIXEL) * PIXEL_PER_METER;
                double px = point.getX();
                double py = point.getY();

                double temp = getDist2LineSegment(px, py, x1, y1, x2, y2);

                //거리가 짧은 링크의 정보를 담아 갱신
                if (distance > temp) {
                    distance = temp;
                    nearestLink = link;
                    myLocation2 = nearLocation;
                }
            }

            Log.d("가장 가까운 링크", nearestLink.toString());
            Log.d("가장 가까운 링크 거리", distance + "m");

            setMyLocationView2(myLocation2);

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
