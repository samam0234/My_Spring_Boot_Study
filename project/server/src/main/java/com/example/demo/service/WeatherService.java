package com.example.demo.service;

import com.example.demo.client.ForecastResponse;
import com.example.demo.client.GeocodingResponse;
import com.example.demo.client.OpenMeteoClient;
import com.example.demo.dto.WeatherResponse;
import com.example.demo.exception.CityNotFoundException;
import com.example.demo.exception.WeatherServiceUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;

    public WeatherService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public WeatherResponse getWeather(String city) {
        try {
            GeocodingResponse geocoding = openMeteoClient.geocode(city);
            if (geocoding.results() == null || geocoding.results().isEmpty()) {
                throw new CityNotFoundException(city);
            }
            GeocodingResponse.GeocodingResult location = geocoding.results().get(0);

            ForecastResponse forecast = openMeteoClient.forecast(location.latitude(), location.longitude());
            ForecastResponse.Current current = forecast.current();

            return new WeatherResponse(
                    location.name(),
                    location.country(),
                    current.temperature(),
                    current.humidity(),
                    current.windSpeed(),
                    describeWeatherCode(current.weatherCode()),
                    current.time()
            );
        } catch (RestClientException e) {
            throw new WeatherServiceUnavailableException();
        }
    }

    // WMO Weather interpretation codes (https://open-meteo.com/en/docs)
    private String describeWeatherCode(int code) {
        if (code == 0) return "맑음";
        if (code == 1 || code == 2) return "대체로 맑음";
        if (code == 3) return "흐림";
        if (code == 45 || code == 48) return "안개";
        if (code >= 51 && code <= 57) return "이슬비";
        if (code >= 61 && code <= 67) return "비";
        if (code >= 71 && code <= 77) return "눈";
        if (code >= 80 && code <= 82) return "소나기";
        if (code == 85 || code == 86) return "소나기 눈";
        if (code >= 95) return "뇌우";
        return "알 수 없음";
    }
}
