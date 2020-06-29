package com.coretec.sensing.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.coretec.sensing.R;
import com.coretec.sensing.adapter.PointAdapter;
import com.coretec.sensing.databinding.ActivityMapBinding;
import com.coretec.sensing.databinding.ContentMapBinding;
import com.coretec.sensing.dialog.AlarmDialog;
import com.coretec.sensing.listener.OnTouchMapListener;
import com.coretec.sensing.listener.RecyclerViewClickListener;
import com.coretec.sensing.model.Link;
import com.coretec.sensing.model.Node;
import com.coretec.sensing.model.Poi;
import com.coretec.sensing.model.Point;
import com.coretec.sensing.sqlite.LinkHelper;
import com.coretec.sensing.sqlite.NodeHelper;
import com.coretec.sensing.sqlite.PoiHelper;
import com.coretec.sensing.utils.CsvManager;
import com.coretec.sensing.utils.ImageUtils;
import com.coretec.sensing.view.MoveImageView;
import com.google.android.material.navigation.NavigationView;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.ArrayList;
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
import static com.coretec.sensing.utils.Const.BOTTOM_BLANK_METER;
import static com.coretec.sensing.utils.Const.LEFT_BLANK_METER;
import static com.coretec.sensing.utils.Const.PIXEL_PER_METER;

public class MapActivity extends AppCompatActivity implements OnTouchMapListener, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, Spinner.OnItemSelectedListener {

    private ActivityMapBinding activityBinding;
    private ContentMapBinding contentBinding;
    private ArrayList<MoveImageView> listPointImage = new ArrayList<>();
    private ArrayList<Point> pointArrayList;
    private PointAdapter pointAdapter;

    private ArrayList<Integer> markerImage = new ArrayList<>();

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
;;
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
                initList();
                loadMarkerImage();
                loadMapImage();
                setBackgroundPosition();

                PoiHelper poiHelper = new PoiHelper();
                NodeHelper nodeHelper = new NodeHelper();
                LinkHelper linkHelper = new LinkHelper();

                ArrayList<Poi> poiArrayList = poiHelper.selectAllPoiList();
                ArrayList<Node> nodeArrayList = nodeHelper.selectAllNodeList();
                ArrayList<Link> linkArrayList = linkHelper.selectAllLinkList();

                ArrayList<Edge> edgeArrayList = new ArrayList<>();
                for (Link link : linkArrayList) {
                    edgeArrayList.add(new Edge(new Vertex<>(link.getNode_start()), new Vertex<>(link.getNode_end()), link.getWeight_p()));
                }
                Graph pathGraph = new Graph(edgeArrayList);

                DijkstraAlgorithm dijkstraAlgorithm = new DijkstraAlgorithm(pathGraph).execute(new Vertex<>(13));

                String pathDistance = (dijkstraAlgorithm.getDistance(new Vertex<>(5)) * PIXEL_PER_METER) + "m";
                LinkedList<Vertex> path = dijkstraAlgorithm.getPath(new Vertex<>(5));

                for (Vertex vertex : path) {
                    Node node = nodeArrayList.get((Integer) vertex.getPayload() - 1);
                    contentBinding.imgMap.addPath((float) node.getPoint().getX(), (float) node.getPoint().getY());
                    Log.d("다익스트라 알고리즘 테스트", "경로 최단 경로 노드 순서 : " + node);
                }

                Log.d("다익스트라 알고리즘 테스트", "경로 최단 경로 거리 값 : " + pathDistance);

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

        contentBinding.btnFind.setOnClickListener(this);
        contentBinding.btnSave.setOnClickListener(this);
        contentBinding.imgMap.setOnTouchMapView(this);
        contentBinding.spinnerStart.setOnItemSelectedListener(this);
        contentBinding.spinnerEnd.setOnItemSelectedListener(this);
        activityBinding.navView.setNavigationItemSelectedListener(this);

//        contentBinding.layoutNavigation.removeAllViews();
    }

    private void initList() {
        pointArrayList = new ArrayList<>();

        pointAdapter = new PointAdapter(pointArrayList, new RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                Point point = pointArrayList.get(position);

                int[] pixelPoint = contentBinding.imgMap.pointMeterToPixel(new double[]{point.getX(), point.getY()});

                Log.d("ddd", pixelPoint[0] + " - " + pixelPoint[1]);

                contentBinding.imgMap.setPointPosition(pixelPoint[0], pixelPoint[1]);

                //터치 이벤트를 발생시켜 마커 이미지의 매트릭스를 지도에 따라 갱신
                onTouchMap();
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        contentBinding.listLocation.setLayoutManager(linearLayoutManager);
        contentBinding.listLocation.setAdapter(pointAdapter);
    }

    private void loadMarkerImage() {
        for (int i = 1; i < 100; i++) { //마커 번호에 대한 이미지 배열 저장
            markerImage.add(getResources().getIdentifier("tracker" + i, "drawable", getPackageName()));
        }
        markerImage.add(getResources().getIdentifier("tracker99_more", "drawable", getPackageName()));
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

    private void initSpinner() {
        ArrayList<String> arrayList = new ArrayList<>();

        for (int i = 0; i < pointArrayList.size(); i++) {
            arrayList.add((i + 1) + "");
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, arrayList);

        contentBinding.spinnerStart.setAdapter(arrayAdapter);
        contentBinding.spinnerEnd.setAdapter(arrayAdapter);
    }

    private void updateDistance() {
        Point startPoint = pointArrayList.get(contentBinding.spinnerStart.getSelectedItemPosition());
        Point endPoint = pointArrayList.get(contentBinding.spinnerEnd.getSelectedItemPosition());

        contentBinding.txtDistance.setText(String.format("%.4fm", ImageUtils.pointToPointDistance(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY())));
    }

    private void setBackgroundPosition() {
        int width = (int) (contentBinding.imgMap.getDrawable().getIntrinsicWidth() / 2f);
        int height = (int) (contentBinding.imgMap.getDrawable().getIntrinsicHeight() / 2f);

        contentBinding.imgMap.initPosition(width, height);
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
        contentBinding.layoutNavigation.addView(imgDonut);
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
            case R.id.btnSave:
                saveLocationFile();
                break;
            case R.id.btnFind:
                Intent intent = new Intent(this, LocationActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateDistance();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void deleteImageView(MoveImageView imageView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("선택한 마커를 삭제하시겠습니까?");

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int position = listPointImage.indexOf(imageView);

                listPointImage.remove(position);
                pointArrayList.remove(position);
                contentBinding.layoutNavigation.removeView(imageView);
                pointAdapter.refreshPointList(pointArrayList);

                for (int i = position; i < listPointImage.size(); i++) {
                    MoveImageView moveImageView = listPointImage.get(i);
                    moveImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), markerImage.get(i)));
                }

                //포인트 삭제 시 스피너 갱신
                initSpinner();
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

    public MoveImageView getImageViewPosition(float srcX, float srcY, Matrix matrix, float scale) {
        float[] dst = new float[2];
        float[] src = new float[2];

        for (int i = 0; i < listPointImage.size(); i++) {
            MoveImageView imageView = listPointImage.get(i);

            int[] posLocation = imageView.getPosLocation();

            src[0] = ImageUtils.getDp(this, posLocation[0]);
            src[1] = ImageUtils.getDp(this, posLocation[1]);
            matrix.mapPoints(dst, src);

            if (srcX > dst[0] - ImageUtils.getDp(this, 80) && srcX < dst[0] + ImageUtils.getDp(this, 50) && srcY > dst[1] - ImageUtils.getDp(this, 80) && srcY < dst[1]) {
                //선택한 마커가 보이도록 리스트 이동
                contentBinding.listLocation.scrollToPosition(i);
                return imageView;
            }
        }
        return null;
    }

    public void setImageViewPosition(MoveImageView imageView, float[] pixelPoint, float[] meterPoint) {
        int position = listPointImage.indexOf(imageView);

        imageView.setPosLocation(pixelPoint);
        listPointImage.set(position, imageView);
        pointArrayList.set(position, new Point(meterPoint[0], meterPoint[1]));
        pointAdapter.refreshPointList(pointArrayList);
    }

    public void addTouchPoint(float[] point) {

        int parentWidth = contentBinding.imgMap.getDrawable().getIntrinsicWidth();
        int parentHeight = contentBinding.imgMap.getDrawable().getIntrinsicHeight();

        float scaleX = (parentWidth / 2848f);
        float scaleY = (parentHeight / 4574f);

        float reverseY = (parentHeight - point[1]) / scaleY;

        point[0] /= scaleX;
        point[1] /= scaleY;

        addPoint(parentWidth, parentHeight, (int) point[0], (int) point[1], markerImage.get(pointArrayList.size()));

        pointArrayList.add(new Point((point[0] * PIXEL_PER_METER) - LEFT_BLANK_METER, (reverseY * PIXEL_PER_METER) - BOTTOM_BLANK_METER));
        pointAdapter.refreshPointList(pointArrayList);

        //추가시 리스트 최하단으로 이동
        contentBinding.listLocation.scrollToPosition(pointAdapter.getItemCount() - 1);

        //포인트 추가 시 스피너 갱신
        initSpinner();
    }

    private void saveLocationFile() {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;
        String fileName = contentBinding.editFileName.getText().toString();

        File fileLocation = new File(filePath, fileName + "_LOCATION.csv");

        if (fileLocation.exists()) {
            AlarmDialog.showDialog(this, "파일명이 중복되어 스캔할 수 없습니다.\n파일을 삭제하거나 작성 파일명을 수정해주세요.");
            return;
        }

        CsvManager csvManager = new CsvManager("/" + fileName + "_LOCATION.csv");
        csvManager.Write("Number,Location_X(m),Location_Y(m)");

        for (int i = 0; i < pointArrayList.size(); i++) {
            Point point = pointArrayList.get(i);

            csvManager.Write((i + 1) + "," + point.getX() + "," + point.getY());
        }
        Toast.makeText(this, "파일이 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
