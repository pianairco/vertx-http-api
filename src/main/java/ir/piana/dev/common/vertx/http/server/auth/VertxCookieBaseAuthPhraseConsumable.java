package ir.piana.dev.common.vertx.http.server.auth;

import io.vertx.core.http.HttpServerRequest;
import ir.piana.dev.common.http.auth.BaseAuthPhraseConsumable;
import ir.piana.dev.common.util.MapAny;

public abstract class VertxCookieBaseAuthPhraseConsumable extends BaseAuthPhraseConsumable<HttpServerRequest> {
    public VertxCookieBaseAuthPhraseConsumable(MapAny configs) {
        super(configs);
    }

    public static class Default extends VertxCookieBaseAuthPhraseConsumable {
        public Default(MapAny configs) {
            super(configs);
        }

        @Override
        public String consume(HttpServerRequest request) {
            return request.getCookie(this.configs.getValue("cookie-name")).getValue();
        }
    }
}
