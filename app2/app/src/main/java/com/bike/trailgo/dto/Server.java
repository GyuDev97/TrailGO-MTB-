package com.bike.trailgo.dto;

public class Server {
    private String IP = com.bike.trailgo.net.Server.BASE_URL;

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        if (IP != null && IP.trim().length() > 0) {
            this.IP = IP;
        }
    }
}
