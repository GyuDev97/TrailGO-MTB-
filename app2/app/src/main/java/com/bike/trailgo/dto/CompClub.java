package com.bike.trailgo.dto;

public class CompClub {
    private int cs_club;
    private int cs_score;
    private String cb_name;
    private String cb_image;

    public String getCb_image() {
        return cb_image;
    }

    public void setCb_image(String cb_image) {
        this.cb_image = cb_image;
    }

    public int getCs_club() {
        return cs_club;
    }
    public void setCs_club(int cs_club) {
        this.cs_club = cs_club;
    }
    public int getCs_score() {
        return cs_score;
    }
    public void setCs_score(int cs_score) {
        this.cs_score = cs_score;
    }
    public String getCb_name() {
        return cb_name;
    }
    public void setCb_name(String cb_name) {
        this.cb_name = cb_name;
    }
}
