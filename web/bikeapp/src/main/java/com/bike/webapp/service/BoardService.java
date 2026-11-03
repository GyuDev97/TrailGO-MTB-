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
public class BoardService {
	@Resource(name="com.bike.webapp.mapper.BoardMapper")
	@Autowired
	private BoardMapper boardMapper;

	public List<Board> getBoardList(int b_club) {
		return boardMapper.getBoardList(b_club);
	}
	
	public int getJoinCLub(String cj_rider) {
		return boardMapper.getJoinCLub(cj_rider);
	}
	
	// 게시글 생성
	public void insertBoard(Board board) throws Exception {
		boardMapper.insertBoard(board);
	}
	
	// 게시글 조회
	public Board getBoardNum(int b_num) {
		return boardMapper.getBoardNum(b_num);
	}
	
	// 게시글 수정
	public void updateBoard(Board board) {
		boardMapper.updateBoard(board);
	}
	
	// 게시글 삭제
	public boolean deleteBoard(int b_num) {
		return boardMapper.deleteBoard(b_num);
	}
	
	//조회수 증가
	public void updateHit(int b_num) {
		boardMapper.updateHit(b_num);
	}
}
