package de.medizininformatik_initiative.process.feasibility.client.store;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.BufferedSink;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

@RequiredArgsConstructor
public class MockServerProxyDispatcher extends Dispatcher {

    @NonNull
    private OkHttpClient client;
    @NonNull
    private HttpUrl targetServiceUrl;

    @Nonnull
    @Override
    public MockResponse dispatch(@Nonnull RecordedRequest req) {
        // TODO: revise - getRequestUrl may be sufficient!
        var target = HttpUrl.parse(req.getPath());
        if (target == null) {
            if (req.getRequestUrl() != null) {
                target = HttpUrl.parse(req.getRequestUrl().toString());
            } else {
                return failedResponse();
            }
        }

        var targetedUrl = target.newBuilder()
                .scheme(targetServiceUrl.scheme())
                .host(targetServiceUrl.host())
                .port(targetServiceUrl.port())
                .build();

        var reqBuilder = new Request.Builder()
                .url(targetedUrl)
                .headers(req.getHeaders())
                .removeHeader(HttpHeaders.HOST);
        addRequestBody(req, reqBuilder);


        Response targetResponse;
        try {
            targetResponse = client.newCall(reqBuilder.build()).execute();
        } catch (IOException e) {
            // TODO: rather nasty - check in review!
            e.printStackTrace();
            return failedResponse();
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
                return failedResponse();
            }
        }

        return response;
    }

    private void addRequestBody(@Nonnull RecordedRequest req, Request.Builder reqBuilder) {
        if (req.getBodySize() != 0) {
            reqBuilder.method(req.getMethod(), new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return MediaType.parse(req.getHeader(CONTENT_TYPE));
                }

                @Override
                public void writeTo(@Nonnull BufferedSink sink) throws IOException {
                    req.getBody().clone().readAll(sink);
                }
            });
        }
    }

    private MockResponse failedResponse() {
        return new MockResponse()
                .setStatus("Rev Proxy Error")
                .setResponseCode(SC_INTERNAL_SERVER_ERROR);
    }
}
