package com.bike.trailgo.ui.recycler;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.bike.trailgo.dto.ScrapStatus;

import java.util.ArrayList;

public class MyScrapAdapter extends RecyclerView.Adapter<MyScrapAdapter.MyRecordHolder>{

    public Context mContext;
    public ArrayList<ScrapStatus> itemList;
    String Save_Path;
    public NavController controller;
    public MainViewModel model;


    public MyScrapAdapter(Context mContext, ArrayList<ScrapStatus> itemList, NavController controller, MainViewModel model) {
        this.mContext = mContext;
        this.itemList = itemList;
        this.controller = controller;
        this.model = model;
    }

    @Override
    public MyRecordHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.scrap_item , viewGroup,false);

        return new MyRecordHolder(v);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull final MyRecordHolder testViewHolder, final int position) {
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

        String hour_s = String.valueOf(hour);
        int des = (itemList.get(position).getRr_distance());
        float km = (float) (des/1000.0);
        String total = km +"Km";
        testViewHolder.c_rider_name.setText(itemList.get(position).getRr_rider());
        testViewHolder.c_name.setText(String.valueOf(itemList.get(position).getRr_name()));
        testViewHolder.c_time.setText(hour_s+":"+min+":"+ sec);
        testViewHolder.c_date.setText(itemList.get(position).getRr_date());
        testViewHolder.c_avg.setText(String.valueOf(itemList.get(position).getRr_avgspeed())+"km");
        testViewHolder.c_getgodo.setText(String.valueOf(itemList.get(position).getRr_high())+"m");
        testViewHolder.c_distance.setText(total);

        testViewHolder.mView.setOnClickListener(v -> {
           // controller.navigate(R.id.action_nav_records_to_myDetailFragment);
        });
    }


    class MyRecordHolder extends RecyclerView.ViewHolder {
        public TextView c_rider_name, c_name, c_time, c_avg, c_getgodo, c_distance;
        public ImageView my_image;
        public LinearLayout viewClick;
        public final View mView;
        public TextView c_date;

        public MyRecordHolder(View itemView) {
            super(itemView);
            mView = itemView;
            c_rider_name = itemView.findViewById(R.id.c_rider_name);
            c_name = itemView.findViewById(R.id.c_name);
            c_time = itemView.findViewById(R.id.c_time);
            c_avg = itemView.findViewById(R.id.c_avg);
            c_getgodo = itemView.findViewById(R.id.c_getgodo);
            c_distance = itemView.findViewById(R.id.c_dis);
            c_date = itemView.findViewById(R.id.c_date);
            my_image = itemView.findViewById(R.id.myimage);
            viewClick = itemView.findViewById(R.id.viewClick);

        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}