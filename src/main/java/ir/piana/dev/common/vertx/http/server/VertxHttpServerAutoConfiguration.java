package ir.piana.dev.common.vertx.http.server;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import ir.piana.dev.common.handler.*;
import ir.piana.dev.common.http.auth.AuthPhraseConsumable;
import ir.piana.dev.common.http.server.*;
import ir.piana.dev.common.util.MapAny;
import ir.piana.dev.common.util.MapStrings;
import ir.piana.dev.common.vertx.VertxAutoConfiguration;
import ir.piana.dev.common.vertx.http.HandlerStatusNatureToHttpStatusConverter;
import ir.piana.dev.common.vertx.http.server.auth.VertxAuthPhraseProvider;
import ir.piana.dev.common.vertx.http.tmpl.VertxThymeleafTemplateEngine;
import ir.piana.dev.jsonparser.json.JsonParser;
import ir.piana.dev.jsonparser.json.JsonTarget;
import ir.piana.dev.jsonparser.json.JsonTargetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;

import java.net.HttpURLConnection;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@Import(VertxAutoConfiguration.class)
public class VertxHttpServerAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    @Profile("vertx-http-server")
    Map<String, AuthPhraseConsumable> authPhraseConsumableMap(
            AnnotationConfigApplicationContext applicationContext,
            List<VertxAuthPhraseProvider> authPhraseProviders) {
        Map<String, AuthPhraseConsumable> authPhraseConsumableMap = new LinkedHashMap<>();
        if (authPhraseProviders == null || authPhraseProviders.isEmpty())
            return authPhraseConsumableMap;
        authPhraseProviders.forEach(provider -> {
            if (provider.items() == null || !provider.items().isEmpty()) {
                provider.items().forEach(item -> {
                    try {
                        Class s = Class.forName(item.getProviderClass());
                        MapAny mapAny = MapAny.toConsume(item.getConfigs());
                        AuthPhraseConsumable authPhraseConsumable = (AuthPhraseConsumable) s.getDeclaredConstructor(
                                MapAny.class).newInstance(mapAny);
                        applicationContext.registerBean(item.getName(), s,
                                () -> authPhraseConsumable);
                    } catch (Exception e) {
                        logger.error("error on AuthPhraseConsumable instantiate: " + item.getProviderClass());
                    }
                });
            }
        });
        return authPhraseConsumableMap;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, Class> vertxHandlerClassMap(List<VertxWebRouterProvider> routerProviders) throws ClassNotFoundException {
        Map<String, Class> map = new LinkedHashMap<>();

        if (routerProviders == null)
            return map;
        for (WebRouterProvider provider : routerProviders) {
            if (provider.webRouters() == null)
                continue;
            for (HttpRouterItem router : provider.webRouters()) {
                for (HttpRouteItem item : router.getRoutes()) {
                    if (item.getResponse() == null && !map.containsKey(item.getHandlerClass())) {
                        map.put(item.getHandlerClass(), Class.forName(item.getHandlerClass()));
                    }
                }
            }
        }
        return map;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, Class> vertxDtoClassMap(List<VertxWebRouterProvider> routerProviders) throws ClassNotFoundException {
        Map<String, Class> map = new LinkedHashMap<>();

        if (routerProviders == null)
            return map;
        for (WebRouterProvider provider : routerProviders) {
            if (provider.webRouters() == null)
                continue;
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
    Map<String, Router> router(Vertx vertx,
                               Map<String, AuthPhraseConsumable> authPhraseConsumableMap,
                               List<VertxWebRouterProvider> routeProviders,
                               Map<String, VertxThymeleafTemplateEngine> vertxThymeleafTemplateEngineMap,
                               JsonParser jsonParser,
                               HandlerRequestBuilder handlerRequestBuilder,
                               HandlerManager handlerManager,
                               Map<String, Class> vertxHandlerClassMap,
                               Map<String, Class> vertxDtoClassMap,
                               MessageSource messageSource) {
        Map<String, Router> routerMap = new LinkedHashMap<>();
        if (routeProviders == null)
            return routerMap;
        for (WebRouterProvider provider : routeProviders) {
            if (provider.webRouters() == null)
                continue;
            for (HttpRouterItem httpRouterItem : provider.webRouters()) {
                if (!routerMap.containsKey(httpRouterItem.getServerName()) && httpRouterItem.getRoutes().size() > 0) {
                    routerMap.put(httpRouterItem.getServerName(), Router.router(vertx));
                }
                Router vertxRouter = routerMap.get(httpRouterItem.getServerName());
                for (HttpRouteItem httpRouteItem : httpRouterItem.getRoutes()) {
                    final AuthPhraseConsumable<HttpServerRequest> authPhraseConsumable;
                    if (httpRouterItem.getAuthPhraseProviderName() != null &&
                            !httpRouterItem.getAuthPhraseProviderName().isEmpty()) {
                        authPhraseConsumable = authPhraseConsumableMap.get(
                                httpRouterItem.getAuthPhraseProviderName());
                        if (authPhraseConsumable == null) {
                            throw new RuntimeException("AuthPhraseProvider not exist : " +
                                    httpRouterItem.getAuthPhraseProviderName());
                        }
                    } else {
                        authPhraseConsumable = null;
                    }
                    HttpMethod httpMethod = HttpMethod.valueOf(httpRouteItem.getMethod().trim().toUpperCase());

                    if (httpRouteItem.getResponse() != null) {
                        vertxRouter.route(httpMethod, httpRouteItem.getPath().startsWith("/") ? httpRouteItem.getPath() : "/" + httpRouteItem.getPath())
                                .handler(routingContext -> {
                                    routingContext.response().setStatusCode(200).end(httpRouteItem.getResponse());
                                });
                    } else {
                        vertxRouter.route(httpMethod, httpRouteItem.getPath().startsWith("/") ? httpRouteItem.getPath() : "/" + httpRouteItem.getPath())
                                .handler(routingContext -> {
                                    try {
                                        final String authPhrase;
                                        if (authPhraseConsumable != null)
                                            authPhrase = authPhraseConsumable.consume(routingContext.request());
                                        else
                                            authPhrase = "";
                                        if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
                                            routingContext.request().bodyHandler(bodyBuffer -> {
                                                try {
                                                    if (httpRouteItem.getDtoType() != null && bodyBuffer.length() == 0) {
                                                        throw new RuntimeException("request body required");
                                                    }
                                                    /**
                                                     * ToDo: body must be Object not Array
                                                     */
                                                    if (httpRouteItem.getConsumeType() != null && httpRouteItem.getConsumeType().equalsIgnoreCase(
                                                            "application/x-www-form-urlencoded")) {
                                                        Map map = Stream.of(bodyBuffer.toString().split("&"))
                                                                .map(p -> p.split("="))
                                                                .map(p -> new AbstractMap.SimpleEntry(p[0], p[1]))
                                                                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                                                        bodyBuffer = JsonObject.mapFrom(map).toBuffer();
                                                    }
                                                    handle(httpRouterItem, httpRouteItem,
                                                            vertxThymeleafTemplateEngineMap,
                                                            handlerManager, vertxHandlerClassMap,
                                                            routingContext,
                                                            handlerRequestBuilder.fromBuffer(bodyBuffer,
                                                                    vertxDtoClassMap.get(httpRouteItem.getDtoType()),
                                                                    MapStrings.toConsume()
                                                                            .putAll(routingContext.request().params())
                                                                            .build(), authPhrase),
                                                            messageSource);
                                                } catch (Exception exception) {
                                                    logger.error(exception.getMessage());
                                                    error(routingContext.response(), messageSource, exception);
                                                }
                                            });
                                        } else {
                                            handle(httpRouterItem, httpRouteItem,
                                                    vertxThymeleafTemplateEngineMap,
                                                    handlerManager, vertxHandlerClassMap,
                                                    routingContext,
                                                    handlerRequestBuilder.withoutBody()
                                                            .setAdditionalParam(
                                                                    (Consumer<MapStrings.Appender>) mapStrings -> {
                                                                        mapStrings.putAll(routingContext.queryParams());
                                                                        mapStrings.putAllOneValue(routingContext.pathParams());
                                                                    })
                                                            .setAuthPhrase(authPhrase)
                                                            .build(),
                                                    messageSource);
                                        }
                                    } catch (Throwable exception) {
                                        logger.error(exception.getMessage());
                                        error(routingContext.response(), messageSource, exception);
                                    }
                                });
                    }
                }
            }
        }

        return routerMap;
    }

    private void handle(
            HttpRouterItem routeritem,
            HttpRouteItem routeItem,
            Map<String, VertxThymeleafTemplateEngine> vertxThymeleafTemplateEngineMap,
            HandlerManager handlerManager,
            Map<String, Class> handlerClassMap,
            RoutingContext routingContext,
            HandlerRequest handlerRequest,
            MessageSource messageSource) {
        try {
            DeferredResult<HandlerResponse> deferredResult = handlerManager.execute(
                    handlerClassMap.get(routeItem.getHandlerClass()), handlerRequest);

            deferredResult.setResultHandler(
                    handlerResponse -> ok(routingContext.response(), handlerResponse,
                            vertxThymeleafTemplateEngineMap.get(routeritem.getTemplateEngineName()),
                            routeItem));

            deferredResult.onError(throwable -> {
                error(routingContext.response(), messageSource, throwable);
            });
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            error(routingContext.response(), messageSource, exception);
        }
    }

    private void ok(HttpServerResponse response, Object handlerResponse,
                    VertxThymeleafTemplateEngine templateEngine,
                    HttpRouteItem routeItem) {
        if (handlerResponse instanceof HandlerResponse<?>) {
            generateResponse(routeItem, response, (HandlerResponse<?>) handlerResponse);
        } else if (handlerResponse instanceof HandlerModelAndViewResponse) {
            JsonObject jsonObject;
            if (((HandlerModelAndViewResponse) handlerResponse).getModel() == null)
                jsonObject = new JsonObject();
            else
                jsonObject = ((HandlerModelAndViewResponse) handlerResponse).getModel().getJsonObject();
            templateEngine.getTemplateEngine().render(jsonObject,
                            templateEngine.getTemplateEngineItem().getDir() + "/" +
                                    ((HandlerModelAndViewResponse) handlerResponse).getView() + "." +
                                    templateEngine.getTemplateEngineItem().getPostfix())
                    .toCompletionStage().whenComplete((buffer, throwable) -> {
                        if (((HandlerModelAndViewResponse) handlerResponse).getAuthPhrase() != null)
                            response.addCookie(Cookie.cookie("session_id", ((HandlerModelAndViewResponse) handlerResponse).getAuthPhrase()));
                        if (throwable == null) {
                            response.setStatusCode(HttpURLConnection.HTTP_OK)
                                    .putHeader("content-type", Optional.ofNullable(routeItem.getProduceType()).orElse("text/html; charset=utf-8"))
                                    .end(buffer);
                        } else {
                            logger.error(throwable.getMessage());
                            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                    .putHeader("content-type", Optional.ofNullable(routeItem.getProduceType()).orElse("text/html; charset=utf-8"))
                                    .end("Internal Error Occurred!");
                        }
                    });
        } else {
            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .putHeader("content-type", Optional.ofNullable(routeItem.getProduceType()).orElse("application/json"))
                    .end(/*ToDo In future it should be return json*/"not response");
        }
    }

    private void error(HttpServerResponse response, MessageSource messageSource, Throwable throwable) {
        HandlerDetailedError.ThrowableError throwableError = null;
        int httpStatus = 500;
        if (throwable instanceof HandlerRuntimeException) {
            HandlerDetailedError detailedError = ((HandlerRuntimeException) throwable).getDetailedError();
            if (((HandlerRuntimeException) throwable).isResponded()) {
                logger.error("Already sent response!");
                return;
            }
            try {
                throwableError = detailedError.toThrowableError(messageSource);
                httpStatus = HandlerStatusNatureToHttpStatusConverter.toHttpStatus(
                        detailedError.getErrorType().getHandlerStatusNature());
            } catch (Exception e) {
                throwableError = HandlerErrorType.UNKNOWN.generateDetailedError(
                                "error occurred on translate message!")
                        .toThrowableError(messageSource);
            }
        } else {
            throwableError = HandlerErrorType.UNKNOWN.generateDetailedError("unknown error!")
                    .toThrowableError(messageSource);
        }

        response.setStatusCode(httpStatus)
                .end(JsonObject.mapFrom(throwableError).toBuffer());
    }

    private void generateResponse(
            HttpRouteItem routeItem,
            HttpServerResponse response,
            HandlerResponse handlerResponse) {
        response.putHeader("content-type",
                Optional.ofNullable(routeItem.getProduceType()).orElse("application/json"));
        if (routeItem.getProduceCooke() != null && !routeItem.getProduceCooke().isEmpty()) {
            routeItem.getProduceCooke().forEach(e -> {
                response.addCookie(Cookie.cookie(e.getName(), cookieValue(e.getValue(), handlerResponse)));
            });
        }
        if (routeItem.getProduceModel() != null && !routeItem.getProduceModel().isEmpty()) {
            JsonTargetBuilder jsonTargetBuilder = JsonTargetBuilder.asObject();
            routeItem.getProduceModel().forEach(e -> {
                jsonTargetBuilder.add(
                        e.getName(), responseValue(e.getValue(), handlerResponse));
            });
            response.setStatusCode(HttpURLConnection.HTTP_OK)
                    .end(jsonTargetBuilder.build().getJsonObject().toBuffer());
        } else {
            response.setStatusCode(HttpURLConnection.HTTP_OK)
                    .end(handlerResponse.getBuffer());
        }
    }

    private String cookieValue(String valPhrase, HandlerResponse handlerResponse) {
        if (valPhrase.startsWith("$")) {
            if (valPhrase.equalsIgnoreCase("$auth-phrase"))
                return handlerResponse.getAuthPhrase();
            else if (valPhrase.startsWith("$response(")) {
                valPhrase = valPhrase.substring(9, valPhrase.length() - 1);
                handlerResponse.getJsonTarget().asString(valPhrase);
            } else {
                return null;
            }
        }
        return valPhrase;
    }

    private String responseValue(String valPhrase, HandlerResponse handlerResponse) {
        if (valPhrase.startsWith("$")) {
            if (valPhrase.equalsIgnoreCase("$auth-phrase"))
                return handlerResponse.getAuthPhrase();
            else if (valPhrase.startsWith("$response(")) {
                valPhrase = valPhrase.substring(9, valPhrase.length() - 1);
                handlerResponse.getJsonTarget().asObject(valPhrase);
            } else {
                return null;
            }
        }
        return valPhrase;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, HttpServer> httpServers(
            Vertx vertx,
            Map<String, Router> routerMap,
            List<VertxWebServerProvider> serverProviders) {
        Map<String, HttpServer> httpServerMap = new LinkedHashMap<>();
        Map<String, HttpServerItem> serverConfMap = new LinkedHashMap<>();
        if (serverProviders == null)
            return httpServerMap;
        for (WebServerProvider provider : serverProviders) {
            if (provider.webServers() == null)
                continue;
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
                if (server.getSpecificConfigs() != null) {
                    MapAny config = MapAny.toConsume(server.getSpecificConfigs());
                    String staticResourceDir = config.getValue("static-resource-dir");
                    String staticResourceBasePath = config.getValue("static-resource-base-path");
                    if (staticResourceDir != null && !staticResourceDir.isEmpty()) {
                        vertxRouter.route("/" + staticResourceBasePath + "/*")
                                .handler(StaticHandler.create(staticResourceDir));
                    }
                }

                if (vertxRouter != null) {
                    var cause = httpServer.requestHandler(vertxRouter).listen().cause();
                    if (cause != null)
                        throw new RuntimeException(cause.getMessage());
                }
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
