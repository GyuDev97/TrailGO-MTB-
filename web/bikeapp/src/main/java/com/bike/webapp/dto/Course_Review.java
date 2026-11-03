package com.bike.webapp.dto;

import java.sql.Timestamp;

public class Course_Review {
	private int cr_num;
	private String cr_rider;
	private Timestamp cr_time;
	private int cr_rnum;
	private String cr_content;
	private String cr_images;
	private String riderimg;
	private String reg_time;
	
	public String getReg_time() {
		return reg_time;
	}
	public void setReg_time(String reg_time) {
		this.reg_time = reg_time;
	}
	public String getRiderimg() {
		return riderimg;
	}
	public void setRiderimg(String riderimg) {
		this.riderimg = riderimg;
	}
	public String getCr_images() {
		return cr_images;
	}
	public void setCr_images(String cr_images) {
		this.cr_images = cr_images;
	}
	public String getCr_content() {
		return cr_content;
	}
	public void setCr_content(String cr_content) {
		this.cr_content = cr_content;
	}
	public int getCr_num() {
		return cr_num;
	}
	public void setCr_num(int cr_num) {
		this.cr_num = cr_num;
	}
	public String getCr_rider() {
		return cr_rider;
	}
	public void setCr_rider(String cr_rider) {
		this.cr_rider = cr_rider;
	}
	public Timestamp getCr_time() {
		return cr_time;
	}
	public void setCr_time(Timestamp cr_time) {
		this.cr_time = cr_time;
	}
	public int getCr_rnum() {
		return cr_rnum;
	}
	public void setCr_rnum(int cr_rnum) {
		this.cr_rnum = cr_rnum;
	}
	
}
