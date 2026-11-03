package com.bike.webapp.dto;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Board {

	private int b_num;

	private String b_rider;

	private int b_area;

	private String b_title;

	private String b_content;
	
	private int b_hit;

	private Timestamp b_day;

	private String b_file;

	private int b_club;

	private String date;
	
	private int b_type;
	
	private int b_subject;
	
	private String b_img;
	
	private String customDate;
	
	public int getB_hit() {
		return b_hit;
	}

	public int getB_subject() {
		return b_subject;
	}

	public void setB_subject(int b_subject) {
		this.b_subject = b_subject;
	}
	
	public void setB_hit(int b_hit) {
		this.b_hit = b_hit;
	}
	
	public String getCustomDate() {
		return customDate;
	}

	public void setCustomDate(String customDate) {
		this.customDate = customDate;
	}

	public String getB_img() {
		return b_img;
	}

	public void setB_img(String b_img) {
		this.b_img = b_img;
	}

	public int getB_type() {
		return b_type;
	}

	public void setB_type(int b_type) {
		this.b_type = b_type;
	}

	public String getDate() {
		date = (b_day.toString().split(" "))[0];
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getB_club() {
		return b_club;
	}

	public void setB_club(int b_club) {
		this.b_club = b_club;
	}

	public int getB_num() {
		return b_num;
	}

	public void setB_num(int b_num) {
		this.b_num = b_num;
	}

	public String getB_rider() {
		return b_rider;
	}

	public void setB_rider(String b_rider) {
		this.b_rider = b_rider;
	}

	public int getB_area() {
		return b_area;
	}

	public void setB_area(int b_area) {
		this.b_area = b_area;
	}

	public String getB_title() {
		return b_title;
	}

	public void setB_title(String b_title) {
		this.b_title = b_title;
	}

	public String getB_content() {
		return b_content;
	}

	public void setB_content(String b_content) {
		this.b_content = b_content;
	}

	public Timestamp getB_day() {
		return b_day;
	}

	public void setB_day(Timestamp b_day) {
		this.b_day = b_day;
	}

	public String getB_file() {
		return b_file;
	}

	public void setB_file(String b_file) {
		this.b_file = b_file;
	}
}
