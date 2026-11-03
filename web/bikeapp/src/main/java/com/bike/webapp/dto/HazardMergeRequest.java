package com.bike.webapp.dto;

import java.util.List;

/**
 * Body of the merge preview and of POST /admin/api/hazards/merge: one representative zone and the zones folded
 * into it. The optional fields edit the representative as part of the merge (null = keep as is).
 */
public class HazardMergeRequest {
	private Integer canonicalId;
	private List<Integer> sourceIds;
	private String title;
	private String hazardType;
	private String description;
	private Integer radiusM;
	private String latitude;
	private String longitude;
	/** The zones have different hazard types and the operator confirmed that this is intended. */
	private boolean confirmTypeMismatch;

	public Integer getCanonicalId() { return canonicalId; }
	public void setCanonicalId(Integer canonicalId) { this.canonicalId = canonicalId; }
	public List<Integer> getSourceIds() { return sourceIds; }
	public void setSourceIds(List<Integer> sourceIds) { this.sourceIds = sourceIds; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getHazardType() { return hazardType; }
	public void setHazardType(String hazardType) { this.hazardType = hazardType; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public Integer getRadiusM() { return radiusM; }
	public void setRadiusM(Integer radiusM) { this.radiusM = radiusM; }
	public String getLatitude() { return latitude; }
	public void setLatitude(String latitude) { this.latitude = latitude; }
	public String getLongitude() { return longitude; }
	public void setLongitude(String longitude) { this.longitude = longitude; }
	public boolean isConfirmTypeMismatch() { return confirmTypeMismatch; }
	public void setConfirmTypeMismatch(boolean confirmTypeMismatch) { this.confirmTypeMismatch = confirmTypeMismatch; }
}
