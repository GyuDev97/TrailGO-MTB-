package com.bike.webapp.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.service.HazardAdminService;

@Controller
public class AdminPageController {

    @Autowired
    private HazardAdminService hazardService;

    @Value("${trailgo.maps.kakao-app-key:}")
    private String kakaoMapAppKey;

    @GetMapping("/admin/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return "redirect:/admin";
        }
        return "admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("stats", hazardService.getStats());
        model.addAttribute("recentHazards", hazardService.findRecent(8));
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/admin/hazards")
    public String hazards(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String hazardType,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String dateFrom,
                          @RequestParam(required = false) String dateTo,
                          @RequestParam(required = false) String reported,
                          @RequestParam(defaultValue = "newest") String sort,
                          @RequestParam(required = false) String source,
                          Model model) {
        model.addAttribute("hazards", hazardService.findAll(
                keyword, hazardType, status, dateFrom, dateTo, reported, sort, source));
        model.addAttribute("source", source);
        model.addAttribute("stats", hazardService.getStats());
        model.addAttribute("keyword", keyword);
        model.addAttribute("hazardType", hazardType);
        model.addAttribute("status", status);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("reported", reported);
        model.addAttribute("sort", sort);
        model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
        model.addAttribute("activeMenu", "hazards");
        return "admin/hazards/list";
    }

    @GetMapping("/admin/hazards/{id}")
    public String hazardDetail(@PathVariable int id, Model model) {
        HazardReview hazard = hazardService.findById(id);
        if (hazard == null) return "redirect:/admin/hazards?notFound";
        model.addAttribute("hazard", hazard);
        model.addAttribute("mergedChildren", hazardService.mergedChildren(id));
        model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
        model.addAttribute("activeMenu", "hazards");
        return "admin/hazards/detail";
    }

    /** Register a zone directly: pick the place on the map, check duplicates, save. */
    @GetMapping("/admin/hazards/new")
    public String newHazard(Model model) {
        model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
        model.addAttribute("activeMenu", "hazards");
        return "admin/hazards/new";
    }

    /** Merge the zones in {@code ids} (comma separated) into one representative. */
    @GetMapping("/admin/hazards/merge")
    public String merge(@RequestParam(required = false) String ids, Model model) {
        java.util.List<Integer> parsed = new java.util.ArrayList<Integer>();
        if (ids != null) {
            for (String part : ids.split(",")) {
                try {
                    Integer value = Integer.valueOf(part.trim());
                    if (!parsed.contains(value)) parsed.add(value);
                } catch (NumberFormatException ignored) {
                    // not a number: left out
                }
            }
        }
        if (parsed.size() > com.bike.webapp.service.HazardAdminService.MAX_MERGE_SOURCES + 1) {
            parsed = parsed.subList(0, com.bike.webapp.service.HazardAdminService.MAX_MERGE_SOURCES + 1);
        }
        model.addAttribute("zones", hazardService.findByIds(parsed));
        model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
        model.addAttribute("activeMenu", "hazards");
        return "admin/hazards/merge";
    }

    @GetMapping("/admin/hazards/merges")
    public String merges(Model model) {
        model.addAttribute("merges", hazardService.merges(null, 200));
        model.addAttribute("activeMenu", "hazards");
        return "admin/hazards/merges";
    }
}
