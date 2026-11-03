package com.bike.trailgo.dto;

public class ScrapStatus {
    private String ss_rider;
    private int ss_rnum;
    //아래 rr_은 앱에서 받기위한 변수임
    private String rr_rider;
    private String rr_name;
    private int rr_avgspeed;
    private int rr_distance;
    private int rr_high;
    private String rr_area;
    private int rr_time;
    private String rr_gpx;
    private String rr_date;


    public String getRr_date() {
        return rr_date;
    }
    public void setRr_date(String rr_date) {
        this.rr_date = rr_date;
    }

    public String getSs_rider() {
        return ss_rider;
    }
    public void setSs_rider(String ss_rider) {
        this.ss_rider = ss_rider;
    }
    public int getSs_rnum() {
        return ss_rnum;
    }
    public void setSs_rnum(int ss_rnum) {
        this.ss_rnum = ss_rnum;
    }
    public String getRr_rider() {
        return rr_rider;
    }
    public void setRr_rider(String rr_rider) {
        this.rr_rider = rr_rider;
    }

    public String getRr_name() {
        return rr_name;
    }
    public void setRr_name(String rr_name) {
        this.rr_name = rr_name;
    }
    public int getRr_avgspeed() {
        return rr_avgspeed;
    }
    public void setRr_avgspeed(int rr_avgspeed) {
        this.rr_avgspeed = rr_avgspeed;
    }
    public int getRr_distance() {
        return rr_distance;
    }
    public void setRr_distance(int rr_distance) {
        this.rr_distance = rr_distance;
    }
    public int getRr_high() {
        return rr_high;
    }
    public void setRr_high(int rr_high) {
        this.rr_high = rr_high;
    }
    public String getRr_area() {
        return rr_area;
    }
    public void setRr_area(String rr_area) {
        this.rr_area = rr_area;
    }
    public int getRr_time() {
        return rr_time;
    }
    public void setRr_time(int rr_time) {
        this.rr_time = rr_time;
    }
    public String getRr_gpx() {
        return rr_gpx;
    }
    public void setRr_gpx(String rr_gpx) {
        this.rr_gpx = rr_gpx;
    }
}
