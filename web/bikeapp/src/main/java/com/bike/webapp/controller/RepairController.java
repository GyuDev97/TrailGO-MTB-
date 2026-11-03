package com.bike.webapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bike.webapp.dto.*;
import com.bike.webapp.service.*;
@Controller
public class RepairController {
	@Autowired
	private RepairService repairService;

	// 정비 메인
	@RequestMapping(value = "/repair", method = RequestMethod.POST)
	public String repair(String rider, Model model) {
		int type = repairService.getType(rider);
		if (type == 1) {
			model.addAttribute("qnaList", repairService.getQNA(rider));
			model.addAttribute("applyList", repairService.getRepairApply(rider));
			model.addAttribute("noQnaList", repairService.getNoAnserQNA(rider));
			model.addAttribute("noApplyList", repairService.getNoConfirmRepairApply(rider));
			model.addAttribute("shop", repairService.getRepairShopDetail(rider));
			return "repair/shop/main";
		}else {
			model.addAttribute("qnaList", repairService.rider_getQNA(rider));
			model.addAttribute("applyList", repairService.rider_getRepairApply(rider));
			model.addAttribute("rider", repairService.getRiderInfo(rider));
			return "repair/main";
		}
		
	}

	// 정비 가이드
	@RequestMapping(value = "/repair/guide", method = RequestMethod.GET)
	public String guide(int result) {
		return "repair/guide";
	}

	// 정비소 검색
	/*
	 * @RequestMapping(value = "/repair/search", method = RequestMethod.GET) public
	 * String repairSearch(int result, String zipcode) { return "repair/search"; }
	 */
	
	// 정비소 검색 - 임시
	@RequestMapping(value = "/repair/search", method = RequestMethod.GET)
	public String repairSearch(Model model) {
		model.addAttribute("shopList", repairService.getRepairShop("%북구%"));
		return "repair/search";
	}
	
	// 정비소 검색 - 주소로
	@RequestMapping(value = "/repair/shop_info/{area}", method = RequestMethod.GET)
	@ResponseBody
	public List<Repair_Shop> repairSearch(@PathVariable String area) {
		return repairService.getRepairShop("%"+area+"%");
	}
	
	// 정비소 상세 보기
	@RequestMapping(value = "/repair/search/{rs_id}", method = RequestMethod.GET)
	public String repairDetail(@PathVariable String rs_id, Model model) {
		model.addAttribute("shop", repairService.getRepairShopDetail(rs_id));
		model.addAttribute("reviewList", repairService.getRepairHistory(rs_id));
		return "repair/shopDetail";
	}
	
	// 문의 작성하기 페이지로 이동
	@RequestMapping(value = "/repair/qna/{rs_id}", method = RequestMethod.GET)
	public String rider_writeQNA(@PathVariable String rs_id, Model model) {
		model.addAttribute("shop", repairService.getRepairShopDetail(rs_id));
		model.addAttribute("page", "qna");
		return "repair/repairWrite";
	}
	
	// 신청 작성하기 페이지로 이동
	@RequestMapping(value = "/repair/apply/{rs_id}", method = RequestMethod.GET)
	public String rider_writeApply(@PathVariable String rs_id, Model model) {
		model.addAttribute("shop", repairService.getRepairShopDetail(rs_id));
		model.addAttribute("page", "apply");
		return "repair/repairWrite";
	}
	
	// 문의 등록 및 정비소 상세 보기로 이동
	@RequestMapping(value="/repair/qna/{rs_id}", method = RequestMethod.POST)
	public String rider_postQNA(@PathVariable String rs_id, Qna qa, Model model) {
		model.addAttribute("shop", repairService.getRepairShopDetail(rs_id));
		model.addAttribute("reviewList", repairService.getRepairHistory(rs_id));
		
		qa.setQa_shop(rs_id);
		repairService.postRider_QNA(qa);
		
		return "repair/shopDetail";
	}
	
	// 신청 등록 및 정비소 상세 보기로 이동
	@RequestMapping(value="/repair/apply/{rs_id}", method = RequestMethod.POST)
	public String rider_postApply(@PathVariable String rs_id, Repair_Apply ra, Model model) {
		model.addAttribute("shop", repairService.getRepairShopDetail(rs_id));
		model.addAttribute("reviewList", repairService.getRepairHistory(rs_id));
		
		ra.setRa_shop(rs_id);
		
		String str = ra.getRa_date();
		str = str.replaceAll("-", "");
		str = str.replaceAll("/", "");
		str = str.replaceAll("T", "");
		str = str.replaceAll(":", "");
		ra.setRa_date(str);
		
		repairService.postRider_Apply(ra);
		
		return "repair/shopDetail";
	}
	
	

	// 정비소 회원 정비 신청 조회
	@RequestMapping(value = "/repair/shop/apply/{ra_num}", method = RequestMethod.GET)
	public String s_repairApplyGet(@PathVariable int ra_num, Model model) {
		model.addAttribute("apply", repairService.getDetailRepairApply(ra_num));
		return "repair/shop/apply";
	}

	// 정비소 회원 정비 신청 수락
	@RequestMapping(value = "/repair/shop/apply", method = RequestMethod.POST)
	public String s_repairApplyPut(Repair_Apply ra, Model model) {
		ra.setRa_confirm(1);
		repairService.updateRepairApply(ra);
		model.addAttribute("rider", ra.getRa_shop());
		return "repair/shop/success";
	}
	
	// 정비소 회원 정비 신청 거절
	@RequestMapping(value = "/repair/shop/noapply", method = RequestMethod.POST)
	public String noapply(Repair_Apply ra, Model model) {
		ra.setRa_confirm(2);
		repairService.updateRepairApply(ra);
		model.addAttribute("rider", ra.getRa_shop());
		return "repair/shop/success";
	}

	// 정비소 회원 QnA 조회
	@RequestMapping(value = "/repair/shop/qna/{qa_num}", method = RequestMethod.GET)
	public String s_repairQnaGet(@PathVariable int qa_num, Model model) {
		model.addAttribute("qna", repairService.getDetailQna(qa_num));
		return "repair/shop/qna";
	}

	// 정비소 회원 QnA 답변
	@RequestMapping(value = "/repair/shop/qna", method = RequestMethod.POST)
	public String s_repairQnaPut(Qna qna, Model model) {
		repairService.updateDetailQna(qna);
		model.addAttribute("rider", qna.getQa_shop());
		return "repair/shop/success";
	}
}