package com.example.footballprediction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class FootballDataClient {

    private static final String BASE_URL = "https://api.football-data.org/v4";
    private static final String AUTH_TOKEN_HEADER = "X-Auth-Token";

    private final RestTemplate restTemplate;
    private final String apiToken;

    public FootballDataClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${football-data.api-token:}") String apiToken
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    public boolean hasToken() {
        return !apiToken.isEmpty();
    }

    public TeamsResponse fetchTeams(String competitionCode, int season) {
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/competitions/{competitionCode}/teams")
                .queryParam("season", season)
                .buildAndExpand(competitionCode)
                .toUri();
        return get(uri, TeamsResponse.class);
    }

    public MatchesResponse fetchMatches(String competitionCode, int season) {
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/competitions/{competitionCode}/matches")
                .queryParam("season", season)
                .buildAndExpand(competitionCode)
                .toUri();
        return get(uri, MatchesResponse.class);
    }

    private <T> T get(URI uri, Class<T> responseType) {
        if (!hasToken()) {
            throw new IllegalStateException("FOOTBALL_DATA_API_TOKEN tanımlı değil.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTH_TOKEN_HEADER, apiToken);

        try {
            return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), responseType).getBody();
        } catch (RestClientException ex) {
            throw new IllegalStateException("football-data.org isteği başarısız oldu: " + ex.getMessage(), ex);
        }
    }

    public record TeamsResponse(List<TeamResponse> teams) {
    }

    public record TeamResponse(Long id, String name, String shortName, String tla) {
    }

    public record MatchesResponse(List<MatchResponse> matches) {
    }

    public record MatchResponse(
            Long id,
            String utcDate,
            String status,
            Integer matchday,
            String stage,
            String group,
            MatchTeamResponse homeTeam,
            MatchTeamResponse awayTeam,
            ScoreResponse score
    ) {
    }

    public record MatchTeamResponse(Long id, String name, String shortName, String tla) {
    }

    public record ScoreResponse(FullTimeScoreResponse fullTime) {
    }

    public record FullTimeScoreResponse(Integer home, Integer away) {
    }
}
