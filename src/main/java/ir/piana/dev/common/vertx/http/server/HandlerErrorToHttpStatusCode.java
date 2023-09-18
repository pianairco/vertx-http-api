package ir.piana.dev.common.vertx.http.server;

import ir.piana.dev.common.handler.HandlerErrorType;
import lombok.Getter;

public enum HandlerErrorToHttpStatusCode {
    HTTP_OK(200),
    HTTP_CREATED(201),
    HTTP_ACCEPTED(202),
    HTTP_NOT_AUTHORITATIVE(203),
    HTTP_NO_CONTENT(204),
    HTTP_RESET(205),
    HTTP_PARTIAL(206),
    HTTP_MULT_CHOICE(300),
    HTTP_MOVED_PERM(301),
    HTTP_MOVED_TEMP(302),
    HTTP_SEE_OTHER(303),
    HTTP_NOT_MODIFIED(304),
    HTTP_USE_PROXY(305),
    HTTP_BAD_REQUEST(400),
    HTTP_UNAUTHORIZED(401),
    HTTP_PAYMENT_REQUIRED(402),
    HTTP_FORBIDDEN(403),
    HTTP_NOT_FOUND(404),
    HTTP_BAD_METHOD(405),
    HTTP_NOT_ACCEPTABLE(406),
    HTTP_PROXY_AUTH(407),
    HTTP_CLIENT_TIMEOUT(408),
    HTTP_CONFLICT(409),
    HTTP_GONE(410),
    HTTP_LENGTH_REQUIRED(411),
    HTTP_PRECON_FAILED(412),
    HTTP_ENTITY_TOO_LARGE(413),
    HTTP_REQ_TOO_LONG(414),
    HTTP_UNSUPPORTED_TYPE(415),
    /** @deprecated */
    @Deprecated
    HTTP_SERVER_ERROR(500),
    HTTP_INTERNAL_ERROR(500),
    HTTP_NOT_IMPLEMENTED(501),
    HTTP_BAD_GATEWAY(502),
    HTTP_UNAVAILABLE(503),
    HTTP_GATEWAY_TIMEOUT(504),
    HTTP_VERSION(505);

    @Getter
    private int statusCode;

    HandlerErrorToHttpStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public static HandlerErrorToHttpStatusCode byHandlerErrorType(HandlerErrorType handlerErrorType) {
        switch (handlerErrorType) {
            case OK -> {
                return HTTP_OK;
            }
            case CANCELLED -> {
                return HTTP_BAD_REQUEST;
            }
            case UNKNOWN -> {
                return HTTP_NOT_FOUND;
            }
            case INVALID_ARGUMENT -> {
                return HTTP_BAD_REQUEST;
            }
            case DEADLINE_EXCEEDED -> {
                return HTTP_INTERNAL_ERROR;
            }
            case NOT_FOUND -> {
                return HTTP_NOT_FOUND;
            }
            case ALREADY_EXISTS -> {
                return HTTP_CONFLICT;
            }
            case UNAUTHENTICATED -> {
                return HTTP_FORBIDDEN;
            }
            case PERMISSION_DENIED -> {
                return HTTP_UNAUTHORIZED;
            }
            case RESOURCE_EXHAUSTED -> {
                return HTTP_BAD_REQUEST;
            }
            case FAILED_PRECONDITION -> {
                return HTTP_BAD_REQUEST;
            }
            case ABORTED -> {
                return HTTP_BAD_REQUEST;
            }
            case OUT_OF_RANGE -> {
                return HTTP_BAD_REQUEST;
            }
            case UNIMPLEMENTED -> {
                return HTTP_NOT_IMPLEMENTED;
            }
            case INTERNAL -> {
                return HTTP_INTERNAL_ERROR;
            }
            case UNAVAILABLE -> {
                return HTTP_UNAVAILABLE;
            }
            case DATA_LOSS -> {
                return HTTP_INTERNAL_ERROR;
            }
        }
        return HTTP_INTERNAL_ERROR;
    }
}
