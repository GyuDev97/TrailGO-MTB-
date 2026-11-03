package com.bike.webapp.mapper;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.*;

@Repository("com.bike.webapp.mapper.AndroidMapper")
public interface AndroidMapper {

	//안드로이드 유저 정보 가져오기
	public LoginInfo getLoginInfo(String LoginId) throws Exception;
	
	//안드로이드 앱에서 라이딩 기록 저장
	public void insertRecord(RidingRecord record) throws Exception;

	public int nextRidingRecordId() throws Exception;
	
	//주행기록 전체보기
	public List<RidingRecord> getRidingRecordAll();
		
	//안드로이드 앱에서 라이딩 기록 리스트 가져오기
	public List<App_RidingRecord> readRecord(String rr_rider);
	
	// 사용자 라이딩 기록 검색
	public List<RidingRecord> getRidingRecordDetail(@Param("rr_rider") String rr_rider, @Param("rr_num") String rr_num) throws Exception;

	// 본인 기록 상세 (작성자 + 번호 일치) / 공개 기록 상세 / 기록 시점 날씨 스냅샷 저장(최초 1회)
	public RidingRecord getOwnRecord(@Param("rr_rider") String rr_rider, @Param("rr_num") int rr_num);

	public RidingRecord getPublicRecord(@Param("rr_num") int rr_num);

	// 메모 수정(작성자+번호 일치 시에만) / 사진
	public int updateRecordMemo(@Param("rr_rider") String rr_rider, @Param("rr_num") int rr_num, @Param("memo") String memo);

	public int countPhotos(@Param("rr_num") int rr_num);

	public int nextPhotoOrder(@Param("rr_num") int rr_num);

	public int insertPhoto(@Param("id") String id, @Param("rr_num") int rr_num, @Param("file") String file, @Param("order") int order);

	public List<RecordPhoto> listPhotos(@Param("rr_num") int rr_num);

	public PhotoAccess findPhotoAccess(@Param("id") String id);

	public String findPhotoFile(@Param("id") String id, @Param("rr_num") int rr_num);

	public int deletePhoto(@Param("id") String id, @Param("rr_num") int rr_num);

	public int updateRecordWeather(@Param("rr_num") int rr_num, @Param("w") com.bike.webapp.weather.WeatherSnapshot snapshot);
	
	
	//코스 가져오기
	public List<App_RidingRecord> getCourse() throws Exception;
		
	//코스 상세보기
	public List<Course> getCourseItem(String c_num) throws Exception;
	
	//코스 스크랩
	public String insertScrap(Scrap_Status scrap) throws Exception;
	
	//스크랩 코스 보기
	public List<Scrap_Status> getScrap(String rr_rider) throws Exception;
	
	//스크랩 코스 상세보기
	public List<Course> getScrapDetail(String rr_rider, String ss_course) throws Exception;

	public List<Course> getRidingRecordAllItem(String rr_num) throws Exception;

	//경쟁전 가져오기
	public List<App_Competition> getCompetition() throws Exception;
	
	//경쟁전 참가 내역 가져오기
	public List<String> getjoinedComp(String rr_rider) throws Exception;
	
	//경쟁전 코스 가져오기
	public String getCompCourse(int c_num) throws Exception;
	
	//경쟁전 클럽 순위 가져오기
	public List<CompClub> getCompClub(int cs_comp) throws Exception;
	
	//경쟁전 뱃지 가져오기
	public Badge getCompBadge(int comp_badge) throws Exception;
	
	//경쟁전 개인 점수 가져오기
	public List<CompScore> getCompScore(int comp_num) throws Exception;
	
	public int RidingOpenSet(RidingRecord record) throws Exception;

	public void delScrap(String ss_rnum) throws Exception;
	

	//좋아요
	public String likeput(Like_Status likestatus) throws Exception;

	public List<DangerousArea> getDanger() throws Exception;
	
	//경쟁전 점수 업데이트
	public void updateCompScore( @Param("avg_speed") int avg_speed, @Param("rr_comp") int rr_comp,
								@Param("r_club") int r_club, @Param("LoginId") String LoginId) throws Exception;
	
	
	public void updateRank( @Param("rr_comp") int rr_comp, @Param("r_club") int r_club,
							@Param("score") int score, @Param("rank") int rank) throws Exception;
	
	public List<Mission_Status> getMissionStatus(String LoginId) throws Exception;
	
	public List<Mission> getNoClearMission (String LoginId) throws Exception;
	
	public void updateMissionStatus ( @Param("LoginId") String LoginId, @Param("typeScore1") int typeScore1,
										@Param("typeScore2") int typeScore2) throws Exception;
	
	public void insertMissionCom ( @Param("LoginId") String LoginId, @Param("mc_mission") int mc_mission, @Param("mc_time") Timestamp mc_time ) throws Exception;

	public String insertLike(RidingRecord record);

	
	public List<RidingRecord> getLikeCount(@Param("rr_num") int rr_num);

	public List<Like_Status> getLikeStatus(@Param("ls_rnum") int ls_rnum, @Param("ls_rider")String ls_rider);
	
	public List<RidingRecord> getRecord();

	public void Taginsert(App_Tag tag);
	
	public List<App_Mission> getAllMission(@Param("LoginId") String LoginId) throws Exception;
	
	public List<String> getComMission(@Param("LoginId") String LoginId) throws Exception;
	
	public Date getWhenCom(@Param("LoginId") String LoginId, @Param("m_num") int m_num) throws Exception;
	
	public List<App_MissionRanking> getMisRanking() throws Exception;
	
	public List<App_CourseReview> getCourseRiview(@Param("c_num") int c_num) throws Exception;
	
	public void updateCourseReview(App_CourseReview C_review) throws Exception;
	
	public void insertCourseReview(App_CourseReview C_review) throws Exception;
	
	public void deleteCourseReview(String cr_num) throws Exception;
	
	public int sameCheck(String LoginId) throws Exception;

    void foinsert(RidingRecord record);

	void foupdate(RidingRecord record);
	
	public void insertDanger (DangerousArea d_area) throws Exception;
}
