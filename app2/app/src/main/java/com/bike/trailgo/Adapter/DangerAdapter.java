package com.bike.trailgo.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.DangerousArea;

import java.util.ArrayList;


public class DangerAdapter extends RecyclerView.Adapter<DangerAdapter.DangerHolder> {
    public Context mContext;
    public ArrayList<DangerousArea> itemList;
    private final OnLocateListener onLocate;

    /** Asked to show a report's position on whatever map the screen uses. */
    public interface OnLocateListener {
        void onLocate(double latitude, double longitude);
    }

    public DangerAdapter(Context mContext, ArrayList<DangerousArea> itemList, OnLocateListener onLocate) {
        this.mContext = mContext;
        this.itemList = itemList;
        this.onLocate = onLocate;
    }

    @NonNull
    @Override
    public DangerHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_dangeritem , viewGroup,false);

        return new DangerHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DangerHolder holder, int position) {
        String status ="";

        holder.mView.setOnClickListener( v -> {
            try {
                DangerousArea item = itemList.get(position);
                onLocate.onLocate(Double.parseDouble(item.getDa_latitude()), Double.parseDouble(item.getDa_longitude()));
            } catch (RuntimeException ignored) {
                // a report with unreadable coordinates just does not move the map
            }
        });

        //holder.imageView.setImageBitmap(myBitmap);
        holder.textView1.setText(itemList.get(position).getDa_addr());
        holder.textView2.setText(itemList.get(position).getDa_content());
        if (itemList.get(position).getDa_status().equals("0"))
            status = "신청중";
        if (itemList.get(position).getDa_status().equals("1"))
            status = "수락";
        if (itemList.get(position).getDa_status().equals("2"))
            status = "거절";
        holder.textView3.setText(status);

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class DangerHolder extends RecyclerView.ViewHolder {
        public TextView textView1, textView2, textView3;
        public LinearLayout viewClick;
        public final View mView;

        public DangerHolder( View itemView) {
            super(itemView);
            mView = itemView;
            textView1 = itemView.findViewById(R.id.textView32);
            textView2 = itemView.findViewById(R.id.textView34);
            textView3 = itemView.findViewById(R.id.textView35);
            viewClick = itemView.findViewById(R.id.viewClick);
        }
    }
}