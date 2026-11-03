package com.bike.webapp.controller.restful;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bike.webapp.dto.App_RidingRecord;
import com.bike.webapp.dto.Course;
import com.bike.webapp.dto.LoginInfo;
import com.bike.webapp.dto.RidingRecord;
import com.bike.webapp.auth.AppIdentity;
import com.bike.webapp.dto.*;
import com.bike.webapp.service.AndroidService;
import com.bike.webapp.service.MemberService;
import com.bike.webapp.service.*;
import com.bike.webapp.weather.RequestRateLimiter;
import com.bike.webapp.weather.WeatherCaptureService;
import com.bike.webapp.weather.WeatherSnapshotTokens;
import com.bike.webapp.weather.WeatherSnapshot;
import com.bike.webapp.weather.WeatherService;


@Controller
@RequestMapping(value = "/app/")
public class AndroidController {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${trailgo.storage-root}")
	private String storageRoot;
	
	//회원 Service객체
	@Autowired
	MemberService memberService;

	//앱 Service객체
	@Autowired
	AndroidService androidService;

	//라이딩 Service객체
	@Autowired
	private RidingService ridingService;

	//기록 시점 날씨 스냅샷 (기상청 API는 서버만 호출한다)
	@Autowired
	private WeatherCaptureService weatherCaptureService;

	//날씨 조회(저장 화면 미리보기) / 라이딩 사진
	@Autowired
	private WeatherService weatherService;

	//라이딩 시작 때 조회한 날씨를 저장 시점에 되돌려 받기 위한 토큰
	@Autowired
	private WeatherSnapshotTokens weatherSnapshotTokens;

	//날씨 조회 엔드포인트 호출 제한 (10분 창): 기기(IP)별, 회원별
	public static final long WEATHER_WINDOW_MS = 10L * 60L * 1000L;
	private RequestRateLimiter weatherIpLimiter = new RequestRateLimiter(30, WEATHER_WINDOW_MS);
	private RequestRateLimiter weatherRiderLimiter = new RequestRateLimiter(30, WEATHER_WINDOW_MS);
	private RequestRateLimiter weatherAnonymousLimiter = new RequestRateLimiter(12, WEATHER_WINDOW_MS);

	//프록시 뒤에서만 true: X-Forwarded-For의 첫 주소를 호출자 IP로 본다
	@Value("${trailgo.weather.trust-forwarded-for:false}")
	private boolean trustForwardedFor;

	@Autowired
	private RecordPhotoService recordPhotoService;

	public static final int MAX_MEMO_LENGTH = 500;
	
	//로그인은 소셜 로그인(/app/auth/**)만 있다. 본인 기록·사진 호출은 로그인 토큰으로 확인한 회원(AppIdentity)만 허용한다.

	//사용자 정보 가져오기
	@RequestMapping(value = "getLoginInfo/{LoginId}")
	public @ResponseBody LoginInfo getLoginInfo(@PathVariable String LoginId) throws Exception{
		return androidService.getLoginInfo(LoginId);
	}

	// 주행기록 등록(안드로이드)
	@RequestMapping(value = "riding/upload", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, String>> insertriding(HttpServletRequest request, @ModelAttribute RidingRecord record) {
		AppIdentity.requireOwner(request, record.getRr_rider());
		Map<String, String> result = new HashMap<String, String>();
		try {
			record.setRr_memo(normalizeMemo(record.getRr_memo()));
			androidService.insertRecord(record); //기록저장
			result.put("saved", "1");
			result.put("rr_num", String.valueOf(record.getRr_num()));
			// 날씨는 저장 이후(커밋 완료 후) 별도 스레드에서 붙인다. 실패해도 기록 저장 결과에는 영향이 없다.
			try {
				weatherCaptureService.captureAsync(record.getRr_num(), record.getRr_weather_token(), record.getRr_start_lat(),
						record.getRr_start_lon(), weatherTime(record.getRr_started_at()));
			} catch (RuntimeException weatherError) {
				log.warn("Weather capture skipped for record {}: {}", record.getRr_num(),
						weatherError.getClass().getSimpleName());
			}
			return ResponseEntity.ok(result);
		}catch(Exception e) {
			log.error("Riding record save failed for rider={}", record.getRr_rider(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	//내 라이딩 기록 목록 (앱이 실제로 호출하는 경로: GET /app/get/{loginId})
	@RequestMapping(value = "get/{loginId}", method = RequestMethod.GET)
	public @ResponseBody List<App_RidingRecord> getRidingRecord(HttpServletRequest request,
			@PathVariable("loginId") String loginId) throws Exception {
		AppIdentity.requireOwner(request, loginId);
		return androidService.readRecord(loginId);
	}

	//내 라이딩 기록 목록 (명시적 경로, 위와 동일한 응답)
	@RequestMapping(value = "riding/records/{riderId}", method = RequestMethod.GET)
	public @ResponseBody List<App_RidingRecord> getMyRidingRecords(HttpServletRequest request,
			@PathVariable("riderId") String riderId) throws Exception {
		AppIdentity.requireOwner(request, riderId);
		return androidService.readRecord(riderId);
	}

	//본인 기록 상세. 작성자(riderId)와 기록 번호가 모두 일치하지 않으면 404 (다른 사용자의 기록은 존재 여부도 알리지 않는다)
	@RequestMapping(value = "riding/records/{riderId}/{rrNum}", method = RequestMethod.GET)
	public ResponseEntity<RidingRecord> getMyRidingRecordDetail(HttpServletRequest request,
			@PathVariable("riderId") String riderId, @PathVariable("rrNum") int rrNum) {
		AppIdentity.requireOwner(request, riderId);
		RidingRecord record = androidService.getOwnRecord(riderId, rrNum);
		if (record == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		record.setPhotos(recordPhotoService.list(rrNum));
		return ResponseEntity.ok(record);
	}

	//메모 수정. 작성자와 번호가 모두 일치할 때만 반영된다 (비우면 메모가 삭제된다).
	@RequestMapping(value = "riding/records/{riderId}/{rrNum}/memo", method = RequestMethod.POST)
	public ResponseEntity<Map<String, String>> updateMemo(HttpServletRequest request,
			@PathVariable("riderId") String riderId, @PathVariable("rrNum") int rrNum,
			@RequestParam(value = "memo", required = false) String memo) {
		AppIdentity.requireOwner(request, riderId);
		if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		int updated = androidService.updateMemo(riderId, rrNum, normalizeMemo(memo));
		if (updated < 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Map<String, String> result = new HashMap<String, String>();
		result.put("updated", "1");
		return ResponseEntity.ok(result);
	}

	//사진 추가 (한 번에 한 장). 최대 10장, jpg/png/webp, 서버가 내용으로 형식을 판별한다.
	@RequestMapping(value = "riding/records/{riderId}/{rrNum}/photos", method = RequestMethod.POST)
	public ResponseEntity<RecordPhoto> addPhoto(HttpServletRequest request, @PathVariable("riderId") String riderId,
			@PathVariable("rrNum") int rrNum, @RequestParam("photo") MultipartFile photo) {
		AppIdentity.requireOwner(request, riderId);
		RecordPhotoService.AddResult result = recordPhotoService.add(riderId, rrNum, photo);
		switch (result.status) {
		case OK:
			return ResponseEntity.ok(result.photo);
		case NOT_FOUND:
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		case TOO_MANY:
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		case TOO_LARGE:
			return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
		case INVALID:
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		default:
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	//사진 삭제 (본인 기록의 사진만)
	@RequestMapping(value = "riding/records/{riderId}/{rrNum}/photos/{photoId}", method = RequestMethod.DELETE)
	public ResponseEntity<Map<String, String>> deletePhoto(HttpServletRequest request,
			@PathVariable("riderId") String riderId, @PathVariable("rrNum") int rrNum,
			@PathVariable("photoId") String photoId) {
		AppIdentity.requireOwner(request, riderId);
		if (!recordPhotoService.delete(riderId, rrNum, photoId)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Map<String, String> result = new HashMap<String, String>();
		result.put("deleted", "1");
		return ResponseEntity.ok(result);
	}

	//사진 보기. 공개 기록의 사진은 누구나, 비공개 기록의 사진은 로그인한 작성자만. 그 외에는 존재 여부도 알리지 않는다.
	@RequestMapping(value = "riding/photos/{photoId}", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getPhoto(HttpServletRequest request,
			@PathVariable("photoId") String photoId) throws IOException {
		RecordPhotoService.PhotoFile photo = recordPhotoService.open(photoId, AppIdentity.riderOf(request));
		if (photo == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(photo.contentType));
		headers.setCacheControl(photo.publicRecord
				? CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePublic().getHeaderValue()
				: CacheControl.maxAge(1, java.util.concurrent.TimeUnit.HOURS).cachePrivate().getHeaderValue());
		return new ResponseEntity<>(Files.readAllBytes(photo.path), headers, HttpStatus.OK);
	}

	//라이딩 날씨 조회(기상청 초단기실황). 항상 weather 객체를 돌려주며, 없으면 {"available": false}.
	//회원은 riderId가 실제 회원이어야 하고, 호출은 기기(IP)·회원별로 제한된다. 기상청 호출은 서버만 한다.
	@RequestMapping(value = "weather", method = RequestMethod.GET)
	public ResponseEntity<RecordWeather> getWeather(HttpServletRequest request,
			@RequestParam("lat") Double lat, @RequestParam("lon") Double lon,
			@RequestParam(value = "at", required = false) String at,
			@RequestParam(value = "riderId", required = false) String riderId) throws Exception {
		long now = System.currentTimeMillis();
		boolean member = riderId != null && !riderId.trim().isEmpty();
		if (member) {
			AppIdentity.requireOwner(request, riderId);
		}
		boolean allowed = weatherIpLimiter.tryAcquire("ip:" + clientAddress(request), now)
				&& (member ? weatherRiderLimiter.tryAcquire("rider:" + riderId.trim(), now)
						: weatherAnonymousLimiter.tryAcquire("anon:" + clientAddress(request), now));
		if (!allowed) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "600").build();
		}
		try {
			Optional<WeatherSnapshot> snapshot = weatherService.lookup(lat, lon, weatherTime(at));
			if (!snapshot.isPresent()) {
				return ResponseEntity.ok(RecordWeather.unavailable());
			}
			WeatherSnapshot s = snapshot.get();
			RecordWeather weather = RecordWeather.from(s.getObservedAtTimestamp(), s.getTemperatureC(), s.getSkyCode(),
					s.getPrecipitationCode(), s.getPrecipitationMm(), s.getLabel());
			return ResponseEntity.ok(weather.withToken(weatherSnapshotTokens.put(s)));
		} catch (RuntimeException e) {
			log.warn("Weather lookup failed: {}", e.getClass().getSimpleName());
			return ResponseEntity.ok(RecordWeather.unavailable());
		}
	}

	private String clientAddress(HttpServletRequest request) {
		if (trustForwardedFor) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.trim().isEmpty()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return request.getRemoteAddr();
	}
	/** Blank memo means "no memo"; anything longer than the limit is cut (the app limits input already). */
	static String normalizeMemo(String memo) {
		if (memo == null || memo.trim().isEmpty()) {
			return null;
		}
		return memo.length() > MAX_MEMO_LENGTH ? memo.substring(0, MAX_MEMO_LENGTH) : memo;
	}

	/** The time the weather should describe: the ride start when the app sent a sane one, otherwise now. */
	static LocalDateTime weatherTime(String startedAt) {
		LocalDateTime now = LocalDateTime.now(WeatherService.KST);
		if (startedAt == null || startedAt.trim().isEmpty()) {
			return now;
		}
		try {
			LocalDateTime parsed = LocalDateTime.parse(startedAt.trim().replace(' ', 'T'));
			return parsed.isAfter(now.plusMinutes(5)) ? now : parsed;
		} catch (java.time.format.DateTimeParseException e) {
			return now;
		}
	}

	//구 주행기록 상세 경로. 작성자를 알 수 없으므로 공개 기록만 반환한다 (기존에는 admin/1 하드코딩).
	@RequestMapping(value = "get/detail/{rrNum}", method = RequestMethod.GET)
	public ResponseEntity<List<RidingRecord>> getRidingRecordDetail(@PathVariable("rrNum") int rrNum) {
		RidingRecord record = androidService.getPublicRecord(rrNum);
		if (record == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(java.util.Collections.singletonList(record));
	}
	
	//주행기록 공개여부 설정
		@RequestMapping(value = "recordset/open", method = RequestMethod.POST)
		public @ResponseBody Map<String, String> RidingOpenSet(HttpServletRequest request) throws Exception {
			RidingRecord record = new RidingRecord();
			record.setRr_num(Integer.parseInt(request.getParameter("rr_num")));
			record.setRr_open(Integer.parseInt(request.getParameter("rr_open")));
			System.out.println(request.getParameter("rr_num")+ " "+request.getParameter("rr_open"));

			int updated = androidService.RidingOpenSet(record);
			// 안드로이드에게 전달하는 데이터
			Map<String, String> result = new HashMap<String, String>();
			result.put("updated", String.valueOf(updated));

			return result;
		}
	
		//GPX파일 저장
		@RequestMapping(value = "fileUpload/{dir}/{fileName}", method = RequestMethod.POST)
		public ResponseEntity<Void> upload(HttpServletRequest request, MultipartFile file1, @PathVariable(value = "dir") String dir,
				@PathVariable(value="fileName") String fileName) {
			log.debug("업로드 파일 명 "+fileName+" "+"업로드 경로 "+dir);
			try {
				Path directory = resolveStorageDirectory(dir);
				if (file1 == null || file1.isEmpty()) {
					return ResponseEntity.badRequest().build();
			}
			Files.createDirectories(directory);
			try (InputStream input = file1.getInputStream()) {
				Files.copy(input, resolveStorageFile(directory, fileName), StandardCopyOption.REPLACE_EXISTING);
			}
			return ResponseEntity.ok().build();
			} catch (IOException e) {
				log.error("GPX upload failed: {}", fileName, e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}

	// 코스 조회
	@RequestMapping(value = "riding/course")
	public @ResponseBody List<App_RidingRecord> getCourse() throws Exception {
		return androidService.getCourse();
	}

	// 코스 조회
	@RequestMapping(value = "riding/course/{c_num}")
	public @ResponseBody List<Course> getCourseItem(@PathVariable(value = "c_num") String c_num) throws Exception {
		System.out.println(c_num);
		return androidService.getCourseItem(c_num);
	}

	// 코스 스크랩하기
	@RequestMapping(value = "riding/coursescrap")
	public @ResponseBody Map<String, String> coursescrap(HttpServletRequest request) throws Exception {
		Scrap_Status scrap = new Scrap_Status();
		scrap.setSs_rnum(Integer.parseInt(request.getParameter("c_num")));
		scrap.setSs_rider(request.getParameter("ss_rider"));
		
		androidService.insertScrap(scrap);

		Map<String, String> result = new HashMap<String, String>();
		result.put("Status", "Ok");

		return result;
	}

	// 사용자 스크랩 코스 조회
	@RequestMapping(value = "riding/scrap/{rr_rider}")
	public @ResponseBody List<Scrap_Status> getScrap(@PathVariable(value = "rr_rider") String rr_rider) throws Exception {
		return androidService.getScrap(rr_rider);
	}

	// 사용자 스크랩 코스 상세보기
	@RequestMapping(value = "riding/scrap/{rr_rider}/{ss_course}", method = RequestMethod.GET)
	public @ResponseBody List<Course> getScrapDetail(@PathVariable(value = "rr_rider") String rr_rider,
			@PathVariable(value = "ss_course") String ss_course) throws Exception {
		return androidService.getScrapDetail(rr_rider, ss_course);
	}
	
	//스크랩 삭제
	@RequestMapping(value = "riding/scrap/del/{ss_rnum}", method = RequestMethod.GET)
	public void delScrap(@PathVariable(value = "ss_rnum") String ss_rnum) throws Exception {
		System.out.println(ss_rnum);
		androidService.delScrap(ss_rnum);
		
	}

	//좋아요
	@RequestMapping(value = "riding/course/like", method = RequestMethod.POST)
	public @ResponseBody  Map<String, String> likeput(HttpServletRequest request) throws Exception {
		System.out.println(request.getParameter("rr_rider") + " " +request.getParameter("rr_num"));
		RidingRecord record = new RidingRecord();
		record.setRr_rider(request.getParameter("rr_rider"));
		record.setRr_num(Integer.parseInt(request.getParameter("rr_num")));
		androidService.insertLike(record);
		Map<String, String> result = new HashMap<String, String>();
		result.put("Status", "Ok");

		return result;
	}
	
	//좋아요 읽기
	@RequestMapping(value = "riding/course/like/{rr_num}", method = RequestMethod.GET)
	public @ResponseBody List<RidingRecord> getLikeCount(@PathVariable(value = "rr_num") int rr_num) throws Exception {
		return androidService.getLikeCount(rr_num);
	}
	
	//좋아요 상태
		@RequestMapping(value = "riding/course/like/{ls_rnum}/{ls_rider}", method = RequestMethod.GET)
		public @ResponseBody List<Like_Status> getLikeStatus(@PathVariable(value = "ls_rnum") int ls_rnum, @PathVariable(value = "ls_rider") String ls_rider) throws Exception {
			return androidService.getLikeStatus(ls_rnum, ls_rider);
		}
		
	// 파일 다운로드
	@RequestMapping(value = "getGPX/{file_dir}/{file_name}", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getImageAsResponseEntity( @PathVariable("file_name") String fileName, @PathVariable("file_dir") String fileDir ) throws IOException {
		Path file = resolveStorageFile(resolveStorageDirectory(fileDir), fileName);
		if (!Files.isRegularFile(file)) {
			return ResponseEntity.notFound().build();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(CacheControl.noCache().getHeaderValue());
		return new ResponseEntity<>(Files.readAllBytes(file), headers, HttpStatus.OK);
	}

	private Path resolveStorageDirectory(String directory) throws IOException {
		if (directory == null || !directory.matches("[A-Za-z0-9_-]+")) {
			throw new IOException("Invalid storage directory");
		}
		Path root = Paths.get(storageRoot).toAbsolutePath().normalize();
		Path resolved = root.resolve(directory).normalize();
		if (!resolved.startsWith(root)) {
			throw new IOException("Invalid storage directory");
		}
		return resolved;
	}

	private Path resolveStorageFile(Path directory, String fileName) throws IOException {
		if (fileName == null || !fileName.matches("[A-Za-z0-9._-]+")) {
			throw new IOException("Invalid storage file name");
		}
		Path file = directory.resolve(fileName).normalize();
		if (!file.startsWith(directory)) {
			throw new IOException("Invalid storage file name");
		}
		return file;
	}

	// 웹뷰용 라이딩 기록
	@RequestMapping(value = "getRidingRecord")
	public String getAppRidingRecord() {
		return "riding/appRidingRecord";
	}
	
	// 경쟁전 조회
	@RequestMapping(value = "competition")
	public @ResponseBody List<App_Competition> getCompetition() throws Exception {
		return androidService.getCompetition();
	}
	
	//경쟁전 참가내역 가져오기
	@RequestMapping(value = "competition/{rr_rider}")
	public @ResponseBody List<String> getjoinedComp(@PathVariable(value = "rr_rider") String rr_rider) throws Exception {
		return androidService.getjoinedComp(rr_rider);
	}

	// 경쟁전 코스 정보
	@RequestMapping(value = "getAppCompCourse")
	public String getAppCompCourse() {
		return "entertainment/appCompCourse";
	}
	
	// C_NUM으로 GPX파일 조회
	@RequestMapping(value="/getCompCourse", method = RequestMethod.GET)
	@ResponseBody
	public Gpx getCompCourse(int c_num) throws Exception {
		String gpxFile = androidService.getCompCourse(c_num);
		Gpx gpx = new Gpx();
		makeGpx(gpx, gpxFile);
		return gpx;
	}
	
	private void makeGpx(Gpx gpx, String gpxFile) throws Exception {
	
		String path = "/volume1/Develop/bikeapp/uploads/data/gpx/" + gpxFile;
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
	

	
	@RequestMapping("getCompClub/{cs_comp}")
	public @ResponseBody List<CompClub> getCompClub( @PathVariable(value = "cs_comp") int cs_comp) throws Exception {
		return androidService.getCompClub(cs_comp);
	}
	
	//코스검색
	@RequestMapping("riding/CourseSearch")
	public String CourseSearch() throws Exception{
		return "android/Android_CourseSearch";
	}
	
	//위험지역 마커
	@RequestMapping("riding/danger")
	public @ResponseBody List<DangerousArea> RidingDanger() throws Exception {
		return androidService.getDanger();
	}

	@RequestMapping("getCompBadge/{comp_badge}")
	public @ResponseBody Badge getCompBadge ( @PathVariable(value = "comp_badge") int comp_badge) throws Exception {
		return androidService.getCompBadge(comp_badge);
	}
	
	@RequestMapping("getCompScore/{comp_num}")
	public @ResponseBody List<CompScore> getCompScore ( @PathVariable("comp_num") int comp_num) throws Exception {
		return androidService.getCompScore(comp_num);
	}
	
	@RequestMapping(value = "/app/updateCompScore", method = RequestMethod.PUT)
	@ResponseBody
	public void updateCompScore(int avg_speed, int rr_comp, int r_club, String LoginId) throws Exception{

		androidService.updateCompScore(avg_speed, rr_comp, r_club, LoginId);
	}
	

	
	@RequestMapping(value = "updateRank", method = RequestMethod.PUT)
	@ResponseBody
	public void updateRank (int rr_comp, int r_club, int score, int rank) throws Exception {
		androidService.updateRank(rr_comp, r_club, score, rank);
	}
	
	@RequestMapping(value = "getMissionStatus/{LoginId}")
	public @ResponseBody List<Mission_Status> getMissionStatus ( @PathVariable(value="LoginId") String LoginId ) throws Exception {
		return androidService.getMissionStatus(LoginId);
	}
	
	@RequestMapping(value = "getNoClearMission/{LoginId}")
	public @ResponseBody List<Mission> getNoClearMission ( @PathVariable(value="LoginId") String LoginId ) throws Exception {
		return androidService.getNoClearMission(LoginId);
	}
	
	@RequestMapping(value = "updateMissionStatus", method = RequestMethod.PUT)
	@ResponseBody
	public void updateMissionStatus ( String LoginId, int typeScore1, int typeScore2 ) throws Exception {
		androidService.updateMissionStatus(LoginId, typeScore1, typeScore2);
	}
	
	@ResponseBody
	public void insertMissionCom ( String LoginId, int mc_mission, Timestamp mc_time ) throws Exception {
		androidService.insertMissionCom(LoginId, mc_mission, mc_time);
	}
	
	@RequestMapping(value="riding/getrecord")
	@ResponseBody
	public  List<RidingRecord>getRecord () throws Exception {
		return androidService.selectRecord();
	}
	
	@RequestMapping(value="riding/taginsert")
	@ResponseBody
	public Map<String, String>  Taginsert(HttpServletRequest request) {

		System.out.println(request.getParameter("rr_num") + " " + request.getParameter("rr_rider") + " " + request.getParameter("address_dong"));

		App_Tag tag = new App_Tag();
		tag.setTs_rnum(Integer.parseInt(request.getParameter("rr_num")));
		tag.setTs_rider(request.getParameter("rr_rider"));
		tag.setTs_tag(request.getParameter("address_dong"));

		androidService.TagInsert(tag);
		// 안드로이드에게 전달하는 데이터
		Map<String, String> result = new HashMap<>();
		result.put("data1", "성공했쩡");
		return result;
	}
	
	@RequestMapping(value = "getAllMission/{LoginId}")
	public @ResponseBody List<App_Mission> getAllMission ( @PathVariable(value="LoginId") String LoginId) throws Exception {
		return androidService.getAllMission(LoginId);
	}
	
	@RequestMapping(value = "getComMission/{LoginId}")
	public @ResponseBody List<String> getComMission ( @PathVariable(value="LoginId") String LoginId) throws Exception {
		return androidService.getComMission(LoginId);
	}
	
	@RequestMapping(value = "getWhenCom/{LoginId}/{m_num}")
	public @ResponseBody Date getWhenCom ( @PathVariable(value="LoginId") String LoginId,
												@PathVariable(value="m_num") int m_num) throws Exception {
		return androidService.getWhenCom(LoginId, m_num);
	}
	
	@RequestMapping(value = "getMisRanking")
	public @ResponseBody List<App_MissionRanking> getMisRanking () throws Exception {
		return androidService.getMisRanking();
	}
	
	@RequestMapping(value = "getCourseReview/{c_num}")
	public @ResponseBody List<App_CourseReview> getCourseRiview (@PathVariable(value="c_num") int c_num) throws Exception{
		return androidService.getCourseRiview(c_num);
	}

	@RequestMapping("riding/course_view/{rr_num}")
	public String course_view(@PathVariable int rr_num, Model model) throws Exception{
		model.addAttribute("rr_num", rr_num);
		return "android/course_view";
	}

	@RequestMapping("riding/course_share/{rr_num}")
	public String course_share(@PathVariable int rr_num, Model model) throws Exception{
		model.addAttribute("rr_num", rr_num);
		return "android/course_share";
	}

	
	//RR_NUM으로 RIDINGRECORD 조회
	@RequestMapping(value="/getRidingRecordByRR_Num/{rr_num}", method = RequestMethod.GET)
	@ResponseBody
	public RidingRecord getRidingRecordByRR_Num(@PathVariable int rr_num, Model model) throws Exception {
		RidingRecord rr = ridingService.getRidingRecordDetail(rr_num);
		int like = ridingService.getRR_Like(rr_num);
		rr.setRr_like(like);
		return rr;
	}

	@RequestMapping(value = "riding/foup")
	// 주소변경예정 @RequestMapping(value = "/android/recordInsert")
	@ResponseBody
	public Map<String, String> insertFollowRiding(HttpServletRequest request) throws Exception {
		RidingRecord record = new RidingRecord();

		// 현재날짜 timestamp
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// formatter.setTimeZone(TimeZone.getTimeZone("GMT+09"));
		Calendar cal = Calendar.getInstance();
		String today = null;
		today = formatter.format(cal.getTime());
		Timestamp ts = Timestamp.valueOf(today);
		record.setRr_rider(request.getParameter("rr_rider"));
		record.setRr_date(ts);
		record.setRr_distance(Integer.parseInt(request.getParameter("rr_distance")));
		record.setRr_topspeed(Integer.parseInt(request.getParameter("rr_topspeed")));
		record.setRr_avgspeed(Integer.parseInt(request.getParameter("rr_avgspeed")));
		record.setRr_high(Integer.parseInt(request.getParameter("rr_high")));
		record.setRr_gpx(request.getParameter("rr_gpx"));
		record.setRr_open(Integer.parseInt(request.getParameter("rr_open")));
		record.setRr_breaktime(Integer.parseInt(request.getParameter("rr_breaktime")));
		record.setRr_time(Integer.parseInt(request.getParameter("rr_time")));
		record.setRr_area(request.getParameter("rr_area"));
		record.setRr_name(request.getParameter("rr_name"));
		record.setRr_foname(request.getParameter("rr_name"));
		// 안드로이드에게 전달하는 데이터
		Map<String, String> result = new HashMap<String, String>();
		result.put("data1", request.getParameter("rr_rider"));
		androidService.foinsert(record);
		androidService.foupdate(record);
		return result;
	}
	
	@RequestMapping(value = "updateCourseReview")
	@ResponseBody
	public void updateCourseReview(HttpServletRequest request) throws Exception {
		App_CourseReview C_review = new App_CourseReview();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		String today = null;
		today = formatter.format(cal.getTime());
		Timestamp ts = Timestamp.valueOf(today);

		C_review.setCr_content(request.getParameter("cr_content"));
		C_review.setCr_images(request.getParameter("cr_image"));
		C_review.setCr_num(Integer.parseInt(request.getParameter("cr_num")));
		C_review.setCr_time(ts);
		androidService.updateCourseReview(C_review);
	}

	@RequestMapping(value = "insertCourseReview")
	@ResponseBody
	public void insertCourseReview(HttpServletRequest request) throws Exception {
		App_CourseReview C_review = new App_CourseReview();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		String today = null;
		today = formatter.format(cal.getTime());
		Timestamp ts = Timestamp.valueOf(today);

		C_review.setCr_content(request.getParameter("cr_content"));
		C_review.setCr_images(request.getParameter("cr_image"));
		C_review.setCr_time(ts);
		C_review.setCr_rider(request.getParameter("cr_rider"));
		C_review.setCr_rnum(Integer.parseInt(request.getParameter("cr_rnum")));
		androidService.insertCourseReview(C_review);
	}

	@RequestMapping(value = "deleteCourseReview")
	@ResponseBody
	public void deleteCourseReview(HttpServletRequest request) throws Exception {
		String cr_num = "";

		cr_num = request.getParameter("cr_num");

		androidService.deleteCourseReview(cr_num);
	}

	// 사용자 등록 위험 지역 조회
	@RequestMapping(value = "danger/{rr_rider}")
	public @ResponseBody List<DangerousArea> getUserDangerousArea(@PathVariable(value="rr_rider") String rr_rider) throws Exception {
		return ridingService.getUserDangerousArea(rr_rider);
	}
	
	//위험지역 등록하기
	//구 API. 새 앱은 POST /app/hazards 를 쓴다. 제보자는 body의 da_rider가 아니라 로그인 토큰으로 정하고, 로그인 없이는 받지 않는다.
	@RequestMapping(value = "insertDanger")
	public @ResponseBody void insertDanger(HttpServletRequest request, @RequestBody DangerousArea d_area) throws Exception{
		String rider = AppIdentity.riderOf(request);
		if (rider == null) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		d_area.setDa_rider(rider);
		androidService.insertDanger(d_area);
	}

	//긴급 위치알림
	@RequestMapping(value = "dangergps/{lat}/{lon}")
	public  String dangerGps(Model model, @PathVariable(value="lat") String lat, @PathVariable(value="lon") String lon) throws Exception {
	model.addAttribute("lat",lat);
	model.addAttribute("lon",lon);
		return "android/helpgps";
	}
}
