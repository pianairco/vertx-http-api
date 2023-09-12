package ir.piana.dev.common.vertx.http.client;

import ir.piana.dev.common.http.client.mock.MockHttpItem;
import ir.piana.dev.common.http.client.mock.MockWebClientProvider;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class VertxMockWebClientProvider implements MockWebClientProvider {
    private List<MockHttpItem> items;

    @Override
    public List<MockHttpItem> mocks() {
        return items;
    }
}
