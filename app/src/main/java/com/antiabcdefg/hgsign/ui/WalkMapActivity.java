package com.antiabcdefg.hgsign.ui;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.antiabcdefg.hgsign.R;
import com.antiabcdefg.hgsign.bean.TimeBean;
import com.antiabcdefg.hgsign.utils.CommonUtil;
import com.antiabcdefg.hgsign.utils.DBManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;


public class WalkMapActivity extends AppCompatActivity implements LocationSource,
        AMapLocationListener {

    private int[] colors = new int[]{Color.argb(255, 0, 255, 0), Color.argb(255, 0, 150, 136), Color.argb(255, 255, 193, 7), Color.argb(255, 205, 220, 57)};

    private AMap aMap;
    private MapView mapView;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
//    private LBSTraceClient mTraceClient;

    private Button before;
    private Button today;
    private Button after;

    private ProgressDialog mypDialog = null;
    private DBManager dbManager;
    private volatile boolean isFirstZoom = true;

    private String dateTemp;
    private ArrayList<TimeBean> timeInfo;
    private ArrayList<ArrayList<LatLng>> LatLngInfo;
//    private ArrayList<ArrayList<TraceLocation>> TraceInfo;
//    private ConcurrentMap<Integer, TraceOverlay> mOverlayList = new ConcurrentHashMap<Integer, TraceOverlay>();

    private int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_mapshow);
        mapView = $(R.id.mapView);
        mapView.onCreate(savedInstanceState);// ?????????????????????,??????????????????

        initView();
        initListener();
        initData();

        aMap.clear();
        drawMap(dateTemp);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("aaa", "onRestart()");
    }

    /**
     * ???onCreate,onRestart????????????
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("aaa", "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        Log.i("aaa", "onResume()");

    }

    /**
     * ??????????????????
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
        Log.i("aaa", "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("aaa", "onStop()");
    }

    /**
     * ??????????????????
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        dbManager.closeDB();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
        Log.i("aaa", "onDestroy()");
    }

    protected void initView() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        before = $(R.id.btn_before);
        today = $(R.id.btn_today);
        after = $(R.id.btn_after);
    }

    protected void initData() {
        mypDialog = CommonUtil.getProcessDialog(WalkMapActivity.this, "????????????>>>");
        dbManager = new DBManager(WalkMapActivity.this);
//        TraceInfo = new ArrayList<ArrayList<TraceLocation>>();
        LatLngInfo = new ArrayList<>();
        timeInfo = new ArrayList<>();

//        mTraceClient = new LBSTraceClient(getApplicationContext());

        if (getIntent() != null)
            dateTemp = getIntent().getStringExtra("date");
    }

    protected void initListener() {
        before.setOnClickListener(v -> {
            aMap.clear();
            drawMap(CommonUtil.todayBefore(dateTemp));
            dateTemp = CommonUtil.todayBefore(dateTemp);
        });
        today.setOnClickListener(v -> {
            aMap.clear();
            drawMap(CommonUtil.Date2String(new Date()));
            dateTemp = CommonUtil.Date2String(new Date());
        });
        after.setOnClickListener(v -> {
            aMap.clear();
            drawMap(CommonUtil.todayAfter(dateTemp));
            dateTemp = CommonUtil.todayAfter(dateTemp);
        });
        aMap.setOnMapClickListener(latLng -> {
            if (count % 2 == 0) setBtnView(false);
            else setBtnView(true);
            count++;
        });
        aMap.setOnMarkerClickListener(marker -> false);
    }

    public void setBtnView(Boolean bool) {
        if (bool) {
            before.setVisibility(View.VISIBLE);
            today.setVisibility(View.VISIBLE);
            after.setVisibility(View.VISIBLE);
        } else {
            before.setVisibility(View.INVISIBLE);
            today.setVisibility(View.INVISIBLE);
            after.setVisibility(View.INVISIBLE);
        }
    }

    public void setUpMap() {
        aMap.setLocationSource(this);// ??????????????????
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// ????????????????????????????????????
        aMap.setMyLocationEnabled(true);// ?????????true??????????????????????????????????????????false??????????????????????????????????????????????????????false
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                33.5460901204, 119.0362459272), 15));
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
                .fromResource(R.mipmap.location_marker));
        myLocationStyle.strokeWidth(0);
        myLocationStyle.radiusFillColor(Color.alpha(0));
        myLocationStyle.strokeColor(Color.alpha(0));// ???????????????????????????,???????????????
        aMap.setMyLocationStyle(myLocationStyle);

    }

    private void drawMap(String date) {
        mypDialog.show();
        if (!TextUtils.isEmpty(date)) {
            LatLngInfo = dbManager.queryLocationList(date);
            LatLngInfo = dbManager.removeToLong(LatLngInfo);
            LatLngInfo = dbManager.correctLocations(LatLngInfo);
            LatLngInfo = dbManager.correcteZShape(LatLngInfo);
            timeInfo = dbManager.queryTimeList(date);
//          TraceInfo=dbManager.queryTraceList(date);
//            int size = TraceInfo.size();
//            if (size != 0) {
//                for (int i = 0; i < size; i++) {
//                    traceGrasp(1000 + i,TraceInfo.get(i));
//                }
//                mypDialog.dismiss();
//                CommonUtil.ToastLong(WalkMapActivity.this, "????????????");
//            } else {
//                mypDialog.dismiss();
//                CommonUtil.ToastLong(WalkMapActivity.this, "????????????");
//            }

            if (LatLngInfo.size() != 0) {
                for (int i = 0, len = LatLngInfo.size(); i < len; i++) {
                    ArrayList<LatLng> lalotemp = LatLngInfo.get(i);
                    addMarkers(lalotemp.get(0), true, timeInfo.get(i).getStartTime(), i + 1);
                    upMap(lalotemp, colors[(int) (Math.random() * colors.length)]);
                    addMarkers(lalotemp.get(lalotemp.size() - 1), false, timeInfo.get(i).getEndTime(), i + 1);
                }
                mypDialog.dismiss();
                CommonUtil.ToastLong(WalkMapActivity.this, "????????????");
            } else {
                mypDialog.dismiss();
                CommonUtil.ToastLong(WalkMapActivity.this, "????????????");
            }
        }

    }

    private void upMap(List<LatLng> points, int i) {
        aMap.addPolyline((new PolylineOptions()).addAll(points).geodesic(true)
                .useGradient(true).color(i).width(20));
    }

//    private void traceGrasp(int mSequenceLineID, List<TraceLocation> mTraceList) {
//        if (mOverlayList.containsKey(mSequenceLineID)) {
//            TraceOverlay overlay = mOverlayList.get(mSequenceLineID);
//            overlay.zoopToSpan();
//            int status = overlay.getTraceStatus();
//            String tipString = "";
//            if (status == TraceOverlay.TRACE_STATUS_PROCESSING) {
//                tipString = "??????????????????????????????...";
//            } else if (status == TraceOverlay.TRACE_STATUS_FINISH) {
//                tipString = "????????????????????????";
//            } else if (status == TraceOverlay.TRACE_STATUS_FAILURE) {
//                tipString = "?????????????????????";
//            } else if (status == TraceOverlay.TRACE_STATUS_PREPARE) {
//                tipString = "?????????????????????????????????";
//            }
//            CommonUtil.ToastShort(WalkMapActivity.this, tipString);
//            return;
//        }
//        TraceOverlay mTraceOverlay = new TraceOverlay(aMap);
//        mOverlayList.put(mSequenceLineID, mTraceOverlay);
//        List<LatLng> mapList = traceLocationToMap(mTraceList);
//        mTraceOverlay.setProperCamera(mapList);
//        mTraceClient.queryProcessedTrace(mSequenceLineID, mTraceList, LBSTraceClient.TYPE_AMAP, this);
//
//    }

    private void addMarkers(LatLng pointTemp, boolean isStart, String time, int pointTimes) {
        if (isStart)
            aMap.addMarker(new MarkerOptions()
                    .position(pointTemp)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.start))
                    .snippet(time)
                    .title("???" + pointTimes + "???????????????"));
        else
            aMap.addMarker(new MarkerOptions()
                    .position(pointTemp)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.end))
                    .snippet(time)
                    .title("???" + pointTimes + "???????????????"));
    }

    /**
     * ??????????????????
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        Log.i("aaa", "onSaveInstanceState()");
    }

    /**
     * ???????????????????????????
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                // mListener.onLocationChanged(amapLocation);// ?????????????????????
                if (isFirstZoom) {
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                    isFirstZoom = false;
                }
                mlocationClient.stopLocation();// ???????????????????????????
            }

        }
    }

    /**
     * ????????????
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            // ??????????????????
            mlocationClient.setLocationListener(this);
            // ??????????????????????????????
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // ??????????????????
            mLocationOption.setWifiScan(true);
            mLocationOption.setInterval(7000);
            // ??????????????????????????????????????????????????????????????????2000ms?????????????????????????????????stopLocation()???????????????????????????
            mLocationOption.setGpsFirst(true);
            // mLocationOption.setKillProcess(true);
            mlocationClient.setLocationOption(mLocationOption);
            // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // ???????????????????????????????????????????????????onDestroy()??????
            // ?????????????????????????????????????????????????????????????????????stopLocation()???????????????????????????sdk???????????????
            mlocationClient.startLocation();
        }
    }

    /**
     * ????????????
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    public <T extends View> T $(int id) {
        return (T) findViewById(id);
    }

//    @Override
//    public void onRequestFailed(int lineID, String errorInfo) {
//        CommonUtil.ToastShort(WalkMapActivity.this, errorInfo);
//        if (mOverlayList.containsKey(lineID)) {
//            TraceOverlay overlay = mOverlayList.get(lineID);
//            overlay.setTraceStatus(TraceOverlay.TRACE_STATUS_FAILURE);
//        }
//    }

//    @Override
//    public void onTraceProcessing(int lineID, int index, List<LatLng> segments) {
//        if (segments == null) {
//            return;
//        }
//        if (mOverlayList.containsKey(lineID)) {
//            TraceOverlay overlay = mOverlayList.get(lineID);
//            overlay.setTraceStatus(TraceOverlay.TRACE_STATUS_PROCESSING);
//            overlay.add(segments);
//        }
//    }

//    @Override
//    public void onFinished(int lineID, List<LatLng> linepoints, int distance, int watingtime) {
//        if (mOverlayList.containsKey(lineID)) {
//            TraceOverlay overlay = mOverlayList.get(lineID);
//            overlay.setTraceStatus(TraceOverlay.TRACE_STATUS_FINISH);
//        }
//    }

//    public List<LatLng> traceLocationToMap(List<TraceLocation> traceLocationList) {
//        List<LatLng> mapList = new ArrayList<LatLng>();
//        for (TraceLocation location : traceLocationList) {
//            LatLng latlng = new LatLng(location.getLatitude(),
//                    location.getLongitude());
//            mapList.add(latlng);
//        }
//        return mapList;
//    }

}