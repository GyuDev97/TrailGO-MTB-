package com.bike.webapp.dto;

import java.time.LocalDateTime;

public class HazardReview {
    private Integer id;
    private String reporter;
    private String latitude;
    private String longitude;
    private String address;
    private String image;
    private String description;
    private String legacyStatus;
    private String title;
    private String hazardType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String rejectionReason;
    private Integer confirmCount;
    private Integer reportCount;
    private Boolean active;
    private Integer mergedInto;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLegacyStatus() { return legacyStatus; }
    public void setLegacyStatus(String legacyStatus) { this.legacyStatus = legacyStatus; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getHazardType() { return hazardType; }
    public void setHazardType(String hazardType) { this.hazardType = hazardType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Integer getConfirmCount() { return confirmCount; }
    public void setConfirmCount(Integer confirmCount) { this.confirmCount = confirmCount; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getMergedInto() { return mergedInto; }
    public void setMergedInto(Integer mergedInto) { this.mergedInto = mergedInto; }

    private Integer radiusM;
    /** USER (rider report, also every older row) or ADMIN (registered by an operator). */
    private String source;
    private String createdBy;

    public Integer getRadiusM() { return radiusM == null || radiusM <= 0 ? Integer.valueOf(30) : radiusM; }
    public void setRadiusM(Integer radiusM) { this.radiusM = radiusM; }
    /** The radius as sent, null when the caller did not send one ({@link #getRadiusM()} falls back to 30). */
    public Integer rawRadiusM() { return radiusM; }
    public String getSource() { return source == null ? "USER" : source; }
    public void setSource(String source) { this.source = source; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isAdminSource() { return "ADMIN".equals(getSource()); }
    public boolean isMerged() { return "MERGED".equals(status) || mergedInto != null; }

    public String getSourceLabel() { return isAdminSource() ? "운영자 등록" : "사용자 제보"; }

    public String getStatusLabel() { return com.bike.webapp.hazard.HazardLabels.status(status, isMerged()); }

    /** What the buttons may offer, from the same table the service enforces. */
    public boolean isCanApprove() { return canMoveTo("APPROVED"); }
    public boolean isCanReject() { return canMoveTo("REJECTED"); }
    public boolean isCanHide() { return canMoveTo("INACTIVE"); }
    public boolean isCanRestore() { return canMoveTo("PENDING") && ("INACTIVE".equals(status) || "REJECTED".equals(status) || "EXPIRED".equals(status)); }
    public boolean isCanEdit() { return !isMerged(); }

    private boolean canMoveTo(String next) {
        return !isMerged() && com.bike.webapp.hazard.HazardTransitions.canMove(status, next);
    }

    public String getHazardTypeLabel() { return com.bike.webapp.hazard.HazardLabels.type(hazardType); }
}
