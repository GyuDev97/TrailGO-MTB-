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
public class RepairService {
	
	@Resource(name = "com.bike.webapp.mapper.RepairMapper")
	@Autowired
	private RepairMapper repairMapper;
	
	//사용자 타입 조회 0:일반 1:정비소 2:관리자
	public int getType(String rider) {
		return repairMapper.getType(rider);
	}
	
	//정비소 문의 조회
	public List<Qna> getQNA(String shop){
		return repairMapper.getQNA(shop);
	}
		
	//정비소 신청 조회
	public List<Repair_Apply> getRepairApply(String shop){
		return repairMapper.getRepairApply(shop);
	}
	
	//정비소 답변 안한 문의 조회
	public List<Qna> getNoAnserQNA(String shop){
		return repairMapper.getNoAnserQNA(shop);
	}
	
	//정비소 확인 안한 신청 조회
	public List<Repair_Apply> getNoConfirmRepairApply(String shop){
		return repairMapper.getNoConfirmRepairApply(shop);
	}
	
	//문의번호로 문의글 조회
	public Qna getDetailQna(int qa_num) {
		return repairMapper.getDetailQna(qa_num);
	}
	
	//신청번호로 신청 조회
	public Repair_Apply getDetailRepairApply(int ra_num) {
		return repairMapper.getDetailRepairApply(ra_num);
	}
	
	//문의번호로 문의글 답변
	public void updateDetailQna(Qna qna) {
		repairMapper.updateDetailQna(qna);
	}
	
	//신청번호로 신청 수락 및 거절
	public void updateRepairApply(Repair_Apply ra) {
		repairMapper.updateRepairApply(ra);
	}
	
	//사용자 문의 내역 조회
	public List<Qna> rider_getQNA(String rider){
		return repairMapper.rider_getQNA(rider);
	}
	
	//사용자 신청 내역 조회
	public List<Repair_Apply> rider_getRepairApply(String rider){
		return repairMapper.rider_getRepairApply(rider);
	}
	
	//정비소 조회(임시)
	public List<Repair_Shop> getRepairShop(String area){
		return repairMapper.getRepairShop(area);
	}
	
	//정비소 상세정보 조회
	public Repair_Shop getRepairShopDetail(String rs_id) {
		return repairMapper.getRepairShopDetail(rs_id);
	}
	
	//정비소 리뷰 조회
	public List<Repair_History> getRepairHistory(String ra_shop){
		return repairMapper.getRepairHistory(ra_shop);
	}
	
	//사용자 문의 등록
	public void postRider_QNA(Qna qa) {
		repairMapper.postRider_QNA(qa);
	}
	
	//사용자 신청 등록
	public void postRider_Apply(Repair_Apply ra) {
		repairMapper.postRider_Apply(ra);
	}
	
	//사용자 정보조회
	public Rider getRiderInfo(String r_id) {
		return repairMapper.getRiderInfo(r_id);
	}
}
