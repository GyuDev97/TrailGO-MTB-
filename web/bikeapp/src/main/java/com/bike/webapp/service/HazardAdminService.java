package com.bike.webapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bike.webapp.dto.AdminHazardRequest;
import com.bike.webapp.dto.HazardMergeRecord;
import com.bike.webapp.dto.HazardMergeRequest;
import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.dto.HazardRow;
import com.bike.webapp.dto.HazardStats;
import com.bike.webapp.hazard.HazardDuplicateFinder;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.hazard.HazardGeo;
import com.bike.webapp.hazard.HazardLabels;
import com.bike.webapp.hazard.HazardTransitions;
import com.bike.webapp.hazard.HazardType;
import com.bike.webapp.mapper.HazardAdminMapper;
import com.bike.webapp.mapper.HazardAppMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Operator side of the hazard zones: review (approve / reject / hide), registering a zone directly, finding
 * duplicates and merging them. Every change is one transaction and leaves a row in trailgo_hazard_history;
 * nothing is deleted. Who may call this is decided by the security configuration (/admin/** needs ROLE_ADMIN).
 */
@Service
@Transactional
public class HazardAdminService {

    public static final int MAX_EDIT_TITLE_LENGTH = 120;
    public static final int MAX_MERGE_SOURCES = 20;
    public static final int MAX_CANDIDATES = 20;
    private static final int SCAN_LIMIT = 100;

    private static final Set<String> LEGACY_TYPES = new HashSet<String>(java.util.Arrays.asList(
            "ROCKFALL", "ROAD_DAMAGE", "VEHICLE_TRAFFIC", "CONSTRUCTION", "BLOCKED"));

    private final HazardAdminMapper mapper;
    private final HazardAppMapper appMapper;
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    public HazardAdminService(HazardAdminMapper mapper, HazardAppMapper appMapper) {
        this.mapper = mapper;
        this.appMapper = appMapper;
    }

    // ---- reading ------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<HazardReview> findAll(String keyword, String hazardType, String status,
                                      String dateFrom, String dateTo, String reported, String sort, String source) {
        return mapper.findAll(trimToNull(keyword), trimToNull(hazardType), trimToNull(status),
                trimToNull(dateFrom), trimToNull(dateTo), trimToNull(reported), sort, trimToNull(source));
    }

    @Transactional(readOnly = true)
    public List<HazardReview> findRecent(int limit) {
        return mapper.findRecent(limit);
    }

    /** Only these reviewed, active and unexpired hazards are safe for rider devices. */
    @Transactional(readOnly = true)
    public List<HazardReview> findPublicApproved() {
        return mapper.findPublicApproved();
    }

    @Transactional(readOnly = true)
    public HazardReview findById(int id) {
        return mapper.findById(id);
    }

    /** The zones that were merged into this one (their original reports stay readable). */
    @Transactional(readOnly = true)
    public List<HazardReview> mergedChildren(int id) {
        return mapper.findMergedChildren(id);
    }

    @Transactional(readOnly = true)
    public List<HazardReview> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return mapper.findByIds(ids);
    }

    @Transactional(readOnly = true)
    public List<HazardMergeRecord> merges(Integer canonicalId, int limit) {
        return mapper.findMerges(canonicalId, Math.max(1, Math.min(limit, 500)));
    }

    @Transactional(readOnly = true)
    public HazardStats getStats() {
        HazardStats stats = new HazardStats();
        stats.setTotal(mapper.countAll());
        stats.setPending(mapper.countByStatus("PENDING"));
        stats.setReviewing(mapper.countByStatus("REVIEWING"));
        stats.setApproved(mapper.countByStatus("APPROVED"));
        stats.setRejected(mapper.countByStatus("REJECTED"));
        return stats;
    }

    // ---- review: approve / reject / hide / restore --------------------------------------------------

    public void approve(int id, String adminId) {
        transition(id, HazardTransitions.APPROVED, adminId, null, "APPROVED");
    }

    public void reject(int id, String adminId, String reason) {
        transition(id, HazardTransitions.REJECTED, adminId, trimToNull(reason), "REJECTED");
    }

    public void hide(int id, String adminId, String reason) {
        transition(id, HazardTransitions.INACTIVE, adminId, trimToNull(reason), "HIDDEN");
    }

    public void restore(int id, String adminId) {
        transition(id, HazardTransitions.PENDING, adminId, null, "RESTORED");
    }

    /**
     * The one place a review status changes. It refuses a move the policy does not allow (including "approve
     * what is already approved", so a double click or a stale page cannot overwrite who approved it and when),
     * and the update itself only applies while the zone still has the status the operator saw.
     */
    private void transition(int id, String next, String adminId, String reason, String action) {
        HazardReview before = required(id);
        requireNotMerged(before);
        if (next.equals(before.getStatus())) {
            throw new HazardException(HttpStatus.CONFLICT, "ALREADY_IN_STATUS",
                    "이미 '" + HazardLabels.status(before.getStatus(), false) + "' 상태입니다.");
        }
        if (!HazardTransitions.canMove(before.getStatus(), next)) {
            throw new HazardException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "'" + HazardLabels.status(before.getStatus(), false) + "' 상태에서는 '"
                            + HazardLabels.status(next, false) + "'(으)로 바꿀 수 없습니다.");
        }
        mapper.ensureReview(id);
        int changed = mapper.transitionStatus(id, before.getStatus(), next, adminId, reason);
        if (changed != 1) {
            throw new HazardException(HttpStatus.CONFLICT, "STALE",
                    "다른 곳에서 이미 상태가 바뀌었습니다. 화면을 새로고침해 주세요.");
        }
        mapper.updateLegacyStatus(id, legacyStatus(next));
        mapper.insertHistory(id, action, before.getStatus(), next, adminId, reason);
    }

    // ---- editing ------------------------------------------------------------------------------------

    public void update(int id, HazardReview changes, String adminId) {
        HazardReview before = required(id);
        requireNotMerged(before);
        mapper.ensureReview(id);
        changes.setId(id);

        validateEditedFields(changes, before.getHazardType());
        // getRadiusM() answers 30 when none was sent; an edit that did not mention the radius keeps the stored one
        if (changes.rawRadiusM() == null) changes.setRadiusM(before.getRadiusM());
        mapper.updateDangerDetails(changes);
        mapper.updateReviewDetails(changes);
        mapper.insertHistory(id, "UPDATE", before.getStatus(), before.getStatus(), adminId, "관리자 수정");

        String requested = trimToNull(changes.getStatus());
        if (requested != null && !requested.equals(before.getStatus())) {
            if (!HazardTransitions.isKnown(requested)) {
                throw new IllegalArgumentException("허용되지 않은 검수 상태입니다.");
            }
            transition(id, requested, adminId, null, statusAction(requested));
        }
    }

    private void validateEditedFields(HazardReview changes, String currentType) {
        String title = trimToNull(changes.getTitle());
        if (changes.getTitle() != null && (title == null || title.length() > MAX_EDIT_TITLE_LENGTH)) {
            throw new IllegalArgumentException("제목은 1~" + MAX_EDIT_TITLE_LENGTH + "자로 입력해 주세요.");
        }
        changes.setTitle(title);
        String type = trimToNull(changes.getHazardType());
        if (type != null && HazardType.parse(type) == null && !type.equals(currentType)
                && !LEGACY_TYPES.contains(type)) {
            throw new IllegalArgumentException("허용되지 않은 위험 유형입니다.");
        }
        changes.setHazardType(type);
        if (changes.getDescription() != null && changes.getDescription().length() > HazardAppService.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("설명은 " + HazardAppService.MAX_DESCRIPTION_LENGTH + "자 이하로 입력해 주세요.");
        }
        Integer radius = changes.rawRadiusM();
        if (radius != null && (radius < HazardAppService.MIN_RADIUS_M || radius > HazardAppService.MAX_RADIUS_M)) {
            throw new IllegalArgumentException("위험 반경은 " + HazardAppService.MIN_RADIUS_M + "~"
                    + HazardAppService.MAX_RADIUS_M + "m 입니다.");
        }
        String lat = trimToNull(changes.getLatitude());
        String lon = trimToNull(changes.getLongitude());
        if (lat != null || lon != null) {
            Double parsedLat = parseCoordinate(lat);
            Double parsedLon = parseCoordinate(lon);
            if (!HazardGeo.isValidCoordinate(parsedLat, parsedLon)) {
                throw new IllegalArgumentException("위도·경도가 올바르지 않습니다.");
            }
            changes.setLatitude(coordinateText(parsedLat));
            changes.setLongitude(coordinateText(parsedLon));
        } else {
            changes.setLatitude(null);
            changes.setLongitude(null);
        }
    }

    // ---- registering a zone directly -----------------------------------------------------------------

    /**
     * An operator registers a zone: active and approved at once, source ADMIN, with a history entry. If zones
     * that may be the same hazard exist the call is refused (409 DUPLICATE_CANDIDATES, with the candidates)
     * until the operator confirms with {@code acknowledgeDuplicates}.
     */
    public HazardReview createByAdmin(AdminHazardRequest request, String adminId) {
        if (request == null) throw new IllegalArgumentException("요청 내용이 없습니다.");
        Double lat = request.getLatitude();
        Double lon = request.getLongitude();
        if (!HazardGeo.isValidCoordinate(lat, lon) || (lat == 0.0d && lon == 0.0d)) {
            throw new IllegalArgumentException("위치를 지도에서 선택해 주세요.");
        }
        HazardType type = HazardType.parse(request.getType());
        if (type == null) throw new IllegalArgumentException("위험 유형을 선택해 주세요.");
        String title = trimToNull(request.getTitle());
        if (title == null || title.length() > HazardAppService.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("제목은 1~" + HazardAppService.MAX_TITLE_LENGTH + "자로 입력해 주세요.");
        }
        String description = request.getDescription() == null ? "" : request.getDescription().trim();
        if (description.length() > HazardAppService.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("설명은 " + HazardAppService.MAX_DESCRIPTION_LENGTH + "자 이하로 입력해 주세요.");
        }
        int radius = request.getRadiusM() == null ? HazardAppService.DEFAULT_RADIUS_M : request.getRadiusM();
        if (radius < HazardAppService.MIN_RADIUS_M || radius > HazardAppService.MAX_RADIUS_M) {
            throw new IllegalArgumentException("위험 반경은 " + HazardAppService.MIN_RADIUS_M + "~"
                    + HazardAppService.MAX_RADIUS_M + "m 입니다.");
        }
        String address = trimToNull(request.getAddress());
        if (address != null && address.length() > 200) {
            throw new IllegalArgumentException("주소는 200자 이하로 입력해 주세요.");
        }

        if (!request.isAcknowledgeDuplicates()) {
            List<HazardDuplicateFinder.Candidate> candidates = findCandidates(
                    new HazardDuplicateFinder.Probe(lat, lon, radius, type.name(), title, description, 0));
            if (!candidates.isEmpty()) {
                throw new HazardException(HttpStatus.CONFLICT, "DUPLICATE_CANDIDATES",
                        "근처에 같은 위험지역일 수 있는 항목이 " + candidates.size() + "건 있습니다. 확인해 주세요.")
                        .with("candidates", candidateMaps(candidates));
            }
        }

        HazardRow row = new HazardRow();
        row.setReporter("admin:" + adminId);
        row.setAddress(address == null ? "" : address);
        row.setDescription(description);
        row.setRadiusM(radius);
        mapper.insertAdminDanger(row, coordinateText(lat), coordinateText(lon));
        mapper.insertAdminReview(row.getId(), title, type.name(), adminId);
        mapper.insertHistory(row.getId(), "ADMIN_CREATE", null, HazardTransitions.APPROVED, adminId, "운영자 직접 등록");
        return required(row.getId());
    }

    // ---- duplicate candidates ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> duplicatesOf(int id) {
        HazardReview zone = required(id);
        Double lat = parseCoordinate(zone.getLatitude());
        Double lon = parseCoordinate(zone.getLongitude());
        if (!HazardGeo.isValidCoordinate(lat, lon)) return Collections.emptyList();
        return candidateMaps(findCandidates(new HazardDuplicateFinder.Probe(lat, lon, zone.getRadiusM(),
                zone.getHazardType(), zone.getTitle(), zone.getDescription(), id)));
    }

    /** Candidates for a zone that is not saved yet (the registration form). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> duplicatesFor(AdminHazardRequest request) {
        if (request == null || !HazardGeo.isValidCoordinate(request.getLatitude(), request.getLongitude())) {
            throw new IllegalArgumentException("위치를 지도에서 선택해 주세요.");
        }
        int radius = request.getRadiusM() == null ? HazardAppService.DEFAULT_RADIUS_M : request.getRadiusM();
        return candidateMaps(findCandidates(new HazardDuplicateFinder.Probe(request.getLatitude(), request.getLongitude(),
                radius, request.getType(), request.getTitle(), request.getDescription(), 0)));
    }

    private List<HazardDuplicateFinder.Candidate> findCandidates(HazardDuplicateFinder.Probe probe) {
        HazardGeo.Box box = HazardGeo.boundingBox(probe.latitude, probe.longitude,
                probe.radiusM + HazardAppService.MAX_RADIUS_M + HazardDuplicateFinder.NEAR_BUFFER_M);
        List<HazardRow> zones = appMapper.findVisibleInBox(probe.latitude, probe.longitude,
                box.minLat, box.maxLat, box.minLon, box.maxLon, SCAN_LIMIT);
        List<HazardDuplicateFinder.Candidate> found = HazardDuplicateFinder.find(probe, zones);
        return found.size() > MAX_CANDIDATES ? new ArrayList<HazardDuplicateFinder.Candidate>(found.subList(0, MAX_CANDIDATES)) : found;
    }

    private List<Map<String, Object>> candidateMaps(List<HazardDuplicateFinder.Candidate> candidates) {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (HazardDuplicateFinder.Candidate candidate : candidates) {
            HazardRow row = candidate.row;
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("id", row.getId());
            map.put("title", row.getTitle());
            map.put("hazardType", row.getHazardType());
            map.put("hazardTypeLabel", HazardLabels.type(row.getHazardType()));
            map.put("status", row.getStatus());
            map.put("statusLabel", HazardLabels.status(row.getStatus(), false));
            map.put("latitude", row.getLatitude());
            map.put("longitude", row.getLongitude());
            map.put("radiusM", row.getRadiusM() > 0 ? row.getRadiusM() : 30);
            map.put("distanceM", Math.round(candidate.distanceM * 10.0d) / 10.0d);
            map.put("overlap", candidate.overlap);
            map.put("sameType", candidate.sameType);
            map.put("score", candidate.score);
            map.put("reasons", candidate.reasons);
            maps.add(map);
        }
        return maps;
    }

    // ---- merging -------------------------------------------------------------------------------------

    /** What a merge would do and what is wrong with it, without changing anything. */
    @Transactional(readOnly = true)
    public Map<String, Object> previewMerge(HazardMergeRequest request) {
        MergePlan plan = plan(request);
        Map<String, Object> preview = new LinkedHashMap<String, Object>();
        preview.put("canMerge", plan.errors.isEmpty());
        preview.put("errors", plan.errors);
        preview.put("warnings", plan.warnings);
        preview.put("needsTypeConfirmation", plan.typeMismatch);
        preview.put("canonical", plan.canonical);
        preview.put("sources", plan.sources);
        preview.put("after", plan.after());
        preview.put("retainedReports", plan.sources.size());
        return preview;
    }

    /**
     * Folds the source zones into the representative one, in one transaction: any failure rolls everything
     * back. Sources keep their rows, reporter and history; they get status MERGED and merged_into, so riders
     * stop receiving them. Zones that were merged into a source earlier now point at the representative.
     */
    public HazardReview merge(HazardMergeRequest request, String adminId) {
        MergePlan plan = plan(request);
        if (!plan.errors.isEmpty()) {
            Map<String, String> first = plan.errors.get(0);
            throw new HazardException(HttpStatus.CONFLICT, first.get("code"), first.get("message"));
        }
        if (plan.typeMismatch && !request.isConfirmTypeMismatch()) {
            throw new HazardException(HttpStatus.CONFLICT, "TYPE_MISMATCH",
                    "위험 유형이 서로 다른 위험지역입니다. 그래도 병합하려면 확인해 주세요.");
        }
        HazardReview canonical = plan.canonical;
        int canonicalId = canonical.getId();
        String canonicalBefore = snapshot(canonical);

        mapper.ensureReview(canonicalId);
        if (plan.hasEdits) {
            HazardReview edits = new HazardReview();
            edits.setId(canonicalId);
            edits.setTitle(plan.title);
            edits.setHazardType(plan.type);
            edits.setDescription(plan.description);
            edits.setRadiusM(plan.radiusM == null ? canonical.getRadiusM() : plan.radiusM);
            edits.setLatitude(plan.latitude);
            edits.setLongitude(plan.longitude);
            mapper.updateDangerDetails(edits);
            mapper.updateReviewDetails(edits);
        }

        String group = UUID.randomUUID().toString().replace("-", "");
        Map<Integer, String> beforeSources = new LinkedHashMap<Integer, String>();
        for (HazardReview source : plan.sources) {
            int sourceId = source.getId();
            beforeSources.put(sourceId, snapshot(source));
            mapper.ensureReview(sourceId);
            if (mapper.mergeHazard(sourceId, canonicalId) != 1) {
                throw new HazardException(HttpStatus.CONFLICT, "STALE",
                        "위험지역 #" + sourceId + "이(가) 방금 다른 곳에서 변경되었습니다. 화면을 새로고침해 주세요.");
            }
            mapper.updateLegacyStatus(sourceId, "4");
            mapper.repointMerged(sourceId, canonicalId);
            mapper.insertHistory(sourceId, "MERGE", source.getStatus(), HazardTransitions.MERGED, adminId,
                    "위험지역 #" + canonicalId + "로 병합");
        }

        HazardReview canonicalAfter = required(canonicalId);
        String canonicalAfterJson = snapshot(canonicalAfter);
        for (Map.Entry<Integer, String> entry : beforeSources.entrySet()) {
            mapper.insertMerge(group, canonicalId, entry.getKey(), adminId, entry.getValue(), canonicalBefore, canonicalAfterJson);
        }
        mapper.insertHistory(canonicalId, "MERGE_RECEIVED", canonical.getStatus(), canonicalAfter.getStatus(), adminId,
                "위험지역 " + beforeSources.keySet() + " 병합");
        return canonicalAfter;
    }

    /** Everything the merge preview and the merge itself check, so they can never disagree. */
    private MergePlan plan(HazardMergeRequest request) {
        if (request == null || request.getCanonicalId() == null) {
            throw new IllegalArgumentException("대표 위험지역을 선택해 주세요.");
        }
        List<Integer> sourceIds = request.getSourceIds() == null ? Collections.<Integer>emptyList() : request.getSourceIds();
        if (sourceIds.isEmpty()) throw new IllegalArgumentException("병합할 위험지역을 1건 이상 선택해 주세요.");
        Set<Integer> unique = new LinkedHashSet<Integer>(sourceIds);
        if (unique.size() != sourceIds.size()) throw new IllegalArgumentException("같은 위험지역이 중복 선택되었습니다.");
        if (unique.size() > MAX_MERGE_SOURCES) {
            throw new IllegalArgumentException("한 번에 최대 " + MAX_MERGE_SOURCES + "건까지 병합할 수 있습니다.");
        }
        int canonicalId = request.getCanonicalId();
        if (unique.contains(canonicalId)) {
            throw new IllegalArgumentException("대표 위험지역은 병합 대상에 포함할 수 없습니다.");
        }

        List<Integer> all = new ArrayList<Integer>(unique);
        all.add(canonicalId);
        Map<Integer, HazardReview> byId = new LinkedHashMap<Integer, HazardReview>();
        for (HazardReview zone : mapper.findByIds(all)) byId.put(zone.getId(), zone);
        for (Integer id : all) {
            if (!byId.containsKey(id)) {
                throw new HazardException(HttpStatus.NOT_FOUND, "NOT_FOUND", "위험지역 #" + id + "을(를) 찾을 수 없습니다.");
            }
        }

        MergePlan plan = new MergePlan();
        plan.canonical = byId.get(canonicalId);
        for (Integer id : unique) plan.sources.add(byId.get(id));

        if (plan.canonical.isMerged()) {
            plan.error("INVALID_CANONICAL", "대표 위험지역 #" + canonicalId + "은(는) 이미 다른 위험지역에 병합되었습니다.");
        } else if (!HazardTransitions.isVisibleToRiders(plan.canonical.getStatus())) {
            plan.error("INVALID_CANONICAL", "대표 위험지역은 '"
                    + HazardLabels.status(plan.canonical.getStatus(), false) + "' 상태입니다. 활성 상태의 위험지역을 대표로 선택해 주세요.");
        }
        for (HazardReview source : plan.sources) {
            if (source.isMerged()) {
                plan.error("ALREADY_MERGED", "위험지역 #" + source.getId() + "은(는) 이미 병합되었습니다.");
            }
        }

        applyEdits(plan, request);

        Set<String> types = new HashSet<String>();
        types.add(plan.type == null ? plan.canonical.getHazardType() : plan.type);
        for (HazardReview source : plan.sources) types.add(source.getHazardType());
        if (types.size() > 1) {
            plan.typeMismatch = true;
            plan.warnings.add(warning("TYPE_MISMATCH", "병합 대상의 위험 유형이 서로 다릅니다. 대표 위험지역의 유형으로 합쳐집니다."));
        }
        if (!"APPROVED".equals(plan.canonical.getStatus())) {
            for (HazardReview source : plan.sources) {
                if ("APPROVED".equals(source.getStatus())) {
                    plan.warnings.add(warning("VERIFICATION_LOST",
                            "승인된 위험지역 #" + source.getId() + "이(가) 승인되지 않은 대표 위험지역으로 병합됩니다. 대표를 승인된 항목으로 고르는 것을 권장합니다."));
                    break;
                }
            }
        }
        return plan;
    }

    private void applyEdits(MergePlan plan, HazardMergeRequest request) {
        String title = trimToNull(request.getTitle());
        String type = trimToNull(request.getHazardType());
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        Integer radius = request.getRadiusM();
        String lat = trimToNull(request.getLatitude());
        String lon = trimToNull(request.getLongitude());

        if (title != null && title.length() > MAX_EDIT_TITLE_LENGTH) {
            plan.error("INVALID_TITLE", "제목은 " + MAX_EDIT_TITLE_LENGTH + "자 이하로 입력해 주세요.");
        }
        if (type != null && HazardType.parse(type) == null && !type.equals(plan.canonical.getHazardType())
                && !LEGACY_TYPES.contains(type)) {
            plan.error("INVALID_TYPE", "허용되지 않은 위험 유형입니다.");
        }
        if (description != null && description.length() > HazardAppService.MAX_DESCRIPTION_LENGTH) {
            plan.error("INVALID_DESCRIPTION", "설명은 " + HazardAppService.MAX_DESCRIPTION_LENGTH + "자 이하로 입력해 주세요.");
        }
        if (radius != null && (radius < HazardAppService.MIN_RADIUS_M || radius > HazardAppService.MAX_RADIUS_M)) {
            plan.error("INVALID_RADIUS", "위험 반경은 " + HazardAppService.MIN_RADIUS_M + "~" + HazardAppService.MAX_RADIUS_M + "m 입니다.");
        }
        if (lat != null || lon != null) {
            Double parsedLat = parseCoordinate(lat);
            Double parsedLon = parseCoordinate(lon);
            if (HazardGeo.isValidCoordinate(parsedLat, parsedLon)) {
                lat = coordinateText(parsedLat);
                lon = coordinateText(parsedLon);
            } else {
                plan.error("INVALID_COORDINATE", "위도·경도가 올바르지 않습니다.");
            }
        }
        plan.title = title;
        plan.type = type;
        plan.description = description;
        plan.radiusM = radius;
        plan.latitude = lat;
        plan.longitude = lon;
        plan.hasEdits = title != null || type != null || description != null || radius != null || lat != null || lon != null;
    }

    private static Map<String, String> warning(String code, String message) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("code", code);
        map.put("message", message);
        return map;
    }

    private static final class MergePlan {
        HazardReview canonical;
        final List<HazardReview> sources = new ArrayList<HazardReview>();
        final List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        final List<Map<String, String>> warnings = new ArrayList<Map<String, String>>();
        boolean typeMismatch;
        boolean hasEdits;
        String title;
        String type;
        String description;
        Integer radiusM;
        String latitude;
        String longitude;

        void error(String code, String message) {
            errors.add(warning(code, message));
        }

        /** The representative as it will be after the merge. */
        Map<String, Object> after() {
            String effectiveType = type == null ? canonical.getHazardType() : type;
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("id", canonical.getId());
            map.put("title", title == null ? canonical.getTitle() : title);
            map.put("hazardType", effectiveType);
            map.put("hazardTypeLabel", HazardLabels.type(effectiveType));
            map.put("description", description == null ? canonical.getDescription() : description);
            map.put("radiusM", radiusM == null ? canonical.getRadiusM() : radiusM);
            map.put("latitude", latitude == null ? canonical.getLatitude() : latitude);
            map.put("longitude", longitude == null ? canonical.getLongitude() : longitude);
            map.put("status", canonical.getStatus());
            map.put("statusLabel", canonical.getStatusLabel());
            return map;
        }
    }

    // ---- helpers ------------------------------------------------------------------------------------

    private String snapshot(HazardReview zone) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", zone.getId());
        map.put("title", zone.getTitle());
        map.put("hazardType", zone.getHazardType());
        map.put("status", zone.getStatus());
        map.put("latitude", zone.getLatitude());
        map.put("longitude", zone.getLongitude());
        map.put("radiusM", zone.getRadiusM());
        map.put("description", zone.getDescription());
        map.put("reporter", zone.getReporter());
        map.put("source", zone.getSource());
        map.put("createdAt", zone.getCreatedAt() == null ? null : zone.getCreatedAt().toString());
        map.put("snapshotAt", LocalDateTime.now().toString());
        try {
            return json.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("snapshot failed", e);
        }
    }

    private HazardReview required(int id) {
        HazardReview hazard = mapper.findById(id);
        if (hazard == null) throw new HazardException(HttpStatus.NOT_FOUND, "NOT_FOUND", "위험지역을 찾을 수 없습니다.");
        return hazard;
    }

    private void requireNotMerged(HazardReview zone) {
        if (zone.isMerged()) {
            throw new HazardException(HttpStatus.CONFLICT, "MERGED_LOCKED",
                    "병합된 위험지역은 수정하거나 상태를 바꿀 수 없습니다. 대표 위험지역 #" + zone.getMergedInto() + "을(를) 사용해 주세요.");
        }
    }

    private String statusAction(String status) {
        if ("INACTIVE".equals(status)) return "HIDDEN";
        if ("PENDING".equals(status)) return "RESTORED";
        return status;
    }

    private String legacyStatus(String status) {
        if ("APPROVED".equals(status)) return "1";
        if ("REJECTED".equals(status)) return "2";
        if ("REVIEWING".equals(status)) return "3";
        if ("INACTIVE".equals(status) || "EXPIRED".equals(status) || "MERGED".equals(status)) return "4";
        return "0";
    }

    private static Double parseCoordinate(String value) {
        if (value == null) return null;
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String coordinateText(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
