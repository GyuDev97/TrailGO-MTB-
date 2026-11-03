package com.bike.webapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.bike.webapp.dto.HazardRow;

/** Queries behind /app/hazards. Moderation (approve, reject, merge) stays in {@link HazardAdminMapper}. */
@Repository
public interface HazardAppMapper {

	/**
	 * Visible hazards (reported, under review or approved; not hidden, rejected, expired or merged) whose
	 * centre lies in the rectangle, nearest to ({@code lat}, {@code lon}) first.
	 */
	List<HazardRow> findVisibleInBox(@Param("lat") double lat, @Param("lon") double lon,
			@Param("minLat") double minLat, @Param("maxLat") double maxLat,
			@Param("minLon") double minLon, @Param("maxLon") double maxLon,
			@Param("limit") int limit);

	/**
	 * Ids of zones in the rectangle that riders must NOT have any more (hidden, rejected, expired, merged).
	 * The app drops them from its cache, which is how a merge reaches phones that already stored the zone.
	 */
	List<Integer> findHiddenIdsInBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
			@Param("minLon") double minLon, @Param("maxLon") double maxLon, @Param("limit") int limit);

	/** Any hazard, whatever its status, or null. */
	HazardRow findById(@Param("id") int id);

	/** Inserts the legacy row; the generated id is written to {@code row.id}. */
	int insertDanger(@Param("row") HazardRow row, @Param("lat") String lat, @Param("lon") String lon);

	int insertReview(@Param("id") int id, @Param("title") String title, @Param("type") String type);

	int updateDanger(@Param("id") int id, @Param("description") String description, @Param("radiusM") int radiusM);

	int updateReview(@Param("id") int id, @Param("title") String title, @Param("type") String type);

	/** Hides a hazard from everyone (status INACTIVE). The row is kept for the operators. */
	int deactivateReview(@Param("id") int id);

	int deactivateLegacy(@Param("id") int id);
}
