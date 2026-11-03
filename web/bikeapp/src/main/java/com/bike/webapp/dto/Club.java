package com.bike.webapp.dto;

public class Club {
	private int cb_num;

	private String cb_manager;

	private int cb_badge;

	private String cb_name;

	private String cb_image;

	private int cb_member;

	private int cb_premium;
	private String app_userid;
	private int cs_score;
	

	public int getCs_score() {
		return cs_score;
	}

	public void setCs_score(int cs_score) {
		this.cs_score = cs_score;
	}

	public String getApp_userid() {
		return app_userid;
	}

	public void setApp_userid(String app_userid) {
		this.app_userid = app_userid;
	}

	public int getCb_num() {
		return cb_num;
	}

	public void setCb_num(int cb_num) {
		this.cb_num = cb_num;
	}

	public String getCb_manager() {
		return cb_manager;
	}

	public void setCb_manager(String cb_manager) {
		this.cb_manager = cb_manager;
	}

	public int getCb_badge() {
		return cb_badge;
	}

	public void setCb_badge(int cb_badge) {
		this.cb_badge = cb_badge;
	}

	public String getCb_name() {
		return cb_name;
	}

	public void setCb_name(String cb_name) {
		this.cb_name = cb_name;
	}

	public String getCb_image() {
		return cb_image;
	}

	public void setCb_image(String cb_image) {
		this.cb_image = cb_image;
	}

	public int getCb_member() {
		return cb_member;
	}

	public void setCb_member(int cb_member) {
		this.cb_member = cb_member;
	}

	public int getCb_premium() {
		return cb_premium;
	}

	public void setCb_premium(int cb_premium) {
		this.cb_premium = cb_premium;
	}

}