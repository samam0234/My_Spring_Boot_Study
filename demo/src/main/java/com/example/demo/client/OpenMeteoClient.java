package com.example.demo.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class OpenMeteoClient {

    private final RestClient restClient;

    public OpenMeteoClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public GeocodingResponse geocode(String city) {
        URI uri = UriComponentsBuilder.fromUriString("https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "ko")
                .queryParam("format", "json")
                .build()
                .toUri();

        return restClient.get().uri(uri).retrieve().body(GeocodingResponse.class);
    }

    public ForecastResponse forecast(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,wind_speed_10m,weather_code,relative_humidity_2m")
                .queryParam("timezone", "Asia/Seoul")
                .build()
                .toUri();

        return restClient.get().uri(uri).retrieve().body(ForecastResponse.class);
    }
}
