package com.bike.webapp.dto;

import java.time.LocalDateTime;

/** One merged zone in the audit trail (trailgo_hazard_merge). The before/after columns are JSON snapshots. */
public class HazardMergeRecord {
    private Long id;
    private String mergeGroup;
    private Integer canonicalId;
    private Integer mergedId;
    private String adminId;
    private LocalDateTime mergedAt;
    private String mergedBefore;
    private String canonicalBefore;
    private String canonicalAfter;
    private String canonicalTitle;
    private String mergedTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMergeGroup() { return mergeGroup; }
    public void setMergeGroup(String mergeGroup) { this.mergeGroup = mergeGroup; }
    public Integer getCanonicalId() { return canonicalId; }
    public void setCanonicalId(Integer canonicalId) { this.canonicalId = canonicalId; }
    public Integer getMergedId() { return mergedId; }
    public void setMergedId(Integer mergedId) { this.mergedId = mergedId; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public LocalDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(LocalDateTime mergedAt) { this.mergedAt = mergedAt; }
    public String getMergedBefore() { return mergedBefore; }
    public void setMergedBefore(String mergedBefore) { this.mergedBefore = mergedBefore; }
    public String getCanonicalBefore() { return canonicalBefore; }
    public void setCanonicalBefore(String canonicalBefore) { this.canonicalBefore = canonicalBefore; }
    public String getCanonicalAfter() { return canonicalAfter; }
    public void setCanonicalAfter(String canonicalAfter) { this.canonicalAfter = canonicalAfter; }
    public String getCanonicalTitle() { return canonicalTitle; }
    public void setCanonicalTitle(String canonicalTitle) { this.canonicalTitle = canonicalTitle; }
    public String getMergedTitle() { return mergedTitle; }
    public void setMergedTitle(String mergedTitle) { this.mergedTitle = mergedTitle; }
}
