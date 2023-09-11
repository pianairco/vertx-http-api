package ir.piana.dev.common.context;


import ir.piana.dev.common.http.server.HttpServerItem;
import ir.piana.dev.common.http.server.WebServerProvider;
import ir.piana.dev.common.vertx.http.server.VertxWebServerProvider;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.web-server")
@Setter
public class WebServerProviderImpl implements VertxWebServerProvider {
    private List<HttpServerItem> items;

    @Override
    public List<HttpServerItem> webServers() {
        return items;
    }
}
