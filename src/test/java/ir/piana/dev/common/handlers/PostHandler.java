package ir.piana.dev.common.handlers;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import ir.piana.dev.common.handler.*;
import ir.piana.dev.common.participants.TransformParticipant;
import ir.piana.dev.jsonparser.json.JsonParser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.concurrent.CompletableFuture;

@Handler
//@DependsOn("authApWebClient")
public class PostHandler extends BaseRequestHandler<PostHandler.Request> {
    @Autowired
    private HandlerResponseBuilder handlerResponseBuilder;

    @Autowired
    private JsonParser jsonParser;

    @Autowired
    private TransformParticipant transformParticipant;

    @Value("classpath:real-to-ap.yml")
    private Resource realResource;

    @Value("classpath:legal-to-ap.yml")
    private Resource legalResource;

    @Autowired
    private HandlerResponseBuilder responseBuilder;

    @Autowired
//    @Qualifier("authApWebClient")
    private WebClient webClient;

    @Autowired
    protected PostHandler(
            ContextLoggerProvider contextLoggerProvider) {
        super(contextLoggerProvider);
    }

    @ChainStep(order = 1)
//    @Transactional(propagation = Propagation.REQUIRED)
    public void step1(HandlerRequest <Request> handlerRequest, HandlerInterStateTransporter transporter) {
        contextLogger.info(handlerRequest.getJsonTarget().asString("message"));
    }

    @ChainStep(order = 2)
    public CompletableFuture<HttpResponse<Buffer>> step2(
            HandlerRequest<Request> handlerRequest, HandlerInterStateTransporter transporter) {
        return webClient.post("/connect/token")
                .sendJson(JsonObject.of())
                .toCompletionStage().toCompletableFuture();
    }

    @ChainStep(order = 3)
    public void step3(HandlerRequest<Request> handlerRequest, HandlerInterStateTransporter transporter) {
        HttpResponse<Buffer> value = transporter.getValue("step2");
        contextLogger.info(value.bodyAsString());
    }

    @Override
    public HandlerResponse provideResponse(
            HandlerRequest<Request> handlerRequest,
            HandlerInterStateTransporter transporter) {
        return responseBuilder.fromDto(new Response(
                1, handlerRequest.getDto().message)).build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        private String message;

        public Request(String message) {
            this.message = message;
        }

    }

    @Getter
    @Setter
    public static class Response {
        private int id;
        private String message;

        public Response(int id, String message) {
            this.id = id;
            this.message = message;
        }
    }
}
