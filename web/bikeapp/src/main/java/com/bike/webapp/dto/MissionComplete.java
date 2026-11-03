package com.bike.webapp.dto;

public class MissionComplete {
	private int mc_num;
	private String mc_rider;
	private int mc_mission;
	private String mc_time;
	
	private String m_content;
	private String m_name;
	
	public String getM_content() {
		return m_content;
	}
	public void setM_content(String m_content) {
		this.m_content = m_content;
	}
	public String getM_name() {
		return m_name;
	}
	public void setM_name(String m_name) {
		this.m_name = m_name;
	}
	public int getMc_num() {
		return mc_num;
	}
	public void setMc_num(int mc_num) {
		this.mc_num = mc_num;
	}
	public String getMc_rider() {
		return mc_rider;
	}
	public void setMc_rider(String mc_rider) {
		this.mc_rider = mc_rider;
	}
	public int getMc_mission() {
		return mc_mission;
	}
	public void setMc_mission(int mc_mission) {
		this.mc_mission = mc_mission;
	}
	public String getMc_time() {
		return mc_time;
	}
	public void setMc_time(String mc_time) {
		this.mc_time = mc_time;
	}

	
}
