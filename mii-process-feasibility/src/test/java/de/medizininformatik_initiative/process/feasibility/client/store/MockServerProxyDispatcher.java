package de.medizininformatik_initiative.process.feasibility.client.store;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.BufferedSink;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpHeaders.PROXY_AUTHENTICATE;
import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED;

public class MockServerProxyDispatcher extends Dispatcher {

    private OkHttpClient client;
    private HttpUrl targetServiceUrl;
    private List<RecordedRequest> recordedRequests = new ArrayList<>();
    private List<MockResponse> recordedResponses = new ArrayList<>();

    public MockServerProxyDispatcher(OkHttpClient client, HttpUrl targetServiceUrl) {
        this.client = requireNonNull(client);
        this.targetServiceUrl = requireNonNull(targetServiceUrl);
    }

    public static MockServerProxyDispatcher createReverseProxyDispatcher(OkHttpClient client,
                                                                         HttpUrl targetServiceUrl) {
        return new MockServerProxyDispatcher(client, targetServiceUrl);
    }

    public static MockServerProxyDispatcher createForwardProxyDispatcher(OkHttpClient client,
                                                                         HttpUrl targetServiceUrl) {
        return new MockServerForwardProxyDispatcher(client, targetServiceUrl);
    }

    @Nonnull
    @Override
    public MockResponse dispatch(@Nonnull RecordedRequest req) {
        recordedRequests.add(req);

        // TODO: revise - getRequestUrl may be sufficient!
        var target = HttpUrl.parse(req.getPath());
        if (target == null) {
            if (req.getRequestUrl() != null) {
                target = HttpUrl.parse(req.getRequestUrl().toString());
            } else {
                return failedResponse();
            }
        }
        var method = req.getMethod();
        var requestBody = extractRequestBody(req);
        var headers = new Headers.Builder()
                .addAll(req.getHeaders())
                .add("X-Forwarded-Proto", req.getRequestUrl().scheme())
                .add("X-Forwarded-Host", "%s:%s".formatted(req.getRequestUrl().host(), req.getRequestUrl().port()))
                .build();

        var response = requestTarget(target, method, requestBody, headers);

        return response;
    }

    MockResponse requestTarget(HttpUrl url, String method, RequestBody body, Headers headers) {
        var targetedUrl = url.newBuilder()
                .scheme(targetServiceUrl.scheme())
                .host(targetServiceUrl.host())
                .port(targetServiceUrl.port())
                .build();

        var reqBuilder = new Request.Builder()
                .url(targetedUrl)
                .headers(headers)
                .removeHeader(HttpHeaders.HOST)
                .method(method, body);

        Response targetResponse;
        try {
            targetResponse = client.newCall(reqBuilder.build()).execute();
        } catch (IOException e) {
            // TODO: rather nasty - check in review!
            e.printStackTrace();
            return recordResponse(failedResponse());
        }

        var response = new MockResponse()
                .setHeaders(targetResponse.headers())
                .setResponseCode(targetResponse.code());


        var targetResponseBody = targetResponse.body();
        if (targetResponseBody != null) {
            try {
                // Reading possibly chunked body content
                var b = IOUtils.toString(targetResponseBody.byteStream(), StandardCharsets.UTF_8);
                response.setBody(b);
                response.removeHeader(TRANSFER_ENCODING);
            } catch (IOException e) {
                // TODO: rather nasty - check in review!
                e.printStackTrace();
                return recordResponse(failedResponse());
            }
        }

        return recordResponse(response);
    }

    RequestBody extractRequestBody(@Nonnull RecordedRequest req) {
        if (req.getBodySize() != 0) {
            return new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return MediaType.parse(req.getHeader(CONTENT_TYPE));
                }

                @Override
                public void writeTo(@Nonnull BufferedSink sink) throws IOException {
                    req.getBody().clone().readAll(sink);
                }
            };
        } else {
            return null;
        }
    }

    MockResponse failedResponse() {
        return new MockResponse()
                .setStatus("Rev Proxy Error")
                .setResponseCode(SC_INTERNAL_SERVER_ERROR);
    }

    MockResponse recordResponse(@Nonnull MockResponse response) {
        recordedResponses.add(response);
        return response;
    }

    public List<RecordedRequest> getRecordedRequests() {
        return List.copyOf(recordedRequests);
    }

    public List<MockResponse> getRecordedResponses() {
        return List.copyOf(recordedResponses);
    }

    public void clearRecordedRequests() {
        recordedRequests.clear();
    }

    public void clearRecordedResponses() {
        recordedResponses.clear();
    }

    static class MockServerForwardProxyDispatcher extends MockServerProxyDispatcher {

        private static final Pattern REQUEST_LINE_URL = Pattern.compile("^(\\w+)\\s+(https?://[^\\s]+)\\s+.*$");

        public MockServerForwardProxyDispatcher(OkHttpClient client, HttpUrl targetServiceUrl) {
            super(client, targetServiceUrl);
        }

        @Nonnull
        @Override
        public MockResponse dispatch(@Nonnull RecordedRequest req) {
            if (req.getHeader(HttpHeaders.PROXY_AUTHORIZATION) == null) {
                return recordResponse(new MockResponse().setResponseCode(SC_PROXY_AUTHENTICATION_REQUIRED)
                        .setHeader(PROXY_AUTHENTICATE, "Basic"));
            } else {
                var requestLineParts = REQUEST_LINE_URL.matcher(req.getRequestLine());
                if (requestLineParts.matches()) {
                    return recordResponse(requestTarget(HttpUrl.parse(requestLineParts.group(2)),
                            requestLineParts.group(1),
                            extractRequestBody(req),
                            req.getHeaders()));
                } else {
                    return recordResponse(failedResponse());
                }
            }
        }
    }
}
