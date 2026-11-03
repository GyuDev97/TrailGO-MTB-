package com.bike.webapp.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.*;

@Repository("com.bike.webapp.mapper.CommunityMapper")
public interface CommunityMapper {

	// 클럽 생성
	public void insertClub(Club club) throws Exception;

	// 클럽이름 중복검사
	public int checkClubName(String cb_name);

	// 클럽 가입
	public void insertCJ(Club_Join cb_join);

	// 클럽 캘린더 일정 조회
	public List<Club_Calender> getCCList(int cc_club);

	// 클럽 캘린더 일정 등록
	public void postCC(Club_Calender cc);

	// 클럽 캘린더 일정 수정
	public void updateCC(Club_Calender cc);

	// 클럽 캘린더 일정 삭제
	public void deleteCC(int cc_num);

	// 클럽 멤버 조회
	public int getMember(Club cb_num);

	// 클럽 조회
	public List<Club> getClub(Club cb_num);

	// 클럽 가입
	public void signClub(Club_Join cb_join);
	
	//가입 클럽 조회
	public int getJoinCLub(String rider);
	
	//클럽 넘버로 클럽 정보 조회
	public Club getClubInfo(int c_num);
	
	//SNS게시글 조회
	public List<Board> getSNSList(String rider);
	
	//SNS게시글 등록
	public void postSNS(Board board);
	
	//SNS게시글 삭제
	public void deleteSNS(int b_num);
	
	//SNS게시글 수정
	public void updateSNS(Board board);

	//SNS게시글 수정2
	public void updateSNS2(Board board);
	
	//SNS댓글 조회
	public List<Reply> getReply(int re_board);
	
	//SNS댓글 등록
	public void postReply(Reply reply);

	//SNS댓글 삭제
	public void deleteReply(int re_num);
	
	//SNS댓글 수정
	public void putReply(Reply reply);
	
	//거래물품 조회
	public List<Goods> getGoodsList();
	
	//거래물품 조회수 증가
	public void updateGoodsHit(int g_num);
	
	//거래물품 상세조회
	public Goods getGoodsDetail(int g_num);
	
	//사용자 정보 조회
	public Rider getUserInfo(String r_id);
	
	//사용자 거래 내역 조회	
	public Integer getUserTradeInfo(Goods goods);
	
	//거래물품 관심등록
	public void updateGoodsLike(Goods_like gl);
	
	//거래 완료 업데이트 - 거래기록 저장
	public void updateTradeEnd(Goods_like gl);
	
	//거래 완료 업데이트2 - 물품 상태 판매완료 갱신
	public void updateTradeEnd2(Goods_like gl);
	
	//나의 거래 조회
	public Saler getSalerInfo(String g_rider);
	
	//거래 물품 등록
	public void postGoods(Goods goods);
	
	// 사용자 거래물품 조회
	public List<Goods> getRiderGoodsList(@Param("rider") String rider, @Param("status") int status);
	
	//사용자 물품 삭제
	public void removeGoods(int g_num);
	
	//사용자 물품 수정 이미지X
	public void updateGoods(Goods goods);
	
	//사용자 물품 수정 이미지O
	public void updateGoods2(Goods goods);
}
