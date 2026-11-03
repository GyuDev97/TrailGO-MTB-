package com.bike.webapp.dto;

import java.sql.Timestamp;

/** One hazard as stored (dangerous_area joined with trailgo_hazard_review). Never sent to the app as is: it carries the reporter. */
public class HazardRow {
	private int id;
	private String reporter;
	private Double latitude;
	private Double longitude;
	private int radiusM;
	private String address;
	private String description;
	private String title;
	private String hazardType;
	private String status;
	private Timestamp createdAt;
	private Timestamp updatedAt;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getReporter() {
		return reporter;
	}
	public void setReporter(String reporter) {
		this.reporter = reporter;
	}
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
	public int getRadiusM() {
		return radiusM;
	}
	public void setRadiusM(int radiusM) {
		this.radiusM = radiusM;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getHazardType() {
		return hazardType;
	}
	public void setHazardType(String hazardType) {
		this.hazardType = hazardType;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
	public Timestamp getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}
}
