package com.bike.webapp.dto;

import java.sql.Date;

public class Rider {
	private String r_id;
	private String r_nickname;
	private String r_phone;
	private int r_gender;
	private String r_addr;
	private String r_addr2;
	private int r_badge;
	private String r_image;
	private int r_point;
	private String r_regdate;
	private String badge;
	private int total;
	
	public String getBadge() {
		return badge;
	}
	public void setBadge(String badge) {
		this.badge = badge;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public String getR_regdate() {
		return r_regdate;
	}
	public void setR_regdate(String r_regdate) {
		this.r_regdate = r_regdate;
	}
	public String getR_id() {
		return r_id;
	}
	public void setR_id(String r_id) {
		this.r_id = r_id;
	}
	public String getR_nickname() {
		return r_nickname;
	}
	public void setR_nickname(String r_nickname) {
		this.r_nickname = r_nickname;
	}
	public String getR_phone() {
		return r_phone;
	}
	public void setR_phone(String r_phone) {
		this.r_phone = r_phone;
	}
	public int getR_gender() {
		return r_gender;
	}
	public void setR_gender(int r_gender) {
		this.r_gender = r_gender;
	}
	public String getR_addr() {
		return r_addr;
	}
	public void setR_addr(String r_addr) {
		this.r_addr = r_addr;
	}
	public String getR_addr2() {
		return r_addr2;
	}
	public void setR_addr2(String r_addr2) {
		this.r_addr2 = r_addr2;
	}
	public int getR_badge() {
		return r_badge;
	}
	public void setR_badge(int r_badge) {
		this.r_badge = r_badge;
	}
	public String getR_image() {
		return r_image;
	}
	public void setR_image(String r_image) {
		this.r_image = r_image;
	}
	public int getR_point() {
		return r_point;
	}
	public void setR_point(int r_point) {
		this.r_point = r_point;
	}
	
}
