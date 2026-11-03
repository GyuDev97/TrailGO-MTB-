package com.bike.trailgo.ui.competitions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bike.trailgo.Adapter.CompClubAdapter;
import com.bike.trailgo.Adapter.CompScoreAdapter;
import com.bike.trailgo.R;
import com.bike.trailgo.dto.Badge;
import com.bike.trailgo.dto.CompClub;
import com.bike.trailgo.dto.CompScore;
import com.bike.trailgo.dto.Competition;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.riding.FollowStart;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompetitionDetailFragment extends BaseFragment {

    private TextView compName, compDay, compContent, compBadgeName;
    private ImageView compImage, badgeImage1, badgeImage2, badgeImage3;
    private WebView webView;
    private Call<Badge> getCompBadge;
    private Call<List<CompClub>> getCompClub;
    private Call<List<CompScore>> getCompScore;
    private ArrayList<CompClub> clubItemList;
    private ArrayList<CompScore> newScoreItemList;
    private RecyclerView compClubRecyclerView, compScoreRecyclerView;
    private String Save_Path;
    private Button btt;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_competition_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        compName = view.findViewById(R.id.comp_name2);
        compDay = view.findViewById(R.id.comp_day2);
        compContent = view.findViewById(R.id.comp_content);
        compImage = view.findViewById(R.id.imageView6);
        compBadgeName = view.findViewById(R.id.textView14);
        badgeImage1 = view.findViewById(R.id.badge_image);
        badgeImage2 = view.findViewById(R.id.badge_image2);
        badgeImage3 = view.findViewById(R.id.badge_image3);
        compClubRecyclerView = view.findViewById(R.id.compClub_recycle);
        compScoreRecyclerView = view.findViewById(R.id.compScore_recycle);
        btt = view.findViewById(R.id.compjoin_button);

        btt .setOnClickListener(v->{
            Intent intent3 = new Intent(v.getContext(), FollowStart.class);
            intent3.putExtra("comp_num", model.comp_num.getValue());
            intent3.putExtra("check", 1);
            intent3.putExtra("gpx", model.c_gpx.getValue());
            intent3.putExtra("c_name", model.c_name.getValue());
            intent3.putExtra("comp_name", model.comp_name.getValue());
            intent3.putExtra("comp_point", "100");
            intent3.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent3);
        });
        compName.setText(model.comp_name.getValue());
        compDay.setText(model.comp_period.getValue());
        compContent.setText(model.comp_content.getValue());
        Picasso.get().load(Server.getUrl("app/getGPX/comp/" + model.comp_image.getValue()))
                .into(compImage);

        Save_Path = requireContext().getFilesDir().getPath();

        webView = view.findViewById(R.id.comp_Course);
        webView.getSettings().setJavaScriptEnabled(true);//자바스크립트 허용

        WebSettings wbs = webView.getSettings();
        wbs.setLoadWithOverviewMode(true);
        wbs.setJavaScriptEnabled(true);
        webView.loadUrl(Server.getUrl("app/getAppCompCourse?c_num=" + model.comp_course.getValue()));//웹뷰 실행
        webView.setWebChromeClient(new WebChromeClient());//웹뷰에 크롬 사용 허용//이 부분이 없으면 크롬에서 alert가 뜨지 않음
        webView.setWebViewClient(new WebViewClientClass());//새창열기 없이 웹뷰 내에서 다시 열기//페이지 이동 원활히 하기위해 사용

        Log.d("웨웨웨",model.comp_course.getValue());

        getCompBadge = serverApi.getCompBadge(Integer.parseInt(model.comp_badge.getValue()));
        getCompBadge.enqueue(new Callback<Badge>() {
            @Override
            public void onResponse(Call<Badge> call, Response<Badge> response) {
                if ( response.code() == 200) {
                    Badge body = response.body();

                    Log.e("배지이미지", body.getBg_image());

                    Picasso.get().load(Server.getUrl("app/getGPX/badge/" + body.getBg_image() + "1.png"))
                            .into(badgeImage1);
                    Picasso.get().load(Server.getUrl("app/getGPX/badge/" + body.getBg_image() + "2.png"))
                            .into(badgeImage2);
                    Picasso.get().load(Server.getUrl("app/getGPX/badge/" + body.getBg_image() + "3.png"))
                            .into(badgeImage3);

                    compBadgeName.setText(body.getBg_name());
                }
            }
            @Override
            public void onFailure(Call<Badge> call, Throwable t) {
                showServerFailure();
            }
        });

        getCompClub = serverApi.getCompClub(Integer.parseInt(model.comp_num.getValue()));
        getCompClub.enqueue(new Callback<List<CompClub>>() {
            @Override
            public void onResponse(Call<List<CompClub>> call, Response<List<CompClub>> response) {
                if ( response.code() == 200) {
                    List<CompClub> body = response.body();
                    clubItemList = new ArrayList<>();

                    for ( int i = 0; i<5 && i<body.size(); i++){
                        CompClub item = body.get(i);
                        clubItemList.add(item);
                    }

                    CompClubAdapter adapter = new CompClubAdapter(requireContext().getApplicationContext(), clubItemList, Save_Path, controller, model);
                    compClubRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                    compClubRecyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<CompClub>> call, Throwable t) {

            }
        });

        getCompScore = serverApi.getCompScore(Integer.parseInt(model.comp_num.getValue()));
        getCompScore.enqueue(new Callback<List<CompScore>>() {
            @Override
            public void onResponse(Call<List<CompScore>> call, Response<List<CompScore>> response) {
                if ( response.code() == 200) {
                    List<CompScore> body = response.body();

                    ArrayList<CompScore> itemList = new ArrayList<>();
                    newScoreItemList = new ArrayList<>();
                    String past_name="";
                    int i = 0, cnt = 1;

                    for(CompScore item: body) {
                        if (past_name.equals(item.getR_nickname())) {
                            itemList.get(i - cnt).setRr_avgspeed((itemList.get(i - cnt).getRr_avgspeed() + item.getRr_avgspeed()));
                            cnt++;
                        }
                        else {
                            itemList.add(item);
                        }
                        past_name = item.getR_nickname();
                        i++;
                    }

                    ArrayList<Integer> tempInt = new ArrayList<>();

                    // tempInt 배열에 평균속도 넣어줌
                    for ( int j = 0; j < itemList.size(); j++) {
                        tempInt.add(itemList.get(j).getRr_avgspeed());
                    }

                    // 크기순 정렬
                    Collections.sort(tempInt);
                    Collections.reverse(tempInt);

                    // newItemList 리스트에 5개만 저장
                    for ( int l = 0; l < 5 && l < itemList.size() ; l++) {
                        for ( int k = 0; k < itemList.size(); k++) {
                            if (tempInt.get(l) == itemList.get(k).getRr_avgspeed()) {
                                if(itemList.get(k).getR_image() == null){
                                    itemList.get(k).setR_image("noImage.jpg");
                                }
                                newScoreItemList.add(itemList.get(k));

                                CompScoreAdapter adapter = new CompScoreAdapter(requireContext().getApplicationContext(), newScoreItemList, Save_Path, controller, model);
                                compScoreRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                                compScoreRecyclerView.setAdapter(adapter);
                            }
                        }
                    }


                }
            }

            @Override
            public void onFailure(Call<List<CompScore>> call, Throwable t) {

            }
        });
    }

    private class WebViewClientClass extends WebViewClient {//페이지 이동
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri url = request.getUrl();
            view.loadUrl(String.valueOf(url));
            return true;
        }
    }
}
