package ir.piana.dev.common.handlers;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.common.template.TemplateEngine;
import ir.piana.dev.common.handler.*;
import ir.piana.dev.common.util.MapAny;
import ir.piana.dev.jsonparser.json.JsonTarget;
import ir.piana.dev.jsonparser.json.JsonTargetBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Handler
public class StaticResourceHandler extends BaseRequestHandler {
    @Autowired
    protected TemplateEngine templateEngine;

    private MapAny config;

    @Autowired(required = false)
    protected StaticResourceHandler(ContextLoggerProvider contextLoggerProvider,
                                    @Qualifier("staticResourceHandlerConfig") MapAny config) {
        super(contextLoggerProvider);
        this.config = config;
    }

    @PostConstruct
    public void init() {
        System.out.println();
    }

    @Override
    public HandlerModelAndViewResponse provideResponse(
            HandlerRequest handlerRequest, HandlerInterStateTransporter transporter) {
        String filename = handlerRequest.getAdditionalParam().getFirstValue("resourceFilename");
        final JsonTarget jsonTarget = JsonTargetBuilder.asObject()
                .add("foo", "badger")
                .add("bar", "fox")
                .add("context", new JsonObject().put("path", "/test-thymeleaf-template2.html")).build();

        return HandlerModelAndViewResponse.builder()
                .model(jsonTarget)
                .view(filename).build();
    }
}
