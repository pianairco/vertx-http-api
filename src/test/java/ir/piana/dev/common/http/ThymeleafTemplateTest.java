package ir.piana.dev.common.http;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.common.template.TemplateEngine;
import ir.piana.dev.common.context.PropertyOverrideContextInitializer;
import ir.piana.dev.common.vertx.http.client.VertxHttpClientAutoConfiguration;
import ir.piana.dev.vertxthymeleaf.ThymeleafTemplateEngine;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
@ContextConfiguration(initializers = PropertyOverrideContextInitializer.class)
@Import(value = {ThymeleafTemplateTest.TestConfig.class})
public class ThymeleafTemplateTest {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Configuration
    @ComponentScan("ir.piana.dev")
    @Import(VertxHttpClientAutoConfiguration.class)
    static class TestConfig {
        /*@Bean
        TemplateEngine thymeleafTemplateEngine(Vertx vertx) {
            return ThymeleafTemplateEngine.create(vertx);
        }*/
    }

    @Autowired
    @Qualifier("localWebClient")
    private WebClient webClient;

    @Test
    void htmlTest(@Value("classpath:post-test.json") Resource resource) throws InterruptedException {
        CompletableFuture<HttpResponse<Buffer>> f = webClient.get("/web-ui/test")
                .send()
                .toCompletionStage().toCompletableFuture().whenComplete((res, thr) -> {
                    if (thr == null) {
                        logger.info(res.statusCode() + " : " + res.bodyAsString());
                    } else {
                        thr.printStackTrace();
                        logger.error(thr.getMessage());
                    }
                });
        f.join();
    }
}
