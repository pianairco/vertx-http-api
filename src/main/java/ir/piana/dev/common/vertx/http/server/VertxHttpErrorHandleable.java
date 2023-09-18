package ir.piana.dev.common.vertx.http.server;

import io.vertx.core.http.HttpServerRequest;
import ir.piana.dev.common.handler.CommonResponse;

public interface VertxHttpErrorHandleable {
    CommonResponse handle(int httpErrorCode, HttpServerRequest request);
}
