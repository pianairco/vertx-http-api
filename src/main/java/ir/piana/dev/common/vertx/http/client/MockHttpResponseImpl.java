package ir.piana.dev.common.vertx.http.client;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.HttpResponse;
import ir.piana.dev.common.http.client.mock.MockHttpResponse;

import java.util.List;

public class MockHttpResponseImpl<T> implements HttpResponse<T> {

    private final MockHttpResponse httpResponse;

    public MockHttpResponseImpl(MockHttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    @Override
    public HttpVersion version() {
        return null;
    }

    @Override
    public int statusCode() {
        return httpResponse.getStatus();
    }

    @Override
    public String statusMessage() {
        return null;
    }

    @Override
    public MultiMap headers() {
        return MultiMap.caseInsensitiveMultiMap().addAll(httpResponse.getHeaders());
    }

    @Override
    public String getHeader(String headerName) {
        return httpResponse.getHeaders().get(headerName);
    }

    @Override
    public MultiMap trailers() {
        return null;
    }

    @Override
    public String getTrailer(String trailerName) {
        return null;
    }

    @Override
    public List<String> cookies() {
        return null;
    }

    @Override
    public T body() {
        return (T) httpResponse.getBody();
    }

    @Override
    public Buffer bodyAsBuffer() {
        return Buffer.buffer(httpResponse.getBody().getBytes());
    }

    @Override
    public List<String> followedRedirects() {
        return null;
    }

    @Override
    public JsonArray bodyAsJsonArray() {
        return null;
    }
}
