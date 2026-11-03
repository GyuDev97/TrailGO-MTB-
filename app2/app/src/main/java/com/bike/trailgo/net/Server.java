package com.bike.trailgo.net;

import com.bike.trailgo.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class Server{
    /** Single source of truth for every TrailGo server request. */
    public static final String BASE_URL = normalizeBaseUrl(BuildConfig.TRAILGO_SERVER_BASE_URL);
    public static final int CONNECT_TIMEOUT_MILLIS = 7_000;
    public static final int READ_TIMEOUT_MILLIS = 10_000;
    private static Server instance;
    private ServerApi api;
    private final OkHttpClient httpClient;


    private Server(){
        OkHttpClient plain = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .writeTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build();
        // Renewing a session must not itself go through the session handling.
        final ServerApi refreshApi = retrofit(plain).create(ServerApi.class);
        SessionAuth sessionAuth = new SessionAuth(BASE_URL, refreshToken -> refreshApi.refreshSession(refreshToken).execute());
        httpClient = plain.newBuilder()
                .addInterceptor(sessionAuth)
                .authenticator(sessionAuth)
                .build();
        api = retrofit(httpClient).create(ServerApi.class);
    }

    private static Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /** The client that carries the member's session; image loading shares it. */
    public OkHttpClient getClient() { return httpClient; }
    public static Server getInstance(){
        if ( instance == null ) instance = new Server();
        return instance;
    }

    public ServerApi getApi() { return api; }

    public static String getUrl(String path) {
        if (path == null || path.length() == 0) {
            return BASE_URL;
        }
        return BASE_URL + (path.startsWith("/") ? path.substring(1) : path);
    }

    public static String getRiderImageUrl(String imageName) {
        return getUrl("app/getGPX/rider/" + imageName);
    }

    /** Opens a server URL with the same bounded timeouts used by Retrofit. */
    public static HttpURLConnection openConnection(String path) throws IOException {
        return openUrlConnection(getUrl(path));
    }

    /** For an already-composed URL, such as a GPX URL passed through a parser. */
    public static HttpURLConnection openUrlConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setDoInput(true);
        return connection;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().length() == 0) {
            throw new IllegalStateException("TRAILGO_SERVER_BASE_URL must be configured by Gradle");
        }
        String normalized = baseUrl.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    public static MultipartBody.Part getFilePart(String path, String fileName){
        MultipartBody.Part part = null;
        File file = new File(path);
        if(file.exists()){
            RequestBody body = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            part = MultipartBody.Part.createFormData("file1", fileName, body);
        }
        return part;
    }
}
