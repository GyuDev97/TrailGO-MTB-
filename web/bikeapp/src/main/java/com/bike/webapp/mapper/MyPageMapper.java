package com.bike.webapp.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.*;

@Repository("com.bike.webapp.mapper.MyPageMapper")
public interface MyPageMapper {
	//사용자 주행기록 조회
	public List<RidingRecord> getRR(String rider);
	
	//사용자 배지 조회
	public List<Badge> getBadge(String rider);
	
	//사용자 정보 조회
	public Rider getUserInfo(String nickname);
	
	//사용자 닉네임 조회
	public String getUserNickname(String r_id);
	
	//사용자 총주행거리 조회
	public double getUserTotalDistance(String r_id);
	
	//클럽게시판 게시글 조회
	public List<Board> getUserClubPost(String r_id);
	
	//SNS 게시글 조회
	public List<Board> getUserSNSPost(String r_id);
	
	//댓글 조회
	public List<Reply> getUserReply(String r_id);
	
	//원본 게시글 조회
	public Board getOriginPost(int b_num);
	
	//대표배지 변경
	public void changeRiderBadge(Rider rider);
	
	//사용자 닉네임, 이미지 조회
	public Rider getRider(String r_id);
	
	//리뷰 조회
	public List<Reply> getReply(int re_board);
}
