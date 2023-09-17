package ir.piana.dev.common.vertx.http.server.auth;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import ir.piana.dev.common.http.auth.BaseAuthPhraseConsumable;
import ir.piana.dev.common.util.MapAny;

import java.util.Optional;

public abstract class VertxCookieBaseAuthPhraseConsumable extends BaseAuthPhraseConsumable<HttpServerRequest, HttpServerResponse> {
    public VertxCookieBaseAuthPhraseConsumable(MapAny configs) {
        super(configs);
    }

    Cookie empty = Cookie.cookie("empty", "");

    public static class Default extends VertxCookieBaseAuthPhraseConsumable {
        public Default(MapAny configs) {
            super(configs);
        }

        @Override
        public String consume(HttpServerRequest request) {
            return Optional.ofNullable(request.getCookie(this.configs.getValue("cookie-name")))
                    .orElse(empty).getValue();
        }

        @Override
        public void produce(HttpServerResponse response, String authPhrase) {
            response.addCookie(Cookie.cookie(this.configs.getValue("cookie-name"), authPhrase));
        }
    }
}
