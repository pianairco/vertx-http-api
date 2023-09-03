package ir.piana.dev.common.vertx.http.client;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ir.piana.dev.common.http.client.HttpClientItem;
import ir.piana.dev.common.http.client.WebClientProvider;
import ir.piana.dev.common.vertx.VertxAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Import(VertxAutoConfiguration.class)
public class VertxHttpClientAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    @Primary
    @Profile("vertx-http-client")
    public WebClient webClient(
            List<WebClientProvider> providers,
            Vertx vertx, /*VertxHttpClient httpClient,*/
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
                    WebClient webClient = WebClient.create(vertx, new WebClientOptions()
                            .setSsl(item.isSsl())
                            .setMaxPoolSize(item.getMaxPoolSize())
                            .setReusePort(Boolean.TRUE)
                            .setTcpQuickAck(Boolean.TRUE)
                            .setTcpCork(Boolean.TRUE)
                            .setTcpFastOpen(Boolean.TRUE)
                            .setDefaultHost(item.getHost())
                            .setDefaultPort(item.getPort())
                            .setConnectTimeout(10000)
                            .setReadIdleTimeout(10000)
                            .setSslHandshakeTimeout(10000));
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
