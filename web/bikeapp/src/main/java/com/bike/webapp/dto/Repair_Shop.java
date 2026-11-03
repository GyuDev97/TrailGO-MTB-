package com.bike.webapp.dto;

public class Repair_Shop {
	private String rs_id;
	private String rs_info;
	private String rs_holiday;
	private String rs_time;
	private String r_phone;
	private String r_addr;
	private String r_addr2;
	private String r_image;
	private String r_nickname;
	private String rs_lat;
	private String rs_lon;
	
	
	public String getRs_lat() {
		return rs_lat;
	}
	public void setRs_lat(String rs_lat) {
		this.rs_lat = rs_lat;
	}
	public String getRs_lon() {
		return rs_lon;
	}
	public void setRs_lon(String rs_lon) {
		this.rs_lon = rs_lon;
	}
	public void setOpentime(String opentime) {
		this.opentime = opentime;
	}
	
	/* 웹출력용변수 */
	private String opentime;
	
	
	
	public String getOpentime() {
		String str = rs_time.substring(0, 2)+":"+rs_time.substring(2, 4)+" ~ "+rs_time.substring(4, 6)+":"+rs_time.substring(6);
		opentime = str;
		return opentime;
	}
	public String getR_nickname() {
		return r_nickname;
	}
	public void setR_nickname(String r_nickname) {
		this.r_nickname = r_nickname;
	}
	public String getRs_id() {
		return rs_id;
	}
	public void setRs_id(String rs_id) {
		this.rs_id = rs_id;
	}
	public String getRs_info() {
		return rs_info;
	}
	public void setRs_info(String rs_info) {
		this.rs_info = rs_info;
	}
	public String getRs_holiday() {
		return rs_holiday;
	}
	public void setRs_holiday(String rs_holiday) {
		this.rs_holiday = rs_holiday;
	}
	public String getRs_time() {
		return rs_time;
	}
	public void setRs_time(String rs_time) {
		this.rs_time = rs_time;
	}
	public String getR_phone() {
		return r_phone;
	}
	public void setR_phone(String r_phone) {
		this.r_phone = r_phone;
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
	public String getR_image() {
		return r_image;
	}
	public void setR_image(String r_image) {
		this.r_image = r_image;
	}
}
