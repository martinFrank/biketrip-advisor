package de.biketrip.advisor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.biketrip.advisor.config.RoutingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeoRoutingServiceTest {

    private GeoRoutingService service;

    @BeforeEach
    void setUp() {
        RoutingConfig config = new RoutingConfig("", "https://api.openrouteservice.org");
        // RestClients are not called in location extraction tests
        service = new GeoRoutingService(config, RestClient.create(), RestClient.create(), new ObjectMapper());
    }

    @Test
    void extractLocationsFindsStartAndEndPerDay() {
        String plan = """
                Tag 1: Freiburg → Breisach
                Tag 2: Breisach → Colmar
                Tag 3: Colmar → Basel
                """;

        List<String> locations = service.extractLocations(plan);

        assertThat(locations).containsExactly("Freiburg", "Breisach", "Colmar", "Basel");
    }

    @Test
    void extractLocationsDeduplicatesSharedWaypoints() {
        String plan = """
                Tag 1: München → Starnberg
                Tag 2: Starnberg → Garmisch
                """;

        List<String> locations = service.extractLocations(plan);

        assertThat(locations).containsExactly("München", "Starnberg", "Garmisch");
    }

    @Test
    void extractLocationsHandlesArrowVariants() {
        String plan = """
                Tag 1: Berlin -> Potsdam
                Tag 2: Potsdam → Dresden
                """;

        List<String> locations = service.extractLocations(plan);

        assertThat(locations).containsExactly("Berlin", "Potsdam", "Dresden");
    }

    @Test
    void extractLocationsReturnsEmptyForNoMatches() {
        String noRoutes = "Dieser Text enthält keine Routeninformationen.";

        List<String> locations = service.extractLocations(noRoutes);

        assertThat(locations).isEmpty();
    }

    @Test
    void extractLocationsCleansMarkdownFormatting() {
        String plan = """
                Tag 1: **[Freiburg]** (Altstadt) → [Breisach] (Rhein)
                """;

        List<String> locations = service.extractLocations(plan);

        assertThat(locations).containsExactly("Freiburg", "Breisach");
    }

    @Test
    void extractLocationsHandlesNumberVariants() {
        String plan = """
                Tag1: Zürich → Bern
                Tag 2: Bern → Luzern
                Tag  3 : Luzern → St. Gallen
                """;

        List<String> locations = service.extractLocations(plan);

        assertThat(locations).containsExactly("Zürich", "Bern", "Luzern", "St. Gallen");
    }

    @Test
    void processReturnsNullWhenNoLocationsFound() {
        RouteResult result = service.process("Kein Tagesplan hier");

        assertThat(result).isNull();
    }
}
