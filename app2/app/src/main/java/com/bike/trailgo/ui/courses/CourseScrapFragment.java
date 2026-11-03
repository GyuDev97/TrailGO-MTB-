package com.bike.trailgo.ui.courses;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.dto.ScrapStatus;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.recycler.MyReportAdapter;
import com.bike.trailgo.ui.recycler.MyScrapAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseScrapFragment extends BaseFragment {
    private Call<ArrayList<ScrapStatus>> request;
    private List<ScrapStatus> items;
    private RecyclerView recyclerView;
    private MyReportAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // 최소한의 코드
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        request = serverApi.getScrap(model.r_Id.getValue());

        request.enqueue(new Callback<ArrayList<ScrapStatus>>() {
            @Override
            public void onResponse(Call<ArrayList<ScrapStatus>> call, Response<ArrayList<ScrapStatus>> response) {
                if(response.code()==200){
                    items = response.body();

                    ArrayList<ScrapStatus> itemList = new ArrayList<>();
                    for(ScrapStatus item: items){
                        itemList.add(item);
                    }

                    MyScrapAdapter adapter = new MyScrapAdapter(requireContext().getApplicationContext(), itemList, controller, model);
                    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                    recyclerView.setAdapter(adapter);

                }
            }

            @Override
            public void onFailure(Call<ArrayList<ScrapStatus>> call, Throwable t) {

            }
        });
    }
}