package ir.piana.dev.common.context;


import ir.piana.dev.common.http.client.HttpClientItem;
import ir.piana.dev.common.vertx.http.client.VertxWebClientProvider;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.web-clients")
@Setter
public class WebClientProviderImpl implements VertxWebClientProvider {
    private List<HttpClientItem> items;

    @Override
    public List<HttpClientItem> webClients() {
        return items;
    }
}
