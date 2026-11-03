package com.bike.webapp.dto;

/**
 * Body of POST /app/hazards (and PUT /app/hazards/{id}, which ignores the coordinates: a hazard does not move).
 * The reporter is never part of the body; it is the signed-in rider.
 */
public class HazardRequest {
	private Double latitude;
	private Double longitude;
	private Integer radiusM;
	private String type;
	private String title;
	private String description;

	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	public Integer getRadiusM() {
		return radiusM;
	}
	public void setRadiusM(Integer radiusM) {
		this.radiusM = radiusM;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
