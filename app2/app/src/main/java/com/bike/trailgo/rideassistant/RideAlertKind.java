package com.bike.trailgo.rideassistant;

/** What a ride alert is about; decides its title and which settings switch controls it. */
public enum RideAlertKind {
    DANGER("위험지역 접근", 3201),
    COURSE("코스 이탈", 3202),
    BATTERY("배터리 부족", 3203);

    public final String title;
    public final int notificationId;

    RideAlertKind(String title, int notificationId) {
        this.title = title;
        this.notificationId = notificationId;
    }
}
