package com.bike.webapp.dto;

/** What the server needs to decide whether a photo may be served. */
public class PhotoAccess {

	private String fileName;
	private String ownerId;
	private boolean publicRecord;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public boolean isPublicRecord() {
		return publicRecord;
	}

	public void setPublicRecord(boolean publicRecord) {
		this.publicRecord = publicRecord;
	}
}