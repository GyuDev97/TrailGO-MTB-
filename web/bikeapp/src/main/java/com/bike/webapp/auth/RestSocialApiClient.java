package com.bike.webapp.auth;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Short timeouts; the token travels only in the Authorization header and is never logged. */
@Component
public class RestSocialApiClient implements SocialApiClient {

	private final RestTemplate restTemplate;

	public RestSocialApiClient() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000);
		factory.setReadTimeout(5000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public Response get(String url, String bearerToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
		try {
			ResponseEntity<String> answer = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Void>(headers),
					String.class);
			return new Response(answer.getStatusCodeValue(), answer.getBody());
		} catch (HttpStatusCodeException e) {
			return new Response(e.getRawStatusCode(), e.getResponseBodyAsString());
		} catch (RestClientException e) {
			throw new SocialUnavailableException("provider not reachable: " + e.getClass().getSimpleName());
		}
	}
}
