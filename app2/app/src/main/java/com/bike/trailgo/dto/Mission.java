package com.bike.trailgo.dto;

public class Mission {

	private int m_num;
	private int m_badge;
	private String m_name;
	private String m_content;
	private int m_type;
	private int m_point;

	private String bg_name;
	private String bg_image;

	private int ms_score;

	public int getM_point() {
		return m_point;
	}

	public void setM_point(int m_point) {
		this.m_point = m_point;
	}

	public String getBg_name() {
		return bg_name;
	}

	public void setBg_name(String bg_name) {
		this.bg_name = bg_name;
	}

	public int getM_num() {
		return m_num;
	}

	public void setM_num(int m_num) {
		this.m_num = m_num;
	}

	public int getM_badge() {
		return m_badge;
	}

	public void setM_badge(int m_badge) {
		this.m_badge = m_badge;
	}

	public String getM_name() {
		return m_name;
	}

	public void setM_name(String m_name) {
		this.m_name = m_name;
	}

	public String getM_content() {
		return m_content;
	}

	public void setM_content(String m_content) {
		this.m_content = m_content;
	}

	public int getM_type() {
		return m_type;
	}

	public void setM_type(int m_type) {
		this.m_type = m_type;
	}

	public String getBg_image() {
		return bg_image;
	}

	public void setBg_image(String bg_image) {
		this.bg_image = bg_image;
	}

	public int getMs_score() {
		return ms_score;
	}

	public void setMs_score(int ms_score) {
		this.ms_score = ms_score;
	}


}
