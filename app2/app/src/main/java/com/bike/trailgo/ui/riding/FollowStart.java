package com.bike.trailgo.ui.riding;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.bike.trailgo.R;
import com.bike.trailgo.SoundManager;
import com.bike.trailgo.databinding.ActivityRidingStartBinding;
import com.bike.trailgo.databinding.FlowBinding;
import com.bike.trailgo.dto.DangerousArea;
import com.bike.trailgo.gpx.GPXParser;
import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.net.ServerApi;
import com.bike.trailgo.setting.RideSettings;
import com.google.android.material.tabs.TabLayout;
import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("HandlerLeak")
public class FollowStart extends AppCompatActivity
        implements LocationListener, MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    private static final long SESSION_CHECKPOINT_INTERVAL_MS = 10_000L;
    private SoundPool soundPool;
    private SoundManager soundManager;
    private boolean play;
    private FlowBinding binding;
    private MapView mMapView;
    private MapPOIItem mCustomMarker;
    private LocationManager locationManager;
    private boolean rideLocationReceiverRegistered;
    private final BroadcastReceiver rideLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(RideTrackingService.EXTRA_LOCATION);
            if (location != null) {
                onLocationChanged(location);
            }
        }
    };
    private Location mLastlocation = null;
    private long lastSessionCheckpointMillis = 0L;
    private int cnt=0;

    private String getDalat, getDalon, getDacon, address_dong;
    //각종 변수
    private double latitude, lonngitude, getSpeed = 0, hap = 0, avg = 0, maX = 0, maxLat = 0, maxLon = 0, minLat = 1000, minLon = 1000;
    private int getgodo, getgodoval = 0;
    private int  total_time=0, rest=0;
    private ArrayList<Double> witch_lat = new ArrayList<>();
    private ArrayList<Double> witch_lon = new ArrayList<>();
    private ArrayList<Double> ele = new ArrayList<>();
    private ArrayList<Float> godoArray = new ArrayList<>();
    private String a;
    //형변환용변수
    private String cha_dis = "0", cha_max = "0", cha_avg = "0", adress_value = "";

    private List<Address> addr = null;
    private Geocoder gCoder;

    //이동시간 핸들러
    long BaseTime;
    final static int Init = 0;
    final static int Run = 1;
    final static int Pause = 2;
    int cur_status = Init;
    long PauseTime;
    private Thread timeThread = null;
    private Boolean isRunning = true;
    private Spinner spinner;
    private ArrayList<String> list;
    private String number;
    Intent intent;
    SpeechRecognizer mRecognizer;
    private boolean voicePermissionOfferShown;
    private String gpx_name;
    private String c_name, comp_point, comp_name;
    private final CourseGuidanceEngine courseGuidance = new CourseGuidanceEngine();
    private TextView courseFollowStatus;
    private TextView courseFollowProgress;

    //gpx관련
    Gpx parsedGpx = null;
    GPXParser mParser = new GPXParser();
    //레트로핏
    private ServerApi serverApi = Server.getInstance().getApi();
    private Call<List<DangerousArea>> DangerGet;
    private List<DangerousArea> getDanger;


    // CalloutBalloonAdapter 인터페이스 구현
    class CustomCalloutBalloonAdapter implements CalloutBalloonAdapter {
        private final View mCalloutBalloon;

        public CustomCalloutBalloonAdapter() {
            mCalloutBalloon = getLayoutInflater().inflate(R.layout.custom_callout_balloon, null);
        }

        @Override
        public View getCalloutBalloon(MapPOIItem poiItem) {
            ((ImageView) mCalloutBalloon.findViewById(R.id.badge)).setImageResource(R.drawable.danger_icon);
            ((TextView) mCalloutBalloon.findViewById(R.id.title)).setText(poiItem.getItemName());
            return mCalloutBalloon;
        }

        @Override
        public View getPressedCalloutBalloon(MapPOIItem poiItem) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FlowBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        registerRideLocationReceiver();

        SharedPreferences phone = getSharedPreferences("auto", MODE_PRIVATE);
        number = phone.getString("sosphone", "");

        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        //id refernece for wizet
        spinner = (Spinner) findViewById(R.id.help);

        Intent intentt = new Intent(this.getIntent());
        intentt.getStringExtra("c_name");
        int check = intentt.getIntExtra("check", 0);
        if ( check == 1)
            comp_point = intentt.getStringExtra("comp_point");
        String comp_num = intentt.getStringExtra("comp_num");
        binding.couseInfo.setText(intentt.getStringExtra("comp_name"));
        Log.d("확인",intentt.getStringExtra("c_name") + ""+ intentt.getStringExtra("gpx"));

        //input array data
        list = new ArrayList<>();
        list.add("");
        list.add("119에 전화");
        list.add("119에 위치전송");
        list.add("지정번호에 전화" + "(" + number + ")");
        list.add("지정번호에 위치전송" + "(" + number + ")");


        //using ArrayAdapter
        ArrayAdapter spinnerAdapter;
        spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(spinnerAdapter);

        //event listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Toast.makeText(getApplicationContext(),"선택된 아이템 : "+position+spinner.getItemAtPosition(position),Toast.LENGTH_SHORT).show();
                switch (position) {
                    case 1:
                        SosHelper.confirmEmergencyServiceCall(FollowStart.this);
                        break;
                    case 2:
                        SosHelper.confirmEmergencyServiceMessage(FollowStart.this, emergencyMessage());
                        break;
                    case 3:
                        SosHelper.confirmContactCall(FollowStart.this);
                        break;
                    case 4:
                        SosHelper.confirmContactMessage(FollowStart.this, emergencyMessage());
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        soundPool = new SoundPool.Builder().build();
        soundManager = new SoundManager(this, soundPool);
        soundManager.addSound(0, R.raw.sos);

        //긴급알림
        Button sns_sound = (Button) findViewById(R.id.sos_sound);
        sns_sound.setOnClickListener(v -> {
            if (!play) {
                play = true;
            } else {
                soundManager.playSound(0);
                play = false;
            }
        });

        mMapView = new MapView(this);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mMapView);
        mMapView.setCurrentLocationEventListener(this);
        mMapView.setShowCurrentLocationMarker(true);
        mMapView.setMapViewEventListener(this);
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);

        // 구현한 CalloutBalloonAdapter 등록
        mMapView.setCalloutBalloonAdapter(new CustomCalloutBalloonAdapter());

        // Course detail and competition detail used different extra names in the legacy UI.
        // Normalize them before restoring a previously active follow session.
        c_name = firstNotBlank(intentt.getStringExtra("comp_name"), intentt.getStringExtra("c_name"));
        gpx_name = firstNotBlank(intentt.getStringExtra("gpx"), intentt.getStringExtra("comp_gpx"));
        boolean restoredSession = restoreRideSession();
        if (!restoredSession) {
            BaseTime = SystemClock.elapsedRealtime();
            cur_status = Run;
            persistRideSession();
        }
        if (cur_status == Run) {
            RidingTimer.sendEmptyMessage(0);
        }

        courseFollowStatus = binding.courseFollowStatus;
        courseFollowProgress = binding.courseFollowProgress;
        if(isBlank(gpx_name)){
            binding.flowInfo.setVisibility(View.GONE);
        }else{
            binding.flowInfo.setVisibility(View.VISIBLE);
            binding.couseInfo.setText(c_name);
            binding.courseFollowStatus.setText("코스를 준비하는 중입니다");
            loadCourseRoute();
        }
        startRideTracking();

        //send_sms = (Button) findViewById(R.id.help);
        binding.mapLayout.setVisibility(View.VISIBLE); //지도
        binding.speedPre.setVisibility(View.VISIBLE); //지도탭에서 보여지는거
        binding.speedTap.setVisibility(View.GONE); //속도계
        binding.statusLayout.setVisibility(View.GONE);
        binding.resumeBt.setVisibility(View.GONE);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // TODO : process tab selection event.
                int pos = tab.getPosition();
                changeView(pos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // do nothing
            }
        });


        binding.endriding.setOnClickListener(v -> {
            //형변환한거

                Intent intent = new Intent(getApplicationContext(), FollowEndActivity.class);
                intent.putExtra("ingtime", total_time); //경과시간
                intent.putExtra("distence", (int)hap); //이동거리
                intent.putExtra("maxspeed", (int)maX); //최대속도
                intent.putExtra("avgspeed", (int)avg); //평균속도
                intent.putExtra("getgodo", getgodo); //획득고도
                intent.putExtra("resttime", rest); //휴식시간
                intent.putExtra("addr", adress_value);
                intent.putExtra("address_dong",address_dong);
                intent.putExtra("comp_name", c_name);
                if ( check == 1 ) {
                    intent.putExtra("rr_comp", comp_num);
                    intent.putExtra("comp_point", comp_point);
                }
                else {
                    intent.putExtra("rr_comp", "null");
                    intent.putExtra("comp_point", "0");
                }
                intent.putExtra("check", check);
                intent.putExtra("course_name", c_name);

                try {
                    intent.putExtra(
                            RideRouteCache.EXTRA_CACHE_FILE,
                            RideRouteCache.save(this, witch_lat, witch_lon, ele, godoArray,
                                    maxLat, maxLon, minLat, minLon)
                    );
                } catch (IOException exception) {
                    Log.e("RideEnd", "Unable to prepare route data", exception);
                    Toast.makeText(this, "주행 데이터를 준비하지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    return;
                }

                stopRideTracking();
                persistRideSession();

                Log.d("로그랑",total_time+ " " + hap + " " + maX + " " + avg + " " + getgodo + " " +rest + " " + adress_value + " " );
                startActivity(intent);
                finish();
        });

        if (RideSettings.areDangerAlertsEnabled(this)) {
            DangerGet = serverApi.getDangerArea();
        DangerGet.enqueue(new Callback<List<DangerousArea>>() {
            @Override
            public void onResponse(Call<List<DangerousArea>> call, Response<List<DangerousArea>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    getDanger= response.body();

                    for (DangerousArea danger : getDanger) {
                        try {
                        getDalat = danger.getDa_latitude();
                        getDalon = danger.getDa_longitude();
                        getDacon = danger.getDa_content();


                        mCustomMarker = new MapPOIItem();
                        String name = "Custom Marker";
                        mCustomMarker.setItemName(getDacon);
                        mCustomMarker.setTag(1);
                        mCustomMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(getDalat), Double.parseDouble(getDalon)));

                        mCustomMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

                        mCustomMarker.setCustomImageResourceId(R.drawable.custom_marker_red);
                        mCustomMarker.setCustomImageAutoscale(false);
                        mCustomMarker.setCustomImageAnchor(0.5f, 1.0f);

                        mMapView.addPOIItem(mCustomMarker);
                        } catch (RuntimeException error) {
                            Log.w("RideDanger", "Skipping invalid danger marker", error);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<DangerousArea>> call, Throwable t) {
                Log.w("RideDanger", "Danger markers unavailable; ride tracking continues", t);
            }
        });
        }

    }

    private void loadCourseRoute() {
        final String requestedGpx = gpx_name;
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            ArrayList<CourseGuidanceEngine.Point> points = new ArrayList<>();
            boolean usedCache = false;
            String errorMessage = null;
            try {
                if (CourseRouteStore.exists(this, requestedGpx)) {
                    usedCache = true;
                    input = CourseRouteStore.open(this, requestedGpx);
                } else if (CourseRouteStore.isLocalReference(requestedGpx)) {
                    errorMessage = "불러온 GPX 파일을 찾을 수 없습니다.";
                } else {
                    connection = Server.openConnection("app/getGPX/gpx/" + requestedGpx);
                    input = connection.getInputStream();
                    CourseRouteStore.save(this, requestedGpx, input);
                    input.close();
                    input = CourseRouteStore.open(this, requestedGpx);
                }
                if (errorMessage == null) {
                    parsedGpx = mParser.parse(input);
                    CourseGpxAnalyzer.Summary summary = CourseGpxAnalyzer.analyze(parsedGpx, c_name);
                    points.addAll(summary.guidancePoints);
                }
                if (errorMessage == null && points.size() < 2) {
                    errorMessage = "코스 GPX에 유효한 경로가 없습니다.";
                }
            } catch (IOException | XmlPullParserException error) {
                Log.w("CourseFollow", "Unable to load course GPX", error);
                errorMessage = CourseRouteStore.isLocalReference(requestedGpx)
                        ? "불러온 GPX 파일을 읽지 못했습니다. 다른 파일을 선택해 주세요."
                        : "코스 GPX를 불러오지 못했습니다. 인터넷 연결 후 다시 시작해 주세요.";
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
                if (connection != null) connection.disconnect();
            }

            final ArrayList<CourseGuidanceEngine.Point> routePoints = points;
            final boolean fromCache = usedCache;
            final String failure = errorMessage;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (failure != null) {
                    courseFollowStatus.setText(failure);
                    courseFollowProgress.setText("주행 기록은 계속됩니다");
                    return;
                }
                courseGuidance.setRoute(routePoints);
                if (!courseGuidance.hasRoute()) {
                    courseFollowStatus.setText("코스 GPX에 유효한 경로가 없습니다.");
                    return;
                }
                MapPolyline routeLine = new MapPolyline();
                routeLine.setTag(1000);
                routeLine.setLineColor(Color.argb(255, 0, 74, 196));
                for (CourseGuidanceEngine.Point point : routePoints) {
                    routeLine.addPoint(MapPoint.mapPointWithGeoCoord(point.latitude, point.longitude));
                }
                if (mMapView != null) {
                    mMapView.addPolyline(routeLine);
                    mMapView.moveCamera(CameraUpdateFactory.newMapPointBounds(
                            new MapPointBounds(routeLine.getMapPoints()), 100));
                }
                courseFollowStatus.setText(fromCache ? "저장된 GPX 코스 준비 완료" : "코스 준비 완료");
                courseFollowProgress.setText("진행률 0% · 남은 거리 "
                        + formatCourseDistance(courseGuidance.update(routePoints.get(0).latitude,
                        routePoints.get(0).longitude, false, 0f).totalMeters));
            });
        }).start();
    }

    private void updateCourseGuidance(Location location) {
        if (!courseGuidance.hasRoute() || courseFollowStatus == null) return;
        CourseGuidanceEngine.Update update = courseGuidance.update(
                location.getLatitude(), location.getLongitude(), location.hasAccuracy(), location.getAccuracy());
        if (!update.usableLocation) {
            courseFollowStatus.setText("GPS 정확도 낮음 · 코스 이탈 판정 대기");
            return;
        }
        if (update.offRoute) {
            courseFollowStatus.setText("코스 이탈 · 경로까지 " + formatCourseDistance(update.distanceToRouteMeters));
        } else {
            courseFollowStatus.setText("코스 정상 · 경로까지 " + formatCourseDistance(update.distanceToRouteMeters));
        }
        courseFollowProgress.setText("진행률 " + update.progressPercent() + "% · 남은 거리 "
                + formatCourseDistance(update.remainingMeters()));
        // Settings > Notifications > course deviation. The status line above is shown either way.
        if (!RideSettings.areCourseAlertsEnabled(this)) return;
        if (update.event == CourseGuidanceEngine.Event.OFF_ROUTE) {
            RideTrackingService.announceCourseGuidance(this,
                    "코스에서 벗어났습니다. GPX 경로를 확인해 주세요.");
        } else if (update.event == CourseGuidanceEngine.Event.BACK_ON_ROUTE) {
            RideTrackingService.announceCourseGuidance(this, "코스 경로로 다시 진입했습니다.");
        }
    }

    private static String formatCourseDistance(double meters) {
        if (meters >= 1000d) return String.format(Locale.KOREA, "%.1f km", meters / 1000d);
        return String.format(Locale.KOREA, "%d m", Math.round(meters));
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void changeView(int index) {
        switch (index) {
            case 0:
                binding.mapLayout.setVisibility(View.VISIBLE);
                binding.speedPre.setVisibility(View.VISIBLE);
                binding.speedTap.setVisibility(View.GONE);
                binding.defaultLayout.setBackgroundColor(0xdddddd);
                break;
            case 1:
                binding.mapLayout.setVisibility(View.GONE);
                binding.speedPre.setVisibility(View.GONE);
                binding.speedTap.setVisibility(View.VISIBLE);
                binding.defaultLayout.setBackgroundColor(0xf0f0f0);
                break;
        }

        startRideTracking();
    }

    private void startRideTracking() {
        if (!RidePermissionFlow.hasPreciseLocation(this)) {
            RidePermissionFlow.requestRideLocation(this);
            return;
        }
        RideTrackingService.start(this);
        RidePermissionFlow.requestBackgroundLocationForLongRide(this);
        if (!voicePermissionOfferShown) {
            voicePermissionOfferShown = true;
            offerVoiceCommands();
        }
    }

    private void registerRideLocationReceiver() {
        if (rideLocationReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(RideTrackingService.ACTION_LOCATION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(rideLocationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(rideLocationReceiver, filter);
        }
        rideLocationReceiverRegistered = true;
    }

    private void stopRideTracking() {
        RideTrackingService.stop(this);
    }

    //재개, 정지 버튼

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RidePermissionFlow.REQUEST_RIDE_LOCATION) {
            if (RidePermissionFlow.hasPreciseLocation(this)) {
                startRideTracking();
            } else {
                RidePermissionFlow.showDenied(this, "정확한 위치");
            }
        } else if (requestCode == RidePermissionFlow.REQUEST_MICROPHONE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                RidePermissionFlow.showDenied(this, "음성 명령");
            }
        }
    }

    public void myOnClick(View v) {


        Log.d("현재상태", String.valueOf(cur_status));

        switch (v.getId()) {
            case R.id.pausebt: //정지버튼 활성


            case R.id.resume_bt:
                //재개 활성


                switch (cur_status) {
                    case Init:
                        BaseTime = SystemClock.elapsedRealtime();
                        cur_status = Run; //현재상태를 런상태로 변경
                        Log.d("cur_status", "1");

                        break;

                case Run:
                    PauseTime = SystemClock.elapsedRealtime();
                    RidingTimer.removeMessages(0); //라이딩시간

                    RideTrackingService.pauseAssistant(this);

                        cur_status = Pause;
                        persistRideSession();
                        binding.resumeBt.setVisibility(View.VISIBLE);
                        binding.pausebt.setVisibility(View.GONE);
                        Log.d("cur_status", "실행");
                        break;
                    case Pause:
                        long now2 = SystemClock.elapsedRealtime();
                        rest += (int) ((now2 - PauseTime) / 1000L);
                    binding.resttime.setText(formatDuration(rest));
                    RidingTimer.sendEmptyMessage(0);
                    BaseTime += (now2 - PauseTime);
                    RideTrackingService.resumeAssistant(this);
                    cur_status = Run;
                    persistRideSession();
                        binding.resumeBt.setVisibility(View.GONE);
                        binding.pausebt.setVisibility(View.VISIBLE);
                        Log.d("cur_status", "중지");
                        break;
                }
                break;
        }
    }

    Handler RidingTimer = new Handler() {
        public void handleMessage(Message msg) {
            binding.ingtime.setText(getTimeout());
            binding.mTime.setText(getTimeout());
            total_time = getTimeout2();

            //sendEmptyMessage는 비어있는 메세제를 핸들러에게 전송함
            RidingTimer.sendEmptyMessageDelayed(0, 1000L);
        }


        //현재시간을 계속구해서 출력하는 메소드
        int getTimeout2() {
            long now = SystemClock.elapsedRealtime(); //애플리케이션이 실행되고나서 실제로 경과된 시간(??)^^;
            long outTime = now - BaseTime;

            int easy_outTime2 = (int) outTime / 1000;
            // Log.d("로그", String.valueOf(easy_outTime2));
            return easy_outTime2;
        }

        //현재시간을 계속구해서 출력하는 메소드
        String getTimeout() {
            long now = SystemClock.elapsedRealtime(); //애플리케이션이 실행되고나서 실제로 경과된 시간(??)^^;
            long outTime = now - BaseTime;

            int sec = (int) ((outTime / 1000) % 60);
            int min = (int) ((outTime / 1000 / 60) % 60);
            int hour = (int) ((outTime / 1000) / 3600);


            String easy_outTime = String.format("%02d:%02d:%02d", hour, min, sec);
            //Log.d("로그", String.valueOf(outTime / 1000));
            return easy_outTime;
        }
    };


    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format(Locale.KOREA, "%02d:%02d:%02d", hours, minutes, secs);
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int mSec = msg.arg1 % 100;
            int sec = (msg.arg1 / 100) % 60;
            int min = (msg.arg1 / 100) / 60;
            int hour = (msg.arg1 / 100) / 360;
            //1000이 1초 1000*60 은 1분 1000*60*10은 10분 1000*60*60은 한시간

            @SuppressLint("DefaultLocale") String result = String.format("%02d:%02d:%02d", hour, min, sec);

            rest = msg.arg1 / 100;
            binding.resttime.setText(result);
        }
    };

    public class timeThread implements Runnable {
        @Override
        public void run() {
            int i = 0;

            while (true) {
                while (isRunning) { //일시정지를 누르면 멈춤
                    Message msg = new Message();
                    msg.arg1 = i++;
                    handler.sendMessage(msg);

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            binding.resttime.setText("");
                            binding.resttime.setText("00:00:00");
                        });
                        return; // 인터럽트 받을 경우 return
                    }
                }
            }
        }
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {

    }


    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
        Toast.makeText(this, "Clicked " + mapPOIItem.getItemName() + " Callout Balloon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {
    }


    @Override
    public void onLocationChanged(Location location) {
        if (cur_status != Run) {
            mLastlocation = null;
            return;
        }
        latitude = location.getLatitude();
        lonngitude = location.getLongitude();

        updateCourseGuidance(location);

        // 위도 경도 정보 배열 저장
        witch_lat.add(latitude);
        witch_lon.add(lonngitude);
        godoArray.add((float) location.getAltitude());

        // 최대 최소 위도 경도 계산
        if (maxLat < latitude) maxLat = latitude;
        if (minLat > latitude) minLat = latitude;
        if (maxLon < lonngitude) maxLon = lonngitude;
        if (minLon > lonngitude) minLon = lonngitude;

        // 고도 정보 저장
        ele.add(location.getAltitude());

        //현재속도
        getSpeed = Double.parseDouble(String.format("%.1f", location.getSpeed() * 3600 / 1000));
        binding.nowspeed.setText(String.format("%.1f", getSpeed));  //Get Speed
        binding.mSpeed.setText(String.format("%.1f", getSpeed) + "km/h");

        MapPolyline polyline = new MapPolyline();
        polyline.setLineColor(Color.argb(128, 255, 51, 0));

        polyline.addPoint(MapPoint.mapPointWithGeoCoord(latitude, lonngitude));
        if (mMapView != null) {
            mMapView.addPolyline(polyline);
            mMapView.isShowingCurrentLocationMarker();
        }

        // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
        MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
        int padding = 50; // px
        if (mMapView != null) {
            mMapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding, 2, 3));
        }

        gCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            addr = gCoder.getFromLocation(latitude, lonngitude, 1);
            List<Address> addr2 = gCoder.getFromLocation(latitude, lonngitude, 2);
            List<Address> addr3 = gCoder.getFromLocation(latitude, lonngitude, 3);

            Address a = addr.get(0);
            address_dong = a.getThoroughfare();
            adress_value = a.getAddressLine(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mLastlocation != null) {
            /*폴리라인 그리기 */
            MapPolyline polyline2 = new MapPolyline();
            polyline.setLineColor(Color.argb(128, 255, 51, 0));
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(mLastlocation.getLatitude(), mLastlocation.getLongitude()));
            if (mMapView != null) {
                mMapView.addPolyline(polyline);
            }

            // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
            MapPointBounds mapPointBounds2 = new MapPointBounds(polyline.getMapPoints());
            if (mMapView != null) {
                mMapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds2, padding, 2, 3));
            }

            if (getSpeed > maX) {
                maX = getSpeed;
                binding.maxspeed.setText(String.format("%.1f", maX));
                cha_max = String.format("%.0f", maX);
            }
            //이동거리
            hap = hap + mLastlocation.distanceTo(location);
            cha_dis = String.format("%.0f", hap);

            binding.dis.setText(String.format("%.1f", hap));
            binding.mDistance.setText(String.format("%.1f", hap) + "m");

            double killlo;
            if (hap >= 1000) {
                killlo = hap / 1000.0;
                binding.dis.setText(String.format("%.1f", killlo));
                binding.mDistance.setText(String.format("%.1f", killlo) + "km");
            }

            //평균속도
            avg = total_time > 0 ? (hap / total_time) * 3.6 : 0;
            binding.avgspeed.setText(String.format("%.1f", Double.parseDouble(String.valueOf(avg))));
            cha_avg = String.format("%.0f", (avg));

            //획득고도
            //획득고도
            getgodoval = (int) (location.getAltitude() - mLastlocation.getAltitude());
            if (getgodoval > 0) {
                getgodo += getgodoval;
                binding.getgodo.setText(String.valueOf(getgodo));
            }

            //고도
            binding.Nowgodo.setText(String.format("%.0f", location.getAltitude()) + "m");
        }
        // 현재위치를 지난 위치로 변경
        mLastlocation = location;
        checkpointRideSessionIfDue();
    }

    private void checkpointRideSessionIfDue() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastSessionCheckpointMillis >= SESSION_CHECKPOINT_INTERVAL_MS) {
            persistRideSession();
        }
    }

    private void persistRideSession() {
        RideSessionStore.save(this, new RideSessionStore.Snapshot(
                true, cur_status, BaseTime, PauseTime, rest, total_time,
                hap, maX, avg, getgodo, 0,
                maxLat, maxLon, minLat, minLon,
                latitude, lonngitude,
                mLastlocation != null && mLastlocation.hasAltitude() ? mLastlocation.getAltitude() : 0d,
                adress_value, address_dong,
                witch_lat, witch_lon, ele, godoArray, gpx_name, c_name
        ));
        lastSessionCheckpointMillis = SystemClock.elapsedRealtime();
    }

    private boolean restoreRideSession() {
        RideSessionStore.Snapshot snapshot = RideSessionStore.load(this);
        if (snapshot == null || !snapshot.followMode) {
            return false;
        }

        cur_status = snapshot.status;
        BaseTime = snapshot.baseTime;
        PauseTime = snapshot.pauseTime;
        rest = snapshot.restSeconds;
        total_time = snapshot.activeSeconds;
        hap = snapshot.distanceMeters;
        maX = snapshot.maxSpeedKmh;
        avg = snapshot.averageSpeedKmh;
        getgodo = snapshot.elevationGainMeters;
        maxLat = snapshot.maxLat;
        maxLon = snapshot.maxLon;
        minLat = snapshot.minLat;
        minLon = snapshot.minLon;
        latitude = snapshot.lastLatitude;
        lonngitude = snapshot.lastLongitude;
        adress_value = snapshot.address == null ? "" : snapshot.address;
        address_dong = snapshot.addressDong;
        if (isBlank(gpx_name)) gpx_name = snapshot.courseGpxName;
        if (isBlank(c_name)) c_name = snapshot.courseName;
        witch_lat = new ArrayList<>(snapshot.latitudes);
        witch_lon = new ArrayList<>(snapshot.longitudes);
        ele = new ArrayList<>(snapshot.elevations);
        godoArray = new ArrayList<>(snapshot.elevationSamples);
        if (!witch_lat.isEmpty()) {
            Location last = new Location(LocationManager.GPS_PROVIDER);
            last.setLatitude(latitude);
            last.setLongitude(lonngitude);
            last.setAltitude(snapshot.lastAltitude);
            mLastlocation = last;
        }
        binding.resttime.setText(formatDuration(rest));
        binding.getgodo.setText(String.valueOf(getgodo));
        binding.maxspeed.setText(String.format(Locale.KOREA, "%.1f", maX));
        binding.avgspeed.setText(String.format(Locale.KOREA, "%.1f", avg));
        if (cur_status == Pause) {
            binding.resumeBt.setVisibility(View.VISIBLE);
            binding.pausebt.setVisibility(View.GONE);
        }
        return true;
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        // 위치정보 업데이트
        if (!RidePermissionFlow.hasPreciseLocation(this)) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        RideTrackingService.start(this);
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (courseFollowStatus != null) {
            courseFollowStatus.setText("GPS 신호 없음 · 코스 이탈 판정 대기");
        }
    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {

    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {

    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {

    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
        if (courseFollowStatus != null) {
            courseFollowStatus.setText("현재 위치를 확인할 수 없습니다");
        }
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("주행을 종료할까요?")
                .setMessage("저장하지 않은 주행 기록은 사라집니다.")
                .setNegativeButton("계속 주행", null)
                .setNeutralButton("기록 없이 종료", (dialog, which) -> {
                    stopRideTracking();
                    RideSessionStore.clear(this);
                    finish();
                })
                .setPositiveButton("종료 후 저장", (dialog, which) -> binding.endriding.performClick())
                .show();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            stopRideTracking();
        }
        if (rideLocationReceiverRegistered) {
            unregisterReceiver(rideLocationReceiver);
            rideLocationReceiverRegistered = false;
        }
        RidingTimer.removeCallbacksAndMessages(null);
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }
        super.onDestroy();
        Log.d("onDestroy", "onDestroy");

    }

    @Override
    protected void onPause() {
        persistRideSession();
        super.onPause();
        Log.d("onPause", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        RideSettings.applyRidingDisplay(this);
        Log.d("onResume", "OnResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("다른화면임", "다른화면임");
    }

    private void offerVoiceCommands() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition();
        } else {
            RidePermissionFlow.requestMicrophone(this);
        }
    }

    private void startVoiceRecognition() {
        if (mRecognizer != null || !SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }
        try {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mRecognizer.setRecognitionListener(listener);
            mRecognizer.startListening(intent);
        } catch (SecurityException exception) {
            RidePermissionFlow.showDenied(this, "음성 명령");
        }
    }

    private String emergencyMessage() {
        return SosHelper.buildEmergencyMessage(address_dong, latitude, lonngitude);
    }

    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
           // Toast.makeText(getApplicationContext(), "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

           // Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            if (matches == null || matches.isEmpty()) {
                return;
            }

            for (int i = 0; i < matches.size(); i++) {
                a = matches.get(i);
                Log.d("a의값은", a);
            }
            switch (a) {
                case "위치전송":
                    SosHelper.confirmContactMessage(FollowStart.this, emergencyMessage());
                    break;
                case "119에 전화":
                    SosHelper.confirmEmergencyServiceCall(FollowStart.this);
                    break;
                case "지인에게 문자":
                    SosHelper.confirmContactMessage(FollowStart.this, emergencyMessage());
                    break;
                case "지인에게 통화":
                    SosHelper.confirmContactCall(FollowStart.this);
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };
}
