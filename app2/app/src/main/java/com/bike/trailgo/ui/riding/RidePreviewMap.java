package com.bike.trailgo.ui.riding;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.ViewGroup;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.route.RouteLine;
import com.kakao.vectormap.route.RouteLineLayer;
import com.kakao.vectormap.route.RouteLineOptions;
import com.kakao.vectormap.route.RouteLineSegment;
import com.kakao.vectormap.route.RouteLineStyle;
import com.kakao.vectormap.route.RouteLineStyles;
import com.kakao.vectormap.route.RouteLineStylesSet;

import java.util.ArrayList;
import java.util.List;

/** Owns the native Kakao map displayed while a completed ride is being saved. */
final class RidePreviewMap {
    private static final LatLng DEFAULT_POSITION = LatLng.from(35.8714, 128.6014);

    private final MapView mapView;
    private final ArrayList<LatLng> routePoints = new ArrayList<>();
    private KakaoMap kakaoMap;
    private RouteLineLayer routeLineLayer;
    private RouteLineStyles routeLineStyles;
    private RouteLine routeLine;

    RidePreviewMap(Context context, ViewGroup container, List<Double> latitudes, List<Double> longitudes) {
        addRoutePoints(latitudes, longitudes);
        mapView = new MapView(context);
        container.removeAllViews();
        container.addView(mapView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    void start() {
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d("RidePreviewMap", "Map destroyed");
            }

            @Override
            public void onMapError(Exception error) {
                Log.e("RidePreviewMap", "Unable to start Kakao map", error);
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap readyMap) {
                kakaoMap = readyMap;
                routeLineLayer = kakaoMap.getRouteLineManager().getLayer();
                routeLineStyles = RouteLineStyles.from(
                        RouteLineStyle.from(11, Color.rgb(31, 138, 59), 3, Color.WHITE)
                );
                renderRoute();
            }

            @Override
            public LatLng getPosition() {
                return routePoints.isEmpty() ? DEFAULT_POSITION : routePoints.get(0);
            }

            @Override
            public int getZoomLevel() {
                return routePoints.isEmpty() ? 13 : 16;
            }
        });
    }

    /** Re-fits the camera to the whole route, e.g. after the map card changed size. */
    void refit() {
        renderRoute();
    }

    void resume() {
        mapView.resume();
    }

    void pause() {
        mapView.pause();
    }

    void finish() {
        mapView.finish();
    }

    private void addRoutePoints(List<Double> latitudes, List<Double> longitudes) {
        if (latitudes == null || longitudes == null) {
            return;
        }

        int pointCount = Math.min(latitudes.size(), longitudes.size());
        for (int index = 0; index < pointCount; index++) {
            Double latitude = latitudes.get(index);
            Double longitude = longitudes.get(index);
            if (latitude == null || longitude == null
                    || latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180
                    || (latitude == 0 && longitude == 0)) {
                continue;
            }
            routePoints.add(LatLng.from(latitude, longitude));
        }
    }

    private void renderRoute() {
        if (kakaoMap == null || routeLineLayer == null || routeLineStyles == null || routePoints.size() < 2) {
            return;
        }

        RouteLineSegment segment = RouteLineSegment.from(new ArrayList<>(routePoints), routeLineStyles);
        if (routeLine == null) {
            routeLine = routeLineLayer.addRouteLine(
                    RouteLineOptions.from(segment).setStylesSet(RouteLineStylesSet.from(routeLineStyles))
            );
        } else {
            routeLine.changeSegments(segment);
        }
        kakaoMap.moveCamera(CameraUpdateFactory.fitMapPoints(routePoints.toArray(new LatLng[0]), 56));
    }
}
