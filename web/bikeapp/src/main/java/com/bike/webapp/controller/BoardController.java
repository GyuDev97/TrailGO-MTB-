package com.bike.webapp.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bike.webapp.dto.*;
import com.bike.webapp.service.*;


@Controller
public class BoardController {

	@Autowired
	private BoardService boardService;

	// 게시글 등록
	@RequestMapping(value = "/createBoard/check", method = RequestMethod.POST)
	public String boardCreate(Board board, MultipartFile uploadfile) throws Exception {

		String filename;
		if(!uploadfile.isEmpty()) {
			filename = com.bike.webapp.util.UploadFiles.safeName(uploadfile);
			board.setB_file(filename);
			System.out.println("파일있음");
		}else {
			System.out.println("파일없음");
		}
		
		
		/*
		 * String directory = "/home/ec2-user/data/club"; //String directory =
		 * "C:\\ServerFiles"; String filepath = Paths.get(directory,
		 * filename).toString();
		 */
		
		boardService.insertBoard(board);

		// Save the file locally
		/*
		 * BufferedOutputStream stream = new BufferedOutputStream(new
		 * FileOutputStream(new File(filepath))); stream.write(uploadfile.getBytes());
		 * stream.close();
		 */

		return "redirect:/community/club/"+board.getB_club();
	}

	// 글쓰기 페이지
	@RequestMapping(value = "/community/club/createBoard/{b_club}", method = RequestMethod.GET)
	public String moveCreateBoard(@PathVariable int b_club, Model model) {
		model.addAttribute("b_club", b_club);
		return "community/club/createBoard";
	}

	// 클럽 게시판 페이지
	@RequestMapping(value = "/community/club/club_board/{b_club}", method = RequestMethod.GET)
	public String moveClubBoard(@PathVariable int b_club, Model model) {
		List<Board> list = boardService.getBoardList(b_club);
		model.addAttribute("list", list);
		model.addAttribute("b_club", b_club); // 클럽 번호 전달
		return "community/club/club_board";
	}

	// 클럽 정보 가져오기
	@RequestMapping(value = "/community/club/get", method = RequestMethod.GET)
	@ResponseBody
	public int getClubNum(String cj_rider) {
		try {
			int result = boardService.getJoinCLub(cj_rider);
			return result;
		} catch (Exception e) {
			return 0;
		}
	}
}
