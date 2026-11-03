package com.bike.webapp.mapper;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.*;

@Repository("com.bike.webapp.mapper.RepairMapper")
public interface RepairMapper {
	//사용자 타입 조회 0:일반 1:정비소 2:관리자
	public int getType(String rider);
	
	//정비소 문의 조회
	public List<Qna> getQNA(String shop);
	
	//정비소 신청 조회
	public List<Repair_Apply> getRepairApply(String shop);
	
	//정비소 답변 안한 문의 조회
	public List<Qna> getNoAnserQNA(String shop);
	
	//정비소 확인 안한 신청 조회
	public List<Repair_Apply> getNoConfirmRepairApply(String shop);
	
	//문의번호로 문의글 조회
	public Qna getDetailQna(int qa_num);
	
	//신청번호로 신청 조회
	public Repair_Apply getDetailRepairApply(int ra_num);
	
	//문의번호로 문의글 답변
	public void updateDetailQna(Qna qna);
	
	//신청번호로 신청 수락 및 거절
	public void updateRepairApply(Repair_Apply ra);
	
	//사용자 문의 내역 조회
	public List<Qna> rider_getQNA(String rider);
	
	//사용자 신청 내역 조회
	public List<Repair_Apply> rider_getRepairApply(String rider);
	
	//정비소 조회 (임시)
	public List<Repair_Shop> getRepairShop(String area);
	
	//정비소 상세정보 조회
	public Repair_Shop getRepairShopDetail(String rs_id);
	
	//정비소 리뷰 조회
	public List<Repair_History> getRepairHistory(String ra_shop);
	
	//사용자 문의 등록
	public void postRider_QNA(Qna qa);

	//사용자 신청 등록
	public void postRider_Apply(Repair_Apply ra);
	
	//사용자 정보조회
	public Rider getRiderInfo(String r_id);
}
