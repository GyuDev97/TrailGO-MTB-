package com.bike.webapp.mapper;


import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import com.bike.webapp.dto.*;
@Repository("com.bike.webapp.mapper")
public interface MemberMapper {

	//회원가입 작성
	public String memberInsert(Member member) throws Exception;
	public String memberLogin(Login login) throws Exception;
	public void regRepair_Shop(Repair_Shop rs);
	
	// 사용자 라이딩 기록 검색
	public List<RidingRecord> getRidingRecord(String rr_rider) throws Exception;
	
	// 사용자 라이딩 기록 검색
	public List<RidingRecord> getRidingRecordDetail(@Param("rr_rider")String rr_rider, @Param("rr_num")String rr_num) throws Exception;
	
	//등록된 코스 조회
	public List<Course> getCourse() throws Exception;
	
	//사용자 스크랩 코스 조회
	public List<Course> getScrapCourse(String rr_rider) throws Exception;
	
	// 사용자 스크랩 코스 추가
	public void postScrapCourse(@Param("ss_rider") String ss_rider, @Param("ss_course") int ss_course);
	
	// 사용자 스크랩 코스 삭제
	public void deleteScrapCourse(@Param("ss_rider")String ss_rider, @Param("ss_course") int ss_course) throws Exception;
	
	//사용자가 등록한 위험 지역 조회
	public List<DangerousArea> getUserDangerousArea(String rr_rider) throws Exception;
	
	// 사용자 등록 위험 지역 삭제
	public void deleteDangerousArea(int da_num) throws Exception;
	
	// 다른 사용자 등록 위험 지역 해지 신청
	public void postNoDanger(No_Danger nd) throws Exception;
}