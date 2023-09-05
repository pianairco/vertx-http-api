package ir.piana.dev.common.vertx.http.client;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ir.piana.dev.common.http.client.HttpClientItem;
import ir.piana.dev.common.http.client.WebClientProvider;
import ir.piana.dev.common.http.client.mock.MockHttpItem;
import ir.piana.dev.common.http.client.mock.MockHttpResponse;
import ir.piana.dev.common.http.client.mock.MockRouteItem;
import ir.piana.dev.common.http.client.mock.MockWebClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//@Configuration
//@Profile("vertx-http-client & http-mock")
//@Import(VertxHttpClientAutoConfiguration.class)
public class VertxHttpClientMockAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    @Bean
//    @Primary
//    @Profile("vertx-http-client & http-mock")
    public Map<String, Map<String, MockHttpResponse>> httpResponseMap(List<MockWebClientProvider> mockProviders) {
        Map<String, Map<String, MockHttpResponse>> httpResponseMap = new LinkedHashMap<>();
        for (MockWebClientProvider mockProvider : mockProviders) {
            for (MockHttpItem mock : mockProvider.mocks()) {
                if (httpResponseMap.containsKey(mock.getBeanName())) {
                    httpResponseMap.put(mock.getBeanName(), new LinkedHashMap<>());
                }
                for (MockRouteItem mockRoute : mock.getMockRoutes()) {
                    httpResponseMap.get(mock.getBeanName())
                            .put("**" + mockRoute.getMethod() + "**" + mockRoute.getPath(), mockRoute.getResponse());
                }
            }
        }
        return httpResponseMap;
    }

//    @Bean
//    @Primary
//    @Profile("vertx-http-client & http-mock")
    public WebClient webClient(
            List<WebClientProvider> providers,
            Map<String, Map<String, MockHttpResponse>> mockHttpResponseMap,
            AnnotationConfigApplicationContext applicationContext,
            ConfigurableBeanFactory beanFactory) {
        List<WebClient> list = new ArrayList<>();
        Map<String, WebClient> map = new LinkedHashMap<>();
        if(providers == null)
            return null;
        for (WebClientProvider provider : providers) {
            if(provider.webClients() == null)
                continue;
            for (HttpClientItem item : provider.webClients()) {
                StringBuilder key = new StringBuilder(item.isSsl() ? "https://" : "http://")
                        .append(item.getHost()).append(":").append(item.getPort());

                if (!map.containsKey(key.toString())) {
                    WebClient webClient = new MockWebClient(mockHttpResponseMap.get(item.getBeanName()));
                    list.add(webClient);
                    map.put(key.toString(), webClient);
                    applicationContext.registerBean(item.getBeanName(), WebClient.class, () -> webClient);
                } else {
                    if (!applicationContext.containsBean(item.getBeanName()))
                        applicationContext.registerBean(item.getBeanName(), WebClient.class, () -> map.get(item.getBeanName()));
                }
            }
        }

        return list.isEmpty() ? null : list.get(0);
    }
}
