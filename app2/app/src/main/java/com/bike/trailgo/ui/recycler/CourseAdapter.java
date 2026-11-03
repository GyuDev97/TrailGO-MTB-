package com.bike.trailgo.ui.recycler;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import com.bike.trailgo.MainViewModel;
import com.bike.trailgo.R;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.net.Server;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseHolder> implements OnMapReadyCallback {

    public Context mContext;
    public ArrayList<RidingRecord> itemList;
    public int count = 5;
    private String r_image;
    private NavController controller;
    public MainViewModel model;


    public CourseAdapter(Context mContext, ArrayList<RidingRecord> itemList, String r_image, NavController controller, MainViewModel model) {
        this.mContext = mContext;
        this.itemList = itemList;
        this.r_image = r_image;
        this.controller = controller;
        this.model = model;
    }

    @NonNull
    @Override
    public CourseHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        int layout = i == 1 ? R.layout.course_feature_item : R.layout.course_item;
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup,false);

        return new CourseHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final CourseHolder testViewHolder, final int position) {


        int hour;
        int min;
        int sec;
        sec = (itemList.get(position).getRr_time());

        min = sec/60;
        hour = min/60;
        sec = sec % 60;
        min = min % 60;
        if(hour == 0){
            hour=00;
        }
        if(min==0){
            min=00;
        }

        //휴식시간 계산
        int b_hour, b_min, b_sec = itemList.get(position).getRr_breaktime();
        b_min = b_sec/60; b_hour = b_min/60; b_sec = b_sec % 60; b_min = b_min % 60;

        int des = (itemList.get(position).getRr_distance());
        String hour_s = String.valueOf(hour);
        String total = String.format(Locale.KOREA, "%.2f km", des / 1000.0);
        String ridingTime = String.format(Locale.KOREA, "%d:%02d:%02d", hour, min, sec);

        Picasso.get().load(Server.getRiderImageUrl(itemList.get(position).getR_image()))
                .into(testViewHolder.imageView);

        String rawDate = itemList.get(position).getRr_date();
        String safeDate = rawDate == null ? "" : rawDate;
        String sub1 = safeDate.length() >= 10 ? safeDate.substring(0, 10) : safeDate;
        String sub2 = safeDate.length() >= 19 ? safeDate.substring(11, 19) : "";


        testViewHolder.c_rider_name.setText(itemList.get(position).getRr_rider());
        testViewHolder.c_name.setText(itemList.get(position).getRr_name());
        testViewHolder.c_time.setText(ridingTime);
        String difficulty = itemList.get(position).getRr_high() >= 800 ? "상" : "하";
        testViewHolder.c_avg.setText(position == 0 ? "난이도 " + difficulty : difficulty);
        testViewHolder.c_getgodo.setText(String.valueOf(itemList.get(position).getRr_high())+" m");
        testViewHolder.c_dis.setText(total);
        testViewHolder.c_date.setText(sub1+ " "+sub2);
        testViewHolder.like_count.setText(String.valueOf(itemList.get(position).getRr_like()));
        int finalMin = min;
        int finalSec = sec;
        int finalB_min = b_min;
        int finalB_sec = b_sec;
        testViewHolder.mView.setOnClickListener(v -> {

            controller.navigate(R.id.action_nav_course_view_to_nav_course_detail);

            model.my_rec_time.setValue(hour_s+"시간 "+ finalMin +"분 "+ finalSec + "초");
            model.my_rec_rest.setValue(b_hour+"시간 "+ finalB_min +"분 "+ finalB_sec +"초");
            model.my_rec_dis.setValue(total);
            model.my_rec_max.setValue(itemList.get(position).getRr_topspeed() +"km/h");
            model.my_rec_avg.setValue(itemList.get(position).getRr_avgspeed() +"km/h");
            model.my_rec_get.setValue(itemList.get(position).getRr_high() +"m");
            model.my_rec_adress.setValue(String.valueOf(itemList.get(position).getRr_area()));
            model.my_rec_name.setValue(String.valueOf(itemList.get(position).getRr_name()));
            model.CourseRider.setValue(String.valueOf(itemList.get(position).getRr_rider()));
            model.my_rec_date.setValue(sub1+ " "+sub2);
            model.like_count.setValue(itemList.get(position).getRr_like());
            model.my_rec_gpx.setValue(itemList.get(position).getRr_gpx());
            model.CourseR_image.setValue(itemList.get(position).getR_image());
            model.r_num.setValue(String.valueOf(itemList.get(position).getRr_num()));
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(count, itemList.size());
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 1 : 0;
    }

    class CourseHolder extends RecyclerView.ViewHolder {
        public TextView textView1, textView2, textView3, c_rider_name, c_name, c_time, c_avg, c_getgodo, c_dis, c_date, like_count;
        public ImageView imageView;
        public View viewClick;
        public final View mView;
        public ImageView courseImage;

        public CourseHolder( View itemView) {
            super(itemView);
            mView = itemView;
            textView1 = itemView.findViewById(R.id.textView10);
            textView2 = itemView.findViewById(R.id.textView11);
            textView3 = itemView.findViewById(R.id.textView12);
            imageView = itemView.findViewById(R.id.userimg);
            viewClick = itemView.findViewById(R.id.viewClick);
            courseImage = itemView.findViewById(R.id.course_view);
            c_rider_name = itemView.findViewById(R.id.c_rider_name);
            c_name = itemView.findViewById(R.id.c_name);
            c_time = itemView.findViewById(R.id.c_time);
            c_avg = itemView.findViewById(R.id.c_avg);
            c_getgodo = itemView.findViewById(R.id.c_getgodo);
            c_dis = itemView.findViewById(R.id.c_dis);
            c_date = itemView.findViewById(R.id.c_date);
            like_count = itemView.findViewById(R.id.like_count);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {



    }
}
