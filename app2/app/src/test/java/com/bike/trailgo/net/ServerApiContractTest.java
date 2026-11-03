package com.bike.trailgo.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.lang.reflect.Method;

import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Pins the HTTP method and URL of every ride-record call the app makes. The web project's
 * {@code AndroidApiContractTest} checks the same strings against its controller, so a change on
 * either side breaks a test.
 */
public class ServerApiContractTest {

    private static Method method(String name) {
        for (Method candidate : ServerApi.class.getDeclaredMethods()) {
            if (candidate.getName().equals(name)) {
                return candidate;
            }
        }
        throw new AssertionError("ServerApi." + name + " is missing");
    }

    private static void assertGet(String methodName, String expectedPath) {
        GET annotation = method(methodName).getAnnotation(GET.class);
        assertNotNull(methodName + " must be a GET", annotation);
        assertEquals(expectedPath, annotation.value());
    }

    private static void assertFormPost(String methodName, String expectedPath) {
        Method m = method(methodName);
        POST annotation = m.getAnnotation(POST.class);
        assertNotNull(methodName + " must be a POST", annotation);
        assertEquals(expectedPath, annotation.value());
        assertNotNull(methodName + " must stay form-encoded", m.getAnnotation(FormUrlEncoded.class));
    }

    @Test
    public void recordListIsStillTheSamePath() {
        assertGet("getRecord", "app/get/{loginId}");
        assertGet("getMyRecord", "app/get/{rr_rider}");
    }

    @Test
    public void signInCallsMatchTheServerAuthEndpoints() {
        assertFormPost("signInWithKakao", "app/auth/kakao");
        assertFormPost("signInWithNaver", "app/auth/naver");
        assertFormPost("refreshSession", "app/auth/refresh");
        assertFormPost("signOut", "app/auth/logout");
    }

    @Test
    public void thePasswordLoginAndSignUpCallsAreGone() {
        for (Method candidate : ServerApi.class.getDeclaredMethods()) {
            assertFalse(candidate.getName(), candidate.getName().equals("Login"));
        }
    }

    @Test
    public void recordDetailWithWeatherUsesTheOwnerScopedPath() {
        assertGet("getMyRecordDetail", "app/riding/records/{riderId}/{rrNum}");
    }

    @Test
    public void saveAndVisibilityCallsAreUnchanged() {
        assertFormPost("InsertRecord", "app/riding/upload");
        assertFormPost("setOpen", "app/recordset/open");
    }

    @Test
    public void memoPhotoAndWeatherCallsUseTheOwnerScopedPaths() {
        assertFormPost("updateMemo", "app/riding/records/{riderId}/{rrNum}/memo");
        POST upload = method("uploadPhoto").getAnnotation(POST.class);
        assertNotNull(upload);
        assertEquals("app/riding/records/{riderId}/{rrNum}/photos", upload.value());
        assertNotNull(method("uploadPhoto").getAnnotation(retrofit2.http.Multipart.class));
        retrofit2.http.DELETE delete = method("deletePhoto").getAnnotation(retrofit2.http.DELETE.class);
        assertNotNull(delete);
        assertEquals("app/riding/records/{riderId}/{rrNum}/photos/{photoId}", delete.value());
        assertGet("getWeather", "app/weather");
        // lat, lon, at and riderId are plain query parameters of that call
        assertEquals(4, method("getWeather").getParameterTypes().length);
    }}
