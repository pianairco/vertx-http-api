package ir.piana.dev.common.vertx.http.server;

import ir.piana.dev.common.http.server.HttpServerItem;
import ir.piana.dev.common.http.server.WebServerProvider;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class VertxWebServerProvider implements WebServerProvider {
    private List<HttpServerItem> items;

    @Override
    public List<HttpServerItem> webServers() {
        return items;
    }
}
