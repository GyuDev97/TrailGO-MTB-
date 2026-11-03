package com.bike.trailgo.gpx;

/**
 * A route point (rtept) element.
 */
public class RoutePoint extends Point {

    private RoutePoint(Builder builder) {
        super(builder);
    }

    public static class Builder extends Point.Builder {

        @Override
        public RoutePoint build() {
            return new RoutePoint(this);
        }
    }

}
