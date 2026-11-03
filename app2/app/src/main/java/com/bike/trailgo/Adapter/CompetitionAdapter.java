package com.bike.trailgo.Adapter;

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
import com.bike.trailgo.dto.Competition;
import com.bike.trailgo.R;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CompetitionAdapter extends RecyclerView.Adapter<CompetitionAdapter.CompetitionHolder> {
    public Context mContext;
    public ArrayList<Competition> itemList;
    String Save_Path;
    public NavController controller;
    public MainViewModel model;

    public CompetitionAdapter(Context mContext, ArrayList<Competition> itemList, String Save_Path, NavController controller, MainViewModel model) {
        this.mContext = mContext;
        this.Save_Path = Save_Path;
        this.itemList = itemList;
        this.controller = controller;
        this.model = model;
    }

    @NonNull
    @Override
    public CompetitionHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_competitionitem , viewGroup,false);

        return new CompetitionHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CompetitionHolder holder, int position) {
        String fullDay = itemList.get(position).getComp_period();
        String startDay = fullDay.substring(0,4) + "-" + fullDay.substring(4,6) + "-" + fullDay.substring(6,8);
        String endDay = fullDay.substring(8,12) + "-" + fullDay.substring(12,14) + "-" + fullDay.substring(14,16);
        String day = startDay + " ~ " + endDay;

        long howDay = 0;
        try {
            howDay = getDate(fullDay);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if ( howDay < 0)
            holder.textView2.setText("D - " + Math.abs(howDay));
        else
            holder.textView2.setText(day);


        holder.textView1.setText(itemList.get(position).getComp_name());

        Picasso.get().load("/app/getGPX/comp/" + itemList.get(position).getComp_image())
                .into(holder.imageView);

        holder.mView.setOnClickListener( v -> {

            controller.navigate(R.id.action_nav_competition_view_to_competitionDetailFragment);

            model.comp_num.setValue(String.valueOf(itemList.get(position).getComp_num()));
            model.comp_period.setValue(day);
            model.comp_badge.setValue(String.valueOf(itemList.get(position).getComp_badge()));
            model.comp_course.setValue(String.valueOf(itemList.get(position).getComp_course()));
            model.comp_image.setValue(itemList.get(position).getComp_image());
            model.comp_content.setValue(itemList.get(position).getComp_content());
            model.comp_name.setValue(itemList.get(position).getComp_name());
            model.save_path.setValue(Save_Path);
            model.c_gpx.setValue(itemList.get(position).getC_gpx());
            model.c_name.setValue(itemList.get(position).getC_name());
            model.comp_point.setValue(String.valueOf(itemList.get(position).getComp_point()));

        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class CompetitionHolder extends RecyclerView.ViewHolder {
        public TextView textView1, textView2;
        public ImageView imageView;
        public LinearLayout viewClick;
        public final View mView;

        public CompetitionHolder( View itemView) {
            super(itemView);
            mView = itemView;
            textView1 = itemView.findViewById(R.id.comp_name);
            textView2 = itemView.findViewById(R.id.comp_day);
            imageView = itemView.findViewById(R.id.imageView);
            viewClick = itemView.findViewById(R.id.viewClick);
        }
    }

    public long getDate(String getPeriod) throws ParseException {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd");
        String temp = sdfNow.format(date);
        Date nowTime = sdfNow.parse(temp);
        Date periodTime = sdfNow.parse(String.valueOf(getPeriod.substring(8,16)));

/*            SimpleDateFormat sdfNow2 = new SimpleDateFormat("yyyyMMdd");
            int day1 = Integer.parseInt(sdfNow2.format(date));
            int day2 =  Integer.parseInt(getPeriod.substring(8,16));*/

        return (nowTime.getTime() - periodTime.getTime()) / (24*60*60*1000);

    }
}
