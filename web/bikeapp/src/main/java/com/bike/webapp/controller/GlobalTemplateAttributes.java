package com.bike.webapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalTemplateAttributes {

    @Value("${trailgo.maps.kakao-app-key:}")
    private String kakaoMapAppKey;

    @ModelAttribute("kakaoMapAppKey")
    public String kakaoMapAppKey() {
        return kakaoMapAppKey;
    }
}
