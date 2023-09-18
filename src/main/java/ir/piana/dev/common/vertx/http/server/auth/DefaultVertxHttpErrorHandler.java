package ir.piana.dev.common.vertx.http.server.auth;

import io.vertx.core.http.HttpServerRequest;
import ir.piana.dev.common.handler.CommonResponse;
import ir.piana.dev.common.handler.HandlerResponseBuilder;
import ir.piana.dev.common.vertx.http.server.VertxHttpErrorHandleable;
import ir.piana.dev.common.vertx.http.tmpl.VertxThymeleafTemplateEngine;
import ir.piana.dev.jsonparser.json.JsonTargetBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultVertxHttpErrorHandler implements VertxHttpErrorHandleable {
    @Autowired
    protected HandlerResponseBuilder responseBuilder;

    @Autowired
    protected VertxThymeleafTemplateEngine thymeleaf;

    public CommonResponse handle(int httpErrorCode, HttpServerRequest request) {
        return responseBuilder.fromJsonTarget(JsonTargetBuilder.asObject()
                        .add("message", "error occurred : " + httpErrorCode)
                        .build())
                .build();
    }
}
