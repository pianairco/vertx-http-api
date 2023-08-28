package ir.piana.dev.common.vertx;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Import(VertxAutoConfiguration.class)
public class VertxHttpClientAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    @Primary
    @Profile("vertx-http-client")
    public WebClient webClient(
            Vertx vertx, VertxHttpClient httpClient,
            AnnotationConfigApplicationContext applicationContext,
            ConfigurableBeanFactory beanFactory) {
        List<WebClient> list = new ArrayList<>();
        for (VertxHttpClientItem item : httpClient.items) {
            WebClient webClient = WebClient.create(vertx, new WebClientOptions()
                    .setSsl(item.ssl)
                    .setMaxPoolSize(item.maxPoolSize)
                    .setReusePort(Boolean.TRUE)
                    .setTcpQuickAck(Boolean.TRUE)
                    .setTcpCork(Boolean.TRUE)
                    .setTcpFastOpen(Boolean.TRUE)
                    .setDefaultHost(item.host)
                    .setDefaultPort(item.port)
                    .setConnectTimeout(10000)
                    .setReadIdleTimeout(10000)
                    .setSslHandshakeTimeout(10000));
            list.add(webClient);
            applicationContext.registerBean(item.beanName, WebClient.class, () -> webClient);
        }

        return list.get(0);
    }

    @Setter
    @Component
    @ConfigurationProperties(prefix = "ir.piana.dev.common.vertx.http-client")
    @Profile("vertx-http-client")
    static class VertxHttpClient {
        private List<VertxHttpClientItem> items;
    }

    @Setter
    static class VertxHttpClientItem {
        private String beanName;
        private boolean ssl;
        private String host;
        private int port;
        private int maxPoolSize;
    }
}
