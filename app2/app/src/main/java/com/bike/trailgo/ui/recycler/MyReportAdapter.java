package com.bike.trailgo.ui.recycler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import com.bike.trailgo.MainViewModel;
import com.bike.trailgo.R;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.ui.records.RecordFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MyReportAdapter extends RecyclerView.Adapter<MyReportAdapter.MyRecordHolder> {

    public Context mContext;
    public ArrayList<RidingRecord> itemList;
    public NavController controller;
    public MainViewModel model;

    public MyReportAdapter(Context mContext, ArrayList<RidingRecord> itemList, NavController controller, MainViewModel model) {
        this.mContext = mContext;
        this.itemList = itemList;
        this.controller = controller;
        this.model = model;
    }

    @Override
    public MyRecordHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.myreport_item, viewGroup, false);
        return new MyRecordHolder(v);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull final MyRecordHolder holder, final int position) {
        RidingRecord item = itemList.get(position);

        String totalDistance = RecordFormatter.formatDistance(item.getRr_distance());
        String ridingDuration = RecordFormatter.formatDuration(item.getRr_time());
        String breakDuration = RecordFormatter.formatDuration(item.getRr_breaktime());

        holder.recordName.setText(formatTitle(item));
        holder.recordDate.setText(formatDate(item.getRr_date()));
        holder.recordTime.setText(formatClock(item.getRr_date()));
        holder.recordAddress.setText(firstNotEmpty(item.getRr_area(), "라이딩 코스"));
        holder.recordDistance.setText(totalDistance);
        holder.recordHeight.setText(RecordFormatter.formatElevationGain(item.getRr_high()));
        holder.recordDuration.setText(ridingDuration);
        holder.recordImage.setImageResource(R.drawable.trailgo_route_preview);
        holder.weatherIcon.setImageResource(position % 3 == 0 ? R.drawable.records_ic_sun : R.drawable.records_ic_cloud);
        holder.bookmarkIcon.setImageResource(position == 1 ? R.drawable.records_ic_bookmark_filled : R.drawable.drawer_ic_bookmark_custom);

        holder.mView.setOnClickListener(v -> {
            model.r_num.setValue(String.valueOf(item.getRr_num()));
            model.my_rec_name.setValue(item.getRr_name());
            model.my_rec_date.setValue(item.getRr_date());
            model.my_rec_adress.setValue(item.getRr_area());
            model.my_rec_dis.setValue(totalDistance);
            model.my_rec_get.setValue(RecordFormatter.formatElevationGain(item.getRr_high()));
            model.my_rec_time.setValue(ridingDuration);
            model.my_rec_rest.setValue(breakDuration);
            model.my_rec_open.setValue(item.getRr_open());
            model.my_rec_max.setValue(RecordFormatter.formatSpeed(
                    item.getRr_topspeed(), item.getRr_distance(), item.getRr_time()));
            model.my_rec_avg.setValue(RecordFormatter.formatSpeed(
                    item.getRr_avgspeed(), item.getRr_distance(), item.getRr_time()));
            model.my_rec_gpx.setValue(item.getRr_gpx());
            controller.navigate(R.id.action_nav_records_to_myDetailFragment);
        });
    }

    private String formatTitle(RidingRecord item) {
        String name = firstNotEmpty(item.getRr_name(), "");
        if (name.length() > 0) {
            return name;
        }

        String area = firstNotEmpty(item.getRr_area(), "");
        if (area.length() > 0) {
            return area + " 라이딩";
        }

        return "라이딩 기록";
    }

    private String formatDate(String value) {
        Date date = parseDate(value);
        if (date != null) {
            return new SimpleDateFormat("yyyy. MM. dd (E)", Locale.KOREA).format(date);
        }
        return value == null ? "-" : value;
    }

    private String formatClock(String value) {
        Date date = parseDate(value);
        if (date != null) {
            return new SimpleDateFormat("a h:mm", Locale.KOREA).format(date);
        }
        if (value != null && value.length() >= 16) {
            return value.substring(11, 16);
        }
        return "-";
    }

    private Date parseDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }

        String normalized = value.length() >= 19 ? value.substring(0, 19) : value;
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.KOREA).parse(normalized);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private String firstNotEmpty(String... values) {
        for (String value : values) {
            if (value != null && value.trim().length() > 0) {
                return value.trim();
            }
        }
        return "";
    }

    class MyRecordHolder extends RecyclerView.ViewHolder {
        public TextView recordName, recordDate, recordTime, recordAddress, recordDistance, recordHeight, recordDuration;
        public ImageView recordImage, weatherIcon, bookmarkIcon;
        public View viewClick;
        public final View mView;

        public MyRecordHolder(View itemView) {
            super(itemView);
            mView = itemView;
            recordName = itemView.findViewById(R.id.record_name);
            recordDate = itemView.findViewById(R.id.record_date);
            recordTime = itemView.findViewById(R.id.record_time);
            recordAddress = itemView.findViewById(R.id.record_adress);
            recordDistance = itemView.findViewById(R.id.my_dis);
            recordHeight = itemView.findViewById(R.id.my_get);
            recordDuration = itemView.findViewById(R.id.my_time);
            recordImage = itemView.findViewById(R.id.myimage);
            weatherIcon = itemView.findViewById(R.id.record_weather_icon);
            bookmarkIcon = itemView.findViewById(R.id.record_bookmark_icon);
            viewClick = itemView.findViewById(R.id.viewClick);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
