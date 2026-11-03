package com.bike.webapp.dto;

/**
 * A hazard as the app sees it. No reporter id: only {@code mine} says whether the caller reported it.
 * {@code verified} is true only for an operator-approved report; everything else is a rider's unverified report.
 */
public class HazardView {
	private int id;
	private double latitude;
	private double longitude;
	private int radiusM;
	private String type;
	private String typeLabel;
	private String title;
	private String description;
	private String address;
	private boolean verified;
	private long createdAtMs;
	private long updatedAtMs;
	private boolean mine;
	/** Metres from the query point to the hazard centre; only set by the nearby search. */
	private Double distanceM;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public int getRadiusM() {
		return radiusM;
	}
	public void setRadiusM(int radiusM) {
		this.radiusM = radiusM;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTypeLabel() {
		return typeLabel;
	}
	public void setTypeLabel(String typeLabel) {
		this.typeLabel = typeLabel;
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
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public boolean isVerified() {
		return verified;
	}
	public void setVerified(boolean verified) {
		this.verified = verified;
	}
	public long getCreatedAtMs() {
		return createdAtMs;
	}
	public void setCreatedAtMs(long createdAtMs) {
		this.createdAtMs = createdAtMs;
	}
	public long getUpdatedAtMs() {
		return updatedAtMs;
	}
	public void setUpdatedAtMs(long updatedAtMs) {
		this.updatedAtMs = updatedAtMs;
	}
	public boolean isMine() {
		return mine;
	}
	public void setMine(boolean mine) {
		this.mine = mine;
	}
	public Double getDistanceM() {
		return distanceM;
	}
	public void setDistanceM(Double distanceM) {
		this.distanceM = distanceM;
	}
}
