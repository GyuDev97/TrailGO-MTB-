package com.bike.webapp.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicConfigController {

    @Value("${trailgo.android-download-url:}")
    private String androidDownloadUrl;

    @Value("${trailgo.support-email:bgpoilkj@naver.com}")
    private String supportEmail;

    @GetMapping("/config")
    public Map<String, String> config() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("androidDownloadUrl", androidDownloadUrl);
        config.put("supportEmail", supportEmail);
        return config;
    }
}
