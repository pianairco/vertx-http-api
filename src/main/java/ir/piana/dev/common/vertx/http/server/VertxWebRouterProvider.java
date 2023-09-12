package ir.piana.dev.common.vertx.http.server;

import ir.piana.dev.common.http.server.HttpRouterItem;
import ir.piana.dev.common.http.server.WebRouterProvider;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class VertxWebRouterProvider implements WebRouterProvider {
    private List<HttpRouterItem> items;

    @Override
    public List<HttpRouterItem> webRouters() {
        return items;
    }
}
