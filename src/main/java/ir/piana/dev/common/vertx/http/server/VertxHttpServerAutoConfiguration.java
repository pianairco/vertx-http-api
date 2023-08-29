package ir.piana.dev.common.vertx.http.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import ir.piana.dev.common.handler.*;
import ir.piana.dev.common.http.server.*;
import ir.piana.dev.common.vertx.VertxAutoConfiguration;
import ir.piana.dev.jsonparser.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.net.HttpURLConnection;
import java.util.*;

@Configuration
@Import(VertxAutoConfiguration.class)
public class VertxHttpServerAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    @Profile("vertx-http-server")
    Map<String, Class> vertxHandlerClassMap(List<WebRouterProvider> routerProviders) throws ClassNotFoundException {
        Map<String, Class> map = new LinkedHashMap<>();
        for (WebRouterProvider provider : routerProviders) {
            for (HttpRouterItem router : provider.webRouters()) {
                for (HttpRouteItem item : router.getRoutes()) {
                    if (item.getResponse() == null && !map.containsKey(item.getHandlerClass()))
                        map.put(item.getHandlerClass(), Class.forName(item.getHandlerClass()));
                }
            }
        }
        return map;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, Class> vertxDtoClassMap(List<WebRouterProvider> routerProviders) throws ClassNotFoundException {
        Map<String, Class> map = new LinkedHashMap<>();

        for (WebRouterProvider provider : routerProviders) {
            for (HttpRouterItem router : provider.webRouters()) {
                for (HttpRouteItem item : router.getRoutes()) {
                    HttpMethod httpMethod = HttpMethod.valueOf(item.getMethod().trim().toUpperCase());
                    if (item.getDtoType() != null &&
                            (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) &&
                            item.getResponse() == null &&
                            !map.containsKey(item.getDtoType())) {
                        map.put(item.getDtoType(), Class.forName(item.getDtoType()));
                    }
                }
            }
        }
        return map;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, Router> router(Vertx vertx, List<WebRouterProvider> routeProviders,
                  JsonParser jsonParser,
                  HandlerManager handlerManager,
                  Map<String, Class> vertxHandlerClassMap,
                  Map<String, Class> vertxDtoClassMap) {
        Map<String, Router> routerMap = new LinkedHashMap<>();

        for (WebRouterProvider provider : routeProviders) {
            for (HttpRouterItem router : provider.webRouters()) {
                if(!routerMap.containsKey(router.getServerName()) && router.getRoutes().size() > 0) {
                    routerMap.put(router.getServerName(), Router.router(vertx));
                }
                Router vertxRouter = routerMap.get(router.getServerName());
                for (HttpRouteItem item : router.getRoutes()) {
                    HttpMethod httpMethod = HttpMethod.valueOf(item.getMethod().trim().toUpperCase());

                    if (item.getResponse() != null) {
                        vertxRouter.route(httpMethod, item.getPath().startsWith("/") ? item.getPath() : "/" + item.getPath())
                                .handler(routingContext -> {
                                    routingContext.response().setStatusCode(200).end(item.getResponse());
                                });
                    } else {
                        vertxRouter.route(httpMethod, item.getPath().startsWith("/") ? item.getPath() : "/" + item.getPath())
                                .handler(routingContext -> {
                                    try {
                                        if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
                                            routingContext.request().bodyHandler(bodyHandler -> {
                                                try {
                                                    if (item.getDtoType() != null && bodyHandler.length() == 0) {
                                                        throw new RuntimeException("request body required");
                                                    }
                                                    /**
                                                     * ToDo: body must be Object not Array
                                                     */
                                                    handle(item, handlerManager, vertxHandlerClassMap,
                                                            routingContext,
                                                            HandlerRequestBuilder.fromBuffer(jsonParser, bodyHandler,
                                                                            vertxDtoClassMap.get(item.getDtoType()))
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
            }
        }

        return routerMap;
    }

    private void handle(
            HttpRouteItem item,
            HandlerManager handlerManager,
            Map<String, Class> handlerClassMap,
            RoutingContext routingContext, HandlerRequest handlerRequest) {
        try {
            DeferredResult<HandlerContext<?>> deferredResult = handlerManager.execute(
                    handlerClassMap.get(item.getHandlerClass()), UUID.randomUUID().toString(), handlerRequest);

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

    private void ok(HttpServerResponse response, HandlerContext context, HttpRouteItem item) {
        if (context.responded()) {
            logger.error("Already sent response!");
            return;
        }

        var json = JsonObject.mapFrom(context.resultDto());
        response.setStatusCode(HttpURLConnection.HTTP_OK)
                .putHeader("content-type", Optional.ofNullable(item.getResponseType()).orElse("application/json"))
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
    Map<String, HttpServer> httpServers(
            Vertx vertx, Map<String, Router> routerMap,
            List<WebServerProvider> serverProviders) {
        Map<String, HttpServer> httpServerMap = new LinkedHashMap<>();
        Map<String, HttpServerItem> serverConfMap = new LinkedHashMap<>();
        for (WebServerProvider provider : serverProviders) {
            for (HttpServerItem server : provider.webServers()) {
                HttpServer httpServer = null;
                if (!httpServerMap.containsKey(server.getName()) && serverConfMap.entrySet().stream().noneMatch(entry ->
                        (server.getHost() + ":" + server.getPort()).equals(
                                entry.getValue().getHost() + ":" + entry.getValue().getPort()))) {
                    httpServerMap.put(server.getName(), vertx.createHttpServer(new HttpServerOptions()
                            .setHost(server.getHost())
                            .setPort(server.getPort())
                            .setIdleTimeout(server.getIdleTimeout())
                            .setReusePort(Boolean.TRUE)
                            .setTcpQuickAck(Boolean.TRUE)
                            .setTcpCork(Boolean.TRUE)
                            .setTcpFastOpen(Boolean.TRUE)));
                    httpServer = httpServerMap.get(server.getName());
                }
                Router vertxRouter = routerMap.get(server.getName());

                var cause = httpServer.requestHandler(vertxRouter).listen().cause();
                if (cause != null)
                    throw new RuntimeException(cause.getMessage());

                logger.info("Successfully started HTTP server and listening on {}:{} with native transport {}",
                        server.getHost(), server.getPort(),
                        vertx.isNativeTransportEnabled() ? "enabled" : "not enabled");
            }
        }

        return httpServerMap;
    }

    /*@Setter
    @Component
    @ConfigurationProperties(prefix = "ir.piana.dev.common.vertx.http-server")
    @Profile("vertx-http-server")
    static class VertxHttpServer {
        private String name;
        private String host;
        private int port;
        *//**
         * in seconds
         *//*
        private int idleTimeout;
    }*/

    /*@Setter
    @Component
    @ConfigurationProperties(prefix = "ir.piana.dev.common.vertx.router")
    @Profile("vertx-http-server")
    static class VertxRouter {
        List<HttpRouteItem> items;
    }*/
}
