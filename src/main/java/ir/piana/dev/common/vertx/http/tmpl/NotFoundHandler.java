package ir.piana.dev.common.vertx.http.tmpl;

import ir.piana.dev.common.handler.*;
import ir.piana.dev.jsonparser.json.JsonTargetBuilder;

@Handler
public class NotFoundHandler extends AuthenticableRequestHandler {
    protected NotFoundHandler(ContextLoggerProvider contextLoggerProvider) {
        super(contextLoggerProvider);
    }

    @Override
    public HandlerModelAndViewResponse provideResponse(
            HandlerRequest handlerRequest, HandlerInterStateTransporter transporter)
            throws HandlerRuntimeException {
        throw thrower.generate(HandlerErrorType.NOT_FOUND
                .generateDetailedError("resource.not-found"));
    }
}
