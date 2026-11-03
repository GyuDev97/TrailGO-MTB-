package com.bike.webapp.service;

import java.util.HashMap;
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
public class CommunityService {
	@Resource(name = "com.bike.webapp.mapper.CommunityMapper")
	@Autowired
	private CommunityMapper communityMapper;

	// 클럽 생성
	public void insertClub(Club club) throws Exception {
		communityMapper.insertClub(club);
	}

	/// 클럽이름 중복검사
	public int checkClubName(String cb_name) {
		return communityMapper.checkClubName(cb_name);
	}

	// 클럽 가입
	public void insertCJ(Club_Join cb_join) {
		communityMapper.insertCJ(cb_join);
	}

	// 클럽 캘린더 일정 조회
	public List<Club_Calender> getCCList(int cc_club) {
		return communityMapper.getCCList(cc_club);
	}

	// 클럽 캘린더 일정 등록
	public void postCC(Club_Calender cc) {
		communityMapper.postCC(cc);
	}

	// 클럽 캘린더 일정 수정
	public void updateCC(Club_Calender cc) {
		communityMapper.updateCC(cc);
	}

	// 클럽 캘린더 일정 삭제
	public void deleteCC(int cc_num) {
		communityMapper.deleteCC(cc_num);
	}

	// 클럽 멤버수 조회
	public int getMember(Club cb_num) {
		return communityMapper.getMember(cb_num);
	}

	// 클럽 조회
	public List<Club> getClub(Club cb_num) {
		return communityMapper.getClub(cb_num);
	}

	// 클럽 가입
	public void signClub(Club_Join cb_join) {
		communityMapper.signClub(cb_join);
	}
	
	//가입 클럽 조회
	public int getJoinCLub(String rider) {
		return communityMapper.getJoinCLub(rider);
	}
	//클럽 넘버로 클럽 정보 조회
	public Club getClubInfo(int c_num) {
		return communityMapper.getClubInfo(c_num);
	}
	
	//SNS게시글 조회
	public List<Board> getSNSList(String rider){
		return communityMapper.getSNSList(rider);
	}
	
	//SNS게시글 등록
	public void postSNS(Board board) {
		communityMapper.postSNS(board);
	}
	
	//SNS게시글 삭제
	public void deleteSNS(int b_num) {
		communityMapper.deleteSNS(b_num);
	}
	
	//SNS게시글 수정
	public void updateSNS(Board board) {
		communityMapper.updateSNS(board);
	}
	
	//SNS게시글 수정2
	public void updateSNS2(Board board) {
		communityMapper.updateSNS2(board);
	}
	
	//SNS댓글 조회
	public List<Reply> getReply(int re_board){
		return communityMapper.getReply(re_board);
	}
	
	//SNS댓글 등록
	public void postReply(Reply reply) {
		communityMapper.postReply(reply);
	}
	
	//SNS댓글 삭제
	public void deleteReply(int re_num) {
		communityMapper.deleteReply(re_num);
	}
	
	//SNS댓글 수정
	public void putReply(Reply reply) {
		communityMapper.putReply(reply);
	}
	
	//거래물품 조회
	public List<Goods> getGoodsList(){
		return communityMapper.getGoodsList();
	}
	
	//거래물품 조회수 증가
	public void updateGoodsHit(int g_num) {
		communityMapper.updateGoodsHit(g_num);
	}
	
	//거래물품 상세조회
	public Goods getGoodsDetail(int g_num) {
		return communityMapper.getGoodsDetail(g_num);
	}
	
	//사용자 정보 조회
	public Rider getUserInfo(String r_id) {
		return communityMapper.getUserInfo(r_id);
	}
		
	//사용자 거래 내역 조회	
	public Integer getUserTradeInfo(Goods goods) {
		return communityMapper.getUserTradeInfo(goods);
	}
	
	//거래물품 관심등록
	public void updateGoodsLike(Goods_like gl) {
		communityMapper.updateGoodsLike(gl);
	}
	
	//거래 완료 업데이트 - 거래기록 저장
	public void updateTradeEnd(Goods_like gl) {
		communityMapper.updateTradeEnd(gl);
	}
	
	//거래 완료 업데이트2 - 물품 상태 판매완료 갱신
	public void updateTradeEnd2(Goods_like gl) {
		communityMapper.updateTradeEnd2(gl);
	}
	
	//나의 거래 조회
	public Saler getSalerInfo(String g_rider) {
		return communityMapper.getSalerInfo(g_rider);
	}
	
	//거래 물품 등록
	public void postGoods(Goods goods) {
		communityMapper.postGoods(goods);
	}
	
	// 사용자 거래물품 조회
	public List<Goods> getRiderGoodsList(String rider, int status){
		return communityMapper.getRiderGoodsList(rider, status);
	}
	
	//사용자 물품 삭제
	public void removeGoods(int g_num) {
		communityMapper.removeGoods(g_num);
	}
	
	//사용자 물품 수정 이미지X
	public void updateGoods(Goods goods) {
		communityMapper.updateGoods(goods);
	}
	
	//사용자 물품 수정 이미지O
	public void updateGoods2(Goods goods) {
		communityMapper.updateGoods2(goods);
	}
}
