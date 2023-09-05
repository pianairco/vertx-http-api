package ir.piana.dev.common.vertx.http.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.uritemplate.UriTemplate;
import ir.piana.dev.common.http.client.mock.MockHttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockWebClient implements WebClient {
    Map<String, MockHttpResponse> mockHttpResponseMap;

    public MockWebClient(Map<String, MockHttpResponse> mockHttpResponseMap) {
        this.mockHttpResponseMap = mockHttpResponseMap;
    }

    /**
     * Like {@link #request(HttpMethod, int, String, String)} using the {@code serverAddress} parameter to connect to the
     * server instead of the {@code port} and {@code host} parameters.
     * <p>
     * The request host header will still be created from the {@code port} and {@code host} parameters.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, int port, String host, String requestURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault(
                "**" + method + "**" + requestURI,
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #request(HttpMethod, int, String, UriTemplate)} using the {@code serverAddress} parameter to connect to the
     * server instead of the {@code port} and {@code host} parameters.
     * <p>
     * The request host header will still be created from the {@code port} and {@code host} parameters.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, int port, String host, UriTemplate requestURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + requestURI.toString(),
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #request(HttpMethod, String, String)} using the {@code serverAddress} parameter to connect to the
     * server instead of the default port and {@code host} parameter.
     * <p>
     * The request host header will still be created from the default port and {@code host} parameter.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, String host, String requestURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + requestURI,
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #request(HttpMethod, String, UriTemplate)} using the {@code serverAddress} parameter to connect to the
     * server instead of the default port and {@code host} parameter.
     * <p>
     * The request host header will still be created from the default port and {@code host} parameter.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, String host, UriTemplate requestURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + requestURI.toString(),
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #request(HttpMethod, String)} using the {@code serverAddress} parameter to connect to the
     * server instead of the default port and default host.
     * <p>
     * The request host header will still be created from the default port and default host.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, String requestURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + requestURI,
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #request(HttpMethod, UriTemplate)} using the {@code serverAddress} parameter to connect to the
     * server instead of the default port and default host.
     * <p>
     * The request host header will still be created from the default port and default host.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, UriTemplate requestURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + requestURI.toString(),
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #request(HttpMethod, RequestOptions)} using the {@code serverAddress} parameter to connect to the
     * server instead of the {@code options} parameter.
     * <p>
     * The request host header will still be created from the {@code options} parameter.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> request(HttpMethod method, SocketAddress serverAddress, RequestOptions options) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + options.getURI(),
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Like {@link #requestAbs(HttpMethod, String)} using the {@code serverAddress} parameter to connect to the
     * server instead of the {@code absoluteURI} parameter.
     * <p>
     * The request host header will still be created from the {@code absoluteURI} parameter.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> requestAbs(HttpMethod method, SocketAddress serverAddress, String absoluteURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + parse(absoluteURI),
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    static String parse(String absoluteURI) {
        try {
            return new URL(absoluteURI).getFile();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Like {@link #requestAbs(HttpMethod, UriTemplate)} using the {@code serverAddress} parameter to connect to the
     * server instead of the {@code absoluteURI} parameter.
     * <p>
     * The request host header will still be created from the {@code absoluteURI} parameter.
     * <p>
     * Use {@link SocketAddress#domainSocketAddress(String)} to connect to a unix domain socket server.
     */
    public HttpRequest<Buffer> requestAbs(HttpMethod method, SocketAddress serverAddress, UriTemplate absoluteURI) {
        return new MockHttpRequest(mockHttpResponseMap.getOrDefault("**" + method + "**" + absoluteURI.toString(),
                new MockHttpResponse(404, Buffer.buffer("not implemented mock!"), new LinkedHashMap<>())));
    }

    /**
     * Close the client. Closing will close down any pooled connections.
     * Clients should always be closed after use.
     */
    public void close() {

    }
}
