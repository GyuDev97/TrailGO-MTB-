package com.bike.webapp.dto;

import java.util.Date;

public class App_CourseReview {

	private int cr_num;
	private String cr_rider;
	private Date cr_time;
	private String cr_content;
	private int cr_rnum;
	private String cr_images;

	private String r_nickname;
	private String r_image;
	
	

	public String getR_image() {
		return r_image;
	}

	public void setR_image(String r_image) {
		this.r_image = r_image;
	}

	public String getR_nickname() {
		return r_nickname;
	}

	public void setR_nickname(String r_nickname) {
		this.r_nickname = r_nickname;
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

	public Date getCr_time() {
		return cr_time;
	}

	public void setCr_time(Date cr_time) {
		this.cr_time = cr_time;
	}

	public String getCr_content() {
		return cr_content;
	}

	public void setCr_content(String cr_content) {
		this.cr_content = cr_content;
	}

	public int getCr_rnum() {
		return cr_rnum;
	}

	public void setCr_rnum(int cr_rnum) {
		this.cr_rnum = cr_rnum;
	}

	public String getCr_images() {
		return cr_images;
	}

	public void setCr_images(String cr_images) {
		this.cr_images = cr_images;
	}

}
