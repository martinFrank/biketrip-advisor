package de.biketrip.advisor.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.biketrip.advisor.config.NominatimConfig;
import de.biketrip.advisor.config.RoutingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeoRoutingService {

    private static final Logger log = LoggerFactory.getLogger(GeoRoutingService.class);

    private static final Pattern ROUTE_PATTERN = Pattern.compile(
            "Tag\\s*\\d+\\s*:?\\s*(.+?)\\s*[→\\->]+\\s*(.+?)\\s*(?:\\n|$)",
            Pattern.MULTILINE
    );

    private final RoutingConfig config;
    private final RestClient nominatimClient;
    private final RestClient orsClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeoRoutingService(RoutingConfig routingConfig,
                             NominatimConfig nominatimConfig,
                             ObjectMapper objectMapper) {
        this(routingConfig,
             RestClient.builder()
                     .baseUrl(nominatimConfig.baseUrl())
                     .defaultHeader("User-Agent", "BikeTripAdvisor/1.0")
                     .build(),
             RestClient.builder()
                     .baseUrl(routingConfig.baseUrl())
                     .build(),
             objectMapper);
    }

    // Test-friendly constructor with injectable RestClients
    GeoRoutingService(RoutingConfig config,
                      RestClient nominatimClient,
                      RestClient orsClient,
                      ObjectMapper objectMapper) {
        this.config = config;
        this.nominatimClient = nominatimClient;
        this.orsClient = orsClient;
        this.objectMapper = objectMapper;
    }

    public RouteResult process(String planningOutput) {
        log.info("GeoRouting: extracting locations from plan");

        List<String> locationNames = extractLocations(planningOutput);
        if (locationNames.isEmpty()) {
            log.warn("GeoRouting: no locations found in planning output");
            return null;
        }

        log.info("GeoRouting: found {} unique locations: {}", locationNames.size(), locationNames);

        List<RouteWaypoint> waypoints = geocodeLocations(locationNames);
        if (waypoints.size() < 2) {
            log.warn("GeoRouting: not enough waypoints geocoded ({}/{})", waypoints.size(), locationNames.size());
            return null;
        }

        log.info("GeoRouting: geocoded {}/{} locations", waypoints.size(), locationNames.size());

        return calculateRoute(waypoints);
    }

    List<String> extractLocations(String text) {
        LinkedHashSet<String> locations = new LinkedHashSet<>();
        Matcher matcher = ROUTE_PATTERN.matcher(text);

        while (matcher.find()) {
            String start = cleanLocationName(matcher.group(1));
            String end = cleanLocationName(matcher.group(2));
            if (!start.isBlank()) locations.add(start);
            if (!end.isBlank()) locations.add(end);
        }

        return new ArrayList<>(locations);
    }

    private String cleanLocationName(String name) {
        return name
                .replaceAll("\\[|\\]", "")
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("\\*", "")
                .strip();
    }

    private List<RouteWaypoint> geocodeLocations(List<String> names) {
        List<RouteWaypoint> waypoints = new ArrayList<>();

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            try {
                // Nominatim rate limit: max 1 request/second
                if (i > 0) {
                    Thread.sleep(1100);
                }

                String response = nominatimClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", name)
                                .queryParam("format", "json")
                                .queryParam("limit", "1")
                                .build())
                        .retrieve()
                        .body(String.class);

                List<Map<String, Object>> results = objectMapper.readValue(
                        response, new TypeReference<>() {});

                if (results != null && !results.isEmpty()) {
                    Map<String, Object> result = results.getFirst();
                    double lat = Double.parseDouble(result.get("lat").toString());
                    double lon = Double.parseDouble(result.get("lon").toString());
                    waypoints.add(new RouteWaypoint(name, lat, lon, i + 1));
                    log.debug("Geocoded '{}' -> [{}, {}]", name, lat, lon);
                } else {
                    log.warn("Geocoding failed for '{}'", name);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Geocoding interrupted for '{}'", name);
                break;
            } catch (Exception e) {
                log.warn("Geocoding error for '{}': {}", name, e.getMessage());
            }
        }

        return waypoints;
    }

    private RouteResult calculateRoute(List<RouteWaypoint> waypoints) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            log.warn("GeoRouting: no OpenRouteService API key configured, returning waypoints only");
            return new RouteResult(waypoints, null, 0);
        }

        try {
            List<List<Double>> coordinates = waypoints.stream()
                    .map(wp -> List.of(wp.lon(), wp.lat()))
                    .toList();

            Map<String, Object> requestBody = Map.of("coordinates", coordinates);

            String response = orsClient.post()
                    .uri("/v2/directions/cycling-regular/geojson")
                    .header("Authorization", config.apiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> geojson = objectMapper.readValue(
                    response, new TypeReference<>() {});

            double totalDistance = extractTotalDistance(geojson);
            log.info("GeoRouting: route calculated, total distance: {} km", String.format("%.1f", totalDistance));

            return new RouteResult(waypoints, geojson, totalDistance);
        } catch (Exception e) {
            log.error("Routing calculation failed: {}", e.getMessage());
            return new RouteResult(waypoints, null, 0);
        }
    }

    private double extractTotalDistance(Map<String, Object> geojson) {
        try {
            if (!(geojson.get("features") instanceof List<?> featuresList) || featuresList.isEmpty()) {
                return 0;
            }
            if (!(featuresList.getFirst() instanceof Map<?, ?> feature)) return 0;
            if (!(feature.get("properties") instanceof Map<?, ?> properties)) return 0;
            if (!(properties.get("summary") instanceof Map<?, ?> summary)) return 0;
            if (!(summary.get("distance") instanceof Number distance)) return 0;
            return distance.doubleValue() / 1000.0;
        } catch (Exception e) {
            log.debug("Could not extract distance from GeoJSON: {}", e.getMessage());
            return 0;
        }
    }
}
