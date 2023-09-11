package ir.piana.dev.common.context;

import ir.piana.dev.common.http.server.HttpRouterItem;
import ir.piana.dev.common.http.server.WebRouterProvider;
import ir.piana.dev.common.vertx.http.server.VertxWebRouterProvider;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.web-router")
@Setter
public class WebRouterProviderImpl implements VertxWebRouterProvider {
    private List<HttpRouterItem> items;

    @Override
    public List<HttpRouterItem> webRouters() {
        return items;
    }
}
