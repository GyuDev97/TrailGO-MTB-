package com.bike.trailgo.ui.missions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bike.trailgo.Adapter.MissionAdapter;
import com.bike.trailgo.Adapter.MissionRankingAdapter;
import com.bike.trailgo.R;
import com.bike.trailgo.dto.Mission;
import com.bike.trailgo.dto.MissionRanking;
import com.bike.trailgo.ui.BaseFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionViewFragment extends BaseFragment {
    private ImageView userImage;
    private Bitmap userBitmap;
    private String nickname;
    private TextView userNickname, textView1, textView2, textView3, textView4;
    private Call<List<Mission>> getAllMission;
    private Call<List<String>> getCompMission;
    private Call<List<MissionRanking>> getMisRanking;
    private int allCount;
    private RecyclerView recycleView;
    private CheckBox checkBox;
    private RadioButton radioButton1, radioButton2;
    public ArrayList<Mission> allList = new ArrayList<>();
    public ArrayList<Mission> joinedList = new ArrayList<>();
    public ArrayList<MissionRanking> rankingList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allList = new ArrayList<>();
        joinedList = new ArrayList<>();
        rankingList = new ArrayList<>();

        userImage = view.findViewById(R.id.comp_mem_image);
        userBitmap = BitmapFactory.decodeFile(new File(requireContext().getFilesDir().getPath() + "/" + model.r_Image.getValue()).getAbsolutePath());
        userImage.setImageBitmap(userBitmap);

        userNickname = view.findViewById(R.id.memberId);
        textView1 = view.findViewById(R.id.textView5);
        textView2 = view.findViewById(R.id.textView6);
        textView3 = view.findViewById(R.id.textView7);
        textView4 = view.findViewById(R.id.textView8);
        recycleView = view.findViewById(R.id.recycleView1);
        checkBox = view.findViewById(R.id.checkBox2);
        radioButton1 = view.findViewById(R.id.radioButton1);
        radioButton2 = view.findViewById(R.id.radioButton2);

        nickname = model.r_Nickname.getValue();
        userNickname.setText(nickname + " 님");

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(requireContext(), 2);


        getAllMission = serverApi.getAllMission(model.r_Id.getValue());
        getAllMission.enqueue(new Callback<List<Mission>>() {
            @Override
            public void onResponse(Call<List<Mission>> call, Response<List<Mission>> response) {
                if ( response.code() == 200) {
                    List<Mission> body = response.body();

                    allList.addAll(response.body());

                    int[] typeScore = new int[3];

                    for ( int i = 0; i < body.size(); i++ ){
                        if ( body.get(i).getM_type() == 1)
                            typeScore[0] = body.get(i).getMs_score();
                        else if ( body.get(i).getM_type() == 2)
                            typeScore[1] = body.get(i).getMs_score();
                        else if ( body.get(i).getM_type() == 3)
                            typeScore[2] = body.get(i).getMs_score();
                    }

                    allCount = body.size();

                    textView1.setText("주행한 총 거리       " + typeScore[0] + "km");
                    textView2.setText("경쟁전 참여 횟수   " + typeScore[1] + "회");
                    textView3.setText("미션 완료 갯수       " + typeScore[2] + "개");

                    MissionAdapter adapter = new MissionAdapter(requireContext().getApplicationContext(), body, model.save_path.getValue(), controller, model);
                    recycleView.setLayoutManager(mLayoutManager);
                    recycleView.setAdapter(adapter);

                    getCompMission = serverApi.getCompMission(model.r_Id.getValue());
                    getCompMission.enqueue(new Callback<List<String>>() {
                        @Override
                        public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                            if ( response.code() == 200 ) {
                                List<String> body = response.body();

                                for ( int i = 0; i < body.size(); i++) {
                                    for ( int j = 0; j < allList.size(); j++) {
                                        if (body.get(i).equals(String.valueOf(allList.get(j).getM_num()))) {
                                            joinedList.add(allList.get(j));
                                        }
                                    }
                                }

                                textView4.setText("현재 진행 상황       " + body.size() + " / " + allCount);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<String>> call, Throwable t) {
                            showServerFailure();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Mission>> call, Throwable t) {
                showServerFailure();
            }
        });

        getMisRanking = serverApi.getMisRanking();
        getMisRanking.enqueue(new Callback<List<MissionRanking>>() {
            @Override
            public void onResponse(Call<List<MissionRanking>> call, Response<List<MissionRanking>> response) {
                if ( response.code() == 200 ){
                    List<MissionRanking> body = response.body();

                    rankingList = (ArrayList<MissionRanking>) body;
                }
            }

            @Override
            public void onFailure(Call<List<MissionRanking>> call, Throwable t) {
                showServerFailure();
            }
        });

        checkBox.setOnClickListener( v -> {
            if ( !radioButton2.isChecked() ) {
                if (checkBox.isChecked()) {
                    MissionAdapter adapter = new MissionAdapter(requireContext().getApplicationContext(), joinedList, model.save_path.getValue(), controller, model);
                    recycleView.setLayoutManager(mLayoutManager);
                    recycleView.setAdapter(adapter);
                } else {
                    MissionAdapter adapter = new MissionAdapter(requireContext().getApplicationContext(), allList, model.save_path.getValue(), controller, model);
                    recycleView.setLayoutManager(mLayoutManager);
                    recycleView.setAdapter(adapter);
                }
            }
        });

        radioButton2.setOnClickListener( v -> {
            MissionRankingAdapter adapter3 = new MissionRankingAdapter(requireContext().getApplicationContext(), rankingList, model.save_path.getValue(), allCount);
            recycleView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
            recycleView.setAdapter(adapter3);
        });

        radioButton1.setOnClickListener( v -> {
            MissionAdapter adapter1 = new MissionAdapter(requireContext().getApplicationContext(), joinedList, model.save_path.getValue(), controller, model);
            MissionAdapter adapter2 = new MissionAdapter(requireContext().getApplicationContext(), allList, model.save_path.getValue(), controller, model);
            if ( checkBox.isChecked()) {
                recycleView.setLayoutManager(mLayoutManager);
                recycleView.setAdapter(adapter1);
            }
            else {
                recycleView.setLayoutManager(mLayoutManager);
                recycleView.setAdapter(adapter2);
            }
        });

    }
}