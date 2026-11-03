package com.bike.trailgo.dto;

public class MissionRanking {

    private String mc_rider;
    private String r_nickname;
    private String r_image;
    private int how;

    public String getR_image() {
        return r_image;
    }

    public void setR_image(String r_image) {
        this.r_image = r_image;
    }

    public String getMc_rider() {
        return mc_rider;
    }
    public void setMc_rider(String mc_rider) {
        this.mc_rider = mc_rider;
    }
    public String getR_nickname() {
        return r_nickname;
    }
    public void setR_nickname(String r_nickname) {
        this.r_nickname = r_nickname;
    }
    public int getHow() {
        return how;
    }
    public void setHow(int how) {
        this.how = how;
    }


}