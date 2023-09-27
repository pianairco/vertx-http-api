package ir.piana.dev.common.vertx.http.server;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import ir.piana.dev.common.handler.*;
import ir.piana.dev.common.http.auth.AuthPhraseConsumable;
import ir.piana.dev.common.http.server.*;
import ir.piana.dev.common.util.MapAny;
import ir.piana.dev.common.util.MapStrings;
import ir.piana.dev.common.util.Validation;
import ir.piana.dev.common.vertx.VertxAutoConfiguration;
import ir.piana.dev.common.vertx.http.HandlerStatusNatureToHttpStatusConverter;
import ir.piana.dev.common.vertx.http.server.auth.VertxAuthPhraseProvider;
import ir.piana.dev.common.vertx.http.tmpl.VertxThymeleafTemplateEngine;
import ir.piana.dev.jsonparser.json.JsonTargetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.thymeleaf.exceptions.TemplateProcessingException;

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

    @Bean("vertxRouterNameToHttpRouterItemMap")
    @Profile("vertx-http-server")
    Map<String, HttpRouterItem> vertxRouterNameToHttpRouterItemMap(
            List<VertxWebRouterProvider> routeProviders) {
        Map<String, HttpRouterItem> httpRouterItemMap = new LinkedHashMap<>();
        for (WebRouterProvider provider : routeProviders) {
            if (provider.webRouters() == null)
                continue;
            for (HttpRouterItem httpRouterItem : provider.webRouters()) {
                if (httpRouterItemMap.containsKey(httpRouterItem.getName())) {
                    logger.error("routerName is repeated : " + httpRouterItem.getName());
                } else if (!httpRouterItem.getRoutes().isEmpty()) {
                    httpRouterItemMap.put(httpRouterItem.getName(), httpRouterItem);
                }
            }
        }
        return httpRouterItemMap;
    }

    @Bean("vertxServerNameToHttpServerItemMap")
    @Profile("vertx-http-server")
    Map<String, HttpServerItem> vertxServerNameToHttpServerItemMap(
            List<VertxWebServerProvider> serverProviders) {
        Map<String, HttpServerItem> httpServersItemMap = new LinkedHashMap<>();
        List<String> serverHostAndPortList = new ArrayList<>();
        List<String> serverUrls = new ArrayList<>();
        if (serverProviders == null)
            return httpServersItemMap;
        for (WebServerProvider provider : serverProviders) {
            if (provider.webServers() == null)
                continue;
            for (HttpServerItem httpServerItem : provider.webServers()) {
                if (httpServersItemMap.containsKey(httpServerItem.getName())) {
                    logger.error("server duplicate name : " + httpServerItem.getName());
                } else if (serverHostAndPortList.contains(httpServerItem.getHost() + ":" + httpServerItem.getPort())) {
                    logger.error("server duplicate host or port : " + httpServerItem.getHost() + ":" + httpServerItem.getPort());
                } else if (serverUrls.contains(
                        (httpServerItem.isSecure() ? "https" : "http") + "://" + httpServerItem.getHost() + ":" + httpServerItem.getPort())) {
                    logger.error("server duplicate protochol and host or port : " +
                            (httpServerItem.isSecure() ? "https" : "http") + "://" +
                            httpServerItem.getHost() + ":" + httpServerItem.getPort());
                }
                httpServersItemMap.put(httpServerItem.getName(), httpServerItem);
                serverHostAndPortList.add(httpServerItem.getHost() + ":" + httpServerItem.getPort());
                serverUrls.add((httpServerItem.isSecure() ? "https" : "http") + "://" +
                        httpServerItem.getHost() + ":" + httpServerItem.getPort());
            }
        }
        return httpServersItemMap;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, VertxHttpErrorHandleable> errorHandleableBeanMap(
            List<VertxHttpErrorHandleable> errorHandleableList) {
        Map<String, VertxHttpErrorHandleable> errorHandleableBeanMap = new LinkedHashMap<>();
        errorHandleableList.forEach(e ->
                errorHandleableBeanMap.put(e.getClass().getName(), e)
        );
        return errorHandleableBeanMap;
    }

    @Bean
    @Profile("vertx-http-server")
    Map<String, Map<Integer, VertxHttpErrorHandleable>> serverMapToErrorHandleableMap(
            @Qualifier("errorHandleableBeanMap") Map<String, VertxHttpErrorHandleable> errorHandleableBeanMap,
            Map<String, HttpServerItem> vertxServerNameToHttpServerItemMap) {
        Map<String, Map<Integer, VertxHttpErrorHandleable>> serverMapToErrorHandleableMap = new LinkedHashMap<>();
        vertxServerNameToHttpServerItemMap.forEach((serverName, httpServerItem) -> {
            Map<Integer, VertxHttpErrorHandleable> errorHandleableMap = new LinkedHashMap<>();
            if (httpServerItem.getSpecificConfigs().containsKey("error-handlers")) {
                Map<String, String> errorHandlerConfig = (Map<String, String>) httpServerItem
                        .getSpecificConfigs().get("error-handlers");
                Map<Integer, VertxHttpErrorHandleable> errorMap =
                        errorHandlerConfig.entrySet().stream()
                                .filter(e -> Validation.isValidHttpStatusCode(e.getKey()) &&
                                        errorHandleableBeanMap.containsKey(e.getValue()))
                                .map(e -> new AbstractMap.SimpleEntry<>(
                                        Integer.valueOf(e.getKey()),
                                        errorHandleableBeanMap.get(e.getValue())))
                                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                serverMapToErrorHandleableMap.put(serverName, errorMap);
            } else {
                serverMapToErrorHandleableMap.put(serverName, new LinkedHashMap<>());
            }
        });
        return serverMapToErrorHandleableMap;
    }

    @Bean("vertxServerNameToHttpRouteItemMap")
    @Profile("vertx-http-server")
    Map<String, List<HttpRouteItem>> vertxServerNameToHttpRouteItemMap(
            @Qualifier("vertxServerNameToHttpServerItemMap")
            Map<String, HttpServerItem> httpServersItemMap,
            @Qualifier("vertxRouterNameToHttpRouterItemMap")
            Map<String, HttpRouterItem> routerNameToHttpRouterItemMap,
            @Qualifier("vertxHandlerClassMap") Map<String, Class> vertxHandlerClassMap,
            RuntimeHandlerBuilder runtimeHandlerBuilder,
            HandlerRuntimeExceptionThrower thrower) {
        Map<String, List<HttpRouteItem>> serverNameToHttpRouteItemMap = new LinkedHashMap<>();

        httpServersItemMap.forEach((serverName, httpServerItem) -> {
            final List<HttpRouteItem> httpRouteItems = new ArrayList<>();
            for (String routerName : httpServerItem.getRouters()) {
                routerNameToHttpRouterItemMap.get(routerName).getRoutes().forEach(httpRouteItem -> {
                    if (httpRouteItems.stream().noneMatch(existingRoute ->
                            (existingRoute.getMethod().trim() + " " + existingRoute.getPath()).equals(
                                    httpRouteItem.getMethod().trim() + " " + httpRouteItem.getPath()
                            ))) {
                        httpRouteItems.add(httpRouteItem);
                    } else {
                        logger.error("duplicate route (method and path) for server '" + serverName + "' : " +
                                httpRouteItem.getMethod().trim() + " " + httpRouteItem.getPath());
                    }
                });
            }
            /*if (httpRouteItems.stream().noneMatch(routeItem ->
                    "*".equals(routeItem.getPath()) && routeItem.getMethod() == null)) {
                RequestHandler requestHandler = runtimeHandlerBuilder.baseRequestHandler((handlerRequest, transporter) -> {
                    throw thrower.generate(HandlerErrorType.NOT_FOUND.generateDetailedError("resource.not-found"));
                });
                vertxHandlerClassMap.put(requestHandler.getClass().getName(), requestHandler.getClass());
                httpRouteItems.add(HttpRouteItem.builder()
                        .path("*")
                        .produceType("text/html")
                        .handlerClass(requestHandler.getClass().getName())
                        .build());
            }*/
            serverNameToHttpRouteItemMap.put(serverName, httpRouteItems);
        });
        return serverNameToHttpRouteItemMap;
    }

    @Bean("vertxHttpServerMap")
    @Profile("vertx-http-server")
    Map<String, HttpServer> vertxHttpServerMap(
            Vertx vertx,
            @Qualifier("vertxServerNameToHttpRouteItemMap")
            Map<String, List<HttpRouteItem>> serverNameToHttpRouteItemMap,
            @Qualifier("vertxServerNameToHttpServerItemMap")
            Map<String, HttpServerItem> serverNameToHttpServerItemMap,
            Map<String, AuthPhraseConsumable> authPhraseConsumableMap,
            Map<String, VertxThymeleafTemplateEngine> vertxThymeleafTemplateEngineMap,
            HandlerRequestBuilder<?> handlerRequestBuilder,
            HandlerManager handlerManager,
            Map<String, Class> vertxHandlerClassMap,
            Map<String, Class> vertxDtoClassMap,
            MessageSource messageSource,
            HandlerRuntimeExceptionThrower thrower,
            ApplicationContext applicationContext,
            Map<String, Map<Integer, VertxHttpErrorHandleable>> serverMapToErrorHandleableMap,
            HandlerResponseBuilder handlerResponseBuilder
    ) {
        Map<String, HttpServer> httpServerMap = new LinkedHashMap<>();
        serverNameToHttpServerItemMap.forEach((serverName, httpServerItem) -> {
            HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions()
                    .setHost(httpServerItem.getHost())
                    .setPort(httpServerItem.getPort())
                    .setIdleTimeout(httpServerItem.getIdleTimeout())
                    .setReusePort(Boolean.TRUE)
                    .setTcpQuickAck(Boolean.TRUE)
                    .setTcpCork(Boolean.TRUE)
                    .setTcpFastOpen(Boolean.TRUE));
            httpServerMap.put(httpServerItem.getName(), httpServer);

            Map<Integer, VertxHttpErrorHandleable> errorHandleableMap = null;
            if (serverMapToErrorHandleableMap.containsKey(httpServerItem.getName())) {
                errorHandleableMap = serverMapToErrorHandleableMap.get(httpServerItem.getName());
            }


            Router vertxRouter = createVertxRouter(
                    vertx,
                    httpServerItem,
                    serverNameToHttpRouteItemMap.get(serverName),
                    (AuthPhraseConsumable<HttpServerRequest, HttpServerResponse>) authPhraseConsumableMap.get(
                            httpServerItem.getAuthPhraseProviderName()),
                    vertxThymeleafTemplateEngineMap.get(httpServerItem.getTemplateEngineName()),
                    handlerRequestBuilder,
                    handlerManager,
                    vertxHandlerClassMap,
                    vertxDtoClassMap,
                    messageSource, thrower,
                    errorHandleableMap,
                    handlerResponseBuilder);

            var cause = httpServer.requestHandler(vertxRouter).listen().cause();
            if (cause != null)
                throw new RuntimeException(cause.getMessage());

            logger.info("Successfully started HTTP server and listening on {}:{} with native transport {}",
                    httpServerItem.getHost(), httpServerItem.getPort(),
                    vertx.isNativeTransportEnabled() ? "enabled" : "not enabled");
        });


        return httpServerMap;
    }

    private Router createVertxRouter(
            Vertx vertx,
            HttpServerItem httpServerItem,
            List<HttpRouteItem> httpRouteItems,
            AuthPhraseConsumable<HttpServerRequest, HttpServerResponse> authPhraseConsumable,
            VertxThymeleafTemplateEngine templateEngine,
            HandlerRequestBuilder<?> handlerRequestBuilder,
            HandlerManager handlerManager,
            Map<String, Class> vertxHandlerClassMap,
            Map<String, Class> vertxDtoClassMap,
            MessageSource messageSource,
            HandlerRuntimeExceptionThrower thrower,
            Map<Integer, VertxHttpErrorHandleable> errorHandleableMap,
            HandlerResponseBuilder handlerResponseBuilder) {
        Router vertxRouter = Router.router(vertx);

        if (httpServerItem.getSpecificConfigs() != null) {
            MapAny config = MapAny.toConsume(httpServerItem.getSpecificConfigs());
            String staticResourceDir = config.getValue("static-resource-dir");
            String staticResourceBasePath = config.getValue("static-resource-base-path");
            String staticResourceCacheEnable = config.getValue("static-resource-cache-enable");
            if (staticResourceDir != null && !staticResourceDir.isEmpty()) {
                if (staticResourceBasePath != null && !staticResourceBasePath.isEmpty()) {
                    vertxRouter.route("/" + staticResourceBasePath + "/*")
                            .handler(routingContext -> {
                                if (routingContext.request().path().endsWith(".vue") || routingContext.request().path().endsWith(".cjs")) {
                                    routingContext.response().putHeader(
                                            "Content-Type", "application/javascript; charset=utf-8");
                                }
                                routingContext.next();
                            })
                            .handler(StaticHandler.create(staticResourceDir.startsWith("/") ?
                                    FileSystemAccess.ROOT : FileSystemAccess.RELATIVE,
                                    staticResourceDir)
                                    .setCachingEnabled(
                                    staticResourceCacheEnable != null &&
                                            staticResourceCacheEnable.equalsIgnoreCase("true")));
                } else {
                    vertxRouter.route("/*")
                            .handler(routingContext -> {
                                if (routingContext.request().path().endsWith(".vue") || routingContext.request().path().endsWith(".cjs")) {
                                    routingContext.response().putHeader(
                                            "Content-Type", "application/javascript; charset=utf-8");
                                }
                                routingContext.next();
                            })
                            .handler(StaticHandler.create(staticResourceDir.startsWith("/") ?
                                            FileSystemAccess.ROOT : FileSystemAccess.RELATIVE,
                                    staticResourceDir)
                                    .setCachingEnabled(
                                    staticResourceCacheEnable != null &&
                                            staticResourceCacheEnable.equalsIgnoreCase("true")));
                }
            }
        }

        for (HttpRouteItem httpRouteItem : httpRouteItems) {
            HttpMethod httpMethod = httpRouteItem.getMethod() == null ? null :
                    HttpMethod.valueOf(httpRouteItem.getMethod().trim().toUpperCase());

            final Route route;
            if (httpRouteItem.getResponse() != null) {
                if (httpMethod == null) {
                    route = vertxRouter.route(httpRouteItem.getPath().startsWith("/") ?
                                    httpRouteItem.getPath() : "/" + httpRouteItem.getPath())
                            .handler(routingContext -> {
                                generateResponse(httpRouteItem, routingContext.response(),
                                        handlerResponseBuilder.withoutBody().build());
                                /*routingContext.response().setStatusCode(
                                                Optional.ofNullable(httpRouteItem.getStatus()).orElse(200))
                                        .end(httpRouteItem.getResponse());*/
                            });
                } else {
                    route = vertxRouter.route(httpMethod, httpRouteItem.getPath().startsWith("/") ?
                                    httpRouteItem.getPath() : "/" + httpRouteItem.getPath())
                            .handler(routingContext -> {
                                generateResponse(httpRouteItem, routingContext.response(),
                                        handlerResponseBuilder.withoutBody().build());
                                /*routingContext.response().setStatusCode(
                                                Optional.ofNullable(httpRouteItem.getStatus()).orElse(200))
                                        .end(httpRouteItem.getResponse());*/
                            });
                }
            } else {
                if (httpMethod == null) {
                    route = vertxRouter.route(httpRouteItem.getPath().startsWith("/") ?
                                    httpRouteItem.getPath() : "/" + httpRouteItem.getPath())
                            .handler(routingContext -> handle0(
                                    httpMethod,
                                    routingContext,
                                    httpServerItem,
                                    httpRouteItem,
                                    authPhraseConsumable,
                                    templateEngine,
                                    handlerManager,
                                    handlerRequestBuilder,
                                    vertxHandlerClassMap,
                                    vertxDtoClassMap,
                                    messageSource));
                } else {
                    route = vertxRouter.route(httpMethod, httpRouteItem.getPath().startsWith("/") ?
                                    httpRouteItem.getPath() : "/" + httpRouteItem.getPath())
                            .handler(routingContext -> handle0(
                                    httpMethod,
                                    routingContext,
                                    httpServerItem,
                                    httpRouteItem,
                                    authPhraseConsumable,
                                    templateEngine,
                                    handlerManager,
                                    handlerRequestBuilder,
                                    vertxHandlerClassMap,
                                    vertxDtoClassMap,
                                    messageSource));
                }
            }
            route.failureHandler(routingContext -> {
                final HandlerErrorToHttpStatusCode httpStatus;
                Throwable throwable = routingContext.failure();
                if (throwable instanceof HandlerRuntimeException) {
                    httpStatus = HandlerErrorToHttpStatusCode.byHandlerErrorType(
                            ((HandlerRuntimeException) throwable).getDetailedError().getErrorType());
                } else if (throwable instanceof TemplateProcessingException &&
                        throwable.getCause() instanceof FileSystemException) {
                    httpStatus = HandlerErrorToHttpStatusCode.HTTP_NOT_FOUND;
                } else {
                    httpStatus = HandlerErrorToHttpStatusCode.HTTP_INTERNAL_ERROR;
                }

                final CommonResponse handlerResponse;
                VertxHttpErrorHandleable vertxHttpErrorHandler = errorHandleableMap.get(httpStatus.getStatusCode());
                if (vertxHttpErrorHandler != null) {
                    handlerResponse = vertxHttpErrorHandler.handle(
                            httpStatus.getStatusCode(), routingContext.request());
                } else {
                    handlerResponse = handlerResponseBuilder.withoutBody().build();
                }


                if (handlerResponse instanceof HandlerModelAndViewResponse) {
                    templateEngine.getTemplateEngine().render(((HandlerModelAndViewResponse) handlerResponse)
                                            .getModel().getJsonObject(),
                                    templateEngine.getTemplateEngineItem().getDir() + "/" +
                                            ((HandlerModelAndViewResponse) handlerResponse)
                                                    .getView() + "." +
                                            templateEngine.getTemplateEngineItem().getPostfix())
                            .toCompletionStage().whenComplete((buffer, thr) -> {
                                if (thr == null) {
                                    routingContext.response().setStatusCode(httpStatus.getStatusCode())
                                            .putHeader("content-type", "text/html; charset=utf-8")
                                            .end(buffer);
                                } else {
                                    logger.error(thr.getMessage());
                                    routingContext.response().setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                            .putHeader("content-type", "text/html; charset=utf-8")
                                            .end("Internal Error Occurred!");
                                }
                            });
                } else {
                    routingContext.response().setStatusCode(httpStatus.getStatusCode())
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(((HandlerResponse) handlerResponse).getBuffer());
                }
            });
        }

        errorHandleableMap.forEach((httpErrorCode, vertxHttpErrorHandler) -> {
            vertxRouter.errorHandler(httpErrorCode, routingContext -> {
                final CommonResponse response = vertxHttpErrorHandler
                        .handle(httpErrorCode, routingContext.request());
                if (response != null) {
                    if (response instanceof HandlerModelAndViewResponse) {
                        templateEngine.getTemplateEngine().render(((HandlerModelAndViewResponse) response)
                                                .getModel().getJsonObject(),
                                        templateEngine.getTemplateEngineItem().getDir() + "/" +
                                                ((HandlerModelAndViewResponse) response)
                                                        .getView() + "." +
                                                templateEngine.getTemplateEngineItem().getPostfix())
                                .toCompletionStage().whenComplete((buffer, thr) -> {
                                    if (thr == null) {
                                        routingContext.response().setStatusCode(httpErrorCode)
                                                .putHeader("content-type", "text/html; charset=utf-8")
                                                .end(buffer);
                                    } else {
                                        logger.error(thr.getMessage());
                                        routingContext.response().setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                                .putHeader("content-type", "text/html; charset=utf-8")
                                                .end("Internal Error Occurred!");
                                    }
                                });
                    } else {
                        routingContext.response().setStatusCode(httpErrorCode)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(((HandlerResponse) response).getBuffer());
                    }
                } else {
                    routingContext.response().setStatusCode(httpErrorCode)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(JsonTargetBuilder.asObject().add("message",
                                            messageSource.getMessage("response.error", new Object[0],
                                                    "error code : " + httpErrorCode, Locale.forLanguageTag("utf-8"))).build()
                                    .getJsonObject().toBuffer());
                }
            });
        });

        return vertxRouter;
    }

    private void handle0(
            HttpMethod httpMethod,
            RoutingContext routingContext,
            HttpServerItem httpServerItem,
            HttpRouteItem httpRouteItem,
            AuthPhraseConsumable<HttpServerRequest, HttpServerResponse> authPhraseConsumable,
            VertxThymeleafTemplateEngine templateEngine,
            HandlerManager handlerManager,
            HandlerRequestBuilder handlerRequestBuilder,
            Map<String, Class> vertxHandlerClassMap,
            Map<String, Class> vertxDtoClassMap,
            MessageSource messageSource) {
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
                        handle(httpServerItem,
                                httpRouteItem,
                                authPhraseConsumable,
                                templateEngine,
                                handlerManager,
                                vertxHandlerClassMap.get(httpRouteItem.getHandlerClass()),
                                routingContext,
                                handlerRequestBuilder.fromBuffer(bodyBuffer,
                                        vertxDtoClassMap.get(httpRouteItem.getDtoType()),
                                        MapStrings.toConsume()
                                                .putAll(routingContext.request().params())
                                                .build(), authPhrase),
                                messageSource);
                    } catch (Exception exception) {
                        logger.error(exception.getMessage());
                        error(routingContext.response(), httpServerItem, httpRouteItem, messageSource, exception, templateEngine);
                    }
                });
            } else {
                handle(httpServerItem,
                        httpRouteItem,
                        authPhraseConsumable,
                        templateEngine,
                        handlerManager,
                        vertxHandlerClassMap.get(httpRouteItem.getHandlerClass()),
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
            error(routingContext.response(), httpServerItem, httpRouteItem, messageSource, exception, templateEngine);
        }
    }

    private void handle(
            HttpServerItem httpServerItem,
            HttpRouteItem httpRouteItem,
            AuthPhraseConsumable<HttpServerRequest, HttpServerResponse> authPhraseConsumable,
            VertxThymeleafTemplateEngine templateEngine,
            HandlerManager handlerManager,
            Class handlerClass,
            RoutingContext routingContext,
            HandlerRequest<?> handlerRequest,
            MessageSource messageSource) {
        try {
            DeferredResult<HandlerResponse> deferredResult = handlerManager.execute(
                    handlerClass, handlerRequest);
            deferredResult.setResultHandler(
                    handlerResponse -> ok(routingContext, handlerResponse,
                            authPhraseConsumable,
                            templateEngine,
                            httpRouteItem));

            deferredResult.onError(throwable -> {
                routingContext.fail(throwable instanceof HandlerRuntimeException ?
                        HandlerErrorToHttpStatusCode.byHandlerErrorType(
                                ((HandlerRuntimeException) throwable).getDetailedError()
                                        .getErrorType()).getStatusCode() : 500, throwable);
                /*error(routingContext.response(),
                        httpServerItem, httpRouteItem,
                        messageSource, throwable, templateEngine);*/
            });
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            error(routingContext.response(),
                    httpServerItem,
                    httpRouteItem,
                    messageSource, exception, templateEngine);
        }
    }

    private void ok(RoutingContext routingContext,
                    Object handlerResponse,
                    AuthPhraseConsumable<HttpServerRequest, HttpServerResponse> authPhraseConsumable,
                    VertxThymeleafTemplateEngine templateEngine,
                    HttpRouteItem routeItem) {
        if (handlerResponse instanceof HandlerResponse<?>) {
            generateResponse(routeItem, routingContext.response(), (HandlerResponse<?>) handlerResponse);
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
                            authPhraseConsumable.produce(routingContext.response(), ((HandlerModelAndViewResponse) handlerResponse).getAuthPhrase());
                        if (throwable == null) {
                            routingContext.response().setStatusCode(HttpURLConnection.HTTP_OK)
                                    .putHeader("content-type", Optional.ofNullable(routeItem.getProduceType()).orElse("text/html; charset=utf-8"))
                                    .end(buffer);
                        } else {
                            routingContext.fail(
                                    (throwable instanceof TemplateProcessingException &&
                                            throwable.getCause() instanceof FileSystemException) ? 404 : 500,
                                    throwable);
                            /*logger.error(throwable.getMessage());
                            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                    .putHeader("content-type", Optional.ofNullable(routeItem.getProduceType()).orElse("text/html; charset=utf-8"))
                                    .end("Internal Error Occurred!");*/
                        }
                    });
        } else {
            routingContext.response().setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .putHeader("content-type", Optional.ofNullable(routeItem.getProduceType()).orElse("application/json"))
                    .end(/*ToDo In future it should be return json*/"not response");
        }
    }

    private void error(
            HttpServerResponse response,
            HttpServerItem httpServerItem,
            HttpRouteItem httpRouteItem,
            MessageSource messageSource,
            Throwable throwable,
            VertxThymeleafTemplateEngine templateEngine) {
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

        /*if (throwableError.getType() == HandlerErrorType.PERMISSION_DENIED &&
                httpServerItem.getPermissionDeniedPage() != null &&
                httpRouteItem.getProduceType().equalsIgnoreCase("text/html")) {
            JsonObject jsonObject = JsonObject.mapFrom(throwableError);
            templateEngine.getTemplateEngine().render(jsonObject,
                            templateEngine.getTemplateEngineItem().getDir() + "/" +
                                    httpServerItem.getPermissionDeniedPage() + "." +
                                    templateEngine.getTemplateEngineItem().getPostfix())
                    .toCompletionStage().whenComplete((buffer, thr) -> {
                        if (thr == null) {
                            response.setStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                                    .putHeader("content-type", "text/html; charset=utf-8")
                                    .end(buffer);
                        } else {
                            logger.error(thr.getMessage());
                            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                    .putHeader("content-type", "text/html; charset=utf-8")
                                    .end("Internal Error Occurred!");
                        }
                    });
        } else {*/
        response.setStatusCode(httpStatus)
                .end(JsonObject.mapFrom(throwableError).toBuffer());
        /*}*/

    }

    private void generateResponse(
            HttpRouteItem routeItem,
            HttpServerResponse response,
            HandlerResponse handlerResponse) {
        response.putHeader("content-type",
                Optional.ofNullable(routeItem.getProduceType()).orElse("application/json"));
        if (routeItem.getProduceHeader() != null && !routeItem.getProduceHeader().isEmpty()) {
            routeItem.getProduceHeader().forEach(e -> response.putHeader(e.getName(), e.getValue()));
        }
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
            response.setStatusCode(Optional.ofNullable(routeItem.getStatus()).orElse(HttpURLConnection.HTTP_OK))
                    .end((routeItem.getStatus() != null && routeItem.getStatus().toString().startsWith("3")) ? Buffer.buffer("") :
                            routeItem.getResponse() != null ? Buffer.buffer(routeItem.getResponse()) :
                                    handlerResponse.getBuffer());
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
}
