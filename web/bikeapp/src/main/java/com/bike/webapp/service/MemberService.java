package com.bike.webapp.service;

import java.util.List;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bike.webapp.dto.*;
import com.bike.webapp.mapper.*;

@Service
@Transactional
public class MemberService {
	@Resource(name="com.bike.webapp.mapper")
	@Autowired
	private MemberMapper memberMapper;
	
	//회원가입 처리
	public String memberInsert(Member member) throws Exception {
		return memberMapper.memberInsert(member);
	}
	public void regRepair_Shop(Repair_Shop rs) {
		memberMapper.regRepair_Shop(rs);
	}
	//주행기록 가져오기
	public List<RidingRecord> getRidingRecord(String rr_rider) throws Exception {
		return memberMapper.getRidingRecord(rr_rider);
	}
	
	//주행기록 상세가져오기
	public List<RidingRecord> getRidingRecordDetail(String rr_rider, String rr_num) throws Exception {
		return memberMapper.getRidingRecordDetail(rr_rider, rr_num);
	}
	
	//코스가져오기
	public List<Course> getCourse() throws Exception{
		return memberMapper.getCourse();
	}
	
	//코스스크랩 가져오기
	public List<Course> getScrapCourse(String rr_rider) throws Exception{
		return memberMapper.getScrapCourse(rr_rider);
	}
	
	//스크랩하기
	public void postScrapCourse(String ss_rider, int ss_course){
		memberMapper.postScrapCourse(ss_rider, ss_course);
	}
	
	//코스스크랩 삭제
	public void deleteScrapCourse(String ss_rider, int ss_course) throws Exception{
		memberMapper.deleteScrapCourse(ss_rider, ss_course);
	}
	
	//사용자가 등록한 위험지역 가져오기
	public List<DangerousArea> getUserDangerousArea(String rr_rider) throws Exception{
		return memberMapper.getUserDangerousArea(rr_rider);
	}
	
	
	public void deleteDangerousArea(int da_num) throws Exception{
		memberMapper.deleteDangerousArea(da_num);
	}
	
	//위험지역 취소?
	public void postNoDanger(No_Danger nd) throws Exception{
		memberMapper.postNoDanger(nd);
	}
}