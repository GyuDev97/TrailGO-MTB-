package com.bike.webapp.dto;

public class Competition {

private int comp_num;
private int comp_course;
private int comp_badge;
private String comp_name;
private String comp_period;
private String comp_content;
private String comp_image;
private int cnt;
private String start;
private String end;
private String c_area;
private String c_gpx;
private String c_distance;
private String c_name;
private String bg_image;
private String bg_name;

public int getCnt() {
	return cnt;
}
public void setCnt(int cnt) {
	this.cnt = cnt;
}
public String getBg_name() {
	return bg_name;
}
public void setBg_name(String bg_name) {
	this.bg_name = bg_name;
}
public String getBg_image() {
	return bg_image;
}
public void setBg_image(String bg_image) {
	this.bg_image = bg_image;
}
public String getC_distance() {
	int a = Integer.parseInt(c_distance)/1000;
	int b = (Integer.parseInt(c_distance)-a*1000)/100;
	c_distance = a+"."+b;
	return c_distance;
}
public void setC_distance(String c_distance) {
	this.c_distance = c_distance;
}
public String getC_name() {
	return c_name;
}
public void setC_name(String c_name) {
	this.c_name = c_name;
}
public String getC_area() {
	return c_area;
}
public void setC_area(String c_area) {
	this.c_area = c_area;
}
public String getC_gpx() {
	return c_gpx;
}
public void setC_gpx(String c_gpx) {
	this.c_gpx = c_gpx;
}
public String getStart() {
	this.start =this.comp_period.substring(0, 8);
	start = start.substring(0, 4)+"년 "+start.substring(4, 6)+"월 "+start.substring(6)+"일";
	return start;
}
public void setStart(String start) {
	this.start = start;
}
public String getEnd() {
	this.end =this.comp_period.substring(8);
	end = end.substring(0, 4)+"년 "+end.substring(4, 6)+"월 "+end.substring(6)+"일";
	return end;
}
public void setEnd(String end) {
	this.end = end;
}

public int getComp_num() {
	return comp_num;
}
public void setComp_num(int comp_num) {
	this.comp_num = comp_num;
}
public int getComp_course() {
	return comp_course;
}
public void setComp_course(int comp_course) {
	this.comp_course = comp_course;
}
public int getComp_badge() {
	return comp_badge;
}
public void setComp_badge(int comp_badge) {
	this.comp_badge = comp_badge;
}
public String getComp_name() {
	return comp_name;
}
public void setComp_name(String comp_name) {
	this.comp_name = comp_name;
}
public String getComp_period() {
	return comp_period;
}
public void setComp_period(String comp_period) {
	this.comp_period = comp_period;
}
public String getComp_content() {
	return comp_content;
}
public void setComp_content(String comp_content) {
	this.comp_content = comp_content;
}
public String getComp_image() {
	return comp_image;
}
public void setComp_image(String comp_image) {
	this.comp_image = comp_image;
}
}
