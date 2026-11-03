package com.bike.webapp.dto;

public class Recom_Course {
	private int rs_distance;
	private double rs_avgspeed;
	private int rs_high;
	private String rs_name;
	private String rs_area;
	private int rs_cnt;
	private int truerank;
	private int total;
	private int percent;
	
	public int getTruerank() {
		return truerank;
	}
	public void setTruerank(int truerank) {
		this.truerank = truerank;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getPercent() {
		return percent;
	}
	public void setPercent(int percent) {
		this.percent = percent;
	}
	public String getRs_name() {
		return rs_name;
	}
	public void setRs_name(String rs_name) {
		this.rs_name = rs_name;
	}
	public int getRs_distance() {
		return rs_distance;
	}
	public void setRs_distance(int rs_distance) {
		this.rs_distance = rs_distance;
	}
	public double getRs_avgspeed() {
		return rs_avgspeed;
	}
	public void setRs_avgspeed(double rs_avgspeed) {
		this.rs_avgspeed = rs_avgspeed;
	}
	public int getRs_high() {
		return rs_high;
	}
	public void setRs_high(int rs_high) {
		this.rs_high = rs_high;
	}
	public String getRs_area() {
		return rs_area;
	}
	public void setRs_area(String rs_area) {
		this.rs_area = rs_area;
	}
	public int getRs_cnt() {
		return rs_cnt;
	}
	public void setRs_cnt(int rs_cnt) {
		this.rs_cnt = rs_cnt;
	}
}
