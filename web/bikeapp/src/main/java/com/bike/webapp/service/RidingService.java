package com.bike.webapp.service;

import java.util.List;

import javax.annotation.Resource;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bike.webapp.dto.*;
import com.bike.webapp.mapper.*;

@Service
@Transactional
public class RidingService {
	@Resource(name="com.bike.webapp.mapper.RidingMapper")
	@Autowired
	private RidingMapper ridingMapper;
	
	//로그인 후 페이지 구성을 위한 최신 라이딩 기록 조회
	public List<RidingRecord> getRidingRecordTop3(String rr_rider) throws Exception{
		return ridingMapper.getRidingRecordTop3(rr_rider);
	}
	
	//사용자 라이딩 기록 조회
	public List<RidingRecord> getRidingRecord(String rr_rider) throws Exception {
		return ridingMapper.getRidingRecord(rr_rider);
	}
	
	//라이딩 넘버로 GPX파일명 조회
	public String getGpxFileByRR_Num(int rr_num) throws Exception{
		return ridingMapper.getGpxFileByRR_Num(rr_num);
	}
	
	//라이딩 넘버로 라이딩기록 조회
	public RidingRecord getRidingRecordDetail(int rr_num) throws Exception {
		return ridingMapper.getRidingRecordDetail(rr_num);
	}
	
	//라이딩 기록 공개/비공개 전환
	public void updateRidingRecord(int rr_num, int rr_open) throws Exception{
		ridingMapper.updateRidingRecord(rr_num, rr_open);
	}
	
	//라이딩 기록 이름 변경
	public void updateRidingRecordName(int rr_num, String rr_name) throws Exception{
		ridingMapper.updateRidingRecordName(rr_num, rr_name);
	}
	
	//등록된 위험지역 조회
	public List<DangerousArea> getDangerousArea() throws Exception{
		return ridingMapper.getDangerousArea();
	}

	//사용자가 등록한 위험지역 조회
	public List<DangerousArea> getUserDangerousArea(String rr_rider) throws Exception{
		return ridingMapper.getUserDangerousArea(rr_rider);
	}
	
	//위험지역 등록
	public void postDangerousArea(DangerousArea da) throws Exception {
		ridingMapper.postDangerousArea(da);
	}
	
	//등록된 코스 조회
	public List<RidingRecord> getCourses(String keyword) throws Exception{
		return ridingMapper.getCourses(keyword);
	}
	
	//코스스크랩 조회
	public List<RidingRecord> getScrapCourse(String rr_rider) throws Exception{
		return ridingMapper.getScrapCourse(rr_rider);
	}
	
	// 사용자 스크랩 코스 추가
	public void postScrapCourse(@Param("ss_rider") String ss_rider, @Param("ss_rnum") int ss_rnum) {
		ridingMapper.postScrapCourse(ss_rider, ss_rnum);
	}
	
	// 사용자 스크랩 코스 삭제
	public void deleteScrapCourse(@Param("ss_rider") String ss_rider, @Param("ss_rnum") int ss_rnum) {
		ridingMapper.deleteScrapCourse(ss_rider, ss_rnum);
	}
	
	//라이딩 넘버로 리뷰 조회
	public List<Course_Review> getCourseReviews(int cr_rnum) throws Exception{
		return ridingMapper.getCourseReviews(cr_rnum);
	}
	
	//리뷰 등록
	public void postCourseReview(Course_Review cr) throws Exception{
		ridingMapper.postCourseReview(cr);
	}
	
	//리뷰 삭제
	public void deleteCourseReview(int cr_num) throws Exception{
		ridingMapper.deleteCourseReview(cr_num);
	}
	
	//리뷰 수정
	public void updateCourseReview(int cr_num, String cr_content) throws Exception{
		ridingMapper.updateCourseReview(cr_num, cr_content);
	}
	
	//코스 추천
	public void postLS(Like_Status ls) {
		ridingMapper.postLS(ls);
	}
	
	//코스 추천수 조회
	public int getRR_Like(int rr_num) {
		return ridingMapper.getRR_Like(rr_num);
	}

	// 코스 추천 취소
	public void deleteLS(Like_Status ls) {
		ridingMapper.deleteLS(ls);
	}
	
	//코스 해시태그 조회
	public List<Tag_Status> getTagList(int ts_rnum){
		return ridingMapper.getTagList(ts_rnum);
	}
	
	//해시태그 삭제
	public void removeTag(int ts_num) {
		ridingMapper.removeTag(ts_num);
	}
	
	//해시태그 등록
	public void insertTag(Tag_Status ts) {
		ridingMapper.insertTag(ts);
	}
	
	//코스명 검색 - 검색어 포함
	public List<RidingRecord> searchCourseName(PagingVO vo){
		return ridingMapper.searchCourseName(vo);
	}
	
	//코스명 검색 - 개수 파악
	public int cnt_searchCourseName(String rr_name) {
		return ridingMapper.cnt_searchCourseName(rr_name);
	}

	public void postnoMtb(Nomtb da) {
		ridingMapper.postnoMtb(da);
		
	}

	public List<Nomtb> getNoMtbArea() {
		// TODO Auto-generated method stub
		return ridingMapper.getNoMtbArea();
	}
	
	//전체 라이딩 평균 기록 조회
	public Avg_RidingRecord getAVGRR(String rider) {
		return ridingMapper.getAVGRR(rider);
	}
	
	//사용자의 총 라이딩 횟수 조회
	public int countRidingRecord(String rider) {
		return ridingMapper.countRidingRecord(rider);
	}
	
	//추천코스 제공 - 초급자용
	public List<RidingRecord> getBeginnerCourse(){
		return ridingMapper.getBeginnerCourse();
	}
	
	//추천코스 - 입문자용
	public List<RidingRecord> getRecommandCourse(String rider){
		return ridingMapper.getRecommandCourse(rider);
	}
	
	//사용자 닉네임 조회
	public String getRiderNickName(String rider) {
		return ridingMapper.getRiderNickName(rider);
	}
	
	//추천코스 상세 정보 제공
	public Recom_Course getRecomCourse(int rr_num) {
		return ridingMapper.getRecomCourse(rr_num);
	}
}
