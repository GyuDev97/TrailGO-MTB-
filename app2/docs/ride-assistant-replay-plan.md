# Ride Assistant recorded-replay plan

Current TrailGo records persist a ride summary and a GPX route containing latitude, longitude, and elevation only. They do not retain point timestamps, measured speed, or accuracy, so existing records cannot reproduce the original Ride Assistant context.

Do not infer those missing values from summary data: it would produce misleading pace, stop, and event results.

## Future point schema

Persist an append-only point record associated with the saved ride after a user confirms saving it:

```text
RideTrackPoint
  rideId            UUID
  timestampMs       long
  latitude          double
  longitude         double
  altitudeMeters    double
  speedKmh          double
  accuracyMeters    float
```

## Storage policy

- Keep points in the in-memory/current-ride cache at the existing GPS callback rate.
- Persist a point when at least 3 seconds have elapsed or 8 metres have been travelled.
- Always persist first/last points, pause/resume boundaries, and a point after a meaningful altitude change.
- Upload or store these points separately from the existing `RidingRecord` summary, so old records remain compatible.

With that data, `RideSimulationRunner` can accept recorded `RideTestInput` entries in timestamp order and replay them at 1x/10x/30x/60x without changing `RideContextEngine`, `RideStateDetector`, or `RideEventDetector`.
