package com.bike.webapp.controller.admin;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bike.webapp.dto.AdminHazardRequest;
import com.bike.webapp.dto.HazardMergeRecord;
import com.bike.webapp.dto.HazardMergeRequest;
import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.service.HazardAdminService;

/**
 * JSON API of the admin hazard screens. Everything under /admin/** requires ROLE_ADMIN (SecurityConfig);
 * the operator's name is taken from the login, never from the request.
 */
@RestController
@RequestMapping("/admin/api/hazards")
public class AdminHazardApiController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final HazardAdminService hazardService;

    @Autowired
    public AdminHazardApiController(HazardAdminService hazardService) {
        this.hazardService = hazardService;
    }

    // ---- review ---------------------------------------------------------------------------------------

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable int id, Principal principal) {
        hazardService.approve(id, principal.getName());
        return success(id);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable int id,
                                      @RequestBody(required = false) DecisionRequest request,
                                      Principal principal) {
        hazardService.reject(id, principal.getName(), request == null ? null : request.getReason());
        return success(id);
    }

    @PostMapping("/{id}/hide")
    public Map<String, Object> hide(@PathVariable int id,
                                    @RequestBody(required = false) DecisionRequest request,
                                    Principal principal) {
        hazardService.hide(id, principal.getName(), request == null ? null : request.getReason());
        return success(id);
    }

    @PostMapping("/{id}/restore")
    public Map<String, Object> restore(@PathVariable int id, Principal principal) {
        hazardService.restore(id, principal.getName());
        return success(id);
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable int id,
                                      @RequestBody HazardReview changes,
                                      Principal principal) {
        hazardService.update(id, changes, principal.getName());
        return success(id);
    }

    // ---- registering ----------------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody AdminHazardRequest request, Principal principal) {
        HazardReview created = hazardService.createByAdmin(request, principal.getName());
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("hazard", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ---- duplicates and merging -----------------------------------------------------------------------

    @PostMapping("/duplicates")
    public Map<String, Object> duplicatesForNew(@RequestBody AdminHazardRequest request) {
        return Collections.<String, Object>singletonMap("candidates", hazardService.duplicatesFor(request));
    }

    @GetMapping("/{id}/duplicates")
    public Map<String, Object> duplicates(@PathVariable int id) {
        return Collections.<String, Object>singletonMap("candidates", hazardService.duplicatesOf(id));
    }

    @PostMapping("/merge/preview")
    public Map<String, Object> previewMerge(@RequestBody HazardMergeRequest request) {
        return hazardService.previewMerge(request);
    }

    @PostMapping("/merge")
    public Map<String, Object> merge(@RequestBody HazardMergeRequest request, Principal principal) {
        HazardReview result = hazardService.merge(request, principal.getName());
        return Collections.<String, Object>singletonMap("hazard", result);
    }

    /** Older single-zone form: fold {id} into targetId. Zones of different types still need the explicit merge call. */
    @PostMapping("/{id}/merge")
    public Map<String, Object> mergeInto(@PathVariable int id, @RequestBody MergeRequest request, Principal principal) {
        HazardMergeRequest merge = new HazardMergeRequest();
        merge.setCanonicalId(request.getTargetId());
        merge.setSourceIds(Collections.singletonList(id));
        hazardService.merge(merge, principal.getName());
        return success(id);
    }

    @GetMapping("/merges")
    public Map<String, Object> merges(@RequestParam(required = false) Integer canonicalId,
                                      @RequestParam(defaultValue = "100") int limit) {
        List<HazardMergeRecord> records = hazardService.merges(canonicalId, limit);
        return Collections.<String, Object>singletonMap("merges", records);
    }

    // ---- errors ---------------------------------------------------------------------------------------

    @ExceptionHandler(HazardException.class)
    public ResponseEntity<Map<String, Object>> refused(HazardException exception) {
        return ResponseEntity.status(exception.getStatus()).body(exception.body());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    /** E.g. the unique key on merged zones: two operators merged the same zone at the same moment. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException exception) {
        log.warn("Hazard admin conflict: {}", exception.getClass().getSimpleName());
        return error(HttpStatus.CONFLICT, "CONFLICT", "다른 곳에서 이미 처리된 항목입니다. 화면을 새로고침해 주세요.");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> failed(Exception exception) {
        log.error("Hazard admin API failed", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> success(int id) {
        return Collections.<String, Object>singletonMap("hazard", hazardService.findById(id));
    }

    public static class DecisionRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class MergeRequest {
        private int targetId;
        public int getTargetId() { return targetId; }
        public void setTargetId(int targetId) { this.targetId = targetId; }
    }
}
