package com.bike.webapp.dto;

public class EntertainRider {
	private String nickname;
	private String user_img;
	private String bg_img;
	private int bg_num;
	
	public int getBg_num() {
		return bg_num;
	}
	public void setBg_num(int bg_num) {
		this.bg_num = bg_num;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public String getUser_img() {
		return user_img;
	}
	public void setUser_img(String user_img) {
		this.user_img = user_img;
	}
	public String getBg_img() {
		return bg_img;
	}
	public void setBg_img(String bg_img) {
		this.bg_img = bg_img;
	}
}
