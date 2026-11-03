package com.bike.webapp.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bike.webapp.dto.*;
import com.bike.webapp.service.*;


@Controller
public class EntertainController {

	@Autowired
	private EntertainmentService entertainmentService;

	// 엔터테인먼트 메인
	@RequestMapping(value="/entertainment", method = RequestMethod.POST)
	public String competitions2(String rider, Model model) {
		
		List<Competition> complist = entertainmentService.getRecentComp3(rider);
		List<Mission> missionlist = entertainmentService.getCompleteMission(rider);
		
		model.addAttribute("complist", complist);
		model.addAttribute("missionlist", missionlist);
		
		return "entertainment/main";
	}
	
	// 엔터테인먼트 가이드
	@RequestMapping(value="/entertainment/guide")
	public String guide() {
		return "entertainment/guide";
	}
	
	// 경쟁전
	@RequestMapping(value="/entertainment/competition", method = RequestMethod.GET)
	public String competitions(Principal principal, Model model) {
		List<Competition> complist = entertainmentService.getRecentComp4(principal.getName());
		List<Competition> compinglist = entertainmentService.getIngComp2();
		List<Competition> compendlist = entertainmentService.getEndComp2();
		
		model.addAttribute("complist", complist);
		model.addAttribute("comping1", compinglist.get(0));
		model.addAttribute("comping2", compinglist.get(1));
		model.addAttribute("compend1", compendlist.get(0));
		model.addAttribute("compend2", compendlist.get(1));

		return "entertainment/competition";
	}
	
	// 경쟁전 더보기 페이지
	@RequestMapping(value="/entertainment/competition/history", method = RequestMethod.GET)
	public String competitiongallery() {
		return "entertainment/competitiongallery";
	}
	
	// 경쟁전 상세보기 페이지
	@RequestMapping(value="/entertainment/competition/{comp_num}", method = RequestMethod.GET)
	public String competitioDetail(@PathVariable int comp_num, Model model) {
		
		model.addAttribute("comp", entertainmentService.getComp(comp_num));
		
		return "entertainment/competitiondetail";
	}
	
	//경쟁전 코스 좌표 요청
	@RequestMapping(value="/getInfoByGpxFile/{gpxfile}", method = RequestMethod.GET)
	@ResponseBody
	public Gpx getGpxByRR_Num(@PathVariable String gpxfile) throws Exception {
		Gpx gpx = new Gpx();
		makeGpx(gpx, gpxfile);
		return gpx;
	}

	//경쟁전 기록 페이지
	@RequestMapping("/entertainment/history")
	public String history() {
		return "entertainment/history";
	}

	// 경쟁전 기록보기
	@RequestMapping("/entertainment/history/test")
	public @ResponseBody List<Competition> getCompetition() throws Exception {
		return entertainmentService.getCompetition();
	}

	// 미션 페이지
	@RequestMapping(value="/entertainment/mission", method = RequestMethod.GET)
	public String missions(Principal principal, Model model) {
		
		model.addAttribute("missionList", entertainmentService.getCompleteMission(principal.getName()));
		model.addAttribute("nomissionList", entertainmentService.getNoCompleteMission(principal.getName()));
		
		return "entertainment/mission";
	}
	
	//미션 상세보기 페이지
	@RequestMapping(value = "entertainment/mission/{m_num}", method = RequestMethod.GET)
	public String missionDetailPage(@PathVariable int m_num, Model model) {
		
		model.addAttribute("mission", entertainmentService.getMissionDetail(m_num));
		
		List<EntertainRider> list = entertainmentService.getER_List(m_num);
		
		int success_cnt = list.size();
		int total_cnt = entertainmentService.getTotalRiders();
		
		double success_rate = (double) success_cnt / (double) total_cnt;
		
		String rate =( (int) Math.floor((success_rate*100)) )+"%";
		
		
		model.addAttribute("list", list);
		model.addAttribute("rate", rate);
		
		return "entertainment/missiondetail";
	}

	// 미션 데이터
	@RequestMapping("/entertainment/missions/test")
	public @ResponseBody List<Mission> getMission() throws Exception {
		return entertainmentService.getMission();
	}

	// 미션 완료확인 페이지
	@RequestMapping("/entertainment/missionComplete")
	public String missionComplete() {
		return "entertainment/missionComplete";
	}

//	// 진행중인 경쟁전 데이터
//	@RequestMapping("/entertainment/compIng/test")
//	public @ResponseBody List<CompIng> getCompIng() throws Exception {
//		return entertainmentService.getCompIng();
//	}
	
	// 진행중인 경쟁전 데이터
	@RequestMapping(value = "/entertainment/compIng" , method = RequestMethod.GET)
	public String getCompIng(Model model) throws Exception {
		List<CompIng> list = entertainmentService.getCompIng();
		model.addAttribute("list", list);
		return "entertainment/compIng";
	}
	
	
	//경쟁전 상세보기 데이터
	@RequestMapping(value = "/entertainment/compDetail" , method = RequestMethod.GET)
	public String getCompDetail(Model model) throws Exception {
		List<CompIng> list = entertainmentService.getCompIng();
		model.addAttribute("list", list);
//		switch (list.size()) {
//		case 0: 
//					
//		}
		return "entertainment/compDetail";
	}

	// 미션 등록페이지
	@RequestMapping("/entertainment/missionUpload")
	public String missionUpload() {
		return "entertainment/missionUpload";
	}

	// 미션 등록
	@RequestMapping(value = "/entertainment/missionUpload", method = RequestMethod.POST)
	public String fileUpload(Mission mission) throws Exception {
		entertainmentService.missionUpload(mission);
		return "entertainment/success";

	}

//	 // 배지 등록 가능 체크 페이지
    @RequestMapping(value = "/entertainment/badgeCheck", method = RequestMethod.GET)
    public String getBadgeCheck(Model model, Principal principal, Authentication authentication) throws Exception {
       int list = entertainmentService.getBadgeCheck(principal.getName());
       model.addAttribute("list", list);
       UserDetails userDetails = (UserDetails) authentication.getPrincipal();
       String rider = userDetails.getUsername();
       
       model.addAttribute("rider", rider);
       model.addAttribute("badgeList",entertainmentService.getBadge(rider));
       model.addAttribute("cbadgeList",entertainmentService.getCustomBadge(rider));
       return "entertainment/badgeCheck";
    }
    
    
	// 배지 등록 페이지
	@RequestMapping("/entertainment/badgeUpload")
	public String badgeUpload() {
		return "entertainment/badgeUpload";
	}
	


	// 배지 이미지, 값 등록
		@RequestMapping(value = "/entertainment/badgeUpload/test", method = RequestMethod.POST)
		public String badgeUpload(@RequestPart MultipartFile files, CustomBadge badge, Principal principal) throws Exception {
			String filename = com.bike.webapp.util.UploadFiles.safeName(files);
//			String directory = "C:\\ServerFiles\\cbadge"; //로컬
			String directory = "/home/ec2-user/data/cbadge"; //서버
			String filepath = Paths.get(directory, filename).toString();
			badge.setCbg_image(filename);
			badge.setCbg_id(principal.getName());
			entertainmentService.BadgeUpload(badge);

			BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
			stream.write(files.getBytes());
			stream.close();
			entertainmentService.pricePoint(principal.getName());

			return "entertainment/success";

		}
	
	// 외부 이미지 가져오기
	@GetMapping(value = "/imagetest")
	public @ResponseBody byte[] getImage() throws IOException {
		InputStream in = null;
		
		in = new BufferedInputStream(new FileInputStream("C:\\Users\\zfrzf\\Desktop\\badge.png"));
		return IOUtils.toByteArray(in);
	}
	
	private void makeGpx(Gpx gpx, String gpxFile) throws Exception {
		String path = "/home/ec2-user/data/gpx/"+gpxFile;
		//String path = "C:\\Users\\TACK\\Desktop\\study\\"+gpxFile;
		File file = new File(path);
		String txt = "";
		FileInputStream fis = new FileInputStream(file); 
		while(true) { 
			int res = fis.read(); 
			if(res<0) { 
				break; 
			}else { 
				txt += ((char)res);
			}
		}
		fis.close();
		gpx.setting(txt);
	}
	
	//랭킹조회 - 5등까지
	@GetMapping("/info/comp/rank5/{comp}")
	@ResponseBody
	public List<Club> getRank5(@PathVariable int comp){
		return entertainmentService.getRank5(comp);
	}
	
	//랭킹조회 - 전체
	@GetMapping("/info/comp/rank/{comp}")
	@ResponseBody
	public List<Club> getRank(@PathVariable int comp){
		return entertainmentService.getRank(comp);
	}
	
	//이미지 로딩
		@GetMapping(value = "/entertainment/img/{path}/{imageFile}")
		public @ResponseBody byte[] getImage(@PathVariable String path, @PathVariable String imageFile) throws IOException {
			InputStream in = null;
		    in = new  BufferedInputStream(new FileInputStream("/home/ec2-user/data/"+path+"/"+imageFile)); //서버
//		    in = new  BufferedInputStream(new FileInputStream("C:\\ServerFiles\\"+path+"\\"+imageFile)); //로컬
		    return IOUtils.toByteArray(in);
		}
}
