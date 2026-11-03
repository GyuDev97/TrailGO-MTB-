package com.bike.trailgo.dto;

public class RidingRecord {
    private int rr_num;
    private String rr_rider;
    private String rr_date;
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
    private String r_image;
    // Device-only id used to make an interrupted guest save safe to retry.
    // The field is never sent as part of the legacy server record form.
    private String local_source_id;
    // Optional: absent for guest records, old records and older servers.
    private RecordWeather weather;
    // Optional rider memo (max 500 chars) and photos; null/empty for records saved before they existed.
    private String rr_memo;
    private java.util.List<RecordPhoto> photos;

    public String getRr_memo() {
        return rr_memo;
    }

    public void setRr_memo(String rr_memo) {
        this.rr_memo = rr_memo;
    }

    /** Never null: records without photos give an empty list. */
    public java.util.List<RecordPhoto> getPhotos() {
        return photos == null ? java.util.Collections.<RecordPhoto>emptyList() : photos;
    }

    public void setPhotos(java.util.List<RecordPhoto> photos) {
        this.photos = photos;
    }

    public RecordWeather getWeather() {
        return weather;
    }

    public void setWeather(RecordWeather weather) {
        this.weather = weather;
    }

    public String getR_image() {
        return r_image;
    }

    public void setR_image(String r_image) {
        this.r_image = r_image;
    }

    public String getLocal_source_id() {
        return local_source_id;
    }

    public void setLocal_source_id(String local_source_id) {
        this.local_source_id = local_source_id;
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

    public String getRr_date() {
        return rr_date;
    }

    public void setRr_date(String rr_date) {
        this.rr_date = rr_date;
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

    public int getRr_comp() {
        return rr_comp;
    }

    public void setRr_comp(int rr_comp) {
        this.rr_comp = rr_comp;
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
}
