package org.openrefine.extensions.commons.importer;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClient {

    private static final String USER_AGENT = "OpenRefine-Commons-Extension/0.1.* (https://github.com/OpenRefine/CommonsExtension)";

    public static OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor(USER_AGENT)).build();
        return client;
    }

    static class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            return chain.proceed(chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build());
        }
    }
    private static OkHttpClient client;
}
