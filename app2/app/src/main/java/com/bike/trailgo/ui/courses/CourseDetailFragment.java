package com.bike.trailgo.ui.courses;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.CourseReview;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.gpx.GPXParser;
import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.gpx.Track;
import com.bike.trailgo.gpx.TrackPoint;
import com.bike.trailgo.gpx.TrackSegment;
import com.bike.trailgo.net.HttpClient;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.riding.FollowStart;
import com.squareup.picasso.Picasso;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseDetailFragment extends BaseFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private String rr_num, LoginId, change_Name="", register_Name="";
    private int rr_open;
    private GPXParser mParser = new GPXParser();
    private Gpx parsedGpx = null;
    private MapView mapView;
    private MapPolyline polyline = new MapPolyline();
    private ImageView userImage, modify_image, register_image;
    private Button share_bt, follow_bt, scrap_bt, register_button;
    List<Track> tracks;
    private TextView RidingTime, RidingRest, RidingDistance, RidingMax, RidingAvg, RidingGet, RidingAddr, Course_name, CourseRiderName, CourseDate, LikeCnt;
    private boolean register = false;
    private Uri temp;
    private CourseReviewAdapter adapter3;
    private String Save_Path;
    private RecyclerView recycleView;
    private FrameLayout frameLayout;
    private EditText editText;

    private Call<List<CourseReview>> getReviewList;
    private Call<Void> upload, reviewUpdate, deleteReview, insertReview;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model.courseName.observe(getViewLifecycleOwner(), name->{
            if ( name != null ) {
                AppCompatActivity appCompatActivity = (AppCompatActivity) requireActivity();
                appCompatActivity.getSupportActionBar().setTitle(name);
            }
        });

        mapView = new MapView(requireActivity());
        ViewGroup mapViewContainer = (ViewGroup) view.findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        //폴리라인 그리자~
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(255, 255, 51, 0)); // Polyline 컬러 지정.

        /* 로그인 정보 가져오기 */
        LoginId = model.r_Id.getValue();
        rr_num = model.r_num.getValue();
        share_bt = (Button) view.findViewById(R.id.share_bt);
        RidingTime = (TextView) view.findViewById(R.id.Riding_time);
        RidingRest = (TextView) view.findViewById(R.id.Riding_rest);
        RidingDistance = (TextView) view.findViewById(R.id.Riding_distance);
        RidingMax = (TextView) view.findViewById(R.id.Riding_max);
        RidingAvg = (TextView) view.findViewById(R.id.Riding_avg);
        RidingGet = (TextView) view.findViewById(R.id.Riding_godo);
        RidingAddr = (TextView) view.findViewById(R.id.Riding_addr);
        Course_name = (TextView) view.findViewById(R.id.Course_name);
        CourseRiderName = (TextView) view.findViewById(R.id.CourseRiderName);
        CourseDate = (TextView) view.findViewById(R.id.CourseDate);
        LikeCnt = (TextView) view.findViewById(R.id.LikeCnt);
        follow_bt = (Button) view.findViewById(R.id.follow_bt);
        scrap_bt = (Button) view.findViewById(R.id.scrap_bt);
        userImage = (ImageView) view.findViewById(R.id.imageView5);
        register_image = (ImageView) view.findViewById(R.id.register_image);
        recycleView = (RecyclerView) view.findViewById(R.id.recycle_review);
        frameLayout = (FrameLayout) view.findViewById(R.id.frame_layout);
        register_button = (Button) view.findViewById(R.id.register_button);
        editText = (EditText)view.findViewById(R.id.register_edit);

        RidingTime.setText(model.my_rec_time.getValue());
        RidingRest.setText(model.my_rec_rest.getValue());
        RidingDistance.setText(model.my_rec_dis.getValue());
        RidingMax.setText(model.my_rec_max.getValue());
        RidingAvg.setText(model.my_rec_avg.getValue());
        RidingGet.setText(model.my_rec_get.getValue());
        RidingAddr.setText(model.my_rec_adress.getValue());
        Course_name.setText(model.my_rec_name.getValue());
        CourseRiderName.setText(model.CourseRider.getValue());
        CourseDate.setText(model.my_rec_date.getValue());
        LikeCnt.setText(String.valueOf(model.like_count.getValue()));
        Picasso.get().load(Server.getUrl("app/getGPX/rider/" + model.CourseR_image.getValue()))
                .into(userImage);

        Save_Path = requireContext().getFilesDir().getPath();

        Log.e("이미지", model.CourseR_image.getValue());


        Thread uThread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = Server.openConnection("app/getGPX/gpx/" + model.my_rec_gpx.getValue());
                    conn.setDoInput(true); //Server 통신에서 입력 가능한 상태로 만듦
                    conn.connect(); //연결된 곳에 접속할 때 (connect() 호출해야 실제 통신 가능함)
                    InputStream is = conn.getInputStream(); //inputStream 값 가져오기
                    // InputStream in = getAssets().open("and.gpx");
                    parsedGpx = mParser.parse(is);

                    if (parsedGpx != null) {
                        // log stuff
                        tracks = parsedGpx.getTracks();
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

                        // Polyline 지도에 올리기.
                        mapView.addPolyline(polyline);

                        // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
                        MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
                        int padding = 100; // px
                        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));

                    } else {
                        Log.e("error","Error parsing gpx track!");
                    }
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        };
        uThread.start(); // 작업 Thread 실행

        getReviewList = serverApi.getReviewList(model.r_num.getValue());
        getReviewList.enqueue(new Callback<List<CourseReview>>() {
            @Override
            public void onResponse(Call<List<CourseReview>> call, Response<List<CourseReview>> response) {
                if ( response.code() == 200 ) {
                    List<CourseReview> body = response.body();
                    Log.e("통신", "성공");

                    adapter3 = new CourseReviewAdapter(requireActivity(), requireContext(), (ArrayList)body, Save_Path);
                    recycleView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

                    if (body.size() < 1) {
                        recycleView.setVisibility(View.GONE);

                        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
                        param.setMargins(0, 0, 0, 200);
                        frameLayout.setLayoutParams(param);
                        frameLayout.requestLayout();
                    }
                    recycleView.setAdapter(adapter3);
                    recycleView.requestLayout();
                }
            }

            @Override
            public void onFailure(Call<List<CourseReview>> call, Throwable t) {
                showServerFailure();
            }
        });

        //공유버튼
        share_bt.setOnClickListener(v -> {
            Intent intent1 = new Intent();
            intent1.setAction(Intent.ACTION_SEND);
            intent1.setType("text/plain");
            intent1.putExtra(Intent.EXTRA_SUBJECT, model.r_Nickname.getValue()+"님이"+model.my_rec_name.getValue()+"코스를 공유하였습니다. 해당URL을 누르면 해당 페이지로 이동합니다. ");
            intent1.putExtra(Intent.EXTRA_TEXT, Server.getUrl("app/riding/course_share/" + rr_num));

            Intent chooser = Intent.createChooser(intent1, "친구에게 공유하기");
            startActivity(chooser);

        });

        //스크랩 보관함
        scrap_bt.setOnClickListener(v -> {
            if (!AccountSession.requireMember(requireActivity(), "코스 스크랩")) {
                return;
            }
            Log.d("스크랩",rr_num+" "+LoginId);
            ScrapTask scrap = new ScrapTask();
            Map<String, String> params = new HashMap<String, String>();
            params.put("c_num", rr_num);
            params.put("ss_rider", LoginId);
            scrap.execute(params);
            Log.d("따라가기", rr_num+ " "+ LoginId);

        });

        //따라가기
        follow_bt.setOnClickListener(v->{
            Intent intent2=new Intent(requireContext(), FollowStart.class);
            intent2.putExtra("gpx",model.my_rec_gpx.getValue());
            intent2.putExtra("c_name",model.my_rec_name.getValue());
            startActivity(intent2);

            mapViewContainer.removeAllViews();
        });

        register_image.setOnClickListener(v -> {
            if (!AccountSession.requireMember(requireActivity(), "리뷰 작성")) {
                return;
            }
            register = true;
            Intent register = new Intent();
            register.setType("image/*");
            register.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(register,1);
        });

        //등록 버튼 클릭 시
        register_button.setOnClickListener(v -> {
            if (!AccountSession.requireMember(requireActivity(), "리뷰 작성")) {
                return;
            }
            HashMap<String, Object> params = new HashMap<String, Object>();

            params.put("cr_rider", LoginId);
            params.put("cr_rnum", String.valueOf(model.r_num.getValue()));
            params.put("cr_content", String.valueOf(editText.getText()));
            if ( register_Name == null )
                register_Name = "";
            params.put("cr_image", register_Name);

            insertReview = serverApi.insertReview(params);
            insertReview.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if ( register_Name.equals("")){
                        getReviewList = serverApi.getReviewList(model.r_num.getValue());
                        getReviewList.enqueue(new Callback<List<CourseReview>>() {
                            @Override
                            public void onResponse(Call<List<CourseReview>> call, Response<List<CourseReview>> response) {
                                if ( response.code() == 200 ) {
                                    List<CourseReview> body = response.body();

                                    adapter3 = new CourseReviewAdapter(requireActivity(), requireContext(), (ArrayList)body, Save_Path);
                                    recycleView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

                                    if (body.size() < 1) {
                                        recycleView.setVisibility(View.GONE);

                                        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
                                        param.setMargins(0, 0, 0, 200);
                                        frameLayout.setLayoutParams(param);
                                        frameLayout.requestLayout();
                                    }
                                    else
                                        recycleView.setVisibility(View.VISIBLE);
                                    recycleView.setAdapter(adapter3);
                                    recycleView.requestLayout();
                                }
                            }

                            @Override
                            public void onFailure(Call<List<CourseReview>> call, Throwable t) {
                                showServerFailure();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {

                }
            });

            Log.e("레지스터 이미지", register_Name);
            if ( register_Name.equals(""))
                temp = null;
            // 수정한 파일 서버 업로드, 내부저장소에 다운, 이미지뷰에 띄워줌
            try {
                if ( temp != null ) {
                    ParcelFileDescriptor pf = requireActivity().getContentResolver().openFileDescriptor(temp, "r", null);
                    if (pf != null) {
                        InputStream is = new FileInputStream(pf.getFileDescriptor());
                        File file = new File(Save_Path + "/" + register_Name);
                        try (OutputStream os = new FileOutputStream(file)) {
                            Log.e("네임", register_Name);
                            IOUtils.copy(is, os);
                            MultipartBody.Part part = Server.getFilePart(file.getPath(), register_Name);
                            upload = Server.getInstance().getApi().upload(part, "review", register_Name);
                            upload.enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    Log.e("성공", String.valueOf(response.code()));

                                    getReviewList = serverApi.getReviewList(model.r_num.getValue());
                                    getReviewList.enqueue(new Callback<List<CourseReview>>() {
                                        @Override
                                        public void onResponse(Call<List<CourseReview>> call, Response<List<CourseReview>> response) {
                                            if ( response.code() == 200 ) {
                                                List<CourseReview> body = response.body();

                                                adapter3 = new CourseReviewAdapter(requireActivity(), requireContext(), (ArrayList)body, Save_Path);
                                                recycleView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

                                                if (body.size() < 1) {
                                                    recycleView.setVisibility(View.GONE);

                                                    FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
                                                    param.setMargins(0, 0, 0, 200);
                                                    frameLayout.setLayoutParams(param);
                                                    frameLayout.requestLayout();
                                                }
                                                else
                                                    recycleView.setVisibility(View.VISIBLE);
                                                recycleView.setAdapter(adapter3);
                                                recycleView.requestLayout();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<List<CourseReview>> call, Throwable t) {
                                            showServerFailure();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Log.e("에러", t.toString());
                                }
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            editText.setText("");
            register_image.setImageResource(R.drawable.image_plus);
            register_Name = "";

        });

    }

    public class ScrapTask extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http 요청 준비 작업
            //URL은 현재 자기 아이피번호를 입력해야합니다.
            HttpClient.Builder http = new HttpClient.Builder("POST", "/app/riding/coursescrap");
            // Parameter 를 전송한다.
            http.addAllParameters(maps[0]);
            //Http 요청 전송
            HttpClient post = http.create();
            post.request();

            // 응답 상태코드 가져오기
            int statusCode = post.getHttpStatusCode();

            // 응답 본문 가져오기
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                Toast.makeText(requireContext(), "스크랩 보관함에 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "저장에 실패하였습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
                if (register == false) {
                    try {
                        // 선택한 이미지에서 비트맵 생성
                        if ( data.getData() != null ) {
                            InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                            temp = data.getData();
                            Bitmap img = BitmapFactory.decodeStream(in);
                            in.close();
                            modify_image.setImageBitmap(img);
                            change_Name = getImageNameToUri(data.getData());
                        }

                        // 이미지 표시
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        // 선택한 이미지에서 비트맵 생성
                        InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                        temp = data.getData();
                        Bitmap img = BitmapFactory.decodeStream(in);
                        in.close();
                        register_image.setImageBitmap(img);
                        register_Name = getImageNameToUri(data.getData());

                        // 이미지 표시
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    public String getImageNameToUri(Uri data) {
        String[] proj = {MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = requireContext().getContentResolver().query(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
        cursor.moveToFirst();
        String imgPath = cursor.getString(column_index);
        String imgName = imgPath.substring(imgPath.lastIndexOf("/") + 1);

        return imgName;
    }

    // 코스 리뷰 어댑터 - 사진 불러와서 setImageBitmap 떄문에 여기다 어댑터 붙임
    public class CourseReviewAdapter extends RecyclerView.Adapter<CourseReviewAdapter.CourseReviewHolder>{
        public Activity activity;
        public Context mContext;
        public ArrayList<CourseReview> itemList;
        String Save_Path;

        public CourseReviewAdapter(Activity activity, Context mContext, ArrayList<CourseReview> itemList, String save_path) {
            this.activity = activity;
            this.mContext = mContext;
            this.itemList = itemList;
            this.Save_Path = save_path;
        }

        @NonNull
        @Override
        public CourseReviewAdapter.CourseReviewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_coursereviewitem , viewGroup,false);

            return new CourseReviewAdapter.CourseReviewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CourseReviewAdapter.CourseReviewHolder holder, int position) {

            if (itemList.get(position).getR_image() != null) {
                Picasso.get().load(Server.getUrl("app/getGPX/rider/" + itemList.get(position).getR_image()))
                        .into(holder.imageView1);
            }
            else {
                Picasso.get().load(Server.getUrl("app/getGPX/rider/noImage.jpg"))
                        .into(holder.imageView1);
            }

            if (itemList.get(position).getCr_images() != null) {
                Picasso.get().load(Server.getUrl("app/getGPX/review/" + itemList.get(position).getCr_images()))
                        .into(holder.imageView2);
            }



            SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd");
            String temp2 = sdfNow.format(itemList.get(position).getCr_time());

            //holder.imageView.setImageBitmap(myBitmap);
            holder.textView1.setText(itemList.get(position).getR_nickname());
            holder.textView2.setText(temp2);
            holder.textView3.setText(itemList.get(position).getCr_content());


            if ( itemList.get(position).getCr_images() == null)
                holder.imageView2.setVisibility(View.GONE);

            //수정 버튼 클릭 시
            holder.button1.setOnClickListener( v -> {
                View view1 = v;
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                Bitmap myBitmap2;

                if ( itemList.get(position).getCr_images() != null)
                    myBitmap2 = BitmapFactory.decodeFile(new File(Save_Path + "/" + itemList.get(position).getCr_images()).getAbsolutePath());
                else
                    myBitmap2 = null;

                View view = LayoutInflater.from(activity)
                        .inflate(R.layout.activity_coursereviewmodify, null, false);
                builder.setView(view);

                final AlertDialog dialog = builder.create();

                modify_image = view.findViewById(R.id.modify_image);
                Button button3 = view.findViewById(R.id.button);
                Button button4 = view.findViewById(R.id.cr_cancel);
                EditText editText2 = view.findViewById(R.id.editText);

                editText2.setText(itemList.get(position).getCr_content());
                modify_image.setImageBitmap(myBitmap2);
                if ( itemList.get(position).getCr_images() == null)
                    modify_image.setImageResource(R.drawable.image_plus);

                // 수정에서 이미지 클릭 시
                modify_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        register = false;
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent,1);
                    }

                });

                // 수정에서 확인 버튼 클릭 시
                button3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if ( change_Name.equals("")){
                            change_Name = itemList.get(position).getCr_images();
                            holder.imageView2.setImageBitmap(BitmapFactory.decodeFile(String.valueOf(new File(Save_Path + "/" + itemList.get(position).getCr_images()))));
                        }
                        long now = System.currentTimeMillis();
                        Date date = new Date(now);

                        itemList.get(position).setCr_images(change_Name);
                        itemList.get(position).setCr_content(String.valueOf(editText2.getText()));
                        itemList.get(position).setCr_time(date);

                        notifyItemChanged(position);

                        // 수정한 파일 서버 업로드, 내부저장소에 다운, 이미지뷰에 띄워줌
                        try {
                            ParcelFileDescriptor pf = requireActivity().getContentResolver().openFileDescriptor(temp, "r", null);
                            if (pf != null) {
                                InputStream is = new FileInputStream(pf.getFileDescriptor());
                                File file = new File(Save_Path + "/" + change_Name);
                                try (OutputStream os = new FileOutputStream(file)) {
                                    Log.e("네임", change_Name);
                                    IOUtils.copy(is, os);
                                    MultipartBody.Part part = Server.getFilePart(file.getPath(), change_Name);
                                    upload = Server.getInstance().getApi().upload(part, "review", change_Name);
                                    upload.enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> call, Response<Void> response) {
                                            Log.e("성공",String.valueOf(response.code()));
                                            Picasso.get().load(Server.getUrl("app/getGPX/review/" + change_Name))
                                                    .into(holder.imageView2);
                                        }
                                        @Override
                                        public void onFailure(Call<Void> call, Throwable t) {
                                            Log.e("에러", t.toString());
                                        }
                                    });

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }


                        HashMap<String, Object> params = new HashMap<String, Object>();

                        params.put("cr_num", String.valueOf(itemList.get(position).getCr_num()));
                        params.put("cr_content", itemList.get(position).getCr_content());
                        params.put("cr_image", itemList.get(position).getCr_images());
                        params.put("cr_time", String.valueOf(itemList.get(position).getCr_time()));

                        reviewUpdate = serverApi.reviewUpdate(params);
                        reviewUpdate.enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {

                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {

                            }
                        });

                        change_Name = "";

                        dialog.dismiss();
                    }
                });

                button4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            });

            holder.cr_delete.setOnClickListener( v -> {
                HashMap<String, Object> params = new HashMap<String, Object>();

                params.put("cr_num", String.valueOf(itemList.get(position).getCr_num()));

                deleteReview = serverApi.deleteReview(params);
                deleteReview.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        getReviewList = serverApi.getReviewList(model.r_num.getValue());
                        getReviewList.enqueue(new Callback<List<CourseReview>>() {
                            @Override
                            public void onResponse(Call<List<CourseReview>> call, Response<List<CourseReview>> response) {
                                if ( response.code() == 200 ) {
                                    List<CourseReview> body = response.body();

                                    adapter3 = new CourseReviewAdapter(requireActivity(), requireContext(), (ArrayList)body, Save_Path);
                                    recycleView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

                                    if (body.size() < 1) {
                                        recycleView.setVisibility(View.GONE);

                                        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
                                        param.setMargins(0, 0, 0, 200);
                                        frameLayout.setLayoutParams(param);
                                        frameLayout.requestLayout();
                                    }
                                    recycleView.setAdapter(adapter3);
                                    recycleView.requestLayout();
                                }
                            }

                            @Override
                            public void onFailure(Call<List<CourseReview>> call, Throwable t) {
                                showServerFailure();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });



            });
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        class CourseReviewHolder extends RecyclerView.ViewHolder {
            public TextView textView1, textView2, textView3;
            public ImageView imageView1, imageView2;
            public LinearLayout viewClick;
            public final View mView;
            public Button button1, button2, cr_delete;

            public CourseReviewHolder( View itemView) {
                super(itemView);
                mView = itemView;
                textView1 = itemView.findViewById(R.id.comp_name);
                textView2 = itemView.findViewById(R.id.comp_day);
                button1 = itemView.findViewById(R.id.button);
                button2 = itemView.findViewById(R.id.cr_cancel);
                imageView1 = itemView.findViewById(R.id.imageView5);
                imageView2 = itemView.findViewById(R.id.modify_image);
                textView3 = itemView.findViewById(R.id.textView9);
                viewClick = itemView.findViewById(R.id.viewClick);
                cr_delete = itemView.findViewById(R.id.cr_delete);
            }
        }

    }
}
