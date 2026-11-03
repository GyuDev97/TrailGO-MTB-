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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import com.bike.trailgo.R;
import com.bike.trailgo.SoundManager;
import com.bike.trailgo.databinding.ActivityRidingStartBinding;
import com.bike.trailgo.dto.DangerousArea;
import com.bike.trailgo.gps.ElevationGainTracker;
import com.bike.trailgo.gps.GpsFix;
import com.bike.trailgo.gps.GpsFixFilter;
import com.bike.trailgo.hazard.HazardDetailDialog;
import com.bike.trailgo.hazard.HazardMapLayer;
import com.bike.trailgo.hazard.HazardRepository;
import com.bike.trailgo.gps.GpsQualityConfig;
import com.bike.trailgo.gps.GpsStatusPolicy;
import com.bike.trailgo.gpx.GPXParser;
import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.gpx.Track;
import com.bike.trailgo.gpx.TrackPoint;
import com.bike.trailgo.gpx.TrackSegment;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.net.ServerApi;
import com.bike.trailgo.setting.RideSettings;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.GestureType;
import com.kakao.vectormap.route.RouteLine;
import com.kakao.vectormap.route.RouteLineLayer;
import com.kakao.vectormap.route.RouteLineOptions;
import com.kakao.vectormap.route.RouteLineSegment;
import com.kakao.vectormap.route.RouteLineStyle;
import com.kakao.vectormap.route.RouteLineStyles;
import com.kakao.vectormap.route.RouteLineStylesSet;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
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
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("HandlerLeak")
public class StartActivity extends AppCompatActivity
        implements LocationListener, MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    private static final int LOCATION_PERMISSION_REQUEST = 3101;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 3102;
    private SoundPool soundPool;
    private SoundManager soundManager;
    private boolean play;
    private ActivityRidingStartBinding binding;
    private MapView mMapView;
    private com.kakao.vectormap.MapView kakaoV2MapView;
    private KakaoMap kakaoMap;
    private RouteLineLayer ridingRouteLayer;
    private RouteLine ridingRouteLine;
    private RouteLineStyles ridingRouteStyles;
    private final List<LatLng> ridingRoutePoints = new ArrayList<>();
    private LabelLayer currentLocationLayer;
    private Label currentLocationLabel;
    private LabelStyles currentLocationStyles;
    private boolean locationUpdatesStarted;
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
    private MapPOIItem mCustomMarker;
    private LocationManager locationManager;
    private Location mLastlocation = null;
    private int cnt=0;

    private String getDalat, getDalon, getDacon, address_dong;
    //??醫딆┣??????⑤슢堉???
    private double latitude, lonngitude, getSpeed = 0, hap = 0, avg = 0, maX = 0, maxLat = 0, maxLon = 0, minLat = 1000, minLon = 1000;
    // GPS quality pipeline: every threshold lives in GpsQualityConfig (see gps package).
    private final GpsQualityConfig gpsConfig = GpsQualityConfig.DEFAULT;
    private final GpsFixFilter gpsFilter = new GpsFixFilter(gpsConfig);
    private final ElevationGainTracker gainTracker = new ElevationGainTracker(gpsConfig);
    // Totals restored from a checkpoint; the tracker only knows what was gained since the restore.
    private int gainBaseMeters = 0;
    private int descentBaseMeters = 0;
    private double lastGoodLatitude = 0.0;
    private double lastGoodLongitude = 0.0;
    // GPS chip: when a non-stale position last arrived and how accurate it was
    private long lastFreshFixElapsedMs = -1L;
    private double lastFixAccuracy = Double.NaN;
    private final Handler gpsWatchdog = new Handler(android.os.Looper.getMainLooper());
    private final Runnable gpsWatchdogTask = new Runnable() {
        @Override
        public void run() {
            refreshGpsStatus();
            gpsWatchdog.postDelayed(this, 2000L);
        }
    };
    // Guards against double taps on the pause/resume and finish buttons
    private long lastControlClickAt = 0L;
    private boolean endingRide = false;
    // Weather at the start of the ride (looked up by the TrailGo server, kept with the session)
    private static final long WEATHER_RETRY_MS = 90_000L;
    private static final int WEATHER_MAX_ATTEMPTS = 3;
    private boolean weatherStored;
    private boolean weatherInFlight;
    private int weatherAttempts;
    private long lastWeatherAttemptAt;
    private long rideStartedAtMillis;
    private boolean elevationTotalsShown = false;
    private static final double MAX_REASONABLE_RIDE_SPEED_KMH = 100.0;
    private static final long SESSION_CHECKPOINT_INTERVAL_MS = 10_000L;
    private int getgodo = 0;
    private int totalDescentMeters = 0;
    private Location gradeBaselineLocation = null;
    private long lastSpeedLocationReceivedAtMillis = 0L;
    private long lastSessionCheckpointMillis = 0L;
    private int  total_time=0, rest=0;
    private ArrayList<Double> witch_lat = new ArrayList<>();
    private ArrayList<Double> witch_lon = new ArrayList<>();
    private ArrayList<Double> ele = new ArrayList<>();
    private ArrayList<Float> godoArray = new ArrayList<>();
    private String a;
    //?癲?????볥궙獒??⑤슢堉???
    private String cha_dis = "0", cha_max = "0", cha_avg = "0", adress_value = "";

    private List<Address> addr = null;
    private Geocoder gCoder;

    //???????????癲ル슢??蹂좊쨨??
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
    private String c_name;

    //gpx???援온??
    Gpx parsedGpx = null;
    GPXParser mParser = new GPXParser();
    //?????놁뎽?汝??吏????
    private ServerApi serverApi = Server.getInstance().getApi();
    private Call<List<DangerousArea>> DangerGet;
    private List<DangerousArea> getDanger;

    public StartActivity() {
    }

    // CalloutBalloonAdapter ?癲ル슢?뤸뤃?????볥궙????????닻뇦?
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
        binding = ActivityRidingStartBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        registerRideLocationReceiver();
        binding.ridingSettingsButton.setOnClickListener(v ->
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        );

        SharedPreferences phone = getSharedPreferences("auto", MODE_PRIVATE);
        number = phone.getString("sosphone", "");

        //?????癲ル슢???彛?
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        //id refernece for wizet
        spinner = (Spinner) findViewById(R.id.help);

        //input array data
        list = new ArrayList<>();
        list.add("");
        list.add("119\uC5D0 \uC804\uD654");
        list.add("119\uC5D0 \uC704\uCE58\uC804\uC1A1");
        list.add("\uC9C0\uC815\uBC88\uD638\uC5D0 \uC804\uD654" + "(" + number + ")");
        list.add("\uC9C0\uC815\uBC88\uD638\uC5D0 \uC704\uCE58\uC804\uC1A1" + "(" + number + ")");


        //using ArrayAdapter
        ArrayAdapter spinnerAdapter;
        spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(spinnerAdapter);

        //event listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Toast.makeText(getApplicationContext(),"????ｋ???????썹땟?㈑??: "+position+spinner.getItemAtPosition(position),Toast.LENGTH_SHORT).show();
                switch (position) {
                    case 1:
                        SosHelper.confirmEmergencyServiceCall(StartActivity.this);
                        break;
                    case 2:
                        SosHelper.confirmEmergencyServiceMessage(StartActivity.this, emergencyMessage());
                        break;
                    case 3:
                        SosHelper.confirmContactCall(StartActivity.this);
                        break;
                    case 4:
                        SosHelper.confirmContactMessage(StartActivity.this, emergencyMessage());
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        startKakaoV2Map();

        //??ш끽維驪??????산뭐??쀪쑬夷???쎛??????醫딆쓧??癲ル슢???몄쒜嚥▲룗??
        Intent course_get = new Intent(this.getIntent());
        c_name = course_get.getStringExtra("c_name");
        gpx_name = course_get.getStringExtra("gpx");

        if(gpx_name == null && c_name == null){
            binding.flowInfo.setVisibility(View.GONE);
        }else{
            binding.flowInfo.setVisibility(View.VISIBLE);
            binding.couseInfo.setText(c_name);
            Thread uThread = new Thread() {
                @Override
                public void run() {

                    try {
                        MapPolyline polyline = new MapPolyline();
                        polyline.setTag(1000);
                        polyline.setLineColor(Color.argb(255, 0, 74, 196)); // Polyline ??嚥▲꺂????꿔꺂?????
                        //??嶺뚮Ĳ?됭짆????????????꿔꺂??? URL
                        //Web??????????꿔꺂??? ??醫딆쓧??癲ル슢???몄쒜???ImageView???꿔꺂????癲ル슢怡??뱀떫 Bitmap ?꿔꺂??????筌뚯솃?
                    // Open the GPX file from the server.

                        HttpURLConnection conn = Server.openConnection("app/getGPX/gpx/" + gpx_name);
                    // Configure and open the HTTP connection.


                        conn.setDoInput(true); //Server ???嶺?????????怨몄７ ??醫딆쓧??嚥싳쇎紐??????븐뻤??쒖몱???꿔꺂??????
                        conn.connect(); //????쇰뮚???????곗숯???????????(connect() ?癲ル슢???????ㅿ폑?????繹먮냱議????嶺???醫딆쓧??嚥싳쇎紐??


                        InputStream is = conn.getInputStream(); //inputStream ????醫딆쓧??癲ル슢???몄쒜嚥▲룗??
                        // InputStream in = getAssets().open("and.gpx");
                        parsedGpx = mParser.parse(is);

                        if (parsedGpx != null) {
                            // log stuff
                            List<Track> tracks = parsedGpx.getTracks();
                            for (int i = 0; i < tracks.size(); i++) {
                                Track track = tracks.get(i);
                                Log.d("track ", i + ":");
                                List<TrackSegment> segments = track.getTrackSegments();
                                for (int j = 0; j < segments.size(); j++) {
                                    TrackSegment segment = segments.get(j);

                                    for (TrackPoint trackPoint : segment.getTrackPoints()) {

                                        polyline.addPoint(MapPoint.mapPointWithGeoCoord(trackPoint.getLatitude(), trackPoint.getLongitude()));
                                        Log.d("point: lat ", + trackPoint.getLatitude() + ", lon " + trackPoint.getLongitude());
                                    }
                                }
                            }
                            // Polyline ?꿔꺂???????썹땟????????틓楹?
                            if (mMapView != null) {
                                mMapView.addPolyline(polyline);
                            }

                            // ?꿔꺂???????썹땟????嚥싳쉶瑗??꾧틡?嶺?????ㅽ떝椰?? 嚥싳쉶瑗??裕???⑤즾逾???묆궘?Polyline???꿔꺂??袁ㅻ븶?癲?????볥궖?????썹땟怨⒲뀋???됰슦????
                            MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
                            int padding = 100; // px
                            if (mMapView != null) {
                                mMapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));
                            }
                        } else {
                            Log.e("error","Error parsing gpx track!");
                        }


                    } catch (IOException | XmlPullParserException e) {
                        e.printStackTrace();
                    }
                }
            };
            uThread.start();
        }


        //???????????癲ル슢??蹂좊쨨??
        boolean restoredSession = restoreRideSession();
        if (!restoredSession) {
            BaseTime = SystemClock.elapsedRealtime();
            cur_status = Run;
            // a new ride never inherits the weather of an abandoned earlier one
            RideWeatherFile.clear(getFilesDir());
            persistRideSession();
        }
        weatherStored = RideWeatherFile.load(getFilesDir()) != null;
        if (cur_status == Run) {
            RidingTimer.sendEmptyMessage(0);
        }

        //send_sms = (Button) findViewById(R.id.help);
        binding.mapLayout.setVisibility(View.VISIBLE); //?꿔꺂?????
        binding.speedPre.setVisibility(View.VISIBLE); //?꿔꺂???????꾣뤃????????⑤슢??????鶯????됰Ŧ???
        binding.speedTap.setVisibility(View.GONE); //????戮?덫??
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
        startLocationTracking();
        gpsWatchdog.post(gpsWatchdogTask);


        binding.endriding.setOnClickListener(v -> {
                if (endingRide) {
                    return; // already finishing: ignore repeated taps
                }
                endingRide = true;
                Intent intent = new Intent(StartActivity.this, EndActivity.class);
                intent.putExtra("ingtime", total_time); //?嚥▲굧?????????
                intent.putExtra("distence", (int) hap); //?????룸????좎돦???쎈뼔??
                intent.putExtra("maxspeed", (int) maX); //?꿔꺂????쭍?????戮?덫
                intent.putExtra("avgspeed", (int) avg); //????????戮?덫
                intent.putExtra("getgodo", getgodo); //????蹂λ쑋???쒙쭫??
                intent.putExtra("resttime", rest); //????ㅿ폍筌??????
                intent.putExtra("addr", adress_value);
                intent.putExtra("address_dong", address_dong);
                intent.putExtra("rr_comp", "null");

                try {
                    intent.putExtra(
                            RideRouteCache.EXTRA_CACHE_FILE,
                            RideRouteCache.save(this, witch_lat, witch_lon, ele, godoArray,
                                    maxLat, maxLon, minLat, minLon)
                    );
                } catch (IOException exception) {
                    Log.e("RideEnd", "Unable to prepare route data", exception);
                    Toast.makeText(this, "주행 데이터를 준비하지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    endingRide = false;
                    return;
                }

                Log.d("RideEnd", "time=" + total_time + ", distance=" + hap + ", max=" + maX + ", avg=" + avg + ", high=" + getgodo + ", rest=" + rest + ", addr=" + adress_value);
                Log.d("RideEnd", "gps filter: " + gpsFilter.summary() + ", points=" + witch_lat.size());
                persistRideSession();
                stopRideTracking();
                startActivity(intent);
                finish();
        });

        // Hazard zones: the foreground service keeps the shared cache fresh; this screen only draws it.
        hazardRepository = HazardRepository.get(this);
        registerHazardReceiver();
    }

    // ---- hazard zones: map overlay and warning banner ---------------------------------------------------

    private HazardRepository hazardRepository;
    private HazardMapLayer hazardMapLayer;
    private boolean hazardReceiverRegistered;
    private final android.os.Handler hazardBannerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    /** The service repeats a visible banner every few seconds; silence for longer means the signal is gone. */
    private static final long HAZARD_BANNER_EXPIRY_MS = 12_000L;
    private final Runnable hazardBannerExpiry = () -> showHazardBanner(false, null, null, false);

    private final HazardRepository.Listener hazardListener = hazards -> renderHazards();

    private final android.content.BroadcastReceiver hazardReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showHazardBanner(
                    intent.getBooleanExtra(RideTrackingService.EXTRA_HAZARD_VISIBLE, false),
                    intent.getStringExtra(RideTrackingService.EXTRA_HAZARD_TITLE),
                    intent.getStringExtra(RideTrackingService.EXTRA_HAZARD_DETAIL),
                    intent.getBooleanExtra(RideTrackingService.EXTRA_HAZARD_INSIDE, false));
        }
    };

    private void registerHazardReceiver() {
        if (hazardReceiverRegistered) return;
        IntentFilter filter = new IntentFilter(RideTrackingService.ACTION_HAZARD_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hazardReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(hazardReceiver, filter);
        }
        hazardReceiverRegistered = true;
    }

    private void showHazardBanner(boolean visible, String title, String detail, boolean inside) {
        hazardBannerHandler.removeCallbacks(hazardBannerExpiry);
        if (binding == null) return;
        if (!visible || title == null || !RideSettings.areDangerAlertsEnabled(this)) {
            binding.hazardBanner.setVisibility(View.GONE);
            return;
        }
        binding.hazardBannerTitle.setText(title);
        binding.hazardBannerDetail.setText(detail == null ? "" : detail);
        binding.hazardBanner.setBackgroundResource(inside ? R.drawable.hazard_banner_inside_bg : R.drawable.hazard_banner_bg);
        binding.hazardBanner.setVisibility(View.VISIBLE);
        hazardBannerHandler.postDelayed(hazardBannerExpiry, HAZARD_BANNER_EXPIRY_MS);
    }

    /** Draws the cached zones around the map centre; clears them when the rider switched the display off. */
    private void renderHazards() {
        if (hazardMapLayer == null || kakaoMap == null) return;
        if (!RideSettings.areHazardsShownOnMap(this)) {
            hazardMapLayer.clear();
            return;
        }
        com.kakao.vectormap.camera.CameraPosition camera = kakaoMap.getCameraPosition();
        if (camera == null) return;
        hazardMapLayer.render(hazardRepository.cached(), camera.getPosition(), camera.getZoomLevel());
    }

    private void startHazardOverlay() {
        try {
            hazardMapLayer = new HazardMapLayer(this, kakaoMap);
            hazardMapLayer.setOnHazardClickListener(hazard -> HazardDetailDialog.show(this, hazard, this::renderHazards));
            kakaoMap.setOnCameraMoveEndListener((map, position, gestureType) -> renderHazards());
            hazardRepository.addListener(hazardListener);
            renderHazards();
        } catch (RuntimeException error) {
            // the riding map works without the hazard overlay
            Log.w("RideHazard", "Hazard overlay unavailable", error);
            hazardMapLayer = null;
        }
    }

    private void startKakaoV2Map() {
        kakaoV2MapView = binding.mapView;
        binding.mapStatus.setVisibility(View.VISIBLE);
        binding.mapStatus.setText("\uCE74\uCE74\uC624\uB9F5\uC744 \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4");
        kakaoV2MapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d("KakaoMapV2", "Map destroyed");
            }

            @Override
            public void onMapError(Exception error) {
                Log.e("KakaoMapV2", "Map error", error);
                runOnUiThread(() -> {
                    binding.mapStatus.setVisibility(View.VISIBLE);
                    binding.mapStatus.setText(
                            "\uC9C0\uB3C4\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4\n"
                                    + "\uCE74\uCE74\uC624 \uC571 \uD0A4\uC640 \uD0A4 \uD574\uC2DC\uB97C \uD655\uC778\uD574\uC8FC\uC138\uC694"
                    );
                });
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap readyMap) {
                kakaoMap = readyMap;
                kakaoMap.setGestureEnable(GestureType.Pan, true);
                kakaoMap.setGestureEnable(GestureType.Zoom, true);
                kakaoMap.setGestureEnable(GestureType.RotateZoom, true);
                kakaoMap.setGestureEnable(GestureType.OneFingerZoom, true);
                ridingRouteLayer = kakaoMap.getRouteLineManager().getLayer();
                ridingRouteStyles = RouteLineStyles.from(
                        RouteLineStyle.from(12, Color.rgb(30, 116, 52), 3, Color.WHITE)
                );
                currentLocationLayer = kakaoMap.getLabelManager().getLayer();
                currentLocationStyles = kakaoMap.getLabelManager().addLabelStyles(
                        LabelStyles.from(
                                LabelStyle.from(drawableToBitmap(R.drawable.riding_current_marker, 34))
                                        .setAnchorPoint(0.5f, 0.5f)
                        )
                );
                binding.mapStatus.setVisibility(View.GONE);
                startHazardOverlay();
                drawRestoredRoute();
                if (!ridingRoutePoints.isEmpty()) {
                    LatLng lastPoint = ridingRoutePoints.get(ridingRoutePoints.size() - 1);
                    updateCurrentPosition(lastPoint.getLatitude(), lastPoint.getLongitude(), false);
                }
                Log.d("KakaoMapV2", "Map ready");
            }

            @Override
            public LatLng getPosition() {
                return LatLng.from(35.89617906303501, 128.62171907592318);
            }

            @Override
            public int getZoomLevel() {
                return 15;
            }
        });
    }

    private void updateCurrentPosition(double latitude, double longitude, boolean recordRoute) {
        if (kakaoMap == null) {
            return;
        }

        LatLng currentPosition = LatLng.from(latitude, longitude);
        if (currentLocationLabel == null && currentLocationLayer != null && currentLocationStyles != null) {
            currentLocationLabel = currentLocationLayer.addLabel(
                    LabelOptions.from("current-rider", currentPosition)
                            .setStyles(currentLocationStyles)
                            .setRank(10000)
            );
        } else if (currentLocationLabel != null) {
            currentLocationLabel.moveTo(currentPosition, 350);
        }
        kakaoMap.moveCamera(
                com.kakao.vectormap.camera.CameraUpdateFactory
                        .newCenterPosition(currentPosition, 17)
        );

        if (!recordRoute || ridingRouteLayer == null || ridingRouteStyles == null) {
            Log.d("RideTracking", "current position updated; route recording=" + recordRoute);
            return;
        }

        ridingRoutePoints.add(currentPosition);
        Log.d("RideTracking", "route point recorded; count=" + ridingRoutePoints.size()
                + ", lat=" + latitude + ", lon=" + longitude);
        if (ridingRoutePoints.size() < 2) {
            return;
        }

        RouteLineSegment segment = RouteLineSegment.from(
                new ArrayList<>(ridingRoutePoints),
                ridingRouteStyles
        );
        if (ridingRouteLine == null) {
            RouteLineStylesSet stylesSet = RouteLineStylesSet.from(ridingRouteStyles);
            RouteLineOptions options = RouteLineOptions.from(segment).setStylesSet(stylesSet);
            ridingRouteLine = ridingRouteLayer.addRouteLine(options);
        } else {
            ridingRouteLine.changeSegments(segment);
        }
    }

    private void drawRestoredRoute() {
        if (ridingRouteLayer == null || ridingRouteStyles == null || ridingRoutePoints.size() < 2) {
            return;
        }
        RouteLineSegment segment = RouteLineSegment.from(
                new ArrayList<>(ridingRoutePoints),
                ridingRouteStyles
        );
        RouteLineStylesSet stylesSet = RouteLineStylesSet.from(ridingRouteStyles);
        ridingRouteLine = ridingRouteLayer.addRouteLine(
                RouteLineOptions.from(segment).setStylesSet(stylesSet)
        );
    }

    private Bitmap drawableToBitmap(int drawableId, int sizeDp) {
        int sizePx = Math.round(sizeDp * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable drawable = AppCompatResources.getDrawable(this, drawableId);
        if (drawable != null) {
            drawable.setBounds(0, 0, sizePx, sizePx);
            drawable.draw(canvas);
        }
        return bitmap;
    }
                private void changeView(int index) {
                    switch (index) {
                        case 0:
                            binding.mapLayout.setVisibility(View.VISIBLE);
                            binding.speedPre.setVisibility(View.VISIBLE);
                            binding.speedTap.setVisibility(View.GONE);
                            binding.defaultLayout.setBackgroundColor(0xFFF7F8F7);
                            break;
                        case 1:
                            binding.mapLayout.setVisibility(View.GONE);
                            binding.speedPre.setVisibility(View.GONE);
                            binding.speedTap.setVisibility(View.VISIBLE);
                            binding.defaultLayout.setBackgroundColor(0xFFF7F8F7);
                            break;
                    }
                }

                private void startLocationTracking() {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (!RidePermissionFlow.hasPreciseLocation(this)) {
                        refreshGpsStatus();
                        RidePermissionFlow.requestRideLocation(this);
                        return;
                    }

                    RideTrackingService.start(this);
                    locationUpdatesStarted = true;
                    RidePermissionFlow.requestBackgroundLocationForLongRide(this);
                    requestNotificationPermissionIfNeeded();
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

                private void requestNotificationPermissionIfNeeded() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_REQUEST
                        );
                    }
                }

                private void stopRideTracking() {
                    RideTrackingService.stop(this);
                    locationUpdatesStarted = false;
                }

                @Override
                public void onRequestPermissionsResult(
                        int requestCode,
                        @NonNull String[] permissions,
                        @NonNull int[] grantResults
                ) {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    if (requestCode == LOCATION_PERMISSION_REQUEST) {
                        if (RidePermissionFlow.hasPreciseLocation(this)) {
                            startLocationTracking();
                        } else {
                            refreshGpsStatus();
                            binding.mapStatus.setVisibility(View.VISIBLE);
                            binding.mapStatus.setText("주행 거리와 경로 기록에는 정확한 위치 권한이 필요합니다");
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

                //???? ?癲? ?筌?????

                public void myOnClick(View v) {
                    Log.d("RideStatus", String.valueOf(cur_status));
                    long clickedAt = SystemClock.elapsedRealtime();
                    if (clickedAt - lastControlClickAt < 600L) {
                        return; // a double tap must not pause and immediately resume
                    }
                    lastControlClickAt = clickedAt;

                    switch (v.getId()) {
                        case R.id.pausebt: //?癲??筌???????嶺???


                        case R.id.resume_bt:
                            //??????嶺???


                            switch (cur_status) {
                                case Init:
                                    BaseTime = SystemClock.elapsedRealtime();
                                    cur_status = Run; //????썹땟?????븐뻤??????????????嶺뚮Ĳ?됲걫 ??⑤슢堉???
                                    Log.d("cur_status", "1");

                                    break;

                                case Run:
                                    PauseTime = SystemClock.elapsedRealtime();
                                    RidingTimer.removeMessages(0); //??濚밸Ŧ?????꾩룆梨띰쭛??

                                    RideTrackingService.pauseAssistant(this);

                                    cur_status = Pause;
                                    persistRideSession();
                                    binding.resumeBt.setVisibility(View.VISIBLE);
                                    binding.pausebt.setVisibility(View.GONE);
                                    Log.d("cur_status", "?????덊떀");
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
                                    Log.d("cur_status", "嚥싳쉶瑗??꾧틡?");
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

                        //sendEmptyMessage??????猷뱀쟼?????됲닓 ?꿔꺂???熬곊삳튉??? ?癲ル슢??蹂좊쨨????怨뺤췅??????썹땟戮щ빝??
                        RidingTimer.sendEmptyMessageDelayed(0, 1000L);
                    }


                    //????썹땟??????????影??낟??筌뤾퍔커???????Β?ы닍????β뼯爰귨㎘??꿔꺂??????
                    int getTimeout2() {
                        long now = SystemClock.elapsedRealtime(); //????ｋ????????ㅼ젾????????덊떀???野껊뿈???嶺뚮슣堉?쭕????繹먮냱議???嚥▲굧?????????????)^^;
                        long outTime = now - BaseTime;

                        int easy_outTime2 = (int) outTime / 1000;
                        // Log.d("?汝??吏??, String.valueOf(easy_outTime2));
                        return easy_outTime2;
                    }

                    //????썹땟??????????影??낟??筌뤾퍔커???????Β?ы닍????β뼯爰귨㎘??꿔꺂??????
                    String getTimeout() {
                        long now = SystemClock.elapsedRealtime(); //????ｋ????????ㅼ젾????????덊떀???野껊뿈???嶺뚮슣堉?쭕????繹먮냱議???嚥▲굧?????????????)^^;
                        long outTime = now - BaseTime;

                        int sec = (int) ((outTime / 1000) % 60);
                        int min = (int) ((outTime / 1000 / 60) % 60);
                        int hour = (int) ((outTime / 1000) / 3600);


                        String easy_outTime = String.format("%02d:%02d:%02d", hour, min, sec);
                        //Log.d("?汝??吏??, String.valueOf(outTime / 1000));
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
                        //1000??1??1000*60 ?? 1??1000*60*10?? 10??1000*60*60?? ??嶺?筌??

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
                            while (isRunning) { //??濚밸Ŧ援앾쭛?癲???????썼キ?????꿔꺂?????
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
                                    return; // ?癲ル슢?뤸뤃???????熬곣뫖利????嚥▲굧????return
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
                    latitude = location.getLatitude();
                    lonngitude = location.getLongitude();
                    noteFixForGpsStatus(location);
                    refreshGpsStatus();
                    boolean shouldRecord = cur_status == Run;

                    if (!shouldRecord) {
                        updateCurrentPosition(latitude, lonngitude, false);
                        mLastlocation = null;
                        gainTracker.resetBaseline();
                        gpsFilter.reset();
                        gradeBaselineLocation = null;
                        lastSpeedLocationReceivedAtMillis = 0L;
                        return;
                    }

                    // Quality gate: stale/cached positions, impossible jumps and stationary jitter
                    // are not recorded; nothing is ever rewritten (see GpsFixFilter).
                    GpsFix fix = LocationFixes.from(location);
                    GpsFixFilter.Result quality = gpsFilter.evaluate(fix, SystemClock.elapsedRealtimeNanos());
                    Log.d("GpsFilter", quality.toString()
                            + " | acc=" + fix.horizontalAccuracyMeters
                            + " vAcc=" + fix.verticalAccuracyMeters
                            + " alt=" + fix.altitudeMeters
                            + " speed=" + fix.speedMps);
                    updateGpsQuality(location);
                    if (!quality.isRecorded()) {
                        handleUnrecordedFix(location, quality);
                        checkpointRideSessionIfDue();
                        return;
                    }

                    updateCurrentPosition(latitude, lonngitude, true);
                    lastGoodLatitude = latitude;
                    lastGoodLongitude = lonngitude;
                    maybeRequestRideWeather(latitude, lonngitude,
                            quality.verdict == GpsFixFilter.Verdict.RECORDED_FIRST);
                    witch_lat.add(latitude);
                    witch_lon.add(lonngitude);
                    // Unknown altitude is NaN, never 0 m (GPX then simply has no <ele> for the point).
                    boolean usableAltitude = gainTracker.isUsable(fix);
                    ele.add(usableAltitude ? fix.altitudeMeters : Double.NaN);
                    godoArray.add(usableAltitude ? (float) fix.altitudeMeters : Float.NaN);

                    if (maxLat < latitude) maxLat = latitude;
                    if (minLat > latitude) minLat = latitude;
                    if (maxLon < lonngitude) maxLon = lonngitude;
                    if (minLon > lonngitude) minLon = lonngitude;

                    if (fix.hasAltitude()) {
                        RideUnitText.set(binding.Nowgodo, String.format(Locale.KOREA, "%.0f m", fix.altitudeMeters));
                    }
                    if (usableAltitude) {
                        updateElevationStats(fix);
                    }

                    getSpeed = resolveSpeedKmh(location);
                    showSpeed(getSpeed);

                    // Distance only grows by a verified step from the previous recorded point; the
                    // first point of a segment (start, resume, restore) and re-syncs add none.
                    if (quality.verdict == GpsFixFilter.Verdict.RECORDED_MOVE && mLastlocation != null) {
                        if (getSpeed > maX) {
                            maX = getSpeed;
                            RideUnitText.set(binding.maxspeed, String.format(Locale.KOREA, "%.1f km/h", maX));
                            cha_max = String.format("%.0f", maX);
                        }
                        hap = hap + mLastlocation.distanceTo(location);
                        cha_dis = String.format("%.0f", hap);

                        RideUnitText.set(binding.dis, String.format("%.1f m", hap));
                        binding.mDistance.setText(String.format("%.1f", hap) + "m");

                        double killlo = hap / 1000.0;
                        if (hap >= 1000) {
                            RideUnitText.set(binding.dis, String.format("%.1f km", killlo));
                            binding.mDistance.setText(String.format("%.1f", killlo) + "km");
                        }
                        RideUnitText.set(binding.calories, String.format(Locale.KOREA, "%d kcal", Math.round(killlo * 50.8)));

                        avg = total_time > 0 ? (hap / total_time) * 3.6 : 0;
                        RideUnitText.set(binding.avgspeed, String.format(Locale.KOREA, "%.1f km/h", avg));
                        cha_avg = String.format(Locale.KOREA, "%.0f", avg);

                        updateCurrentGrade(location);
                    }
                    mLastlocation = location;
                    checkpointRideSessionIfDue();
                }

                /** Fixes that did not become track points: keep the UI honest without recording them. */
                private void handleUnrecordedFix(Location location, GpsFixFilter.Result quality) {
                    switch (quality.verdict) {
                        case IGNORED_STATIONARY:
                            // Not moving (or inside GPS jitter): show the device's own speed, count nothing.
                            double reportedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0.0;
                            showSpeed(reportedKmh <= MAX_REASONABLE_RIDE_SPEED_KMH ? reportedKmh : 0.0);
                            break;
                        case REJECTED_STALE:
                            // A cached position may still centre the map before the first real fix.
                            if (!gpsFilter.hasRecordedPoint()) {
                                updateCurrentPosition(location.getLatitude(), location.getLongitude(), false);
                            }
                            break;
                        case WAITING_FIRST_FIX:
                        case DEFERRED_POOR_ACCURACY:
                        case REJECTED_POOR_ACCURACY:
                            refreshGpsStatus();
                            break;
                        default:
                            break;
                    }
                    boolean implausible = quality.verdict == GpsFixFilter.Verdict.REJECTED_JUMP
                            || quality.verdict == GpsFixFilter.Verdict.REJECTED_INVALID;
                    if (implausible && (lastGoodLatitude != 0.0 || lastGoodLongitude != 0.0)) {
                        // An impossible position must not become the position the SOS message reports.
                        latitude = lastGoodLatitude;
                        lonngitude = lastGoodLongitude;
                    } else if (!implausible) {
                        lastGoodLatitude = latitude;
                        lastGoodLongitude = lonngitude;
                    }
                }

                private void showSpeed(double kmh) {
                    binding.nowspeed.setText(String.format("%.1f", kmh));
                    binding.mSpeed.setText(String.format("%.1f", kmh) + "km/h");
                    binding.speedGauge.setSpeedKmh((float) kmh);
                }
                /** Remembers when a fresh (non-cached) position arrived; the chip is derived from that. */
                private void noteFixForGpsStatus(Location location) {
                    long ageMillis = (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000L;
                    if (location.getElapsedRealtimeNanos() <= 0L || ageMillis <= gpsConfig.maxFixAgeMillis) {
                        lastFreshFixElapsedMs = SystemClock.elapsedRealtime();
                        lastFixAccuracy = location.hasAccuracy() ? location.getAccuracy() : Double.NaN;
                    }
                }

                /** Shows the real state of location delivery: permission, GPS switch, fresh fixes, accuracy. */
                private void refreshGpsStatus() {
                    boolean permission = RidePermissionFlow.hasPreciseLocation(this);
                    boolean providerOn = false;
                    try {
                        providerOn = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch (RuntimeException ignored) {
                        // treat as off
                    }
                    long since = lastFreshFixElapsedMs < 0L ? -1L : SystemClock.elapsedRealtime() - lastFreshFixElapsedMs;
                    GpsStatusPolicy.State state = GpsStatusPolicy.evaluate(permission, providerOn, since, lastFixAccuracy,
                            10_000L, gpsConfig.poorHorizontalAccuracyMeters);
                    binding.gpsStatus.setText(state.label);
                    int color;
                    switch (state.tone) {
                        case GOOD:
                            color = 0xFF237A35;
                            break;
                        case WARN:
                            color = 0xFFB26A00;
                            break;
                        case BAD:
                            color = 0xFFC0392B;
                            break;
                        default:
                            color = 0xFF55615A;
                            break;
                    }
                    binding.gpsStatus.setTextColor(color);
                }

                /**
                 * Asks the server (never the weather service directly) for the observed weather where
                 * and when this ride started, and keeps the answer with the ride session. A few
                 * attempts at most; failing to get it never affects recording.
                 */
                private void maybeRequestRideWeather(double lat, double lon, boolean firstPoint) {
                    if (weatherStored || weatherInFlight || weatherAttempts >= WEATHER_MAX_ATTEMPTS) {
                        return;
                    }
                    if (firstPoint && rideStartedAtMillis == 0L) {
                        rideStartedAtMillis = System.currentTimeMillis();
                    }
                    long now = SystemClock.elapsedRealtime();
                    if (weatherAttempts > 0 && now - lastWeatherAttemptAt < WEATHER_RETRY_MS) {
                        return;
                    }
                    if (rideStartedAtMillis == 0L) {
                        // restored ride: the start is the time the ride has been running ago
                        rideStartedAtMillis = System.currentTimeMillis() - (long) (total_time + rest) * 1000L;
                    }
                    weatherAttempts++;
                    lastWeatherAttemptAt = now;
                    weatherInFlight = true;
                    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                    format.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
                    String startedAt = format.format(new java.util.Date(rideStartedAtMillis));
                    String riderId = com.bike.trailgo.setting.AccountSession.isGuest(this)
                            ? null : getSharedPreferences("auto", MODE_PRIVATE).getString("LoginId", null);
                    final java.io.File directory = getFilesDir();
                    serverApi.getWeather(lat, lon, startedAt, riderId)
                            .enqueue(new Callback<com.bike.trailgo.dto.RecordWeather>() {
                                @Override
                                public void onResponse(Call<com.bike.trailgo.dto.RecordWeather> call,
                                                       Response<com.bike.trailgo.dto.RecordWeather> response) {
                                    weatherInFlight = false;
                                    com.bike.trailgo.dto.RecordWeather weather = response.isSuccessful() ? response.body() : null;
                                    if (weather != null && weather.isAvailable()) {
                                        try {
                                            RideWeatherFile.save(directory, weather);
                                            weatherStored = true;
                                        } catch (java.io.IOException error) {
                                            Log.w("RideWeather", "Start weather not kept: " + error.getClass().getSimpleName());
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<com.bike.trailgo.dto.RecordWeather> call, Throwable error) {
                                    weatherInFlight = false;
                                    Log.w("RideWeather", "Start weather unavailable: " + error.getClass().getSimpleName());
                                }
                            });
                }

                private void checkpointRideSessionIfDue() {
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastSessionCheckpointMillis >= SESSION_CHECKPOINT_INTERVAL_MS) {
                        persistRideSession();
                    }
                }

                private void persistRideSession() {
                    RideSessionStore.save(this, new RideSessionStore.Snapshot(
                            false, cur_status, BaseTime, PauseTime, rest, total_time,
                            hap, maX, avg, getgodo, totalDescentMeters,
                            maxLat, maxLon, minLat, minLon,
                            latitude, lonngitude,
                            mLastlocation != null && mLastlocation.hasAltitude() ? mLastlocation.getAltitude() : 0d,
                            adress_value, address_dong,
                            witch_lat, witch_lon, ele, godoArray
                    ));
                    lastSessionCheckpointMillis = SystemClock.elapsedRealtime();
                }

                private boolean restoreRideSession() {
                    RideSessionStore.Snapshot snapshot = RideSessionStore.load(this);
                    if (snapshot == null || snapshot.followMode) {
                        return false;
                    }

                    cur_status = snapshot.status;
                    BaseTime = snapshot.baseTime;
                    PauseTime = snapshot.pauseTime;
                    rest = snapshot.restSeconds;
                    total_time = snapshot.activeSeconds;
                    // Diagnostics only: BaseTime is an elapsedRealtime() value, so the timer keeps
                    // counting across process death, and a reboot makes it meaningless (negative).
                    long restoredElapsedMs = SystemClock.elapsedRealtime() - snapshot.baseTime;
                    Log.w("RideRestore", "status=" + snapshot.status
                            + ", savedActiveSec=" + snapshot.activeSeconds
                            + ", elapsedSinceBaseSec=" + (restoredElapsedMs / 1000L)
                            + ", distanceM=" + snapshot.distanceMeters
                            + ", restSec=" + snapshot.restSeconds
                            + (restoredElapsedMs < 0 ? " [reboot suspected]" : ""));
                    hap = snapshot.distanceMeters;
                    maX = snapshot.maxSpeedKmh;
                    avg = snapshot.averageSpeedKmh;
                    getgodo = snapshot.elevationGainMeters;
                    totalDescentMeters = snapshot.descentMeters;
                    gainBaseMeters = snapshot.elevationGainMeters;
                    descentBaseMeters = snapshot.descentMeters;
                    elevationTotalsShown = true;
                    maxLat = snapshot.maxLat;
                    maxLon = snapshot.maxLon;
                    minLat = snapshot.minLat;
                    minLon = snapshot.minLon;
                    latitude = snapshot.lastLatitude;
                    lonngitude = snapshot.lastLongitude;
                    adress_value = snapshot.address == null ? "" : snapshot.address;
                    address_dong = snapshot.addressDong;
                    witch_lat = new ArrayList<>(snapshot.latitudes);
                    witch_lon = new ArrayList<>(snapshot.longitudes);
                    ele = new ArrayList<>(snapshot.elevations);
                    godoArray = new ArrayList<>(snapshot.elevationSamples);
                    ridingRoutePoints.clear();
                    int pointCount = Math.min(witch_lat.size(), witch_lon.size());
                    for (int index = 0; index < pointCount; index++) {
                        ridingRoutePoints.add(LatLng.from(witch_lat.get(index), witch_lon.get(index)));
                    }
                    if (!witch_lat.isEmpty()) {
                        Location last = new Location(LocationManager.GPS_PROVIDER);
                        last.setLatitude(latitude);
                        last.setLongitude(lonngitude);
                        last.setAltitude(snapshot.lastAltitude);
                        mLastlocation = last;
                        lastGoodLatitude = latitude;
                        lastGoodLongitude = lonngitude;
                    }
                    binding.resttime.setText(formatDuration(rest));
                    RideUnitText.set(binding.getgodo, String.format(Locale.KOREA, "%d m", getgodo));
                    RideUnitText.set(binding.totalDescent, String.format(Locale.KOREA, "%d m", totalDescentMeters));
                    RideUnitText.set(binding.maxspeed, String.format(Locale.KOREA, "%.1f km/h", maX));
                    RideUnitText.set(binding.avgspeed, String.format(Locale.KOREA, "%.1f km/h", avg));
                    if (cur_status == Pause) {
                        binding.resumeBt.setVisibility(View.VISIBLE);
                        binding.pausebt.setVisibility(View.GONE);
                    }
                    return true;
                }

                /**
                 * Climb/descent from smoothed GPS altitude (see ElevationGainTracker): the first usable
                 * altitude is only a baseline, noise and wild samples do not add up, steep real
                 * changes are followed.
                 */
                private void updateElevationStats(GpsFix fix) {
                    ElevationGainTracker.Status status = gainTracker.onFix(fix);
                    if (!elevationTotalsShown && status != ElevationGainTracker.Status.SKIPPED_NO_ALTITUDE
                            && status != ElevationGainTracker.Status.SKIPPED_POOR_ACCURACY) {
                        // First usable altitude: show 0 m instead of "-" for both totals
                        elevationTotalsShown = true;
                        RideUnitText.set(binding.getgodo, String.format(Locale.KOREA, "%d m", getgodo));
                        RideUnitText.set(binding.totalDescent, String.format(Locale.KOREA, "%d m", totalDescentMeters));
                    }
                    if (status == ElevationGainTracker.Status.OUTLIER || status == ElevationGainTracker.Status.RESYNC) {
                        Log.d("GpsElevation", status + " alt=" + fix.altitudeMeters
                                + " smoothed=" + gainTracker.getSmoothedAltitude());
                    }
                    int gain = gainBaseMeters + (int) Math.round(gainTracker.getGainMeters());
                    int descent = descentBaseMeters + (int) Math.round(gainTracker.getDescentMeters());
                    if (gain != getgodo) {
                        getgodo = gain;
                        RideUnitText.set(binding.getgodo, String.format(Locale.KOREA, "%d m", getgodo));
                    }
                    if (descent != totalDescentMeters) {
                        totalDescentMeters = descent;
                        RideUnitText.set(binding.totalDescent, String.format(Locale.KOREA, "%d m", totalDescentMeters));
                    }
                }
                private void updateCurrentGrade(Location location) {
                    if (!location.hasAltitude()) {
                        binding.currentGrade.setText("-- %");
                        return;
                    }

                    if (gradeBaselineLocation == null) {
                        gradeBaselineLocation = new Location(location);
                        return;
                    }

                    float horizontalDistance = gradeBaselineLocation.distanceTo(location);
                    if (horizontalDistance < 10f) return;

                    double grade = ((location.getAltitude() - gradeBaselineLocation.getAltitude())
                            / horizontalDistance) * 100.0;
                    gradeBaselineLocation = new Location(location);
                    // Values outside this range are overwhelmingly likely to be a GPS altitude jump.
                    if (Math.abs(grade) > 35.0) {
                        binding.currentGrade.setText("-- %");
                        return;
                    }
                    binding.currentGrade.setText(String.format(Locale.KOREA, "%+.1f %%", grade));
                }

                private void updateGpsQuality(Location location) {
                    if (location.hasAccuracy()) {
                        binding.gpsAccuracy.setText(String.format(Locale.KOREA, "±%.0f m", location.getAccuracy()));
                    } else {
                        binding.gpsAccuracy.setText("확인 중");
                    }
                }

                private double resolveSpeedKmh(Location location) {
                    long receivedAtMillis = SystemClock.elapsedRealtime();
                    double reportedSpeedKmh = location.getSpeed() * 3.6;
                    if (location.hasSpeed() && reportedSpeedKmh > 0.1 && reportedSpeedKmh <= MAX_REASONABLE_RIDE_SPEED_KMH) {
                        lastSpeedLocationReceivedAtMillis = receivedAtMillis;
                        return reportedSpeedKmh;
                    }

                    if (mLastlocation == null) {
                        lastSpeedLocationReceivedAtMillis = receivedAtMillis;
                        return 0.0;
                    }

                    long elapsedMillis = location.getElapsedRealtimeNanos() > mLastlocation.getElapsedRealtimeNanos()
                            ? (location.getElapsedRealtimeNanos() - mLastlocation.getElapsedRealtimeNanos()) / 1_000_000L
                            : location.getTime() - mLastlocation.getTime();
                    if (elapsedMillis <= 0L && lastSpeedLocationReceivedAtMillis > 0L) {
                        elapsedMillis = receivedAtMillis - lastSpeedLocationReceivedAtMillis;
                    }
                    lastSpeedLocationReceivedAtMillis = receivedAtMillis;
                    if (elapsedMillis < 250L || elapsedMillis > 15_000L) return 0.0;

                    double inferredSpeedKmh = mLastlocation.distanceTo(location) * 3600.0 / elapsedMillis;
                    return inferredSpeedKmh <= MAX_REASONABLE_RIDE_SPEED_KMH ? inferredSpeedKmh : 0.0;
                }


                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {
                    refreshGpsStatus();
                // ????썹땟??癲ル슢???ъ쒜??????욍걛???ш끽維??
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
                    refreshGpsStatus();
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
                    if (locationManager != null && locationUpdatesStarted) {
                        locationManager.removeUpdates(this);
                        locationUpdatesStarted = false;
                    }
                    if (rideLocationReceiverRegistered) {
                        unregisterReceiver(rideLocationReceiver);
                        rideLocationReceiverRegistered = false;
                    }
                    RidingTimer.removeCallbacksAndMessages(null);
                    gpsWatchdog.removeCallbacks(gpsWatchdogTask);
                    hazardBannerHandler.removeCallbacksAndMessages(null);
                    if (hazardReceiverRegistered) {
                        unregisterReceiver(hazardReceiver);
                        hazardReceiverRegistered = false;
                    }
                    if (hazardRepository != null) hazardRepository.removeListener(hazardListener);
                    if (hazardMapLayer != null) {
                        hazardMapLayer.clear();
                        hazardMapLayer = null;
                    }
                    if (kakaoV2MapView != null) {
                        kakaoV2MapView.finish();
                        kakaoV2MapView = null;
                    }
                    kakaoMap = null;
                    ridingRouteLine = null;
                    ridingRouteLayer = null;
                    currentLocationLabel = null;
                    currentLocationLayer = null;
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
                    if (kakaoV2MapView != null) {
                        kakaoV2MapView.pause();
                    }
                    Log.d("onPause", "onPause");
                }

                @Override
                protected void onResume() {
                    super.onResume();
                    RideSettings.applyRidingDisplay(this);
                    renderHazards();
                    if (kakaoV2MapView != null) {
                        kakaoV2MapView.resume();
                    }
                    Log.d("onResume", "OnResume");
                }

                @Override
                protected void onStart() {
                    super.onStart();
                }

                @Override
                protected void onStop() {
                    super.onStop();
                    Log.d("SpeechRecognizer", "stopped");
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
                    return SosHelper.buildEmergencyMessage(adress_value, latitude, lonngitude);
                }

                private RecognitionListener listener = new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        //Toast.makeText(getApplicationContext(), "?????癲ル슢???彛????嶺뚮??ｆ뤃??嶺뚮ㅎ????", Toast.LENGTH_SHORT).show();
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
                                message = "Audio error";
                                break;
                            case SpeechRecognizer.ERROR_CLIENT:
                                message = "Client error";
                                break;
                            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                message = "Permission error";
                                break;
                            case SpeechRecognizer.ERROR_NETWORK:
                                message = "Network error";
                                break;
                            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                message = "Network timeout";
                                break;
                            case SpeechRecognizer.ERROR_NO_MATCH:
                                message = "No match";
                                break;
                            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                message = "Recognizer busy";
                                break;
                            case SpeechRecognizer.ERROR_SERVER:
                                message = "Server error";
                                break;
                            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                message = "Speech timeout";
                                break;
                            default:
                                message = "Unknown error";
                                break;
                        }

                       // Toast.makeText(getApplicationContext(), "????嚥?夷???쎛 ?熬곣뫖利든뜏類ｋ렱?????????? : " + message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResults(Bundle results) {
                        // ?꿔꺂????????????ArrayList????潁????鶯ㅺ동????궰?textView????潁???????ㅿ폎?묐쵈?딂린?곌땀????
                        ArrayList<String> matches =
                                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                        if (matches == null || matches.isEmpty()) {
                            return;
                        }

                        for (int i = 0; i < matches.size(); i++) {
                            a = matches.get(i);
                            Log.d("a????????", a);
                        }
                        switch (a) {
                            case "\uC704\uCE58\uC804\uC1A1":
                                SosHelper.confirmContactMessage(StartActivity.this, emergencyMessage());
                                break;
                            case "119\uC5D0 \uC804\uD654":
                                SosHelper.confirmEmergencyServiceCall(StartActivity.this);
                                break;
                            case "\uC9C0\uC815\uBC88\uD638\uC5D0 \uC704\uCE58\uC804\uC1A1":
                                SosHelper.confirmContactMessage(StartActivity.this, emergencyMessage());
                                break;
                            case "\uC9C0\uC815\uBC88\uD638\uC5D0 \uC804\uD654":
                                SosHelper.confirmContactCall(StartActivity.this);
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
