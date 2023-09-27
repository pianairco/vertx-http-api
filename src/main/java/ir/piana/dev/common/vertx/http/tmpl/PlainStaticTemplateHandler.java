package ir.piana.dev.common.vertx.http.tmpl;

import ir.piana.dev.common.handler.*;
import ir.piana.dev.jsonparser.json.JsonTargetBuilder;

@Handler
public class PlainStaticTemplateHandler extends AuthenticableRequestHandler {
    protected PlainStaticTemplateHandler(ContextLoggerProvider contextLoggerProvider) {
        super(contextLoggerProvider);
    }

    @Override
    public HandlerModelAndViewResponse provideResponse(
            HandlerRequest handlerRequest, HandlerInterStateTransporter transporter)
            throws HandlerRuntimeException {
        String path = handlerRequest.getAdditionalParam().getFirstValue("*");
        if (path == null || path.isEmpty() || path.equalsIgnoreCase("/"))
            path = "index";

        return HandlerModelAndViewResponse.builder()
                .view(path)
                .model(JsonTargetBuilder.asObject()
                        .addObject("auth", transporter.getUserAuthentication().getPrincipal())
                        .build())
                .build();
    }
}
