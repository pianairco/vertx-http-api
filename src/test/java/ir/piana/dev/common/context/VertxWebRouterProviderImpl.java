package ir.piana.dev.common.context;

import ir.piana.dev.common.vertx.http.server.VertxWebRouterProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.web-router")
public class VertxWebRouterProviderImpl extends VertxWebRouterProvider {
}
