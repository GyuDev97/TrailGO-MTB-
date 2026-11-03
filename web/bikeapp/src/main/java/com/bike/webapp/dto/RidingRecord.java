package com.bike.webapp.dto;


import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RidingRecord extends WeatherColumns {

	private int rr_num;
	private String rr_rider;
	private Timestamp rr_date;
	private int rr_distance;
	private int rr_topspeed;
	private int rr_avgspeed;
	private int rr_high;
	private String rr_gpx;
	private int rr_open;
	private int rr_breaktime;
	private int rr_time;
	private String rr_area;
	private int rr_comp;
	private int rr_like;
	private String rr_name;
	private String rr_tag;
	private String rr_foname;
	private String keyword;
	private String rr_start_area;
	private String rr_end_area;
	// Optional ride start position sent by the Android app when a record is saved. It is only used
	// to look up the weather snapshot: never stored, never returned.
	private Double rr_start_lat;
	private Double rr_start_lon;
	// When the ride started ("yyyy-MM-dd HH:mm:ss", Asia/Seoul). Optional; only used to pick the
	// weather observation time. Never stored, never returned.
	private String rr_started_at;
	// Token of the weather the server looked up when the ride started (see /app/weather). Never stored/returned.
	private String rr_weather_token;

	@JsonIgnore
	public String getRr_weather_token() {
		return rr_weather_token;
	}

	public void setRr_weather_token(String rr_weather_token) {
		this.rr_weather_token = rr_weather_token;
	}
	// Free text written by the rider (max 500 chars). NULL for rides saved without one.
	private String rr_memo;
	// Filled only by the single-record detail API.
	private java.util.List<RecordPhoto> photos;

	@JsonIgnore
	public String getRr_started_at() {
		return rr_started_at;
	}

	public void setRr_started_at(String rr_started_at) {
		this.rr_started_at = rr_started_at;
	}

	public String getRr_memo() {
		return rr_memo;
	}

	public void setRr_memo(String rr_memo) {
		this.rr_memo = rr_memo;
	}

	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	public java.util.List<RecordPhoto> getPhotos() {
		return photos;
	}

	public void setPhotos(java.util.List<RecordPhoto> photos) {
		this.photos = photos;
	}

	@JsonIgnore
	public Double getRr_start_lat() {
		return rr_start_lat;
	}

	public void setRr_start_lat(Double rr_start_lat) {
		this.rr_start_lat = rr_start_lat;
	}

	@JsonIgnore
	public Double getRr_start_lon() {
		return rr_start_lon;
	}

	public void setRr_start_lon(Double rr_start_lon) {
		this.rr_start_lon = rr_start_lon;
	}


	public String getRr_start_area() {
		return rr_start_area;
	}

	public void setRr_start_area(String rr_start_area) {
		this.rr_start_area = rr_start_area;
	}

	public String getRr_end_area() {
		return rr_end_area;
	}

	public void setRr_end_area(String rr_end_area) {
		this.rr_end_area = rr_end_area;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getRr_foname() {
		return rr_foname;
	}

	public void setRr_foname(String rr_foname) {
		this.rr_foname = rr_foname;
	}

	public String getRr_tag() {
		return rr_tag;
	}
	public void setRr_tag(String rr_tag) {
		this.rr_tag = rr_tag;
	}
	public int getRr_like() {
		return rr_like;
	}
	public void setRr_like(int rr_like) {
		this.rr_like = rr_like;
	}
	public String getRr_name() {
		return rr_name;
	}
	public void setRr_name(String rr_name) {
		this.rr_name = rr_name;
	}
	public int getRr_num() {
		return rr_num;
	}
	public void setRr_num(int rr_num) {
		this.rr_num = rr_num;
	}
	public String getRr_rider() {
		return rr_rider;
	}
	public void setRr_rider(String rr_rider) {
		this.rr_rider = rr_rider;
	}
	public int getRr_distance() {
		return rr_distance;
	}
	public void setRr_distance(int rr_distance) {
		this.rr_distance = rr_distance;
	}
	public int getRr_topspeed() {
		return rr_topspeed;
	}
	public void setRr_topspeed(int rr_topspeed) {
		this.rr_topspeed = rr_topspeed;
	}
	public int getRr_avgspeed() {
		return rr_avgspeed;
	}
	public void setRr_avgspeed(int rr_avgspeed) {
		this.rr_avgspeed = rr_avgspeed;
	}
	public int getRr_high() {
		return rr_high;
	}
	public void setRr_high(int rr_high) {
		this.rr_high = rr_high;
	}
	public String getRr_gpx() {
		return rr_gpx;
	}
	public void setRr_gpx(String rr_gpx) {
		this.rr_gpx = rr_gpx;
	}
	public int getRr_open() {
		return rr_open;
	}
	public void setRr_open(int rr_open) {
		this.rr_open = rr_open;
	}
	public int getRr_breaktime() {
		return rr_breaktime;
	}
	public void setRr_breaktime(int rr_breaktime) {
		this.rr_breaktime = rr_breaktime;
	}
	public int getRr_time() {
		return rr_time;
	}
	public void setRr_time(int rr_time) {
		this.rr_time = rr_time;
	}
	public String getRr_area() {
		return rr_area;
	}
	public void setRr_area(String rr_area) {
		this.rr_area = rr_area;
	}
	public String getRr_date() {
		String str = String.valueOf(rr_date); 
		int index = str.indexOf(".");
		String result = str.substring(0,index);
		return result;
	}
	public String getRr_dateYYYYMMDD() {
		String str = String.valueOf(rr_date); 
		String result = str.substring(0,10);
		return result;
	}
	public void setRr_date(Timestamp rr_date) {
		this.rr_date = rr_date;
	}
	public int getRr_comp() {
		return rr_comp;
	}
	public void setRr_comp(int rr_comp) {
		this.rr_comp = rr_comp;
	}
}
	
