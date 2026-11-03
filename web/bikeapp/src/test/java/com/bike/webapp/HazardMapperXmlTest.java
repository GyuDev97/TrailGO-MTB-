package com.bike.webapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import com.bike.webapp.mapper.HazardAdminMapper;
import com.bike.webapp.mapper.HazardAppMapper;

/**
 * Loads the real mapper XML files into MyBatis without a database: every mapper method has a statement, the
 * SQL renders, and the dynamic parts (filters, IN lists) produce what they should. It does not run the SQL:
 * that needs MariaDB.
 */
class HazardMapperXmlTest {

	private static Configuration load(Class<?>... mappers) throws Exception {
		Configuration configuration = new Configuration();
		for (String file : new String[] { "/mapper/HazardAdmin.xml", "/mapper/HazardApp.xml" }) {
			try (InputStream in = HazardMapperXmlTest.class.getResourceAsStream(file)) {
				new XMLMapperBuilder(in, configuration, file, configuration.getSqlFragments()).parse();
			}
		}
		return configuration; // the XML parser registers each mapper interface by its namespace
	}

	@Test
	void everyMapperMethodHasAStatement() throws Exception {
		Configuration configuration = load(HazardAdminMapper.class, HazardAppMapper.class);
		for (Class<?> mapper : new Class<?>[] { HazardAdminMapper.class, HazardAppMapper.class }) {
			for (Method method : mapper.getDeclaredMethods()) {
				assertTrue(configuration.hasStatement(mapper.getName() + "." + method.getName()),
						"no statement for " + mapper.getSimpleName() + "." + method.getName());
			}
		}
	}

	@Test
	void theSourceAndStatusFiltersAreAppliedOnlyWhenGiven() throws Exception {
		Configuration configuration = load(HazardAdminMapper.class);
		MappedStatement findAll = configuration.getMappedStatement(HazardAdminMapper.class.getName() + ".findAll");
		Map<String, Object> none = new HashMap<String, Object>();
		for (String key : new String[] { "keyword", "hazardType", "status", "dateFrom", "dateTo", "reported", "sort", "source" }) {
			none.put(key, null);
		}
		String unfiltered = findAll.getBoundSql(none).getSql();
		assertFalse(unfiltered.contains("COALESCE(r.source, 'USER') = ?"));

		Map<String, Object> filtered = new HashMap<String, Object>(none);
		filtered.put("source", "ADMIN");
		filtered.put("status", "MERGED");
		filtered.put("hazardType", "BAD_ROAD");
		BoundSql bound = findAll.getBoundSql(filtered);
		String sql = bound.getSql();
		assertTrue(sql.contains("COALESCE(r.source, 'USER') = ?"));
		assertEquals(3, bound.getParameterMappings().size());
	}

	@Test
	void theIdListBecomesAnInClause() throws Exception {
		Configuration configuration = load(HazardAdminMapper.class);
		MappedStatement findByIds = configuration.getMappedStatement(HazardAdminMapper.class.getName() + ".findByIds");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("ids", Arrays.asList(4, 5, 6));
		BoundSql bound = findByIds.getBoundSql(params);
		assertTrue(bound.getSql().replaceAll("\\s+", "").contains("IN(?,?,?)"), bound.getSql());
		assertEquals(3, bound.getParameterMappings().size());
	}

	@Test
	void theRadiusIsOnlyUpdatedWhenPresent() throws Exception {
		Configuration configuration = load(HazardAdminMapper.class);
		MappedStatement update = configuration.getMappedStatement(HazardAdminMapper.class.getName() + ".updateDangerDetails");
		com.bike.webapp.dto.HazardReview changes = new com.bike.webapp.dto.HazardReview();
		changes.setId(1);
		changes.setRadiusM(60);
		assertTrue(update.getBoundSql(changes).getSql().contains("da_radius_m = ?"));
	}

	@Test
	void theRidersQueriesFilterOnVisibilityInSqlAndTheBoundingBox() throws Exception {
		Configuration configuration = load(HazardAppMapper.class);
		Map<String, Object> params = new HashMap<String, Object>();
		for (String key : new String[] { "lat", "lon", "minLat", "maxLat", "minLon", "maxLon", "limit" }) params.put(key, 1);
		String visible = configuration.getMappedStatement(HazardAppMapper.class.getName() + ".findVisibleInBox")
				.getBoundSql(params).getSql().replaceAll("\\s+", " ");
		assertTrue(visible.contains("d.da_lat BETWEEN ? AND ?"));
		assertTrue(visible.contains("r.merged_into IS NULL"));
		assertTrue(visible.contains("IN ('PENDING', 'REVIEWING', 'APPROVED')"));
		String hidden = configuration.getMappedStatement(HazardAppMapper.class.getName() + ".findHiddenIdsInBox")
				.getBoundSql(params).getSql().replaceAll("\\s+", " ");
		assertTrue(hidden.contains("NOT (COALESCE(r.is_active, 1) = 1"));
		assertTrue(hidden.contains("d.da_lon BETWEEN ? AND ?"));
	}
}
