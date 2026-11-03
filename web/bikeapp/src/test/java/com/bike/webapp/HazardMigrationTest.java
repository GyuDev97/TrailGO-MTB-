package com.bike.webapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

class HazardMigrationTest {

	private static String resource(String path) throws IOException {
		try (InputStream in = HazardMigrationTest.class.getResourceAsStream(path)) {
			assertTrue(in != null, path + " must exist");
			return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
		}
	}

	@Test
	void migrationIsPurelyAdditiveAndRepeatable() throws IOException {
		String sql = resource("/db/migration/V20261102_1__hazard_geo.sql");
		StringBuilder code = new StringBuilder();
		for (String line : sql.split("\\r?\\n")) {
			if (!line.trim().startsWith("--")) {
				code.append(line).append('\n');
			}
		}
		String statements = code.toString().toUpperCase();
		assertTrue(statements.contains("ALTER TABLE DANGEROUS_AREA"));
		assertTrue(statements.contains("ADD COLUMN IF NOT EXISTS DA_RADIUS_M INT NOT NULL DEFAULT 30"));
		assertTrue(statements.contains("ADD COLUMN IF NOT EXISTS DA_LAT "));
		assertTrue(statements.contains("ADD COLUMN IF NOT EXISTS DA_LON "));
		assertTrue(statements.contains("CREATE INDEX IF NOT EXISTS IDX_DANGEROUS_AREA_GEO"));
		assertFalse(statements.contains("DROP "), "migration must not destroy data");
		assertFalse(statements.contains("DELETE "), "migration must not delete rows");
		assertFalse(statements.contains("UPDATE "), "legacy rows are not rewritten");
	}

	@Test
	void schemaSqlAppliedAtStartupCarriesTheSameStatements() throws IOException {
		String schema = resource("/schema.sql");
		assertTrue(schema.contains("ADD COLUMN IF NOT EXISTS da_radius_m INT NOT NULL DEFAULT 30"));
		assertTrue(schema.contains("CREATE INDEX IF NOT EXISTS idx_dangerous_area_geo ON dangerous_area (da_lat, da_lon)"));
	}

	@Test
	void adminMigrationIsAdditiveAndGuardsAgainstMergingTwice() throws IOException {
		String sql = resource("/db/migration/V20261102_2__hazard_admin.sql");
		StringBuilder code = new StringBuilder();
		for (String line : sql.split("\\r?\\n")) {
			if (!line.trim().startsWith("--")) code.append(line).append('\n');
		}
		String statements = code.toString().toUpperCase();
		assertTrue(statements.contains("ADD COLUMN IF NOT EXISTS SOURCE VARCHAR(10) NOT NULL DEFAULT 'USER'"));
		assertTrue(statements.contains("CREATE TABLE IF NOT EXISTS TRAILGO_HAZARD_MERGE"));
		assertTrue(statements.contains("UNIQUE KEY UK_HAZARD_MERGE_MERGED (MERGED_ID)"));
		assertFalse(statements.contains("DROP "));
		assertFalse(statements.contains("DELETE "));
		assertFalse(statements.contains("UPDATE "));
		String schema = resource("/schema.sql");
		assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS trailgo_hazard_merge"));
		assertTrue(schema.contains("ADD COLUMN IF NOT EXISTS source VARCHAR(10) NOT NULL DEFAULT 'USER'"));
	}

	@Test
	void adminQueriesNeverDeleteAndGuardTheirUpdates() throws IOException {
		String xml = resource("/mapper/HazardAdmin.xml");
		assertFalse(xml.toUpperCase().contains("DELETE FROM"), "merged zones and their reports are never erased");
		// merge only touches a zone that is not merged yet; a status change only if the status is still the one shown
		assertTrue(xml.contains("WHERE hazard_id = #{id} AND merged_into IS NULL AND status &lt;&gt; 'MERGED'"));
		assertTrue(xml.contains("WHERE hazard_id = #{id} AND status = #{from} AND merged_into IS NULL"));
		assertFalse(xml.contains("${"), "no string substitution: parameters only");
		String app = resource("/mapper/HazardApp.xml");
		assertTrue(app.contains("findHiddenIdsInBox"));
	}

	@Test
	void mapperXmlIsPresentAndOnlyReturnsVisibleStatuses() throws IOException {
		String xml = resource("/mapper/HazardApp.xml");
		assertTrue(xml.contains("IN ('PENDING', 'REVIEWING', 'APPROVED')"));
		assertTrue(xml.contains("r.merged_into IS NULL"));
		assertFalse(xml.contains("${"), "no string substitution: parameters only");
	}
}
