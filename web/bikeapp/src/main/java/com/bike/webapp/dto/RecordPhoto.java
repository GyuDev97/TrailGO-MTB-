package com.bike.webapp.dto;

/** One photo attached to a riding record. The id is an unguessable token, not a sequence number. */
public class RecordPhoto {

	private String id;
	private int sortOrder;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}