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
import com.bike.trailgo.R;
import com.bike.trailgo.dto.Mission;
import com.bike.trailgo.net.Server;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.MissionHolder> {
    public Context mContext;
    public List<Mission> itemList;
    Mission item;
    String Save_Path;
    int count = 0;
    public NavController controller;
    public MainViewModel model;

    public MissionAdapter(Context mContext, List<Mission> itemList, String Save_Path, NavController controller, MainViewModel model) {
        this.mContext = mContext;
        this.Save_Path = Save_Path;
        this.itemList = itemList;
        this.controller = controller;
        this.model = model;
    }

    @NonNull
    @Override
    public MissionHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_missionitem , viewGroup,false);

        return new MissionHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionHolder holder, int position) {

        Picasso.get().load(Server.getUrl("app/getGPX/mission/" + itemList.get(position).getBg_image()))
                .into(holder.imageView);

        holder.textView1.setText(itemList.get(position).getM_name());

        holder.mView.setOnClickListener( v -> {
            controller.navigate(R.id.action_nav_mission_view_to_missionDetailFragment);
            model.m_image.setValue(itemList.get(position).getBg_image());
            model.m_content.setValue(itemList.get(position).getM_content());
            model.m_name.setValue(itemList.get(position).getM_name());
            model.m_type.setValue(String.valueOf(itemList.get(position).getM_type()));
            model.m_ms_score.setValue(String.valueOf(itemList.get(position).getMs_score()));
            model.m_num.setValue(String.valueOf(itemList.get(position).getM_num()));
            model.m_bg_name.setValue(itemList.get(position).getBg_name());
        });


    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class MissionHolder extends RecyclerView.ViewHolder {
        public TextView textView1;
        public ImageView imageView;
        public LinearLayout viewClick;
        public final View mView;

        public MissionHolder( View itemView) {
            super(itemView);
            mView = itemView;
            textView1 = itemView.findViewById(R.id.textView11);
            imageView = itemView.findViewById(R.id.imageView7);
            viewClick = itemView.findViewById(R.id.viewClick);
        }
    }

}
