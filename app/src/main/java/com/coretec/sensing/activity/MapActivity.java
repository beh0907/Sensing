package com.coretec.sensing.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;

import com.coretec.sensing.R;
import com.coretec.sensing.databinding.ActivityMapBinding;
import com.coretec.sensing.databinding.ContentMapBinding;
import com.coretec.sensing.dialog.ListDialog;
import com.coretec.sensing.listener.OnTouchMapListener;
import com.coretec.sensing.listener.RecyclerViewClickListener;
import com.coretec.sensing.model.Ap;
import com.coretec.sensing.model.Link;
import com.coretec.sensing.model.Node;
import com.coretec.sensing.model.Poi;
import com.coretec.sensing.sqlite.ApHelper;
import com.coretec.sensing.sqlite.LinkHelper;
import com.coretec.sensing.sqlite.NodeHelper;
import com.coretec.sensing.sqlite.PoiHelper;
import com.coretec.sensing.utils.Calculation;
import com.coretec.sensing.view.MoveImageView;
import com.google.android.material.navigation.NavigationView;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import lombok.SneakyThrows;
import no.wtw.android.dijkstra.DijkstraAlgorithm;
import no.wtw.android.dijkstra.model.Edge;
import no.wtw.android.dijkstra.model.Graph;
import no.wtw.android.dijkstra.model.Vertex;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.coretec.sensing.utils.Const.PIXEL_PER_METER;

public class MapActivity extends AppCompatActivity implements OnTouchMapListener, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private ActivityMapBinding activityBinding;
    private ContentMapBinding contentBinding;
    private ArrayList<MoveImageView> listPointImage = new ArrayList<>();

    private PoiHelper poiHelper = new PoiHelper();
    private ApHelper apHelper = new ApHelper();
    private NodeHelper nodeHelper = new NodeHelper();
    private LinkHelper linkHelper = new LinkHelper();

    private ArrayList<Poi> poiArrayList;
    private HashMap<String, Ap> apHashMap;
    private ArrayList<Node> nodeArrayList;
    private ArrayList<Link> linkArrayList;
    private LinkedList<Vertex> path;

    private Graph pathGraph;

    private int pathDistance;
    private boolean isPlaying = false;

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
        ;
        ;
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionListener permissionlistener = new PermissionListener() {
            @SneakyThrows
            @Override
            public void onPermissionGranted() {
                init();
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
                .setPermissions(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH)
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
    }

    private void initPath() {
        poiArrayList = poiHelper.selectAllPoiList();
        apHashMap = apHelper.selectAllApList();
        nodeArrayList = nodeHelper.selectAllNodeList();
        linkArrayList = linkHelper.selectAllLinkList();

        ArrayList<Edge> edgeArrayList = new ArrayList<>();
        for (Link link : linkArrayList) {
            edgeArrayList.add(new Edge(new Vertex<>(link.getNode_start()), new Vertex<>(link.getNode_end()), link.getWeight_p()));
        }
        pathGraph = new Graph(edgeArrayList);
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
    private void searchPath() {
        DijkstraAlgorithm dijkstraAlgorithm = new DijkstraAlgorithm(pathGraph).execute(new Vertex<>(13));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MapActivity.this, android.R.layout.select_dialog_singlechoice);

        for (Poi poi : poiArrayList)
            adapter.add(poi.getName());

        ListDialog.CreateListDialog(adapter, MapActivity.this, new RecyclerViewClickListener() {
            @SneakyThrows
            @Override
            public void onClick(View view, int position) {
                contentBinding.btnFind.setVisibility(View.GONE);
                contentBinding.layoutInfoNavi.setVisibility(View.VISIBLE);

                removeAlMarker();
                contentBinding.imgMap.initPath();

                pathDistance = (int) (dijkstraAlgorithm.getDistance(new Vertex<>(position + 1)) * PIXEL_PER_METER);
                path = dijkstraAlgorithm.getPath(new Vertex<>(position + 1));

                contentBinding.txtNaviLen.setText(pathDistance + "m");
                contentBinding.txtNaviTime.setText((pathDistance / 66 + 1) + "분");

                contentBinding.txtStart.setText("현재 위치");
                contentBinding.txtEnd.setText(poiArrayList.get(position).getName());

                for (int i = 0; i < path.size(); i++) {
                    Vertex vertex = path.get(i);
                    Node node = nodeArrayList.get((Integer) vertex.getPayload() - 1);
                    contentBinding.imgMap.addPath((float) node.getPoint().getX(), (float) node.getPoint().getY());
                    Log.d("다익스트라 알고리즘 테스트", "경로 최단 경로 노드 순서 : " + node);

                    int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
                    int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

                    if (i == 0)
                        addPoint(parentWidth, parentHeight, (int)  node.getPoint().getX(), (int)  node.getPoint().getY(), R.drawable.ic_departure);

                    if (i == path.size() - 1)
                        addPoint(parentWidth, parentHeight, (int)  node.getPoint().getX(), (int)  node.getPoint().getY(), R.drawable.ic_destination);
                }
            }
        });
    }

    //지정된 좌표 상에 이미지 표출
    private void addPoint(int parentWidth, int parentHeight, int posX, int posY, int resId) {
        //경로 추가
//        contentBinding.imgMap.addPath(posX, posY);

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
    }

    @Override
    public void onClick(View v) {
        int resId = v.getId();

        switch (resId) {
            case R.id.btnFind:
                searchPath();
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
                contentBinding.imgMap.initPath();
                break;
        }
    }
}
