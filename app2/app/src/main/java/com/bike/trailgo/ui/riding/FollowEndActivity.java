package com.bike.trailgo.ui.riding;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.bike.trailgo.R;
import com.bike.trailgo.SendGPXFile;
import com.bike.trailgo.databinding.FragmentRidingEndBinding;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.Competition_Status;
import com.bike.trailgo.dto.Mission;
import com.bike.trailgo.dto.Mission_Status;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.gpx.GpxInfo;
import com.bike.trailgo.net.HttpClient;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.net.ServerApi;
import com.bike.trailgo.setting.RideSettings;
import com.bike.trailgo.setting.AccountSession;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.Gson;

import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;

import org.json.JSONArray;
import org.json.JSONObject;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowEndActivity extends AppCompatActivity {
    private FragmentRidingEndBinding binding;
    private final ServerApi serverApi = Server.getInstance().getApi();

    //?????????ロ룂???????????좊읈??嶺뚮ㅎ?닸쾮??String?嶺???
    int MaxSpeed=0;
    int AvgSpeed=0;
    int Getgodo=0;
    int RestTime=0;
    int IngTime=0;
    int Distence=0;
    String rr_num = "";
    String rr_rider="";
    String rr_comp="";
    String address_dong="";
    String course_name="";
    String comp_point="";
    //?????????ロ룂???????????좊읈??嶺뚮ㅎ?닸쾮???좊즴????????몄릇?嶺뚮ㅎ?붷ㅇ????源놁젳


    int check, clearCount=0, riding_point=0;

    private RidePreviewMap ridePreviewMap;
    //DB????ш끽維뽬땻????꾨탿 ??ш낄援η뵳??怨뚮뼚???
    String open = "1";

    //?棺??짆??嶺뚮ㅎ???륁녃??袁⑸즵?룸돁????됰씚逾?
    SharedPreferences auto;
    String LoginId;
    int[] typeScore;

    int r_club;
    int dis;

    //???怨뚮뼚???
    String comp_name, adress_value;

    Intent intent;

    private LineChart lineChart;
    private Object endActivity;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentRidingEndBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //?棺??짆??嶺뚮ㅎ???륁녃??袁⑸즵?룸돁????됰씚逾?
        auto = getSharedPreferences("auto", Activity.MODE_PRIVATE);
        LoginId  = auto.getString("LoginId","");
        r_club = auto.getInt("r_club", 0);

        endActivity = this;

        //??筌뚯슦苑??intent???壤굿??몃탿
        intent = new Intent(this.getIntent());

        Distence = intent.getIntExtra("distence",0); //????猷뱀돥?얠쉪??ㅽ떜??
        dis = Distence;
        MaxSpeed = intent.getIntExtra("maxspeed",0); //癲ル슔?됭짆?????뽦뵣
        AvgSpeed = intent.getIntExtra("avgspeed",0); //????????뽦뵣
        Getgodo = intent.getIntExtra("getgodo",0); //????볥뜪??關履??
        RestTime = intent.getIntExtra("resttime",0); //???⑤８六??癰???
        IngTime = intent.getIntExtra("ingtime",0); //??繹먮끏???獄쏅챶六????筌먐뼘??
        check = intent.getIntExtra("check", 0);
        rr_comp = intent.getStringExtra("rr_comp");
        comp_name = intent.getStringExtra("comp_name");
        adress_value = intent.getStringExtra("addr");
        address_dong = intent.getStringExtra("address_dong");
        course_name = intent.getStringExtra("course_name");
        comp_point = intent.getStringExtra("comp_point");


        Log.d("End Activity", Distence + " " + MaxSpeed + " " + AvgSpeed + " " + Getgodo + " " + RestTime + " " + IngTime +" " +check + " " + rr_comp + " "+ comp_name+ " " + adress_value      );
        RideRouteCache.RouteData cachedRoute = RideRouteCache.load(
                this, intent.getStringExtra(RideRouteCache.EXTRA_CACHE_FILE)
        );
        ArrayList<Double> witch_lat = cachedRoute != null
                ? cachedRoute.latitudes : (ArrayList<Double>) intent.getSerializableExtra("witch_lat");
        ArrayList<Double> witch_lon = cachedRoute != null
                ? cachedRoute.longitudes : (ArrayList<Double>) intent.getSerializableExtra("witch_lon");
        ArrayList<Double> ele = cachedRoute != null
                ? cachedRoute.elevations : (ArrayList<Double>) intent.getSerializableExtra("ele");
        ArrayList<Float> godolist = cachedRoute != null
                ? cachedRoute.elevationSamples : (ArrayList<Float>) intent.getSerializableExtra("godoarray");
        if (witch_lat == null) witch_lat = new ArrayList<>();
        if (witch_lon == null) witch_lon = new ArrayList<>();
        if (ele == null) ele = new ArrayList<>();
        if (godolist == null) godolist = new ArrayList<>();
        Bundle extras = intent.getExtras();
        double maxLat = cachedRoute != null ? cachedRoute.maxLat : extras == null ? 0 : extras.getDouble("maxLat", 0);
        double maxLon = cachedRoute != null ? cachedRoute.maxLon : extras == null ? 0 : extras.getDouble("maxLon", 0);
        double minLat = cachedRoute != null ? cachedRoute.minLat : extras == null ? 0 : extras.getDouble("minLat", 0);
        double minLon = cachedRoute != null ? cachedRoute.minLon : extras == null ? 0 : extras.getDouble("minLon", 0);

        //????숆강筌???
        ArrayList<Entry> entry_chart = new ArrayList<>();
        lineChart = (LineChart) findViewById(R.id.chart);//layout??id
        LineData chartData = new LineData();
        for(int g=0; g<godolist.size(); g++){
            entry_chart.add(new Entry(g, godolist.get(g)));
        }

            /* 癲ル슢??節됰쑏?(2, 3) add???寃뗏?(2, 5)??筌먲퐢???關履???
            ??れ삀???(2, 3)??????瑜????釉먮윪????ш끽維???x??좊읈? 2?嶺뚮∥梨뜻쾮??y??좊읈? 3, 5???????癲ル슔?蹂?엥??*/
        LineDataSet lineDataSet = new LineDataSet(entry_chart, "Elevation");
        lineDataSet.setColor(Color.parseColor("#1F8A3B"));
        lineDataSet.setLineWidth(2.25f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillColor(Color.parseColor("#E4F4E7"));
        chartData.addDataSet(lineDataSet);

        lineChart.setData(chartData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setDrawAxisLine(false);
        lineChart.getAxisLeft().setDrawLabels(false);
        lineChart.getAxisLeft().setDrawAxisLine(false);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#E7ECE8"));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.invalidate();


        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        ridePreviewMap = new RidePreviewMap(this, mapViewContainer, witch_lat, witch_lon);
        ridePreviewMap.start();
        MapPolyline polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline ??濡ル㎥??癲ル슣????

        // Polyline ???щ뮡嶺?癲ル슣????
        int mapPointCount = Math.min(witch_lat.size(), witch_lon.size());
        for (int i = 0; i < mapPointCount; i++) {
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(witch_lat.get(i), witch_lon.get(i)));
        }

        Button save = (Button) findViewById(R.id.saveriding);
        RadioGroup openselect = (RadioGroup)findViewById(R.id.selectgroup);
        RideSettings.migrate(this);
        open = RideSettings.isRecordPublic(this) ? "1" : "0";
        if (AccountSession.isGuest(this)) {
            open = "0";
        }
        openselect.check("0".equals(open) ? R.id.NoOpen : R.id.AllOpen);
        if (AccountSession.isGuest(this)) {
            openselect.setEnabled(false);
            for (int i = 0; i < openselect.getChildCount(); i++) {
                openselect.getChildAt(i).setEnabled(false);
            }
            android.widget.Toast.makeText(this, "비회원 기록은 이 기기에만 비공개로 저장됩니다.", android.widget.Toast.LENGTH_LONG).show();
        }
        findViewById(R.id.cancelriding).setOnClickListener(v -> confirmDiscardRide());
        findViewById(R.id.cancelriding_bottom).setOnClickListener(v -> confirmDiscardRide());

        //??嚥▲꺆諭???袁⑸즵?룸돁???影??????源낃도 ????몄릇?嶺뚮ㅎ?붷ㅇ????源놁젳
        binding.endavg.setText(String.valueOf(AvgSpeed)+"km/h"); //????????뽦뵣
        binding.endmax.setText(String.valueOf(MaxSpeed)+"km/h"); //癲ル슔?됭짆?????뽦뵣


        //??낆뒩???????몄릇??
        String text = adress_value == null ? "" : adress_value.replace("\uB300\uD55C\uBBFC\uAD6D", "").trim();
        binding.addr.setText(text);

        //???⑤８六??癰?????癲ル슣??????몃빰??
        int hour;
        int min;
        int sec = IngTime;

        min = sec/60;
        hour = min/60;
        sec = sec % 60;
        min = min % 60;
        if(hour == 0){
            hour=0;
        }
        if(min==0){
            min=0;
        }

        int r_sec = RestTime;
        int r_hour;
        int r_min;

        r_min = r_sec/60;
        r_hour = r_min/60;
        r_sec = r_sec%60;
        r_min = r_min % 60;
        if(r_hour == 0){
            r_hour=00;
        }
        if(r_min==0){
            r_min=00;
        }
        binding.ending.setText(String.valueOf(hour + "\uC2DC\uAC04 " + min + "\uBD84 " + sec + "\uCD08"));
        binding.endresttime.setText(String.valueOf(r_hour + "\uC2DC\uAC04 " + r_min + "\uBD84 " + r_sec + "\uCD08"));

        binding.endget.setText(String.valueOf(Getgodo)+"m");
        double killlo = 0;

        if (dis >= 1000) {
            killlo = dis / 1000.0;

            binding.enddis.setText(String.format("%.1f", killlo) + "km");
        }else{
            binding.enddis.setText(Distence+"m");
        }


        // gpx???????筌먯룄肄????ш낄援????ш끽維????癰???????엮??堉?
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        String nowTime = sdfNow.format(date);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        NetworkTask2 networkTask = new NetworkTask2();
        final String select;

        //???????? ???ャ뀕??
        openselect.setOnCheckedChangeListener((group, checkedId) -> {
            // The legacy API currently accepts private/public only.
            open = checkedId == R.id.NoOpen ? "0" : "1";
        });


        // gpx ???????獄쏅똻??
        GpxInfo gpxInfoObj = new GpxInfo();
        gpxInfoObj.setCreator("TrailGO");

        GpxInfo.metadata metaObj = new GpxInfo.metadata();
        gpxInfoObj.setMetadata(metaObj);

        metaObj.setDesc("TrailGO");

        GpxInfo.bounds boundsObj = new GpxInfo.bounds();
        metaObj.setBounds(boundsObj);

        boundsObj.setMaxLat(maxLat);
        boundsObj.setMaxLon(maxLon);
        boundsObj.setMinLat(minLat);
        boundsObj.setMinLon(minLon);

        GpxInfo.trk trkObj = new GpxInfo.trk();
        gpxInfoObj.setTrk(trkObj);

        GpxInfo.trkseg trksegObj = new GpxInfo.trkseg();
        trkObj.setTrkseg(trksegObj);

        List<GpxInfo.trkpt> trkptArray = new ArrayList<GpxInfo.trkpt>();
        ArrayList<Entry> entries = new ArrayList<>();
        int gpxPointCount = Math.min(mapPointCount, ele.size());
        for (int i = 0; i < gpxPointCount; i++) {
            GpxInfo.trkpt trkptObj = new GpxInfo.trkpt(witch_lat.get(i), witch_lon.get(i), ele.get(i));
            trkptArray.add(trkptObj);
        }
        trksegObj.setTrkptList(trkptArray);
        // Create a file to save to and make sure to use the path provided from
        // getFilesDir().getPath().
        File xmlFile = new File(getFilesDir().getPath() + "/gpx_" + nowTime + ".gpx");
        // Serialize the Person
        try
        {
            Serializer serializer = new Persister();
            serializer.write(gpxInfoObj, xmlFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Create a second person object
        GpxInfo gpxInfoObj2 = null;

        // Deserialize the Person
        if (xmlFile.exists())
        {
            try
            {
                Serializer serializer = new Persister();
                gpxInfoObj2 = serializer.read(GpxInfo.class, xmlFile);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        boolean b = gpxInfoObj.equals(gpxInfoObj2);
        // ???源껉펾??? gpx???????獄쏅똻??


        save.setOnClickListener(v -> {
            String rideName = binding.ridingNameinput.getText().toString().trim();
            if (rideName.isEmpty()) {
                rideName = "라이딩 기록";
            }
            if (AccountSession.isGuest(this)) {
                saveGuestRide(xmlFile, rideName, save);
                return;
            }
            if (LoginId == null || LoginId.trim().isEmpty()) {
                android.widget.Toast.makeText(this, "로그인 정보를 확인할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            String gpxFileName = "gpx_" + nowTime + ".gpx";
            MultipartBody.Part gpxPart = Server.getFilePart(xmlFile.getAbsolutePath(), gpxFileName);
            if (gpxPart == null) {
                android.widget.Toast.makeText(FollowEndActivity.this, "GPX 파일을 만들지 못해 기록을 저장할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            save.setEnabled(false);
            HashMap<String, Object> insert = new HashMap<>();
            insert.put("rr_rider", LoginId);
            insert.put("rr_distance", String.valueOf(Distence));
            insert.put("rr_topspeed", String.valueOf(MaxSpeed));
            insert.put("rr_avgspeed", String.valueOf(AvgSpeed));
            insert.put("rr_high", String.valueOf(Getgodo));
            insert.put("rr_gpx", gpxFileName);
            insert.put("rr_open", open);
            insert.put("rr_breaktime", String.valueOf(RestTime));
            insert.put("rr_time", String.valueOf(IngTime));
            insert.put("rr_area", adress_value == null ? "" : adress_value);
            insert.put("rr_name", rideName);
            if (rr_comp != null && !rr_comp.trim().isEmpty() && !"null".equalsIgnoreCase(rr_comp.trim())) {
                insert.put("rr_comp", rr_comp);
            }
            insert.put("rr_fonum", course_name == null ? "" : course_name);

            serverApi.upload(gpxPart, "gpx", gpxFileName).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        save.setEnabled(true);
                        android.widget.Toast.makeText(FollowEndActivity.this, "GPX 업로드에 실패했습니다. 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveFollowRideRecord(insert, save);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable error) {
                    Log.e("RideSave", "Unable to upload followed GPX", error);
                    save.setEnabled(true);
                    android.widget.Toast.makeText(FollowEndActivity.this, "네트워크 연결을 확인한 뒤 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveGuestRide(File xmlFile, String rideName, Button save) {
        if (!xmlFile.isFile() || xmlFile.length() == 0L) {
            android.widget.Toast.makeText(this, "GPX 파일을 만들지 못해 이 기기에 저장할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        save.setEnabled(false);
        new Thread(() -> {
            try {
                LocalRideStore.save(
                        FollowEndActivity.this,
                        rideName,
                        Distence,
                        MaxSpeed,
                        AvgSpeed,
                        Getgodo,
                        RestTime,
                        IngTime,
                        adress_value,
                        xmlFile
                );
                runOnUiThread(() -> finishGuestSave());
            } catch (IOException error) {
                Log.e("GuestRideSave", "Unable to save local guest ride", error);
                runOnUiThread(() -> {
                    save.setEnabled(true);
                    android.widget.Toast.makeText(FollowEndActivity.this, "이 기기에 기록을 저장하지 못했습니다. 저장공간을 확인해 주세요.", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void finishGuestSave() {
        android.widget.Toast.makeText(this, "주행 기록을 이 기기에 저장했습니다.", android.widget.Toast.LENGTH_SHORT).show();
        RideSessionStore.clear(this);
        RideRouteCache.clear(this);
        Intent home = new Intent(this, com.bike.trailgo.MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    private void saveFollowRideRecord(HashMap<String, Object> insert, Button save) {
        serverApi.InsertRecord(insert).enqueue(new Callback<java.util.Map<String, String>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, String>> call, Response<java.util.Map<String, String>> response) {
                if (!response.isSuccessful()) {
                    save.setEnabled(true);
                    android.widget.Toast.makeText(FollowEndActivity.this, "기록 저장에 실패했습니다. 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                android.widget.Toast.makeText(FollowEndActivity.this, "라이딩 기록을 저장했습니다.", android.widget.Toast.LENGTH_SHORT).show();
                RideSessionStore.clear(FollowEndActivity.this);
                RideRouteCache.clear(FollowEndActivity.this);
                Intent home = new Intent(FollowEndActivity.this, com.bike.trailgo.MainActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);
                finish();
            }

            @Override
            public void onFailure(Call<java.util.Map<String, String>> call, Throwable error) {
                Log.e("RideSave", "Unable to save followed ride", error);
                save.setEnabled(true);
                android.widget.Toast.makeText(FollowEndActivity.this, "네트워크 연결을 확인한 뒤 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class GetRecordTask extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected String doInBackground(Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/riding/getrecord");

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                Log.d("GetRecord", s);
                String tempData = s;

                JSONArray jarray2 = new JSONArray(tempData);
                for (int i = 0; i < jarray2.length(); i++) {
                    JSONObject jObject2 = jarray2.getJSONObject(i);
                    rr_num = jObject2.getString("rr_num");
                }
                Log.d("GetRecord2", rr_num);

                TagInsert tgagInsert = new TagInsert();
                Map<String, String> par = new HashMap<String, String>();
                par.put("rr_num", rr_num);
                par.put("rr_rider",LoginId);
                par.put("address_dong","#"+address_dong);
                tgagInsert.execute(par);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public class TagInsert extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected String doInBackground(Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("POST", "app/riding/taginsert");
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;

        }

        @Override
        protected void onPostExecute(String s) {
            try {
                Log.d("JSON_RESULT", s);
                String tempData = s;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ridePreviewMap != null) {
            ridePreviewMap.resume();
        }
    }

    @Override
    protected void onPause() {
        if (ridePreviewMap != null) {
            ridePreviewMap.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ridePreviewMap != null) {
            ridePreviewMap.finish();
            ridePreviewMap = null;
        }
        super.onDestroy();
    }


    public class NetworkTask2 extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected String doInBackground(Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("POST", "app/riding/foup");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {

        }
    }

    // ?濡ろ뜑??????ш낄援??????? ????낅묄 ????녿ぅ??熬곣뫀肄?
    public class UpdateCompScore extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected String doInBackground(Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/updateCompScore");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {
        }
    }

    //?濡ろ뜑????癲ル슢?꾤땟?????????ш낄援????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
    public class getCompAllScore extends AsyncTask<Map<String, String>, Integer, String> {

        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/getCompAllScore/" + rr_comp);
            // Parameter ????ш끽維뽬땻??筌먲퐢??

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                String tempData = s;

                Gson gson = new Gson();
                ArrayList<Competition_Status> itemList = new ArrayList<>();
                Competition_Status[] items = gson.fromJson(tempData, Competition_Status[].class);

                for ( int i = 0; i<items.length; i++){
                    if ( items[i].getCs_club() == r_club)
                        items[i].setCs_score( items[i].getCs_score() + Integer.parseInt(String.valueOf(AvgSpeed)));
                    itemList.add(items[i]);
                }

                ArrayList<Integer> tempInt = new ArrayList<>();

                for ( int j = 0; j < itemList.size(); j++) {
                    tempInt.add(itemList.get(j).getCs_score());
                }

                // ??????嶺뚮㉡?ｈ??
                Collections.sort(tempInt);
                Collections.reverse(tempInt);

                ArrayList<Competition_Status> NewItemList = new ArrayList<>();

                for ( int l = 0; l < itemList.size(); l ++){
                    for ( int k = 0; k < itemList.size(); k++){
                        if ( tempInt.get(l) == itemList.get(k).getCs_score() )
                            NewItemList.add(itemList.get(k));
                    }
                }

                int count = NewItemList.size();

                updateRank[] UpdateRank = new updateRank[count];
                Map<String, String>[] params3 = new HashMap[count];

                for ( int k = 0; k < count; k++) {
                    UpdateRank[k] = new updateRank();
                    params3[k] = new HashMap<>();
                    params3[k].put("rr_comp", rr_comp);
                    params3[k].put("r_club", String.valueOf(NewItemList.get(k).getCs_club()));
                    params3[k].put("score", String.valueOf(NewItemList.get(k).getCs_score()));
                    params3[k].put("rank", String.valueOf(k+1));
                    UpdateRank[k].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params3[k]);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    // ??筌믨퀡彛?????녿ぅ??熬곣뫀肄?
    public class updateRank extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected synchronized String doInBackground( Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/updateRank/");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {
        }
    }

    public class getMissionStatus extends AsyncTask<Map<String, String>, Integer, String> {

        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/getMissionStatus/" + LoginId);
            // Parameter ????ш끽維뽬땻??筌먲퐢??

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                String tempData = s;

                Gson gson = new Gson();
                ArrayList<Mission_Status> itemList = new ArrayList<>();
                Mission_Status[] items = gson.fromJson(tempData, Mission_Status[].class);

                for ( int i = 0; i<items.length; i++){
                    itemList.add(items[i]);
                }

                typeScore = new int[itemList.size()];

                for ( int j = 0; j<itemList.size(); j++) {
                    if ( itemList.get(j).getMs_type() == 1)
                        typeScore[0] = itemList.get(0).getMs_score() + Integer.parseInt(String.valueOf(Distence)); // ??癲꾧퀗??????節뚮쳮雅?
                    else if ( itemList.get(j).getMs_type() == 2) {
                        typeScore[1] = itemList.get(1).getMs_score(); // ?濡ろ뜑????癲ル슔?蹂?덫??????낅묄 ??節뚮쳮雅?
                        if (!"null".equals(rr_comp))
                            typeScore[1] += 1;
                    }
                    else if ( itemList.get(j).getMs_type() == 3) {
                        typeScore[2] = itemList.get(j).getMs_score();
                    }

                }

                new getNoClearMission().execute();

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public class getNoClearMission extends AsyncTask<Map<String, String>, Integer, String> {

        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/getNoClearMission/" + LoginId);
            // Parameter ????ш끽維뽬땻??筌먲퐢??

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                String tempData = s;

                Gson gson = new Gson();
                ArrayList<Mission> itemList = new ArrayList<>();
                ArrayList<Mission> clearList = new ArrayList<>();
                Mission[] items = gson.fromJson(tempData, Mission[].class);

                for ( int i = 0; i<items.length; i++){
                    itemList.add(items[i]);
                }

                for ( int j = 0; j<items.length; j++) {
                    if (itemList.get(j).getM_type() == 1) {
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if (typeScore[0] >= Integer.parseInt(changeStr)) {
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }
                    else if ( itemList.get(j).getM_type() == 2){
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if ( typeScore[1] >= Integer.parseInt(changeStr)){
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }

                    if ( itemList.get(j).getM_type() == 3){
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if ( typeScore[2] >= Integer.parseInt(changeStr)){
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }

                    if ( itemList.get(j).getM_type() == 4){
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if ( Integer.parseInt(String.valueOf(AvgSpeed)) >= Integer.parseInt(changeStr)){
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }
                }

                updateMissionStatus UMS = new updateMissionStatus();

                Map<String, String> params4 = new HashMap<>();

                params4.put("LoginId", LoginId);
                params4.put("typeScore1", String.valueOf(typeScore[0]));
                params4.put("typeScore2", String.valueOf(typeScore[2]));
                UMS.execute(params4);


                int count = clearList.size();
                if ( count >= 1 ){
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date time = new Date();
                    String time2 = format.format(time);
                    insertMissionCom[] IMC = new insertMissionCom[count];
                    Map<String, String>[] params5 = new HashMap[count];
                    for ( int k = 0; k < clearList.size(); k++){
                        IMC[k] = new insertMissionCom();
                        params5[k] = new HashMap<>();
                        params5[k].put("LoginId", LoginId);
                        params5[k].put("mc_mission", String.valueOf(clearList.get(k).getM_num()));
                        params5[k].put("mc_time", time2);
                        params5[k].put("m_point", String.valueOf(clearList.get(k).getM_point()));

                        riding_point += clearList.get(k).getM_point();
                        IMC[k].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params5[k]);
                    }
                }

                // ???源낇꼧???⑥????袁⑸즴????
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder((Context) endActivity);
                // 癲ル슢??袁λ빝?壤?
                String text = "??繹먮끏?????れ삀??쎈뭄?????潁뺛깾逾???怨?????덊렡";
                String text1 = "";
                String text2 = "";
                String text3 = riding_point + " ??繹먮끏????????????볥뜪 !";

                alert_confirm.setMessage(text + "\n" + text3);

                // ?濡ろ뜑??????ш끽維??
                if (!"null".equals(rr_comp) && clearList.size() < 1) {
                    text1 = comp_name;
                    alert_confirm.setMessage( text +"\n" + text1+"??癲ル슔?蹂앸듋???筌뚯삩???????n" + text3);
                }

                // ?濡ろ뜑????? 雅?퍔瑗띰㎖????ш끽維??
                if ( "null".equals(rr_comp) && clearList.size() >= 1 ){
                    for ( int l = 0; l < clearList.size(); l++){
                        text2 += clearList.get(l).getM_name() + " 雅?퍔瑗띰㎖???濚밸Þ?볠쾮?n";
                    }
                    alert_confirm.setMessage( text + "\n" + text2 + "\n" + text3);
                }

                // 雅?퍔瑗띰㎖?? ?濡ろ뜑??????ш끽維??
                if ( !"null".equals(rr_comp) || clearList.size() >= 1){
                    text1 = comp_name;
                    for ( int l = 0; l < clearList.size(); l++){
                        text2 += clearList.get(l).getM_name() + " 雅?퍔瑗띰㎖???濚밸Þ?볠쾮?n";
                    }
                    alert_confirm.setMessage( text +"\n" + text1+"??癲ル슔?蹂앸듋???筌뚯삩???????n" + text2 + "\n" + text3);
                }
                // ?嶺뚮Ĳ?됮??類?????域밸Ŧ遊얕짆??
               /* alert_confirm.setPositiveButton("?嶺뚮Ĳ?됮?, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent2 = new Intent(FollowEndActivity.this, SubActivity.class);
                        startActivity(intent2);
                        finish();
                    }
                });*/
                // ???源낇꼧???⑥?????獄쏅똻??
                AlertDialog alert = alert_confirm.create();
                // ???源낇꼧???⑥????????
                alert.setTitle("\uB77C\uC774\uB529 \uC800\uC7A5 \uC644\uB8CC");
                // ???源낇꼧???⑥????怨뚮옖??逾?
                alert.show();

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        confirmDiscardRide();
    }

    private void confirmDiscardRide() {
        new AlertDialog.Builder(this)
                .setTitle("저장하지 않고 나갈까요?")
                .setMessage("이번 주행 기록은 저장되지 않습니다.")
                .setNegativeButton("계속 작성", null)
                .setPositiveButton("나가기", (dialog, which) -> {
                    RideSessionStore.clear(FollowEndActivity.this);
                    RideRouteCache.clear(FollowEndActivity.this);
                    finish();
                })
                .show();
    }

    public class updateMissionStatus extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected synchronized String doInBackground( Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/updateMissionStatus/");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {
        }
    }

    public class insertMissionCom extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected synchronized String doInBackground( Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/insertMissionCom/");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {

        }
    }

}
