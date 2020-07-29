package com.coretec.sensing.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;

import com.coretec.sensing.R;
import com.coretec.sensing.databinding.ActivityMapBinding;
import com.coretec.sensing.databinding.ContentMapBinding;
import com.coretec.sensing.dialog.LoadingDialog;
import com.coretec.sensing.listener.OnTouchMapListener;
import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Link;
import com.coretec.sensing.model.Node;
import com.coretec.sensing.model.Poi;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.sqlite.ApHelper;
import com.coretec.sensing.sqlite.DBDownload;
import com.coretec.sensing.sqlite.LinkHelper;
import com.coretec.sensing.sqlite.NodeHelper;
import com.coretec.sensing.sqlite.PoiHelper;
import com.coretec.sensing.utils.Calculation;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.DateUtils;
import com.coretec.sensing.utils.PrefManager;
import com.coretec.sensing.utils.Sort;
import com.coretec.sensing.view.MoveImageView;
import com.github.dakusui.combinatoradix.Combinator;
import com.google.android.material.navigation.NavigationView;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
    private ArrayList<MoveImageView> listPointImage = new ArrayList<>();

    private MoveImageView myLocationView;
    private MoveImageView myLocationView2;

    private PoiHelper poiHelper;
    private ApHelper apHelper;
    private NodeHelper nodeHelper;
    private LinkHelper linkHelper;

    private ArrayList<Poi> poiArrayList;
    private HashMap<String, Ap> apHashMap;
    private ArrayList<Node> nodeArrayList;
    private ArrayList<Link> linkArrayList;
    private LinkedList<Vertex> path;

    private Graph pathGraph;

    private int pathDistance;
    private boolean isPlaying = false;

    private Point myLocation;
    private Point myLocation2;
    private Point nearLocation;

    private CsvManager locationCsvManager;


    private static TimerTask wifiTimer;
    private static TimerTask rttTimer;

    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private WifiRttManager wifiRttManager;

    private RttRangingResultCallback rttRangingResultCallback;

    private ArrayList<ScanResult> accessPoints;
    private HashMap<String, ScanResult> accessPointsSupporting80211mc;
    private HashMap<String, RangingResult> accessPointsSupporting80211mcInfo;

    private boolean startScan = false;

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

        if (wifiScanReceiver != null)
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (wifiScanReceiver != null)
            unregisterReceiver(wifiScanReceiver);
    }

    private void downloadDB() {
        PrefManager pref = new PrefManager(this);
//        if (!pref.isDownloadDB())
        DBDownload.copyDB(pref, this);
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
                initPath();
                loadMapImage();
                setBackgroundPosition();


                createLocationCsvFile(DateUtils.getCurrentCsvFileName());

                LoadingDialog.showDialog(MapActivity.this, "AP를 스캔 중입니다... (" + accessPointsSupporting80211mc.size() + "/10)");
                scanWifi();
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
                .setPermissions(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH, ACCESS_WIFI_STATE, ACCESS_BACKGROUND_LOCATION)
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

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();

        wifiRttManager = (WifiRttManager) getApplicationContext().getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        rttRangingResultCallback = new RttRangingResultCallback();

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
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

        Bitmap result = BitmapFactory.decodeResource(resource, R.drawable.bg_map, options);
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
        myLocationView.initPosition();
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
        }
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


    public void createLocationCsvFile(String fileName) {
        locationCsvManager = new CsvManager(fileName + ".csv");
        locationCsvManager.Write("DATE,TIME,SEC,point(X),point(Y)");
    }


    public void wifiStartScanning() {
        wifiStopScanning();
        wifiScanDelayRequest();
    }

    public void rttStartScanning() {
        rttStopScanning();
        rttScanDelayRequest();
    }

    public void wifiStopScanning() {
        if (wifiTimer != null) {
            wifiTimer.cancel();
            wifiTimer = null;
        }
    }

    public void rttStopScanning() {
        if (rttTimer != null) {
            rttTimer.cancel();
            rttTimer = null;
        }
    }


    //2분 4회의 wifi 스캔 제한이 있기 떄문에
    //30초에 한번식 강제적으로 스캔 시도
    private void wifiScanDelayRequest() {
        wifiTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        scanWifi();
//                            wifiScanDelayRequest();
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(wifiTimer, 0, 5000);
    }

    private void rttScanDelayRequest() {
        rttTimer = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() { //ui 동작을 하기 위해 runOnUiThread 사용
                    @SuppressLint("DefaultLocale")
                    public void run() {
                        startRangingRequest();
                    }
                });
            }
        };

        // 0초후 첫실행, 설정된 Delay마다 실행
        Timer timer = new Timer();
        timer.schedule(rttTimer, 0, 1000);
    }

    private void startRangingRequest() {
//        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT))
//            return;

        //체크된 RTT지원 항목만 얻음
        ArrayList<ScanResult> results = new ArrayList<>(accessPointsSupporting80211mc.values());

        //RTT 리스트가 1개 이상 있을경우 콜백 이벤트 등록
        if (results.size() > 0) {
            RangingRequest rangingRequest =
                    new RangingRequest.Builder().addAccessPoints(results).build();

            wifiRttManager.startRanging(rangingRequest, getApplication().getMainExecutor(), rttRangingResultCallback);
        }
    }


    public void scanWifi() {
        wifiManager.startScan();

//        if (isScan) {
//            Toast.makeText(getApplicationContext(), "와이파이를 조회하고 있습니다.", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(getApplicationContext(), "스캔 한도가 초과하여 스캔에 실패했습니다.", Toast.LENGTH_SHORT).show();
//        }
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

                distanceList.put(ap.getMacAddress(), Calculation.getDistance(myLocation, ap.getPoint()));
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

    private class WifiScanReceiver extends BroadcastReceiver {
        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {

            List<ScanResult> scanResults = wifiManager.getScanResults();

//            Log.d("와이파이 데이터 테스트 태그", scanResults.toString());

            //RTT를 지원하지 않는 WIFI 객체 리스트 저장
            findAccessPoints(scanResults);

            //RTT를 지원하는 WIFI 객체 리스트 저장
            find80211mcSupportedAccessPoints(scanResults);

            if (accessPointsSupporting80211mc.size() < 10) {
                LoadingDialog.updateMessage("AP를 스캔 중입니다... (" + accessPointsSupporting80211mc.size() + "/10)");
                scanWifi();
            } else {
                rttStartScanning();
                wifiStartScanning();

                LoadingDialog.hideDialog();
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
                if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                    accessPointsSupporting80211mcInfo.put(rangingResult.getMacAddress().toString(), rangingResult);
                }
            }

//            Log.d("RTT 데이터 테스트 태그", accessPointsSupporting80211mcInfo.toString());
//            wifiAdapter.swapData(accessPoints, accessPointsSupporting80211mc, accessPointsSupporting80211mcInfo);

            getMyLocation(list);
        }

        private void getMyLocation(List<RangingResult> list) {
            ArrayList<Double> locationXList = new ArrayList<>();
            ArrayList<Double> locationYList = new ArrayList<>();
            Sort.Ascending ascending = new Sort.Ascending();


            for (List<RangingResult> each : new Combinator<>(list, 4)) {
                double[][] myLocation = Calculation.getMyLocation(each, apHashMap);

                if (myLocation != null) {
                    locationXList.add(myLocation[0][0]);
                    locationYList.add(myLocation[1][0]);
                }
            }
            Collections.sort(locationXList, ascending);
            Collections.sort(locationYList, ascending);

            if (locationXList.size() == 0 || locationYList.size() == 0) {
                return;
            }

            myLocation = new Point(Math.abs(locationXList.get(locationXList.size() / 2)), Math.abs(locationYList.get(locationYList.size() / 2)));

//            locationCsvManager.Write(DateUtils.getCurrentDateTime() + "," + myLocation.getX() + "," + myLocation.getY());

            setMyLocationView(myLocation);
            getNearestLink(nodeArrayList, linkArrayList, myLocation);

            Log.d("RTT 실내위치 측위", myLocation.toString());
            Log.d("RTT 실내위치 측위 보정", myLocation2.toString());
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
