package ir.piana.dev.common.context;

import ir.piana.dev.common.vertx.http.client.VertxMockWebClientProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.mock-web-clients")
public class VertxMockWebClientProviderImpl extends VertxMockWebClientProvider {
}
