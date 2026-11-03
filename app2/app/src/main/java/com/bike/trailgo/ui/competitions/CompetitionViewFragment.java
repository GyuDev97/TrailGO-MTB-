package com.bike.trailgo.ui.competitions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bike.trailgo.Adapter.CompetitionAdapter;
import com.bike.trailgo.R;
import com.bike.trailgo.dto.Competition;
import com.bike.trailgo.ui.BaseFragment;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompetitionViewFragment extends BaseFragment {
    private ImageView userImage;
    private TextView userId, userNickname, userClubName;
    private Bitmap userBitmap;
    private RecyclerView recycleView;
    private String Save_Path;
    private ArrayList<String> joinedList;
    private ArrayList<Competition> nowItemList;
    private ArrayList<Competition> pastItemList;
    private ArrayList<Competition> itemList;
    private Call<List<String>> getJoinedList;
    private Call<List<Competition>> getCompetition;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_competition_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userImage = view.findViewById(R.id.comp_mem_image);
        userBitmap = BitmapFactory.decodeFile(new File(requireContext().getFilesDir().getPath() + "/" + model.r_Image.getValue()).getAbsolutePath());
        userImage.setImageBitmap(userBitmap);

        userId = view.findViewById(R.id.memberId);
        userNickname = view.findViewById(R.id.nickname);
        userClubName = view.findViewById(R.id.club_name);

        userId.setText(model.r_Id.getValue());
        userNickname.setText(model.r_Nickname.getValue());
        userClubName.setText(model.r_ClubName.getValue());

        Save_Path = requireContext().getFilesDir().getPath();

        recycleView = view.findViewById(R.id.recycleView1);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // TODO : process tab selection event.
                int pos = tab.getPosition();
                changeView(pos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // do nothing
            }
        });

        getJoinedList = serverApi.getJoinedList(model.r_Id.getValue());
        getJoinedList.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if ( response.code() == 200) {
                    List<String> body = response.body();
                    String pastItem = "";
                    joinedList = new ArrayList<>();

                    for (String item : body) {
                        if (!pastItem.equals(item))
                            joinedList.add(item);
                        pastItem = item;
                    }

                    getCompetition = serverApi.getCompetition();
                    getCompetition.enqueue(new Callback<List<Competition>>() {
                        @Override
                        public void onResponse(Call<List<Competition>> call, Response<List<Competition>> response) {
                            if ( response.code() == 200) {
                                pastItemList = new ArrayList<>();
                                itemList = new ArrayList<>();
                                nowItemList = new ArrayList<>();

                                List<Competition> body = response.body();

                                for (Competition item : body) {
                                    for (int i = 0; i < joinedList.size(); i++) {
                                        if (item.getComp_num() == Integer.parseInt(joinedList.get(i))) {
                                            itemList.add(item);
                                        }
                                    }


                                    String fullDay = item.getComp_period();
                                    long howDay = 0;
                                    try {
                                        howDay = getDate(fullDay);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }

                                    if (howDay < 0) {
                                        nowItemList.add(item);
                                    } else {
                                        pastItemList.add(item);
                                    }
                                }

                                CompetitionAdapter adapter = new CompetitionAdapter(requireContext().getApplicationContext(), nowItemList, Save_Path, controller, model);
                                recycleView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                                recycleView.setAdapter(adapter);

                            }
                        }

                        @Override
                        public void onFailure(Call<List<Competition>> call, Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {

            }
        });
    }

    private void changeView(int index) {
        switch (index) {
            case 0:
                CompetitionAdapter nowAdapter = new CompetitionAdapter(requireContext().getApplicationContext(), nowItemList, Save_Path, controller, model);
                recycleView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                recycleView.setAdapter(nowAdapter);
                break;
            case 1:
                CompetitionAdapter pastAdapter = new CompetitionAdapter(requireContext().getApplicationContext(), pastItemList, Save_Path, controller, model);
                recycleView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                recycleView.setAdapter(pastAdapter);
                break;

            case 2:
                CompetitionAdapter pastAdapter2 = new CompetitionAdapter(requireContext().getApplicationContext(), itemList, Save_Path, controller, model);
                recycleView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                recycleView.setAdapter(pastAdapter2);
                break;
        }
    }


    public long getDate(String getPeriod) throws ParseException {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd");
        String temp = sdfNow.format(date);
        Date nowTime = sdfNow.parse(temp);
        Date periodTime = sdfNow.parse(String.valueOf(getPeriod.substring(8, 16)));

        return (nowTime.getTime() - periodTime.getTime()) / (24 * 60 * 60 * 1000);

    }
}