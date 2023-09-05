package ir.piana.dev.common.context;

import ir.piana.dev.common.http.client.mock.MockHttpItem;
import ir.piana.dev.common.vertx.http.client.VertxMockWebClientProvider;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ir.piana.dev.common.test.mock-web-clients")
@Setter
public class MockWebClientProviderImpl implements VertxMockWebClientProvider {
    private List<MockHttpItem> items;

    @Override
    public List<MockHttpItem> mocks() {
        return items;
    }
}
