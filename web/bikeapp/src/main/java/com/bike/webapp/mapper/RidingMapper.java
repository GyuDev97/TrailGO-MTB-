package com.bike.webapp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.*;

@Repository("com.bike.webapp.mapper.RidingMapper")
public interface RidingMapper {
	//로그인 후 페이지 구성을 위한 최신 라이딩 기록 조회
	public List<RidingRecord> getRidingRecordTop3(String rr_rider) throws Exception;
	
	//사용자 라이딩 기록 조회
	public List<RidingRecord> getRidingRecord(String rr_rider) throws Exception;
	
	//라이딩 넘버로 GPX파일명 조회
	public String getGpxFileByRR_Num(int rr_num) throws Exception;
	
	//라이딩 넘버로 라이딩기록 조회
	public RidingRecord getRidingRecordDetail(int rr_num) throws Exception;
	
	//라이딩 기록 공개/비공개 전환
	public void updateRidingRecord(@Param("rr_num")int rr_num, @Param("rr_open") int rr_open) throws Exception;
	
	//라이딩 기록 이름 변경
	public void updateRidingRecordName(@Param("rr_num") int rr_num, @Param("rr_name") String rr_name) throws Exception;
		
	//등록된 위험 지역 조회
	public List<DangerousArea> getDangerousArea() throws Exception;
	
	//사용자가 등록한 위험 지역 조회
	public List<DangerousArea> getUserDangerousArea(String rr_rider) throws Exception;
	
	// 위험지역 등록 신청
	public void postDangerousArea(DangerousArea da) throws Exception;
	
	//현재는 주행기록을 조회하기에 최종결정시에 수정필요함
	//등록된 코스 조회
	public List<RidingRecord> getCourses(String keyword) throws Exception;
	
	//사용자 스크랩 코스 조회
	public List<RidingRecord> getScrapCourse(String rr_rider) throws Exception;
	
	// 사용자 스크랩 코스 추가
	public void postScrapCourse(@Param("ss_rider") String ss_rider, @Param("ss_rnum") int ss_rnum);
	
	// 사용자 스크랩 코스 삭제
	public void deleteScrapCourse(@Param("ss_rider") String ss_rider, @Param("ss_rnum") int ss_rnum);
	
	//라이딩 넘버로 리뷰 조회
	public List<Course_Review> getCourseReviews(int cr_rnum) throws Exception;
	
	//리뷰 등록
	public void postCourseReview(Course_Review cr) throws Exception;
	
	//리뷰 삭제
	public void deleteCourseReview(int cr_num) throws Exception;
	
	//리뷰 수정
	public void updateCourseReview(@Param("cr_num") int cr_num, @Param("cr_content") String cr_content) throws Exception;

	//코스 추천
	public void postLS(Like_Status ls);
	
	//코스 추천수 조회
	public int getRR_Like(int rr_num);

	//코스 추천 취소
	public void deleteLS(Like_Status ls);
	
	//코스 해시태그 조회
	public List<Tag_Status> getTagList(int ts_rnum);
	
	//해시태그 삭제
	public void removeTag(int ts_num);

	//해시태그 등록
	public void insertTag(Tag_Status ts);
	
	//코스명 검색 - 검색어 포함
	public List<RidingRecord> searchCourseName(PagingVO vo);
	
	//코스명 검색 - 개수 파악
	public int cnt_searchCourseName(String rr_name);

	public void postnoMtb(Nomtb da);

	public List<Nomtb> getNoMtbArea();
	
	//전체 라이딩 평균 기록 조회
	public Avg_RidingRecord getAVGRR(String rider);
	
	//사용자의 총 라이딩 횟수 조회
	public int countRidingRecord(String rider);
	
	//추천코스 - 초급자용
	public List<RidingRecord> getBeginnerCourse();
	
	//추천코스 - 입문자용
	public List<RidingRecord> getRecommandCourse(String rider);
	
	//사용자 닉네임 조회
	public String getRiderNickName(String rider);
	
	//추천코스 상세 정보 제공
	public Recom_Course getRecomCourse(int rr_num);
}
