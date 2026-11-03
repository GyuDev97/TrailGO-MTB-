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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.bike.webapp.dto.*;
import com.bike.webapp.service.*;
@Controller
public class RidingController {
	@Autowired
	private RidingService ridingService;

	// 코스 메뉴 진입
	@RequestMapping(value = "/riding/course", method = RequestMethod.GET)
	public String course(Principal principal, Model model) throws Exception {
		List<RidingRecord> rrlist = ridingService.getRidingRecord(principal.getName());
		for(int i=0;i<rrlist.size();i++) {
			rrlist.get(i).setRr_gpx(rrlist.get(i).getRr_dateYYYYMMDD());
			int like = ridingService.getRR_Like(rrlist.get(i).getRr_num());
			rrlist.get(i).setRr_like(like);
		}//course.html에서 사용되지않는 gpx변수를 원하는 문자열을 표현하기 위해 사용
		List<RidingRecord> scraplist = ridingService.getScrapCourse(principal.getName());
		for(int i=0;i<scraplist.size();i++) {
			scraplist.get(i).setRr_gpx(scraplist.get(i).getRr_dateYYYYMMDD());
			int like = ridingService.getRR_Like(scraplist.get(i).getRr_num());
			scraplist.get(i).setRr_like(like);
		}
		model.addAttribute("rider", principal.getName());
		model.addAttribute("nickname", ridingService.getRiderNickName(principal.getName()));
		model.addAttribute("ridingrecords", rrlist);
		model.addAttribute("scrapcourses", scraplist);
		
		int ridingCount = ridingService.countRidingRecord(principal.getName());
		if(ridingCount>=5) {
			model.addAttribute("r_status", 5);
			model.addAttribute("recommand", ridingService.getRecommandCourse(principal.getName()));
		}else {
			model.addAttribute("r_status", ridingCount);
			model.addAttribute("recommand", ridingService.getBeginnerCourse() );
		}
		
		return "riding/course2";
	}
	
	// 메인화면 구성을 위한 최신주행기록 3개 조회
	@RequestMapping(value="/ridingrecordTop3", method=RequestMethod.GET)
	@ResponseBody
	public List<RidingRecord> getRidingRecordTop3(String rr_rider) throws Exception{
		List<RidingRecord> list = ridingService.getRidingRecordTop3(rr_rider);
		return list;
	}

	// RR_NUM으로 GPX파일 조회
	@RequestMapping(value="/getGpxByRR_Num", method = RequestMethod.GET)
	@ResponseBody
	public Gpx getGpxByRR_Num(int rr_num) throws Exception {
		String gpxFile = ridingService.getGpxFileByRR_Num(rr_num);
		Gpx gpx = new Gpx();
		if(gpxFile.contains("NEW")||gpxFile.contains("new")) {
			gpxFile = gpxFile.split("/")[1];
			gpx.setStatus("new");
		}
		makeGpx(gpx, gpxFile);
		gpx.setRr_num(rr_num);
		return gpx;
		
	}
	
	//RR_NUM으로 RIDINGRECORD 조회
	@RequestMapping(value="/getRidingRecordByRR_Num", method = RequestMethod.GET)
	@ResponseBody
	public RidingRecord getRidingRecordByRR_Num(int rr_num) throws Exception {
		RidingRecord rr = ridingService.getRidingRecordDetail(rr_num);
		int like = ridingService.getRR_Like(rr_num);
		rr.setRr_like(like);
		return rr;
	}
	
	//RR_NUM으로 해시태그 조회
	@RequestMapping("/info/riding/tag/{rr_num}")
	@ResponseBody
	public List<Tag_Status> getTagList(@PathVariable int rr_num) throws Exception {
		return ridingService.getTagList(rr_num);
	}
	
	//해시태그 삭제
	@RequestMapping(value="/info/riding/tag/{ts_num}", method = RequestMethod.DELETE)
	@ResponseBody
	public String removeTag(@PathVariable int ts_num) {
		
		ridingService.removeTag(ts_num);
		
		return "success";
	}
	
	//해시태그 등록
	@RequestMapping(value="/info/riding/tag", method = RequestMethod.POST)
	@ResponseBody
	public String insertTag(Tag_Status ts ) {
		
		if( ! ts.getTs_tag().substring(0, 1).equals("#") ) {
			ts.setTs_tag( "#"+ts.getTs_tag() );
		}
		
		try {
			ridingService.insertTag(ts);
		} catch (Exception e) {
			return "fail";
		}
		
		return "success";
	}

	// 사용자 주행 기록 공개비공개
	@RequestMapping(value = "/riding/update", method = RequestMethod.PUT)
	@ResponseBody
	public String updateRidingRecord(int rr_num, int rr_open){
		try {
			ridingService.updateRidingRecord(rr_num, rr_open);
		} catch (Exception e) {
			return "fail";
		}
		
		if(rr_open==1) {
			return "y";
		}else {
			return "n";
		}
	}
	
	// 주행 기록 코스 명 변경
	@RequestMapping(value = "/ridingrecord/rr_name", method = RequestMethod.PUT)
	@ResponseBody
	public String updateRidingRecordName(int rr_num, String rr_name) throws Exception{
		ridingService.updateRidingRecordName(rr_num, rr_name);
		return "success";
	}
	
	// 코스 조회
	@RequestMapping(value = "/riding/course/list", method = RequestMethod.GET)
	public @ResponseBody List<RidingRecord> getCourse(@RequestParam Map<String, Object> test) throws Exception {
		String keyword = (String) test.get("keyword");
		List<RidingRecord> data = ridingService.getCourses(keyword);
		return data; 
	}

	// 사용자 스크랩 코스 조회
	@RequestMapping(value = "/riding/scrap", method = RequestMethod.GET)
	public @ResponseBody List<RidingRecord> getScrapCourse(String rr_rider) throws Exception {
		return ridingService.getScrapCourse(rr_rider);
	}

	// 사용자 스크랩 코스 등록
	@RequestMapping(value = "/riding/scrap/{ss_rnum}/{ss_rider}", method = RequestMethod.POST)
	@ResponseBody
	public String postScrapCourse(@PathVariable int ss_rnum, @PathVariable String ss_rider) {
		try {
			List<RidingRecord> list = ridingService.getRidingRecord(ss_rider);
			for(int i=0;i<list.size();i++) {
				if(ss_rnum==list.get(i).getRr_num()) {
					return "failMyRR";
				}
			}
		} catch (Exception e1) {}
		try {
			ridingService.postScrapCourse(ss_rider, ss_rnum);
			return "success";
		} catch (Exception e) {
			return "fail";
		}
	}

	// 사용자 스크랩 코스 삭제
	@RequestMapping(value = "/riding/scrap", method = RequestMethod.DELETE)
	@ResponseBody
	public String deleteScrapCourse(String ss_rider, int ss_rnum) throws Exception {
		
		ridingService.deleteScrapCourse(ss_rider, ss_rnum);
		 
		return "success";
	}

	// 위험 지역 조회
	@RequestMapping(value = "/riding/DA", method = RequestMethod.GET)
	public @ResponseBody List<DangerousArea> getDangerousArea() throws Exception {
		return ridingService.getDangerousArea();
	}
	
	// 위험 지역 조회
		@RequestMapping(value = "/riding/NO", method = RequestMethod.GET)
		public @ResponseBody List<Nomtb> getNoMtbArea() throws Exception {
			return ridingService.getNoMtbArea();
		}
		

	// 사용자 등록 위험 지역 조회
	@RequestMapping(value = "/riding/DA/check", method = RequestMethod.GET)
	public @ResponseBody List<DangerousArea> getUserDangerousArea(String rr_rider) throws Exception {
		return ridingService.getUserDangerousArea(rr_rider);
	}
	
	// 위험지역 등록 신청
	@RequestMapping(value = "/riding/da", method = RequestMethod.POST)
	@ResponseBody
	public  String postDangerousArea(DangerousArea da) throws Exception {
		
		ridingService.postDangerousArea(da);
		
		return "success";
		
	}
	
	// 위험지역 등록 신청
	@RequestMapping(value = "/dangerousArea", method = RequestMethod.POST)
	@ResponseBody
	public  String regDangerousArea(DangerousArea da) throws Exception {
		
		ridingService.postDangerousArea(da);
		
		return "success";
		
	}
	
	//입산통제지역 신청
	
	@RequestMapping(value = "/nomtbAction", method = RequestMethod.POST)
	@ResponseBody
	public  String noMtb(HttpServletRequest request, @RequestParam("file") MultipartFile file) throws Exception {
		
		Nomtb nomtb = new Nomtb();
		nomtb.setPr_lattude(request.getParameter("nomtb_lot"));
		nomtb.setPr_longitude(request.getParameter("nomtb_lon"));
		nomtb.setPr_addr(request.getParameter("nomtb_addr"));
		nomtb.setPr_content(request.getParameter("nomtb_content"));
		nomtb.setPr_image(com.bike.webapp.util.UploadFiles.safeName(file));
		nomtb.setPr_rider(request.getParameter("da_rider"));
		
		
		ridingService.postnoMtb(nomtb);

		
		return "success";
		
	}
	
	
		
	// 사용자 등록 위험 지역 삭제
	@RequestMapping(value = "/riding/DA/delete", method = RequestMethod.DELETE)
	public @ResponseBody String deleteDangerousArea(@RequestBody DangerousArea da) throws Exception {
		/*
		 * System.out.println(da.toString());
		 * memberService.deleteDangerousArea(da.getDa_num());
		 */
		return "success";
	}
	// 다른 사용자 등록 위험 지역 해지 신청
	@RequestMapping(value = "/riding/DA/delete", method = RequestMethod.POST)
	public @ResponseBody String postNO_DangerousArea(@RequestBody No_Danger nd) throws Exception {
		/*
		 * System.out.println(nd.getNd_num()+"/"+nd.getNd_rider()+"/"+nd.getNd_content()
		 * +"/"+nd.getNd_image()); try { memberService.postNoDanger(nd); return
		 * "success"; } catch (Exception e) { return "fail"; }
		 */
		return "success";
	}

	// 내 코스 공유
	@RequestMapping(value = "/riding/course/my/share", method = RequestMethod.GET)
	public String sharecourseget() {

		return "/riding/course/my/share";
	}

	// 내 코스 리뷰
	@RequestMapping(value = "/riding/course/my/review", method = RequestMethod.POST)
	public String reviewpost() {

		return "/riding/course/my/review";
	}

	// 내 코스 리뷰
	@RequestMapping(value = "/riding/myreview", method = RequestMethod.GET)
	public String myreviewget(int result, String courseId) {

		return "/riding/myreview";
	}

	// 라이딩 넘버로 코스 리뷰 조회
	@RequestMapping(value = "/riding/reviews/{cr_rnum}", method = RequestMethod.GET)
	@ResponseBody
	public List<Course_Review> getReviews(@PathVariable int cr_rnum) throws Exception {

		return ridingService.getCourseReviews(cr_rnum);
	}
		
	// 코스 리뷰 등록
	@RequestMapping(value = "/riding/review", method = RequestMethod.POST)
	@ResponseBody
	public String myreviewpost(@RequestPart MultipartFile files, Course_Review cr ) throws Exception {
		
		if(!files.isEmpty()) {
			String filename = com.bike.webapp.util.UploadFiles.safeName(files);
	        String directory = "/home/ec2-user/data/review/";
	        //String directory = "C:\\Users\\woolu\\Desktop\\workspace\\data\\review\\";
	        String filepath = Paths.get(directory, filename).toString();             
	        
	        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)));
	        stream.write(files.getBytes());
	        stream.close();
	        
	        cr.setCr_images(filename);
		}
		// 현재날짜 timestamp
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// formatter.setTimeZone(TimeZone.getTimeZone("GMT+09"));
		Calendar cal = Calendar.getInstance();
		String today = null;
		today = formatter.format(cal.getTime());
		Timestamp ts = Timestamp.valueOf(today);
		
		cr.setCr_time(ts);
		
		ridingService.postCourseReview(cr);
		return "success";
		
	}

	// 코스 리뷰 삭제
	@RequestMapping(value = "/riding/review", method = RequestMethod.DELETE)
	@ResponseBody
	public String deleteCourseReview(int cr_num) throws Exception {
		ridingService.deleteCourseReview(cr_num);
		return "success";
	}
	
	//리뷰 수정
	@RequestMapping(value = "/riding/review", method = RequestMethod.PUT)
	@ResponseBody
	public String updateCourseReview(int cr_num, String cr_content) throws Exception {
		ridingService.updateCourseReview(cr_num, cr_content);
		return "success";
	}

	// 코스 검색 진입
	@RequestMapping(value = "/riding/search", method = RequestMethod.GET)
	public String coursesearch() {
		//return "riding/search";
		return "riding/search2";
	}
	// 코스 검색 진입
	@RequestMapping(value = "/riding/search2", method = RequestMethod.GET)
	public String coursesearch2() {
		return "riding/search2";
	}
	
	//코스 추천
	@RequestMapping(value = "/riding/like", method = RequestMethod.POST)
	@ResponseBody
	public String postLS(Like_Status ls) {
		
		try {
			
			List<RidingRecord> list = ridingService.getRidingRecord(ls.getLs_rider());
			
			for(int i=0;i<list.size();i++) {
				if(list.get(i).getRr_num()==ls.getLs_rnum()) {
					return "myrr";
				}
			}
			
			ridingService.postLS(ls);
			
		} catch (Exception e) {
			return "already";
		}
		
		return "success";
	}
	
	//코스 추천 취소
	@RequestMapping(value = "/riding/like", method = RequestMethod.DELETE)
	@ResponseBody
	public String deleteLS(Like_Status ls) {
		
		ridingService.deleteLS(ls);
		
		return "success";
	}
		
	// 코스 인원모집
	@RequestMapping(value = "/riding/course/member", method = RequestMethod.POST)
	public String coursesearch(int result, String courseId, String courseImage, String area, String time, String member,
			String content) {
		System.out.println(result);
		System.out.println(courseId);
		System.out.println(courseImage);
		System.out.println(area);
		System.out.println(time);
		System.out.println(member);
		System.out.println(content);
		return "/riding/course/member";
	}

	// 인원모집
	@RequestMapping(value = "/riding/comeon", method = RequestMethod.GET)
	public String comeon() {

		return "/riding/comeon";
	}
	
	//코스명으로 검색
	@RequestMapping(value="/info/riding/search/{nowPage}/{rr_name}", method = RequestMethod.GET)
	@ResponseBody
	public List<RidingRecord> getSearchCourse(@PathVariable int nowPage, @PathVariable String rr_name){
		rr_name = "%"+rr_name+"%";
		
		
		int total = ridingService.cnt_searchCourseName(rr_name);
		
		PagingVO vo = new PagingVO(total, nowPage, 5);
		
		vo.setName(rr_name);
		List<RidingRecord> result = ridingService.searchCourseName(vo);
		
		//rr_time > 시작페이지 rr_breaktime 마지막페이지로 사용
		if(total!=0) {
			result.get(0).setRr_time(vo.getStartPage());
			result.get(0).setRr_breaktime(vo.getEndPage());
		}
		
		return result;
	}
	
	private void makeGpx(Gpx gpx, String gpxFile) throws Exception {
		String path = "D:/github/bikeapp/web/bikeapp/src/main/resources/static/uploads/riding/gpx/"+gpxFile;
		//String path = "C:\\Users\\woolu\\Desktop\\workspace\\data\\gpx\\"+gpxFile;
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
	//라이딩기록 전체 평균정보
	@RequestMapping(value="/info/riding/totalavg", method = RequestMethod.GET)
	@ResponseBody
	public Avg_RidingRecord getAVGRR(Principal principal) {
		return ridingService.getAVGRR(principal.getName());
	}
	
	//맞춤형 추천 코스 조회
	@RequestMapping(value = "/info/riding/recom/{rr_num}", method = RequestMethod.GET)
	@ResponseBody
	public Recom_Course getRecomCourse(@PathVariable int rr_num) {
		return ridingService.getRecomCourse(rr_num);
	}
	
	//이미지 로딩
	@GetMapping(value = "/image/review/{imageFile}")
	public @ResponseBody byte[] getImage(@PathVariable String imageFile) throws IOException {
		InputStream in = null;
	    in = new  BufferedInputStream(new FileInputStream("/home/ec2-user/data/review/"+imageFile)); 
	    //in = new  BufferedInputStream(new FileInputStream("C:\\Users\\TACK\\Desktop\\study\\review\\"+imageFile)); 
	    //in = new  BufferedInputStream(new FileInputStream("C:\\Users\\woolu\\Desktop\\workspace\\data\\review\\"+imageFile)); 
	    return IOUtils.toByteArray(in);
	}
		
	//이미지 업로드
	private void uploadImage(MultipartFile uploadfile) throws IOException {
		String filename = com.bike.webapp.util.UploadFiles.safeName(uploadfile);
		String directory = "/home/ec2-user/data/review";
		//String directory = "C:\\Users\\TACK\\Desktop\\study\\review";
		String filepath = Paths.get(directory, filename).toString();
		
		// Save the file locally
		BufferedOutputStream stream =
				new BufferedOutputStream(new FileOutputStream(new File(filepath)));
		stream.write(uploadfile.getBytes());
		stream.close();
	}
}
