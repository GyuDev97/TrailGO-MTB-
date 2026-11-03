package com.bike.trailgo;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    public MutableLiveData<String> courseName = new MutableLiveData<>();
    public MutableLiveData<String> r_Id = new MutableLiveData<>();
    public MutableLiveData<String> r_Image = new MutableLiveData<>();
    public MutableLiveData<String> r_Nickname = new MutableLiveData<>();
    public MutableLiveData<String> r_ClubName = new MutableLiveData<>();

    //라이딩 기록 관련(코스 공용)
    public MutableLiveData<String> r_num = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_name = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_date = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_adress = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_rest = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_dis = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_get = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_time = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_max = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_avg = new MutableLiveData<>();
    public MutableLiveData<Integer> my_rec_open = new MutableLiveData<>();
    public MutableLiveData<String> my_rec_gpx = new MutableLiveData<>();
    public MutableLiveData<Integer> like_count = new MutableLiveData<>();
    public MutableLiveData<String> CourseRider = new MutableLiveData<>();
    public MutableLiveData<String> CourseR_image = new MutableLiveData<>();

    //경쟁전
    public MutableLiveData<String> comp_num = new MutableLiveData<>();
    public MutableLiveData<String> comp_period = new MutableLiveData<>();
    public MutableLiveData<String> comp_badge = new MutableLiveData<>();
    public MutableLiveData<String> comp_course = new MutableLiveData<>();
    public MutableLiveData<String> comp_image = new MutableLiveData<>();
    public MutableLiveData<String> comp_content = new MutableLiveData<>();
    public MutableLiveData<String> comp_name = new MutableLiveData<>();
    public MutableLiveData<String> save_path = new MutableLiveData<>();
    public MutableLiveData<String> c_gpx = new MutableLiveData<>();
    public MutableLiveData<String> c_name = new MutableLiveData<>();
    public MutableLiveData<String> comp_point = new MutableLiveData<>();

    //미션
    public MutableLiveData<String> m_image = new MutableLiveData<>();
    public MutableLiveData<String> m_content = new MutableLiveData<>();
    public MutableLiveData<String> m_name = new MutableLiveData<>();
    public MutableLiveData<String> m_type = new MutableLiveData<>();
    public MutableLiveData<String> m_ms_score = new MutableLiveData<>();
    public MutableLiveData<String> m_num = new MutableLiveData<>();
    public MutableLiveData<String> m_bg_name = new MutableLiveData<>();
}
