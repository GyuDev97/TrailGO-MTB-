package com.bike.trailgo.dto;

public class LoginInfo {
	
	private int cj_club;
	private String r_nickname;
	private String r_image;
	private String cb_name;

	public String getCb_name() {
		return cb_name;
	}

	public void setCb_name(String cb_name) {
		this.cb_name = cb_name;
	}

	public int getCj_club() {
		return cj_club;
	}
	public void setCj_club(int cj_club) {
		this.cj_club = cj_club;
	}
	public String getR_nickname() {
		return r_nickname;
	}
	public void setR_nickname(String r_nickname) {
		this.r_nickname = r_nickname;
	}
	public String getR_image() {
		return r_image;
	}
	public void setR_image(String r_image) {
		this.r_image = r_image;
	}
	
	
}
