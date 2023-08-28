package ir.piana.dev.common.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import ir.piana.dev.common.handler.*;
import ir.piana.dev.jsonparser.json.JsonParser;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.util.*;

@Configuration
@Import(VertxAutoConfiguration.class)
public class VertxHttpServerAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    @Profile("vertx-http-server")
    Map<String, Class> vertxHandlerClassMap(VertxRouter routerItems) throws ClassNotFoundException {
        Map<String, Class> map = new LinkedHashMap<>();
        for (VertxRouteItem item : routerItems.items) {
            if (item.response == null)
                map.put(item.handlerClass, Class.forName(item.handlerClass));
        }
        return map;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, Class> vertxDtoClassMap(VertxRouter routerItems) throws ClassNotFoundException {
        Map<String, Class> map = new LinkedHashMap<>();

        for (VertxRouteItem item : routerItems.items) {
            HttpMethod httpMethod = HttpMethod.valueOf(item.method.trim().toUpperCase());
            if (item.dtoType != null && (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) && item.response == null) {
                map.put(item.dtoType, Class.forName(item.dtoType));
            }
        }
        return map;
    }

    @Bean
    @Profile("vertx-http-server")
    Router router(Vertx vertx, VertxRouter vertxRouter,
                  JsonParser jsonParser,
                  HandlerManager handlerManager,
                  Map<String, Class> vertxHandlerClassMap,
                  Map<String, Class> vertxDtoClassMap) {
        Router router = Router.router(vertx);

        for (VertxRouteItem item : vertxRouter.items) {
            HttpMethod httpMethod = HttpMethod.valueOf(item.method.trim().toUpperCase());

            if (item.response != null) {
                router.route(httpMethod, item.path.startsWith("/") ? item.path : "/" + item.path)
                        .handler(routingContext -> {
                            routingContext.response().setStatusCode(200).end(item.response);
                        });
            } else {

                router.route(httpMethod, item.path.startsWith("/") ? item.path : "/" + item.path)
                        .handler(routingContext -> {
                            try {
                                if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
                                    routingContext.request().bodyHandler(bodyHandler -> {
                                        try {
                                            /*if (item.dtoType != null && bodyHandler.length() == 0) {
                                                throw new HandlerRuntimeException(
                                                        new BaseHandlerContext().addResultDto(new ResultDto(new DetailedError(
                                                                -1, "request body required",
                                                                DetailedError.ErrorTypes.BAD_REQUEST))));
                                            }*/
                                            /**
                                             * ToDo: body must be Object not Array
                                             */
                                            handle(item, handlerManager, vertxHandlerClassMap,
                                                    routingContext,
                                                    HandlerRequestBuilder.fromBuffer(jsonParser, bodyHandler,
                                                                    vertxDtoClassMap.get(item.dtoType))
                                                            .addAdditionalParams(routingContext.request().params())
                                                            .build());
                                        } catch (Exception exception) {
                                            logger.error(exception.getMessage());
                                            error(routingContext.response(), exception);
                                        }
                                    });
                                } else {
                                    handle(item, handlerManager, vertxHandlerClassMap,
                                            routingContext,
                                            HandlerRequestBuilder.withoutRequest()
                                                    .addAdditionalParams(routingContext.queryParams())
                                                    .build());
                                }
                            } catch (Throwable exception) {
                                logger.error(exception.getMessage());
                                error(routingContext.response(), exception);
                            }
                        });
            }
        }

        return router;
    }

    private void handle(
            VertxRouteItem item,
            HandlerManager handlerManager,
            Map<String, Class> handlerClassMap,
            RoutingContext routingContext, HandlerRequest handlerRequest) {
        try {
            DeferredResult<HandlerContext<?>> deferredResult = handlerManager.execute(
                    handlerClassMap.get(item.handlerClass), UUID.randomUUID().toString(), handlerRequest);

            deferredResult.setResultHandler(ctx -> {
                HandlerContext handlerContext = (HandlerContext) ctx;
                ResultDto resultDto = handlerContext.resultDto();
                if (resultDto.isSuccess()) {
                    ok(routingContext.response(), handlerContext, item);
                } else {
                    error(routingContext.response(), handlerContext);
                }
            });

            deferredResult.onError(throwable -> {
                error(routingContext.response(), throwable);
            });
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            error(routingContext.response(), exception);
        }
    }

    private void ok(HttpServerResponse response, HandlerContext context, VertxRouteItem item) {
        if (context.responded()) {
            logger.error("Already sent response!");
            return;
        }

        var json = JsonObject.mapFrom(context.resultDto());
        response.setStatusCode(HttpURLConnection.HTTP_OK)
                .putHeader("content-type", Optional.ofNullable(item.responseType).orElse("application/json"))
                .end(json.toBuffer());
    }

    private void error(HttpServerResponse response, HandlerContext context) {
        if (context == null)
            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR).end(JsonObject.mapFrom(
                            new DetailedError(-1, "unhandled error!", DetailedError.ErrorTypes.UNKNOWN))
                    .toBuffer());
        else {
            if (context.responded()) {
                logger.error("Already sent response!");
                return;
            }
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST).end(JsonObject.mapFrom(context.resultDto())
                    .toBuffer());
        }
    }

    private void error(HttpServerResponse response, Throwable throwable) {
        if (throwable instanceof HandlerRuntimeException) {
            HandlerContext context = ((HandlerRuntimeException) throwable).getContext();
            if (context.responded()) {
                logger.error("Already sent response!");
                return;
            }
            response.setStatusCode(400).end(
                    JsonObject.mapFrom(context.resultDto()).toBuffer());
        } else {
            response.setStatusCode(400).end(JsonObject.mapFrom(new DetailedError(
                    -1, "unhandled error!", DetailedError.ErrorTypes.UNKNOWN)).toBuffer());
        }
    }

    @Bean
    @Profile("vertx-http-server")
    HttpServer httpServer(Vertx vertx, Router router, VertxHttpServer server) {
        HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions()
                .setHost(server.host)
                .setPort(server.port)
                .setIdleTimeout(server.idleTimeout)
                .setReusePort(Boolean.TRUE)
                .setTcpQuickAck(Boolean.TRUE)
                .setTcpCork(Boolean.TRUE)
                .setTcpFastOpen(Boolean.TRUE));
        var cause = httpServer.requestHandler(router).listen().cause();
        if (cause != null)
            throw new RuntimeException(cause.getMessage());

        logger.info("Successfully started HTTP server and listening on {}:{} with native transport {}",
                server.host, server.port,
                vertx.isNativeTransportEnabled() ? "enabled" : "not enabled");

        return httpServer;
    }

    @Setter
    @Component
    @ConfigurationProperties(prefix = "ir.piana.dev.common.vertx.http-server")
    @Profile("vertx-http-server")
    static class VertxHttpServer {
        private String host;
        private int port;
        /**
         * in seconds
         */
        private int idleTimeout;
    }

    @Setter
    @Component
    @ConfigurationProperties(prefix = "ir.piana.dev.common.vertx.router")
    @Profile("vertx-http-server")
    static class VertxRouter {
        List<VertxRouteItem> items;
    }

    @Setter
    static class VertxRouteItem {
        private String method;
        private String path;
        private String handlerClass;
        private List<String> roles;
        private String dtoType;
        private String responseType;
        private String response;
    }
}
