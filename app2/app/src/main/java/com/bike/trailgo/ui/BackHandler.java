package com.bike.trailgo.ui;

/**
 * Implemented by a screen that has its own inner pages (the settings lists): the system Back button
 * first goes one page back inside the screen, and only leaves it from its first page.
 */
public interface BackHandler {
    /** @return true when Back was used up inside the screen */
    boolean onBackPressed();
}
