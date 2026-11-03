package com.bike.trailgo.ui.missions;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bike.trailgo.R;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.ui.BaseFragment;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionDetailFragment extends BaseFragment {
    private TextView missionName, missionContent, badgeName, missionNow, missionWhen;
    private ImageView missionImage;
    private Call<Date> getWhenCom;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        missionName = view.findViewById(R.id.comp_name2);
        missionImage = view.findViewById(R.id.imageView6);
        missionContent = view.findViewById(R.id.mission_content);
        badgeName = view.findViewById(R.id.mission_name);
        missionNow = view.findViewById(R.id.mission_now);
        missionWhen = view.findViewById(R.id.mission_when);

        missionName.setText(model.m_name.getValue());
        Picasso.get().load(Server.getUrl("app/getGPX/badge/" + model.m_image.getValue()))
                .into(missionImage);
        missionContent.setText(model.m_content.getValue());
        badgeName.setText(model.m_bg_name.getValue());

        if ( Integer.parseInt(model.m_type.getValue()) == 1)
            missionNow.setText("진행상황 " + String.valueOf(model.m_ms_score.getValue()) + "km");
        else if ( Integer.parseInt(model.m_type.getValue()) == 2)
            missionNow.setText("진행상황 " + String.valueOf(model.m_ms_score.getValue()) + "회");
        else if ( Integer.parseInt(model.m_type.getValue()) == 3)
            missionNow.setText("진행상황 " + String.valueOf(model.m_ms_score.getValue()) + "개");

        getWhenCom = serverApi.getWhenCom(model.r_Id.getValue(), model.m_num.getValue());
        getWhenCom.enqueue(new Callback<Date>() {
            @Override
            public void onResponse(Call<Date> call, Response<Date> response) {
                if ( response.code() == 200 ) {
                    Date body = response.body();

                    SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd");
                    String temp = sdfNow.format(body);
                    missionWhen.setText("획득일자 " + temp);
                    if ( Integer.parseInt(model.m_type.getValue()) == 4)
                        missionNow.setText("진행상황 획득");

                }
            }

            @Override
            public void onFailure(Call<Date> call, Throwable t) {
                if ( Integer.parseInt(model.m_type.getValue()) == 4)
                    missionNow.setText("진행상황 미획득");
                missionWhen.setText("획득일자 미획득");
            }
        });
    }
}
