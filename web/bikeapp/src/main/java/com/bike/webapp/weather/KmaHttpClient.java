package com.bike.webapp.weather;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls 기상청 단기예보 조회서비스 2.0 / getUltraSrtNcst (초단기실황) with short timeouts.
 *
 * <p>The service key comes from {@code trailgo.weather.kma-service-key} (environment variable
 * {@code KMA_SERVICE_KEY}) and is part of the request URL. Exception messages embed that URL, so
 * they are never logged: only the exception class and HTTP status are. Response bodies are never
 * logged either.</p>
 */
@Component
public class KmaHttpClient implements KmaWeatherClient {

	private static final Logger log = LoggerFactory.getLogger(KmaHttpClient.class);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

	private final String serviceKey;
	private final String baseUrl;
	private final RestTemplate restTemplate;

	public KmaHttpClient(
			@Value("${trailgo.weather.kma-service-key:}") String serviceKey,
			@Value("${trailgo.weather.kma-base-url:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}") String baseUrl,
			@Value("${trailgo.weather.connect-timeout-ms:2000}") int connectTimeoutMs,
			@Value("${trailgo.weather.read-timeout-ms:3000}") int readTimeoutMs) {
		this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
		this.baseUrl = baseUrl;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		this.restTemplate = new RestTemplate(factory);
	}

	public boolean isConfigured() {
		return !serviceKey.isEmpty();
	}

	@Override
	public RawResponse fetchObservation(int nx, int ny, LocalDateTime base) {
		if (!isConfigured()) {
			return null;
		}
		try {
			URI uri = URI.create(baseUrl + "/getUltraSrtNcst"
					+ "?serviceKey=" + encodedKey()
					+ "&pageNo=1&numOfRows=100&dataType=JSON"
					+ "&base_date=" + base.format(DATE)
					+ "&base_time=" + base.format(TIME)
					+ "&nx=" + nx + "&ny=" + ny);
			return new RawResponse(200, restTemplate.getForObject(uri, String.class));
		} catch (HttpStatusCodeException e) {
			// 429 / 5xx etc.: the body (if any) may still carry the service's own error code.
			log.warn("KMA getUltraSrtNcst failed: HTTP {} (nx={}, ny={})", e.getRawStatusCode(), nx, ny);
			return new RawResponse(e.getRawStatusCode(), e.getResponseBodyAsString());
		} catch (RuntimeException e) {
			// Deliberately no message/stack trace: RestClientException text contains the request URL.
			log.warn("KMA getUltraSrtNcst failed: {} (nx={}, ny={})", e.getClass().getSimpleName(), nx, ny);
			return null;
		}
	}

	/** Accepts both the "Encoding" and "Decoding" key from the data portal and encodes exactly once. */
	private String encodedKey() {
		try {
			String decoded = serviceKey.contains("%") ? URLDecoder.decode(serviceKey, "UTF-8") : serviceKey;
			return URLEncoder.encode(decoded, "UTF-8");
		} catch (UnsupportedEncodingException | IllegalArgumentException e) {
			throw new IllegalStateException("Invalid KMA service key encoding");
		}
	}
}
