package com.bike.webapp.dto;

/** Body of POST /admin/api/hazards (an operator registers a hazard zone) and of the duplicate check. */
public class AdminHazardRequest extends HazardRequest {
	private String address;
	/** The operator saw the duplicate candidates and wants to register anyway. */
	private boolean acknowledgeDuplicates;

	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public boolean isAcknowledgeDuplicates() {
		return acknowledgeDuplicates;
	}
	public void setAcknowledgeDuplicates(boolean acknowledgeDuplicates) {
		this.acknowledgeDuplicates = acknowledgeDuplicates;
	}
}
