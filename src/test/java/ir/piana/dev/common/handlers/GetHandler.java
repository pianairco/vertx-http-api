package ir.piana.dev.common.handlers;

import ir.piana.dev.common.handler.ChainStep;
import ir.piana.dev.common.handler.Handler;
import ir.piana.dev.common.handler.HandlerContext;
import ir.piana.dev.common.handler.ResultDto;

@Handler
public class GetHandler {
    @ChainStep(order = 1)
    public void step1(HandlerContext<PostHandler.Request> context) {
        context.addResultDto(new ResultDto(new PostHandler.Response(1, "hello get!")));
    }
}
