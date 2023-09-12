package ir.piana.dev.common.vertx.http.client;

import ir.piana.dev.common.http.client.HttpClientItem;
import ir.piana.dev.common.http.client.WebClientProvider;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class VertxWebClientProvider implements WebClientProvider {
    private List<HttpClientItem> items;

    @Override
    public List<HttpClientItem> webClients() {
        return items;
    }
}
