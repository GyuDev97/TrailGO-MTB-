package com.bike.webapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.HazardReview;

@Repository
public interface HazardAdminMapper {
    List<HazardReview> findAll(@Param("keyword") String keyword,
                               @Param("hazardType") String hazardType,
                               @Param("status") String status,
                               @Param("dateFrom") String dateFrom,
                               @Param("dateTo") String dateTo,
                               @Param("reported") String reported,
                               @Param("sort") String sort,
                               @Param("source") String source);
    List<HazardReview> findByIds(@Param("ids") List<Integer> ids);
    List<HazardReview> findMergedChildren(@Param("id") int id);
    int transitionStatus(@Param("id") int id,
                         @Param("from") String from,
                         @Param("to") String to,
                         @Param("adminId") String adminId,
                         @Param("reason") String reason);
    int repointMerged(@Param("from") int from, @Param("to") int to);
    int insertAdminDanger(@Param("row") com.bike.webapp.dto.HazardRow row,
                          @Param("lat") String lat, @Param("lon") String lon);
    int insertAdminReview(@Param("id") int id, @Param("title") String title,
                          @Param("type") String type, @Param("adminId") String adminId);
    int insertMerge(@Param("group") String group,
                    @Param("canonicalId") int canonicalId,
                    @Param("mergedId") int mergedId,
                    @Param("adminId") String adminId,
                    @Param("mergedBefore") String mergedBefore,
                    @Param("canonicalBefore") String canonicalBefore,
                    @Param("canonicalAfter") String canonicalAfter);
    List<com.bike.webapp.dto.HazardMergeRecord> findMerges(@Param("canonicalId") Integer canonicalId,
                                                           @Param("limit") int limit);
    List<HazardReview> findRecent(@Param("limit") int limit);
    List<HazardReview> findPublicApproved();
    HazardReview findById(@Param("id") int id);
    int countAll();
    int countByStatus(@Param("status") String status);
    int ensureReview(@Param("id") int id);
    int updateDangerDetails(HazardReview hazard);
    int updateReviewDetails(HazardReview hazard);
    int updateReviewStatus(@Param("id") int id,
                           @Param("status") String status,
                           @Param("adminId") String adminId,
                           @Param("reason") String reason);
    int updateLegacyStatus(@Param("id") int id, @Param("legacyStatus") String legacyStatus);
    int mergeHazard(@Param("id") int id, @Param("targetId") int targetId);
    int insertHistory(@Param("hazardId") int hazardId,
                      @Param("action") String action,
                      @Param("previousStatus") String previousStatus,
                      @Param("nextStatus") String nextStatus,
                      @Param("adminId") String adminId,
                      @Param("reason") String reason);
}
