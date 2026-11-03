package com.bike.trailgo.ui.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GpxExportTest {

    @Test
    public void titleAndDateMakeTheFileName() {
        assertEquals("북한산 주말 라이딩_2026-10-13.gpx", GpxExport.fileName("북한산 주말 라이딩", "2026-10-13 22:59:08"));
        assertEquals("대구 라이딩_2026-10-13.gpx", GpxExport.fileName("대구 라이딩", "2026-10-13"));
    }

    @Test
    public void charactersAFileNameCannotHaveAreReplaced() {
        assertEquals("a_b_c_d.gpx", GpxExport.fileName("a/b\\c:d", null));
        assertEquals("what.gpx", GpxExport.fileName("what?", null)); // trailing separators are dropped
        assertTrue(!GpxExport.fileName("../../etc/passwd", null).contains("/"));
    }

    @Test
    public void missingPartsFallBack() {
        assertEquals("TrailGo.gpx", GpxExport.fileName(null, null));
        assertEquals("TrailGo.gpx", GpxExport.fileName("   ", "not a date"));
        assertEquals("TrailGo_2026-01-02.gpx", GpxExport.fileName("..", "2026-01-02T10:00:00Z"));
    }

    @Test
    public void aLongTitleIsCut() {
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < 100; i++) title.append('가');
        String name = GpxExport.fileName(title.toString(), null);
        assertEquals(60 + ".gpx".length(), name.length());
    }
}
