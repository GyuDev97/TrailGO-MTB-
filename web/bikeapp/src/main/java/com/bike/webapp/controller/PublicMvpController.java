package com.bike.webapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bike.webapp.dto.App_RidingRecord;
import com.bike.webapp.dto.DangerousArea;
import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.service.AndroidService;
import com.bike.webapp.service.HazardAdminService;

/** Anonymous, read-only contract for the store MVP. */
@RestController
@RequestMapping("/api/public")
public class PublicMvpController {
    @Autowired private AndroidService androidService;
    @Autowired private HazardAdminService hazardAdminService;

    @GetMapping("/courses")
    public List<App_RidingRecord> courses() throws Exception {
        return androidService.getCourse();
    }

    @GetMapping("/hazards")
    public List<DangerousArea> hazards() {
        java.util.ArrayList<DangerousArea> result = new java.util.ArrayList<>();
        for (HazardReview review : hazardAdminService.findPublicApproved()) {
            DangerousArea area = new DangerousArea();
            area.setDa_num(review.getId());
            area.setDa_latitude(review.getLatitude());
            area.setDa_longitude(review.getLongitude());
            area.setDa_addr(review.getAddress());
            area.setDa_image(review.getImage());
            area.setDa_content(review.getDescription());
            area.setDa_status("APPROVED");
            result.add(area);
        }
        return result;
    }
}
