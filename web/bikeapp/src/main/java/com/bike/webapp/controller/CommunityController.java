package com.bike.webapp.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bike.webapp.dto.*;
import com.bike.webapp.service.*;


@Controller
public class CommunityController {
	@Autowired
	private CommunityService communityService;
	@Autowired
	private BoardService boardService;

	// 커뮤니티
	@RequestMapping(value = "/community", method = RequestMethod.GET)
	public String comunity() {
		// return "/community";
		return "community/community2";
	}

	// 커뮤니티 로그인 후
	@RequestMapping(value = "/community", method = RequestMethod.POST)
	public String comunity(String rider, Model model) {
		int club = 0;
		try {
			club = communityService.getJoinCLub(rider);
		} catch (Exception e) {

		}
		model.addAttribute("club", club);
		return "community/community2";
	}

	// 커뮤니티 클럽
	@RequestMapping(value = "/community/club", method = RequestMethod.GET)
	public String comunityclub() {
		// return "community/club/club";
		return "community/club/club2";
	}

	// 커뮤니티 클럽 게시판 글 상세
	@RequestMapping(value = "/community/club/boardGet", method = RequestMethod.GET)
	public String boardGet(@RequestParam("b_num") int b_num, Model model) {
		model.addAttribute("board", boardService.getBoardNum(b_num));
		boardService.updateHit(b_num);
		return "community/club/boardGet";
	}
	
	// 게시글 수정
	@RequestMapping(value = "/community/club/boardUpdate", method = RequestMethod.GET)
	public String modify(@RequestParam("b_num") int b_num, Board board, Model model) {
		boardService.updateBoard(board);
		model.addAttribute("board", boardService.getBoardNum(b_num));
		return "community/club/boardGet";
	}
	
	// 게시글 삭제
	@RequestMapping(value = "/community/club/boardDelete")
	public String delete(@RequestParam("b_num") int b_num) {
		Board b = boardService.getBoardNum(b_num);
		boardService.deleteBoard(b_num);
		return "redirect:/community/club/"+b.getB_club();
	}
	
	// 커뮤니티 클럽 게시판 글 수정
	@RequestMapping(value = "/community/club/modify", method = RequestMethod.GET)
	public String modify(@RequestParam("b_num") int b_num, Model model) {
		model.addAttribute("board", boardService.getBoardNum(b_num));
		
		return "community/club/modify";
	}

	// 커뮤니티 클럽 캘린더 이동
	@RequestMapping(value = "/community/club/calender/{c_num}", method = RequestMethod.GET)
	public String comunityclub2(@PathVariable int c_num, Model model) {

		model.addAttribute("club", communityService.getClubInfo(c_num));

		return "community/club/club_calender";
	}

	// 커뮤니티 클럽 게시판 이동
	@RequestMapping(value = "/community/club/{c_num}", method = RequestMethod.GET)
	public String comunityclubtest(@PathVariable int c_num, Model model) {

		model.addAttribute("club", communityService.getClubInfo(c_num));
		model.addAttribute("postlist", boardService.getBoardList(c_num));

		return "community/club/club_main";
	}

	// 커뮤니티 클럽 글쓰기 페이지
	@RequestMapping(value = "/community/club/write/{c_num}", method = RequestMethod.GET)
	public String comunityBoardWrite(@PathVariable int c_num, Model model) {

		model.addAttribute("club", communityService.getClubInfo(c_num));

		return "community/club/boardWrite";
	}

	// 커뮤니티 클럽 만들기 페이지
	@RequestMapping(value = "/community/club/create", method = RequestMethod.GET)
	public String moveClubCreatePage() {
		return "community/club/create";
	}

	// 커뮤니티 클럽 만들기
	@RequestMapping(value = "/community/club/create", method = RequestMethod.POST)
	public String clubcreate(Club club, MultipartFile uploadfile) throws Exception {

		String filename = com.bike.webapp.util.UploadFiles.safeName(uploadfile);
		String directory = "/home/ec2-user/data/club"; // 서버
		// String directory = "C:\\ServerFiles"; // 로컬
		String filepath = Paths.get(directory, filename).toString();

		club.setCb_image(filename);
		communityService.insertClub(club);
		Club_Join cb_join = new Club_Join();
		cb_join.setCj_rider(club.getCb_manager()); // cb_join 안에 club의 cb_manager를 삽입
		communityService.insertCJ(cb_join);
		// Save the file locally
		BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
		stream.write(uploadfile.getBytes());
		stream.close();

		return "community/club/success";
	}

	// 클럽명 중복 검사
	@RequestMapping(value = "/community/club/create/check", method = RequestMethod.GET)
	@ResponseBody
	public String checkClubName(String cb_name) {
		int result = 0;
		try {
			result = communityService.checkClubName(cb_name);
			if (result == 1) {
				return "fail";
			} else {
				return "success";
			}
		} catch (Exception e) {
			return "success";
		}
	}

	// 커뮤니티 클럽 가입 페이지
	@RequestMapping(value = "/community/club/join", method = RequestMethod.GET)
	public String moveClubBoard(Club cb_num, Model model) {
		List<Club> list = communityService.getClub(cb_num);
		model.addAttribute("list", list);
		model.addAttribute("cb_num", cb_num); // 클럽 번호 전달
		return "/community/club/join";
	}

	// 테스트
	@RequestMapping(value = "/community/club/testeeee", method = RequestMethod.GET)
	public String moveClubBoardssssss() {
		return "community/club/index";
	}

	// 커뮤니티 클럽 가입
	@RequestMapping(value = "/join/decision/{cb_num}/{cj_rider}", method = RequestMethod.GET)
	public String clubSignUp(@PathVariable int cb_num, @PathVariable String cj_rider, Model model) throws Exception {
		Club_Join cj = new Club_Join();
		cj.setCj_club(cb_num);
		cj.setCj_rider(cj_rider);
		communityService.signClub(cj);
		model.addAttribute("c_num", cb_num);
		return "/community/club/success";
	}

	// 클럽 게시판
	@RequestMapping(value = "/community/club/clubboard", method = RequestMethod.GET)
	public String clubboard() {
		return "community/club/myclub/clubboard";
	}

	// 클럽 게시판 검색
	@RequestMapping(value = "/community/club/myclub/clubboard/search", method = RequestMethod.GET)
	public String clubcreateget() {
		return "community/club/myclub/clubboard/search";
	}

	// 클럽 게시판 글쓰기
	@RequestMapping(value = "/community/club/myclub/clubboard/posting", method = RequestMethod.POST)
	public String clubcreatepost() {
		return "community/club/myclub/clubboard/posting";
	}

	// 클럽 캘린더 페이지로 이동
	@RequestMapping(value = "/community/club/calender", method = RequestMethod.GET)
	public String clubcalender() throws ParseException {
		return "community/club/calender";
	}

	// 클럽 캘린더 일정 조회
	@RequestMapping(value = "/community/calender/{cc_club}", method = RequestMethod.GET)
	@ResponseBody
	public List<CC_Event> getCC(@PathVariable int cc_club) {

		List<Club_Calender> list = communityService.getCCList(cc_club);

		List<CC_Event> result = new ArrayList<CC_Event>();
		for (int i = 0; i < list.size(); i++) {
			CC_Event cce = new CC_Event();

			cce.set_id(list.get(i).getCc_num());
			cce.setTitle(list.get(i).getCc_content());
			cce.setStart(list.get(i).getCc_start());
			cce.setEnd(list.get(i).getCc_end());
			cce.setUsername(list.get(i).getCc_rider());
			cce.setBackgroundColor(list.get(i).getCc_color());
			cce.setAllDay(list.get(i).getCc_allday() == 1 ? true : false);

			result.add(cce);
		}

		return result;
	}

	// 클럽 캘린더 일정 등록
	@RequestMapping(value = "/community/calender/{cc_club}", method = RequestMethod.POST)
	@ResponseBody
	public String postCC(@PathVariable int cc_club, CC_Event cc_e) {

		Club_Calender cc = new Club_Calender();
		cc.setCc_club(cc_club);
		cc.setCc_rider(cc_e.getUsername());
		cc.setCc_content(cc_e.getTitle());
		cc.setCc_start(cc_e.getStart());
		cc.setCc_end(cc_e.getEnd());
		cc.setCc_color(cc_e.getBackgroundColor());
		cc.setCc_allday(cc_e.isAllDay() ? 1 : 0);

		communityService.postCC(cc);

		return "success";
	}

	// 클럽 캘린더 일정 수정
	@RequestMapping(value = "/community/club/calender/{cc_club}", method = RequestMethod.PUT)
	@ResponseBody
	public String updateCC(@PathVariable int cc_club, CC_Event cc_e, int _id) {

		Club_Calender cc = new Club_Calender();
		cc.setCc_num(_id);
		cc.setCc_content(cc_e.getTitle());
		cc.setCc_start(cc_e.getStart());
		cc.setCc_end(cc_e.getEnd());
		cc.setCc_color(cc_e.getBackgroundColor());
		cc.setCc_allday(cc_e.isAllDay() ? 1 : 0);

		communityService.updateCC(cc);
		return "success";
	}

	// 클럽 캘린더 일정 삭제
	@RequestMapping(value = "/community/club/calender/{cc_club}", method = RequestMethod.DELETE)
	@ResponseBody
	public String deleteCC(@PathVariable int cc_club, int cc_num) {

		communityService.deleteCC(cc_num);

		return "success";
	}

	// SNS
	@RequestMapping(value = "/community/mutub", method = RequestMethod.GET)
	public String snsget(Principal principal, Model model) {
		
		List<Board> list = communityService.getSNSList(principal.getName());
		
		model.addAttribute("list", list);
		
		return "community/sns";
	}

	// SNS 게시글 등록
	@RequestMapping(value = "/community/sns", method = RequestMethod.POST)
	public String snspost(Board sns, MultipartFile file) throws IOException {
		
		if(!file.isEmpty()) {
			String filename = com.bike.webapp.util.UploadFiles.safeName(file);
	        String directory = "/home/ec2-user/data/sns/";
	        //String directory = "C:\\Users\\woolu\\Desktop\\workspace\\data\\img\\sns\\";
	        String filepath = Paths.get(directory, filename).toString();             
	        
	        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
	        stream.write(file.getBytes());
	        stream.close();
	        
	        sns.setB_file(filename);
		}
		
		communityService.postSNS(sns);
		
		return "redirect:/community/mutub";
	}
	
	// SNS 게시글 삭제
	@RequestMapping(value = "/community/sns/{b_num}", method = RequestMethod.GET)
	public String snsDelete(@PathVariable int b_num) throws IOException {
		
		communityService.deleteSNS(b_num);
		
		return "redirect:/community/mutub";
	}
	
	// SNS 게시글 수정
	@RequestMapping(value = "/community/sns/{b_num}", method = RequestMethod.POST)
	public String snsUPDATE(@PathVariable int b_num, Board board, MultipartFile file) throws IOException {
		
		board.setB_num(b_num);
		
		if( !file.isEmpty() ) {
			String filename = com.bike.webapp.util.UploadFiles.safeName(file);
			String directory = "/home/ec2-user/data/sns/";
	        //String directory = "C:\\Users\\woolu\\Desktop\\workspace\\data\\img\\sns\\";
	        String filepath = Paths.get(directory, filename).toString();             
	        
	        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
	        stream.write(file.getBytes());
	        stream.close();
			
	        board.setB_file(filename);
	        
	        communityService.updateSNS(board);
		} else {
			
			communityService.updateSNS2(board);
		}
		
		return "redirect:/community/mutub";
	}
	
	//SNS 댓글 조회
	@GetMapping(value="/data/reply/{re_board}")
	@ResponseBody
	public List<Reply> getReply(@PathVariable int re_board){
		return communityService.getReply(re_board);
	}
	
	//SNS 댓글 등록
	@PostMapping(value="/data/reply/{re_board}")
	public String postReply(@PathVariable int re_board, Reply reply, Principal principal) {
		
		reply.setRe_rider(principal.getName());
		reply.setRe_board(re_board);
		
		communityService.postReply(reply);
		
		return "redirect:/community/mutub";
	}
	
	//SNS 댓글 삭제
	@GetMapping(value="/data/reply/delete/{re_num}")
	public String deleteReply(@PathVariable int re_num){
		
		communityService.deleteReply(re_num);
		
		return "redirect:/community/mutub";
	}
	
	//SNS 댓글 삭제
	@PutMapping(value="/data/reply/{re_num}")
	@ResponseBody
	public String putReply(@PathVariable int re_num, String re_content){
		
		Reply reply = new Reply();
		reply.setRe_num(re_num);
		reply.setRe_content(re_content);
		
		communityService.putReply(reply);
		
		return "success";
	}

	// 중고거래 게시판 이동
	@RequestMapping(value = "/community/market", method = RequestMethod.GET)
	public String tradeget(Model model, Principal principal) {
		
		model.addAttribute("saler", communityService.getSalerInfo(principal.getName()));
		
		List<Goods> goodsList = communityService.getGoodsList();
		for(int i=0;i<goodsList.size();i++) {
			if( goodsList.get(i).getG_content().length() > 25 ) {
				goodsList.get(i).setG_content( goodsList.get(i).getG_content().substring(0, 25)+"..." );
			}
		}
		model.addAttribute("goodsList", goodsList);
		
		return "community/market";
	}
	
	// 거래물품 상세보기
	@RequestMapping(value = "/community/market/{g_num}", method = RequestMethod.GET)
	public String goGoodsDetailPage(@PathVariable int g_num, Model model) {
		
		communityService.updateGoodsHit(g_num);
		
		
		
		Goods goods = communityService.getGoodsDetail(g_num);
		Trade_Info ti = new Trade_Info();
		
		Goods goods2 = new Goods();
		goods2.setG_rider(goods.getG_rider());
		
		Integer cnt;
		
		model.addAttribute("rider", communityService.getUserInfo( goods.getG_rider() ));
		
		goods2.setG_status(0);

		cnt = communityService.getUserTradeInfo(goods2);
		if(cnt==null) {
			cnt=0;
		}
		ti.setYet(cnt);
		
		goods2.setG_status(1);
		cnt = communityService.getUserTradeInfo(goods2);
		if(cnt==null) {
			cnt=0;
		}
		ti.setIng(cnt);
		
		goods2.setG_status(2);
		cnt = communityService.getUserTradeInfo(goods2);
		if(cnt==null) {
			cnt=0;
		}
		ti.setEd(cnt);
		
		model.addAttribute("trade_info",  ti);
		
		model.addAttribute("goods",  goods);
		
		return "community/goodsDetail";
	}
	
	//거래물품 관심 등록
	@RequestMapping(value="/community/goodslike/{g_num}", method = RequestMethod.POST)
	@ResponseBody
	public String updateGoodsLike(@PathVariable int g_num, Principal principal) {
		Goods_like gl = new Goods_like();
		gl.setGl_goods(g_num);
		gl.setGl_rider(principal.getName());
		try {
			communityService.updateGoodsLike(gl);
		} catch (Exception e) {
			return "fail";
		}
		Goods goods = communityService.getGoodsDetail(g_num);
		
		return ""+goods.getG_like();
	}
	
	//물품 구매시
	@RequestMapping("/community/buyGoods/{g_num}")
	public String buyGoods(@PathVariable int g_num, Model model, Principal principal) {
		
		Goods goods = communityService.getGoodsDetail(g_num);
		Rider rider = communityService.getUserInfo(principal.getName());
		
		model.addAttribute("goods", goods);
		model.addAttribute("buyer", rider);
		
		return "community/kakaoPay";
	}
	
	//물품 구매 후 - 성공
	@RequestMapping("/community/buyGoods/success/{g_num}/{g_rider}")
	public String completeBuyGoods(@PathVariable int g_num, @PathVariable String g_rider) {
		
		Goods_like gl = new Goods_like();
		
		gl.setGl_goods(g_num);
		gl.setGl_rider(g_rider);
		
		communityService.updateTradeEnd(gl);
		communityService.updateTradeEnd2(gl);
		
		return "redirect:/community/market/"+g_num;
	}
	
	//물품 구매 후 - 성공
	@RequestMapping("/community/buyGoods/fail/{g_num}/{g_rider}")
	public String completefailBuyGoods(@PathVariable int g_num, @PathVariable String g_rider) {
		
		return "redirect:/community/market/"+g_num;
	}

	//거래 물품 등록하기 페이지로 이동
	@RequestMapping(value = "/community/regist/goods", method = RequestMethod.GET)
	public String tradeposting() {
		return "community/reg_goods";
	}
	
	//거래 물품 등록하기
	@RequestMapping(value = "/community/regist/goods", method = RequestMethod.POST)
	public String tradepost(Goods goods, MultipartFile img_file, Principal principal) throws IOException {
		
		if(!img_file.isEmpty()) {
			String filename = com.bike.webapp.util.UploadFiles.safeName(img_file);
	        String directory = "/home/ec2-user/data/goods/";
	        //String directory = "C:\\Users\\woolu\\Desktop\\workspace\\data\\img\\goods\\";
	        String filepath = Paths.get(directory, filename).toString();             
	        
	        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
	        stream.write(img_file.getBytes());
	        stream.close();
	        
	        goods.setG_image(filename);
		}
		
		goods.setG_rider(principal.getName());
		
		communityService.postGoods(goods);
		
		return "redirect:/community/market";
	}
	
	@RequestMapping(value = "/community/myMarket", method = RequestMethod.GET)
	public String myMarket(Principal principal, Model model) {
		
		model.addAttribute("ingGoodsList", communityService.getRiderGoodsList(principal.getName(), 0));
		
		model.addAttribute("edGoodsList", communityService.getRiderGoodsList(principal.getName(), 2));
		
		return "community/myMarket";
	}
	
	// 거래물품 상세보기
	@RequestMapping(value = "/community/myMarket/{g_num}", method = RequestMethod.GET)
	public String goMyGoodsDetailPage(@PathVariable int g_num, Model model) {
		
		model.addAttribute("goods", communityService.getGoodsDetail(g_num) );
		
		return "community/modifyGoods";
	}
	
	//거래 물품 수정하기
	@RequestMapping(value = "/community/myMarket/{g_num}", method = RequestMethod.POST)
	public String updatepost(@PathVariable int g_num, Goods goods, String price,MultipartFile img_file, Principal principal) throws IOException {
		
		goods.setG_num(g_num);
		
		if(price.contains("원")) {
			 System.out.println( price.substring(0,  price.lastIndexOf("원") ) );
			 goods.setG_price( Integer.parseInt( price.substring(0,  price.lastIndexOf("원") ) ) );
		}
		
		if(!img_file.isEmpty()) {
			String filename = com.bike.webapp.util.UploadFiles.safeName(img_file);
	        String directory = "/home/ec2-user/data/goods/";
	        //String directory = "C:\\Users\\woolu\\Desktop\\workspace\\data\\img\\goods\\";
	        String filepath = Paths.get(directory, filename).toString();             
	        
	        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
	        stream.write(img_file.getBytes());
	        stream.close();
	        
	        goods.setG_image(filename);
	        
	        communityService.updateGoods2(goods);
		}
		
		 communityService.updateGoods(goods);
		
		return "redirect:/community/myMarket/"+g_num;
	}
	
	
	// 거래물품 삭제
	@RequestMapping(value = "/community/removeMyGoods/{g_num}", method = RequestMethod.GET)
	public String removeMyGoods(@PathVariable int g_num) {
		
		communityService.removeGoods(g_num);
		
		return "redirect:/community/myMarket";
	}
	
	//이미지 로딩
	@GetMapping(value = "/data/img/{place}/{b_file}")
	public @ResponseBody byte[] getImage(@PathVariable String place ,@PathVariable String b_file) throws IOException {
		InputStream in = null;
	    in = new  BufferedInputStream(new FileInputStream("/home/ec2-user/data/"+place+"/"+b_file));
	    //in = new  BufferedInputStream(new FileInputStream("C:\\Users\\TACK\\Desktop\\finai_workspace\\data\\"+place+"\\"+b_file)); 
	    return IOUtils.toByteArray(in);
	}
}