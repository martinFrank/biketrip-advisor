package de.biketrip.advisor.agent;

public record RouteWaypoint(
        String name,
        double lat,
        double lon,
        int dayNumber
) {}
