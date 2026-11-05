package com.Bd.or.Ballondor.external;

import com.Bd.or.Ballondor.config.FootballApiProperties;
import com.Bd.or.Ballondor.exception.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

@Component
public class FootballApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FootballApiClient.class);

    private final RestClient restClient;
    private final FootballApiProperties properties;

    public FootballApiClient(RestClient footballRestClient, FootballApiProperties properties) {
        this.restClient = footballRestClient;
        this.properties = properties;
    }

    public List<PlayerSeasonData> searchPlayers(String name) {
        List<PlayerSeasonData> result = new ArrayList<>();
        int pages = 1;
        int total = 1;
        for (int page = 1; page <= properties.getMaxPages(); page++) {
            pages = page;
            int currentPage = page;
            JsonNode root = get("/players", builder -> builder.queryParam("search", name)
                    .queryParam("league", properties.getCompetitionId())
                    .queryParam("season", properties.getSeason())
                    .queryParam("page", currentPage));
            result.addAll(parsePlayers(root));
            total = totalPages(root);
            if (page >= total) break;
        }
        if (pages < total) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SEARCH_RESULTS_INCOMPLETE",
                    "The provider's player search exceeded the configured pagination limit");
        }
        return result;
    }

    public PlayerSeasonData getPlayerDetails(int playerId) {
        JsonNode root = get("/players", builder -> builder.queryParam("id", playerId));
        List<PlayerSeasonData> response = parsePlayers(root);
        if (response.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND", "Player was not found by the football data provider");
        return response.get(0);
    }

    public PlayerSeasonData getPlayerStatistics(int playerId, int season) {
        JsonNode root = get("/players", builder -> builder.queryParam("id", playerId)
                .queryParam("league", properties.getCompetitionId()).queryParam("season", season));
        return parsePlayers(root).stream().filter(entry -> entry.statistics() != null).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND",
                        "No UEFA Champions League statistics are available for this player and season"));
    }

    public List<PlayerSeasonData> getSeasonPlayers(int season) {
        List<PlayerSeasonData> result = new ArrayList<>();
        int pages = 1;
        int total = 1;
        for (int page = 1; page <= properties.getMaxPages(); page++) {
            pages = page;
            int currentPage = page;
            JsonNode root = get("/players", builder -> builder.queryParam("league", properties.getCompetitionId())
                    .queryParam("season", season).queryParam("page", currentPage));
            result.addAll(parsePlayers(root));
            total = totalPages(root);
            if (page >= total) break;
        }
        if (pages < total) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SEASON_DATA_INCOMPLETE",
                    "The provider's player list exceeded the configured pagination limit; no partial candidate pool was used");
        }
        if (result.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND", "The provider has no player statistics for this season");
        }
        return result;
    }

    public List<Integer> getSupportedSeasons() {
        JsonNode root = get("/leagues", builder -> builder.queryParam("id", properties.getCompetitionId()));
        List<Integer> seasons = new ArrayList<>();
        JsonNode response = root.path("response");
        if (response.isArray()) {
            for (JsonNode league : response) {
                for (JsonNode season : league.path("seasons")) {
                    if (season.path("year").canConvertToInt() && season.path("coverage").path("players").asBoolean(false)) {
                        seasons.add(season.path("year").intValue());
                    }
                }
            }
        }
        return seasons.stream().distinct().sorted().toList();
    }

    private JsonNode get(String path, Consumer<UriBuilder> queryParameters) {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FOOTBALL_API_NOT_CONFIGURED",
                    "Configure FOOTBALL_API_KEY before using football data endpoints");
        }
        try {
            JsonNode root = restClient.get().uri(uriBuilder -> {
                        UriBuilder request = uriBuilder.path(path);
                        queryParameters.accept(request);
                        return request.build();
                    })
                    .header("x-apisports-key", properties.getKey()).retrieve().body(JsonNode.class);
            if (root == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "FOOTBALL_API_FAILURE", "Football data provider returned an empty response");
            JsonNode errors = root.path("errors");
            if (!errors.isMissingNode() && !errors.isNull() && !errors.isEmpty()) {
                logProviderFailure(200, path, root.toString());
                String errorText = errors.toString().toLowerCase(Locale.ROOT);
                if (errorText.contains("rate") || errorText.contains("limit")) {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "FOOTBALL_API_RATE_LIMIT", "Football data provider rate limit was reached");
                }
                if (errorText.contains("token") || errorText.contains("key") || errorText.contains("auth")) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "FOOTBALL_API_AUTHENTICATION_FAILED", "Football data provider rejected the configured API key");
                }
                throw new ApiException(HttpStatus.BAD_GATEWAY, "FOOTBALL_API_FAILURE", "Football data provider rejected the request");
            }
            return root;
        } catch (RestClientResponseException exception) {
            logProviderFailure(exception.getStatusCode().value(), path, exception.getResponseBodyAsString());
            if (exception.getStatusCode().value() == 429) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "FOOTBALL_API_RATE_LIMIT", "Football data provider rate limit was reached");
            }
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "FOOTBALL_API_AUTHENTICATION_FAILED", "Football data provider rejected the configured API key");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FOOTBALL_API_FAILURE", "Football data provider request failed");
        } catch (RestClientException exception) {
            LOGGER.error("Football provider request to {} failed without an HTTP response: {}: {}",
                    path, exception.getClass().getSimpleName(), sanitize(exception.getMessage()));
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FOOTBALL_API_FAILURE", "Football data provider is unavailable");
        }
    }

    private void logProviderFailure(int status, String path, String responseBody) {
        String body = sanitize(responseBody);
        LOGGER.error("Football provider returned HTTP {} for GET {}. Response body: {}",
                status, path, body == null || body.isBlank() ? "(empty)" : body);
    }

    private String sanitize(String value) {
        String key = properties.getKey();
        return value != null && key != null && !key.isBlank() ? value.replace(key, "[REDACTED]") : value;
    }

    private int totalPages(JsonNode root) {
        return Math.max(1, root.path("paging").path("total").asInt(1));
    }

    private List<PlayerSeasonData> parsePlayers(JsonNode root) {
        List<PlayerSeasonData> players = new ArrayList<>();
        JsonNode response = root.path("response");
        if (!response.isArray()) return players;
        for (JsonNode item : response) {
            JsonNode player = item.path("player");
            int id = player.path("id").asInt(0);
            if (id == 0) continue;
            JsonNode team = item.path("statistics").path(0).path("team");
            JsonNode games = item.path("statistics").path(0).path("games");
            ExternalPlayer externalPlayer = new ExternalPlayer(id, text(player, "name"), integer(player, "age"),
                    text(player, "nationality"), text(games, "position"), text(team, "name"),
                    integer(team, "id"), text(player, "photo"));
            JsonNode stats = item.path("statistics").path(0);
            ExternalStatistics externalStatistics = item.path("statistics").isArray() && item.path("statistics").size() > 0
                    ? statistics(stats) : null;
            players.add(new PlayerSeasonData(externalPlayer, externalStatistics));
        }
        return players;
    }

    private ExternalStatistics statistics(JsonNode stats) {
        JsonNode games = stats.path("games");
        JsonNode goals = stats.path("goals");
        JsonNode shots = stats.path("shots");
        JsonNode passes = stats.path("passes");
        JsonNode tackles = stats.path("tackles");
        return new ExternalStatistics(integer(games, "appearances"), integer(goals, "total"),
                integer(goals, "assists"), integer(games, "minutes"), decimal(games, "rating"),
                integer(shots, "total"), integer(shots, "on"), integer(passes, "key"),
                percentage(passes, "accuracy"), integer(tackles, "total"), integer(tackles, "interceptions"),
                integer(goals, "saves"), integer(goals, "conceded"));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() ? value.intValue() : null;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.decimalValue();
        if (value.isTextual()) {
            try { return new BigDecimal(value.asText()); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private Integer percentage(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return Integer.valueOf(value.replace("%", "").trim()); }
        catch (NumberFormatException ignored) { return null; }
    }
}
