package ir.piana.dev.common.vertx.http.client;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.uritemplate.Variables;
import ir.piana.dev.common.http.client.mock.MockHttpResponse;

import java.util.List;
import java.util.Map;

public class MockVertxHttpRequest implements HttpRequest<Buffer> {
    private MockHttpResponse response;

    public MockVertxHttpRequest(MockHttpResponse response) {
        this.response = response;
    }

    @Override
    public HttpRequest<Buffer> method(HttpMethod value) {
        return this;
    }

    @Override
    public HttpMethod method() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> port(int value) {
        return this;
    }

    @Override
    public int port() {
        return 0;
    }

    @Override
    public <U> HttpRequest<U> as(BodyCodec<U> responseCodec) {
        return (HttpRequest<U>) this;
    }

    @Override
    public BodyCodec<Buffer> bodyCodec() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> host(String value) {
        return this;
    }

    @Override
    public String host() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> virtualHost(String value) {
        return this;
    }

    @Override
    public String virtualHost() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> uri(String value) {
        return this;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> putHeaders(MultiMap headers) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> putHeader(String name, String value) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> putHeader(String name, Iterable<String> value) {
        return this;
    }

    @Override
    public MultiMap headers() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> authentication(Credentials credentials) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> ssl(Boolean value) {
        return this;
    }

    @Override
    public Boolean ssl() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> timeout(long value) {
        return this;
    }

    @Override
    public long timeout() {
        return 0;
    }

    @Override
    public HttpRequest<Buffer> addQueryParam(String paramName, String paramValue) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> setQueryParam(String paramName, String paramValue) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> setTemplateParam(String paramName, String paramValue) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> setTemplateParam(String paramName, List<String> paramValue) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> setTemplateParam(String paramName, Map<String, String> paramValue) {
        return this;
    }

    @Override
    public HttpRequest<Buffer> followRedirects(boolean value) {
        return this;
    }

    @Override
    public boolean followRedirects() {
        return false;
    }

    @Override
    public HttpRequest<Buffer> proxy(ProxyOptions proxyOptions) {
        return this;
    }

    @Override
    public ProxyOptions proxy() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> expect(ResponsePredicate predicate) {
        return this;
    }

    @Override
    public List<ResponsePredicate> expectations() {
        return null;
    }

    @Override
    public MultiMap queryParams() {
        return null;
    }

    @Override
    public Variables templateParams() {
        return null;
    }

    @Override
    public HttpRequest<Buffer> copy() {
        return this;
    }

    @Override
    public HttpRequest<Buffer> multipartMixed(boolean allow) {
        return this;
    }

    @Override
    public boolean multipartMixed() {
        return false;
    }

    @Override
    public HttpRequest<Buffer> traceOperation(String traceOperation) {
        return this;
    }

    @Override
    public String traceOperation() {
        return null;
    }

    @Override
    public void sendStream(ReadStream<Buffer> body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    @Override
    public Future<HttpResponse<Buffer>> sendStream(ReadStream<Buffer> body) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void sendBuffer(Buffer body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    @Override
    public Future<HttpResponse<Buffer>> sendBuffer(Buffer body) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void sendJsonObject(JsonObject body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    @Override
    public Future<HttpResponse<Buffer>> sendJsonObject(JsonObject body) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void sendJson(Object body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    @Override
    public Future<HttpResponse<Buffer>> sendJson(Object body) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void sendForm(MultiMap body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    public Future<HttpResponse<Buffer>> sendForm(MultiMap body) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void sendForm(MultiMap body, String charset, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    @Override
    public Future<HttpResponse<Buffer>> sendForm(MultiMap body, String charset) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void sendMultipartForm(MultipartForm body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    public Future<HttpResponse<Buffer>> sendMultipartForm(MultipartForm body) {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }

    @Override
    public void send(Handler<AsyncResult<HttpResponse<Buffer>>> handler) {

    }

    @Override
    public Future<HttpResponse<Buffer>> send() {
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        promise.complete(new MockHttpResponseImpl(response));
        return promise.future();
    }
}
