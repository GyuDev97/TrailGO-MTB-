package com.bike.trailgo.net;


import com.bike.trailgo.dto.AuthSession;
import com.bike.trailgo.dto.Badge;
import com.bike.trailgo.dto.CompClub;
import com.bike.trailgo.dto.CompScore;
import com.bike.trailgo.dto.Competition;
import com.bike.trailgo.dto.CourseReview;
import com.bike.trailgo.dto.DangerousArea;
import com.bike.trailgo.dto.LoginInfo;

import com.bike.trailgo.dto.Mission;
import com.bike.trailgo.dto.MissionRanking;
import com.bike.trailgo.dto.RecordPhoto;
import com.bike.trailgo.dto.RecordWeather;
import com.bike.trailgo.dto.RidingRecord;

import com.bike.trailgo.dto.ScrapStatus;
import com.bike.trailgo.dto.Tag_Status;
import com.bike.trailgo.dto.User;
import com.bike.trailgo.hazard.Hazard;
import com.bike.trailgo.hazard.HazardReport;
import com.bike.trailgo.hazard.NearbyHazards;
import com.bike.trailgo.ui.riding.EndActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ServerApi {
    @GET("app/users")
    Call<List<User>> getUsers();

    @GET("app/users/{id}")
    Call<User> getUser(@Path("id") int id);

    // Social sign-in: the provider's access token goes in, the server's own session comes out.
    @FormUrlEncoded
    @POST("app/auth/kakao")
    Call<AuthSession> signInWithKakao(@Field("access_token") String accessToken);

    @FormUrlEncoded
    @POST("app/auth/naver")
    Call<AuthSession> signInWithNaver(@Field("access_token") String accessToken);

    @FormUrlEncoded
    @POST("app/auth/refresh")
    Call<AuthSession> refreshSession(@Field("refresh_token") String refreshToken);

    @FormUrlEncoded
    @POST("app/auth/logout")
    Call<Void> signOut(@Field("refresh_token") String refreshToken);

    @GET("app/getLoginInfo/{LoginId}")
    Call<LoginInfo> getLoginInfo(@Path("LoginId") String LoginId);

    @GET("app/getGPX/{directory}/{url}")
    Call<ResponseBody> getFile(@Path("directory") String directory, @Path("url") String url);

    @GET("app/get/{rr_rider}")
    Call<List<RidingRecord>> getMyRecord(@Path("rr_rider") String rr_rider);

    //라이딩 기록 저장
    @FormUrlEncoded
    @POST("app/riding/upload")
    Call<Map<String, String>> InsertRecord(@FieldMap HashMap<String, Object> param);

    //라이딩 기록 저장 후 해당 기록 가져오기
    @GET("app/riding/getrecord")
    Call<List<RidingRecord>> getInsertSel();

    //태그삽입
    @FormUrlEncoded
    @POST("app/riding/taginsert")
    Call<Tag_Status> tagInsert(@FieldMap Map<String, String> param);

    //위험지역가져오기
    @GET("api/public/hazards")
    Call<List<DangerousArea>> getDangerArea();

    //라이딩 기록
    @GET("app/get/{loginId}")
    Call<ArrayList<RidingRecord>> getRecord(@Path("loginId") String loginId);

    //라이딩 상세보기
    @GET("app/get/detail/{rr_num}")
    Call<List<RidingRecord>> getRecordDetail(@Path("rr_num") String rr_num);

    //본인 라이딩 기록 상세 (기록 시점 날씨 스냅샷 포함). 작성자와 번호가 모두 맞아야 조회된다.
    @GET("app/riding/records/{riderId}/{rrNum}")
    Call<RidingRecord> getMyRecordDetail(@Path("riderId") String riderId, @Path("rrNum") String rrNum);

    //라이딩 메모 수정 (비우면 삭제). 본인 기록만.
    @FormUrlEncoded
    @POST("app/riding/records/{riderId}/{rrNum}/memo")
    Call<Map<String, String>> updateMemo(@Path("riderId") String riderId, @Path("rrNum") String rrNum,
                                         @Field("memo") String memo);

    //라이딩 사진 추가(한 장씩) / 삭제. 본인 기록만, 최대 10장.
    @Multipart
    @POST("app/riding/records/{riderId}/{rrNum}/photos")
    Call<RecordPhoto> uploadPhoto(@Path("riderId") String riderId, @Path("rrNum") String rrNum,
                                  @Part MultipartBody.Part photo);

    @DELETE("app/riding/records/{riderId}/{rrNum}/photos/{photoId}")
    Call<Map<String, String>> deletePhoto(@Path("riderId") String riderId, @Path("rrNum") String rrNum,
                                          @Path("photoId") String photoId);

    //라이딩 당시 날씨 미리보기 (기상청 호출은 서버가 한다). at = 라이딩 시작 시각 "yyyy-MM-dd HH:mm:ss" (KST)
    @GET("app/weather")
    Call<RecordWeather> getWeather(@Query("lat") double lat, @Query("lon") double lon, @Query("at") String at,
                                   @Query("riderId") String riderId);
    //라이딩 공개설정
    @FormUrlEncoded
    @POST("app/recordset/open")
    Call<Map<String, String>> setOpen(@FieldMap HashMap<String, Object> param);

    //라이딩 기록
    @GET("app/riding/scrap/{rr_rider}")
    Call<ArrayList<ScrapStatus>> getScrap(@Path("rr_rider") String rr_rider);

    //태그 기록
    @GET("app/record/gettag/{rr_num}")
    Call<List<Tag_Status>> getTag(@Path("rr_num") String rr_num);


    //완료한 경쟁전 목록
    @GET("app/competition/{id}")
    Call<List<String>> getJoinedList(@Path("id") String id);

    //경쟁전 목록
    @GET("app/competition")
    Call<List<Competition>> getCompetition();

    //경쟁전 배지 정보
    @GET("app/getCompBadge/{comp_badge}")
    Call<Badge> getCompBadge(@Path("comp_badge") int comp_badge);

    //경쟁전 클럽 순위
    @GET("app/getCompClub/{comp_num}")
    Call<List<CompClub>> getCompClub(@Path("comp_num") int comp_num);

    //경쟁전 개인 순위
    @GET("app/getCompScore/{comp_num}")
    Call<List<CompScore>> getCompScore(@Path("comp_num") int comp_num);

    //미션 정보
    @GET("app/getAllMission/{id}")
    Call<List<Mission>> getAllMission(@Path("id") String id);

    //완료한 미션 정보
    @GET("app/getComMission/{id}")
    Call<List<String>> getCompMission(@Path("id") String id);

    //미션 순위
    @GET("app/getMisRanking")
    Call<List<MissionRanking>> getMisRanking();

    //미션 완료 일자
    @GET("app/getWhenCom/{id}/{m_num}")
    Call<Date> getWhenCom(@Path("id") String id, @Path("m_num") String m_num);

    //위험지역 정보
    @GET("app/danger/{id}")
    Call<List<DangerousArea>> getDanger(@Path("id") String id);

    //파일 업로드
    @Multipart
    @POST("app/fileUpload/{dir}/{fileName}")
    Call<Void> upload(
            @Part MultipartBody.Part file1,
            @Path("dir") String dir,
            @Path("fileName") String fileName
    );

    //위험지역 등록 (구 API: 인증 없이 제보자를 신뢰한다. 신규 화면은 reportHazard를 쓴다)
    @Deprecated
    @POST("app/insertDanger")
    Call<Void> insertDanger(@Body DangerousArea d_area);

    //주변 위험지역 (게스트 가능). 반경은 위험구역 경계까지의 거리.
    @GET("app/hazards/nearby")
    Call<NearbyHazards> getNearbyHazards(@Query("lat") double lat, @Query("lon") double lon,
                                         @Query("radius") int radiusMeters);

    //위험지역 제보 (로그인 회원만). 제보자는 서버가 토큰으로 정한다.
    @POST("app/hazards")
    Call<Hazard> reportHazard(@Body HazardReport report);

    //내 제보 철회 (숨김 처리)
    @DELETE("app/hazards/{id}")
    Call<Map<String, Object>> deleteHazard(@Path("id") int id);

    @GET("app/riding/course")
    Call<List<RidingRecord>> getCourseList();

    @GET("app/getCourseReview/{c_num}")
    Call<List<CourseReview>> getReviewList(@Path("c_num") String c_num);

    @FormUrlEncoded
    @POST("app/updateCourseReview")
    Call<Void> reviewUpdate(@FieldMap HashMap<String, Object> param);

    @FormUrlEncoded
    @POST("app/deleteCourseReview")
    Call<Void> deleteReview(@FieldMap HashMap<String, Object> param);

    @FormUrlEncoded
    @POST("app/insertCourseReview")
    Call<Void> insertReview(@FieldMap HashMap<String, Object> param);
}
