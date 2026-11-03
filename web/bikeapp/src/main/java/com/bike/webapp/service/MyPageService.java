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
public class MyPageService {
	@Resource(name="com.bike.webapp.mapper.MyPageMapper")
	@Autowired
	private MyPageMapper myPageMapper;
	
	//사용자 주행기록 조회
	public List<RidingRecord> getRR(String rider){
		return myPageMapper.getRR(rider);
	}
	
	//배지 조회
	public List<Badge> getBadge(String rider) throws Exception{
		return myPageMapper.getBadge(rider);
	}
	
	//사용자 정보 조회
	public Rider getUserInfo(String nickname) {
		return myPageMapper.getUserInfo(nickname);
	}
	
	//사용자 닉네임 조회
	public String getUserNickname(String r_id) {
		return myPageMapper.getUserNickname(r_id);
	}
	
	//사용자 총주행거리 조회
	public double getUserTotalDistance(String r_id) {
		return myPageMapper.getUserTotalDistance(r_id);
	}
	
	//클럽게시판 게시글 조회
	public List<Board> getUserClubPost(String r_id){
		return myPageMapper.getUserClubPost(r_id);
	}
	
	//SNS 게시글 조회
	public List<Board> getUserSNSPost(String r_id){
		return myPageMapper.getUserSNSPost(r_id);
	}
	
	//댓글 조회
	public List<Reply> getUserReply(String r_id){
		return myPageMapper.getUserReply(r_id);
	}
	
	//원본 게시글 조회
	public Board getOriginPost(int b_num) {
		return myPageMapper.getOriginPost(b_num);
	}
	
	//대표배지 변경
	public void changeRiderBadge(Rider rider) {
		myPageMapper.changeRiderBadge(rider);
	}
	
	//사용자 닉네임, 이미지 조회
	public Rider getRider(String r_id) {
		return myPageMapper.getRider(r_id);
	}
	
	//리뷰 조회
	public List<Reply> getReply(int re_board){
		return myPageMapper.getReply(re_board);
	}
}
