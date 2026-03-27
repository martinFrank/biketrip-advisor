package de.biketrip.advisor.agent;

import java.util.List;
import java.util.Map;

public record RouteResult(
        List<RouteWaypoint> waypoints,
        Map<String, Object> geojson,
        double totalDistanceKm
) {}
