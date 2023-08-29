package ir.piana.dev.common.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import ir.piana.dev.common.context.PropertyOverrideContextInitializer;
import ir.piana.dev.jsonparser.json.JsonParser;
import ir.piana.dev.jsonparser.json.JsonTarget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
@Import(value = {HttpTest.TestConfig.class})
public class HttpTest {

    @Configuration
    @ComponentScan("ir.piana.dev")
    static class TestConfig {

    }

    @Autowired
    private JsonParser jsonParser;

    @Autowired
    private WebClient webClient;

    @Test
    void PostHandlerTest(@Value("classpath:post-test.json") Resource resource) throws InterruptedException {
        byte[] contentAsByteArray = null;
        try {
            contentAsByteArray = resource.getContentAsString(Charset.forName("utf-8")).getBytes("utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JsonTarget request = jsonParser.fromBytes(contentAsByteArray,
                null, true);

        CompletableFuture<HttpResponse<Buffer>> f = webClient.post("/api/test/post")
                .sendJson(request.getJsonObject())
                .toCompletionStage().toCompletableFuture().whenComplete((res, thr) -> {
                    if (thr == null) {
                        System.out.println(res.statusCode());
                        System.out.println(res.bodyAsString());
                    } else {
                        thr.printStackTrace();
                        System.out.println(thr.getMessage());
                    }
                });

        f.join();
    }
}
