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
import com.bike.trailgo.dto.CompScore;
import com.bike.trailgo.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CompScoreAdapter extends RecyclerView.Adapter<CompScoreAdapter.CompClubHolder> {
    public Context mContext;
    public ArrayList<CompScore> itemList;
    String Save_Path;
    public NavController controller;
    public MainViewModel model;
    public CompScoreAdapter(Context mContext, ArrayList<CompScore> itemList, String save_path, NavController controller, MainViewModel model) {
        this.mContext = mContext;
        this.itemList = itemList;
        this.Save_Path = save_path;
        this.controller = controller;
        this.model = model;
    }

    @NonNull
    @Override
    public CompClubHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_competitionclubitem , viewGroup,false);

        return new CompClubHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CompClubHolder holder, int position) {

        String name = itemList.get(position).getR_nickname();
        int score = itemList.get(position).getRr_avgspeed();
        String mem_image = itemList.get(position).getR_image();

        Picasso.get().load("/app/getGPX/rider/" + mem_image)
                .into(holder.imageView);

        //holder.imageView.setImageBitmap(myBitmap);
        holder.textView1.setText((position+1) + "등");
        holder.textView2.setText(name);
        holder.textView3.setText(String.valueOf(score));

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class CompClubHolder extends RecyclerView.ViewHolder {
        public TextView textView1, textView2, textView3;
        public ImageView imageView;
        public LinearLayout viewClick;
        public final View mView;

        public CompClubHolder( View itemView) {
            super(itemView);
            mView = itemView;
            textView1 = itemView.findViewById(R.id.textView10);
            textView2 = itemView.findViewById(R.id.textView11);
            textView3 = itemView.findViewById(R.id.textView12);
            imageView = itemView.findViewById(R.id.imageView7);
            viewClick = itemView.findViewById(R.id.viewClick);
        }
    }
}