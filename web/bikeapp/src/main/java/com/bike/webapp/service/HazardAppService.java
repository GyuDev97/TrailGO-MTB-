package com.bike.webapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bike.webapp.dto.HazardRequest;
import com.bike.webapp.dto.HazardRow;
import com.bike.webapp.dto.HazardView;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.hazard.HazardGeo;
import com.bike.webapp.hazard.HazardType;
import com.bike.webapp.mapper.HazardAdminMapper;
import com.bike.webapp.mapper.HazardAppMapper;

/**
 * Rider-reported hazard zones for the app. A zone is a circle (centre + radius). New reports are unverified
 * until an operator approves them in the admin screens; hidden, rejected, expired and merged zones are never returned.
 */
@Service
public class HazardAppService {

	public static final int DEFAULT_RADIUS_M = 30;
	public static final int MIN_RADIUS_M = 10;
	public static final int MAX_RADIUS_M = 200;
	public static final int MAX_TITLE_LENGTH = 50;
	public static final int MAX_DESCRIPTION_LENGTH = 300;

	public static final int DEFAULT_NEARBY_M = 1000;
	public static final int MAX_NEARBY_M = 5000;
	public static final int MAX_RESULTS = 200;
	/** Rows read from the database before the exact distance filter; bounds the work of one request. */
	static final int SCAN_LIMIT = 500;
	/** At most this many "removed" ids per answer; they only matter for zones a phone already has. */
	static final int MAX_REMOVED_IDS = 300;

	/** A new report this close to a visible zone of the same type is taken to be the same hazard. */
	public static final int DUPLICATE_DISTANCE_M = 25;

	private final HazardAppMapper mapper;
	private final HazardAdminMapper adminMapper;

	@Autowired
	public HazardAppService(HazardAppMapper mapper, HazardAdminMapper adminMapper) {
		this.mapper = mapper;
		this.adminMapper = adminMapper;
	}

	/** Result of a nearby search; {@code truncated} means more zones matched than were returned. */
	public static final class Nearby {
		private final List<HazardView> items;
		private final boolean truncated;
		private final int radiusM;
		private final List<Integer> removedIds;

		Nearby(List<HazardView> items, boolean truncated, int radiusM, List<Integer> removedIds) {
			this.items = items;
			this.truncated = truncated;
			this.radiusM = radiusM;
			this.removedIds = removedIds;
		}

		/** Zones in the area that riders must not keep any more (hidden, rejected, expired, merged). */
		public List<Integer> getRemovedIds() {
			return removedIds;
		}

		public List<HazardView> getItems() {
			return items;
		}

		public boolean isTruncated() {
			return truncated;
		}

		public int getRadiusM() {
			return radiusM;
		}
	}

	// ---- reading ------------------------------------------------------------------------------------

	public Nearby nearby(Double latitude, Double longitude, Integer radiusM, String viewer) {
		if (!HazardGeo.isValidCoordinate(latitude, longitude)) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_COORDINATE", "위도·경도가 올바르지 않습니다.");
		}
		int radius = radiusM == null ? DEFAULT_NEARBY_M : radiusM;
		if (radius < 1 || radius > MAX_NEARBY_M) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_RADIUS",
					"조회 반경은 1~" + MAX_NEARBY_M + "m 입니다.");
		}
		// The box covers zones whose centre is up to a zone radius further out but whose edge reaches us.
		HazardGeo.Box box = HazardGeo.boundingBox(latitude, longitude, radius + MAX_RADIUS_M);
		List<HazardRow> rows = mapper.findVisibleInBox(latitude, longitude,
				box.minLat, box.maxLat, box.minLon, box.maxLon, SCAN_LIMIT);

		List<HazardView> matched = new ArrayList<HazardView>();
		for (HazardRow row : rows) {
			if (row.getLatitude() == null || row.getLongitude() == null) {
				continue;
			}
			double distance = HazardGeo.distanceMeters(latitude, longitude, row.getLatitude(), row.getLongitude());
			if (distance - zoneRadius(row) <= radius) {
				HazardView view = toView(row, viewer);
				view.setDistanceM(round1(distance));
				matched.add(view);
			}
		}
		Collections.sort(matched, new Comparator<HazardView>() {
			@Override
			public int compare(HazardView a, HazardView b) {
				return Double.compare(a.getDistanceM() - a.getRadiusM(), b.getDistanceM() - b.getRadiusM());
			}
		});
		boolean truncated = matched.size() > MAX_RESULTS || rows.size() >= SCAN_LIMIT;
		List<HazardView> items = matched.size() > MAX_RESULTS ? new ArrayList<HazardView>(matched.subList(0, MAX_RESULTS)) : matched;
		List<Integer> removed = mapper.findHiddenIdsInBox(box.minLat, box.maxLat, box.minLon, box.maxLon, MAX_REMOVED_IDS);
		return new Nearby(items, truncated, radius, removed == null ? Collections.<Integer>emptyList() : removed);
	}

	public HazardView get(int id, String viewer) {
		HazardRow row = mapper.findById(id);
		if (row == null || !isVisible(row)) {
			throw notFound();
		}
		return toView(row, viewer);
	}

	// ---- writing ------------------------------------------------------------------------------------

	@Transactional
	public HazardView report(String rider, HazardRequest request) {
		requireRider(rider);
		if (request == null) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_BODY", "요청 내용이 없습니다.");
		}
		if (!HazardGeo.isValidCoordinate(request.getLatitude(), request.getLongitude())
				|| (request.getLatitude() == 0.0d && request.getLongitude() == 0.0d)) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_COORDINATE", "위도·경도가 올바르지 않습니다.");
		}
		Fields fields = validate(request);
		double lat = request.getLatitude();
		double lon = request.getLongitude();

		HazardRow duplicate = findDuplicate(lat, lon, fields.type);
		if (duplicate != null) {
			throw new HazardException(HttpStatus.CONFLICT, "DUPLICATE_HAZARD", "근처에 이미 같은 유형의 제보가 있습니다.")
					.with("hazardId", duplicate.getId());
		}

		HazardRow row = new HazardRow();
		row.setReporter(rider);
		row.setDescription(fields.description);
		row.setRadiusM(fields.radiusM);
		mapper.insertDanger(row, coordinateText(lat), coordinateText(lon));
		mapper.insertReview(row.getId(), fields.title, fields.type.name());
		adminMapper.insertHistory(row.getId(), "REPORTED", null, "PENDING", "rider:" + rider, null);

		HazardRow saved = mapper.findById(row.getId());
		if (saved == null) {
			throw new IllegalStateException("hazard " + row.getId() + " not readable after insert");
		}
		return toView(saved, rider);
	}

	/** Owner only; the place never changes. Approved reports are locked: ask an operator. */
	@Transactional
	public HazardView update(String rider, int id, HazardRequest request) {
		requireRider(rider);
		if (request == null) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_BODY", "요청 내용이 없습니다.");
		}
		HazardRow row = requireOwned(rider, id);
		if ("APPROVED".equals(row.getStatus())) {
			throw new HazardException(HttpStatus.CONFLICT, "LOCKED", "확인된 제보는 수정할 수 없습니다.");
		}
		Fields fields = validate(request);
		adminMapper.ensureReview(id);
		mapper.updateDanger(id, fields.description, fields.radiusM);
		mapper.updateReview(id, fields.title, fields.type.name());
		return toView(mapper.findById(id), rider);
	}

	/** Owner only. The zone is hidden (INACTIVE), not erased, so operators keep the history. */
	@Transactional
	public void delete(String rider, int id) {
		requireRider(rider);
		HazardRow row = requireOwned(rider, id);
		adminMapper.ensureReview(id);
		mapper.deactivateReview(id);
		mapper.deactivateLegacy(id);
		adminMapper.insertHistory(id, "WITHDRAWN", row.getStatus(), "INACTIVE", "rider:" + rider, null);
	}

	// ---- helpers ------------------------------------------------------------------------------------

	private static final class Fields {
		HazardType type;
		String title;
		String description;
		int radiusM;
	}

	private static Fields validate(HazardRequest request) {
		Fields fields = new Fields();
		fields.type = HazardType.parse(request.getType());
		if (fields.type == null) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "위험 유형이 올바르지 않습니다.");
		}
		String title = request.getTitle() == null ? "" : request.getTitle().trim();
		if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_TITLE",
					"제목은 1~" + MAX_TITLE_LENGTH + "자로 입력해 주세요.");
		}
		fields.title = title;
		String description = request.getDescription() == null ? "" : request.getDescription().trim();
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_DESCRIPTION",
					"설명은 " + MAX_DESCRIPTION_LENGTH + "자 이하로 입력해 주세요.");
		}
		fields.description = description;
		int radius = request.getRadiusM() == null ? DEFAULT_RADIUS_M : request.getRadiusM();
		if (radius < MIN_RADIUS_M || radius > MAX_RADIUS_M) {
			throw new HazardException(HttpStatus.BAD_REQUEST, "INVALID_RADIUS",
					"위험 반경은 " + MIN_RADIUS_M + "~" + MAX_RADIUS_M + "m 입니다.");
		}
		fields.radiusM = radius;
		return fields;
	}

	private HazardRow findDuplicate(double lat, double lon, HazardType type) {
		HazardGeo.Box box = HazardGeo.boundingBox(lat, lon, DUPLICATE_DISTANCE_M);
		for (HazardRow existing : mapper.findVisibleInBox(lat, lon, box.minLat, box.maxLat, box.minLon, box.maxLon, 50)) {
			if (existing.getLatitude() != null && existing.getLongitude() != null
					&& type == HazardType.parseOrOther(existing.getHazardType())
					&& HazardGeo.distanceMeters(lat, lon, existing.getLatitude(), existing.getLongitude()) <= DUPLICATE_DISTANCE_M) {
				return existing;
			}
		}
		return null;
	}

	private HazardRow requireOwned(String rider, int id) {
		HazardRow row = mapper.findById(id);
		if (row == null || !isVisible(row)) {
			throw notFound();
		}
		if (!rider.equals(row.getReporter())) {
			throw new HazardException(HttpStatus.FORBIDDEN, "NOT_OWNER", "본인이 등록한 제보만 변경할 수 있습니다.");
		}
		return row;
	}

	private static void requireRider(String rider) {
		if (rider == null || rider.trim().isEmpty()) {
			throw new HazardException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
		}
	}

	private static HazardException notFound() {
		return new HazardException(HttpStatus.NOT_FOUND, "NOT_FOUND", "위험지역을 찾을 수 없습니다.");
	}

	private static boolean isVisible(HazardRow row) {
		String status = row.getStatus();
		return "PENDING".equals(status) || "REVIEWING".equals(status) || "APPROVED".equals(status);
	}

	private static int zoneRadius(HazardRow row) {
		return row.getRadiusM() > 0 ? row.getRadiusM() : DEFAULT_RADIUS_M;
	}

	private static HazardView toView(HazardRow row, String viewer) {
		HazardType type = HazardType.parseOrOther(row.getHazardType());
		HazardView view = new HazardView();
		view.setId(row.getId());
		view.setLatitude(row.getLatitude() == null ? 0.0d : row.getLatitude());
		view.setLongitude(row.getLongitude() == null ? 0.0d : row.getLongitude());
		view.setRadiusM(zoneRadius(row));
		view.setType(type.name());
		view.setTypeLabel(type.getLabel());
		view.setTitle(row.getTitle());
		view.setDescription(row.getDescription() == null ? "" : row.getDescription());
		view.setAddress(row.getAddress() == null ? "" : row.getAddress());
		view.setVerified("APPROVED".equals(row.getStatus()));
		view.setCreatedAtMs(row.getCreatedAt() == null ? 0L : row.getCreatedAt().getTime());
		view.setUpdatedAtMs(row.getUpdatedAt() == null ? 0L : row.getUpdatedAt().getTime());
		view.setMine(viewer != null && viewer.equals(row.getReporter()));
		return view;
	}

	private static String coordinateText(double value) {
		return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP).toPlainString();
	}

	private static double round1(double value) {
		return Math.round(value * 10.0d) / 10.0d;
	}
}
