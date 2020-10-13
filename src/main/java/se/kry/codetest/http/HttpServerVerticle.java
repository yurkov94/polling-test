package se.kry.codetest.http;

import com.mchange.v2.lang.StringUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.cache.ServicesCache;
import se.kry.codetest.service.UrlService;

import java.sql.Timestamp;
import java.util.stream.Collectors;

import static se.kry.codetest.datasource.DBConnectorVerticle.DBCONNECTOR_QUEUE;

public class HttpServerVerticle extends AbstractVerticle {

    private UrlService urlService;
    private ServicesCache servicesCache;

    private static final String NOT_FOUND_ERROR_MESSAGE = "There is no service with name %s";
    private static final String SERVICE_ALREADY_EXISTS_ERROR_MESSAGE = "Service with name %s already exists";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_ERROR = "Error";

    public HttpServerVerticle(ServicesCache servicesCache) {
        this.servicesCache = servicesCache;
    }

    @Override
    public void start(Future<Void> future) {

        urlService = UrlService.createProxy(vertx, DBCONNECTOR_QUEUE);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        setRoutes(router);
        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(8080, result -> {
                if (result.succeeded()) {
                    System.out.println("KRY code test service started");
                    future.complete();
                } else {
                    future.fail(result.cause());
                }
            });
    }
    private void setRoutes(Router router){
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(this::listServiceHandler);
        router.post("/service").handler(this::newServiceHandler);
        router.delete("/service").handler(this::deleteServiceHandler);
    }

    private void listServiceHandler(RoutingContext req) {
            JsonObject response = new JsonObject();
            response.put("services",
                servicesCache.getCachedServices().stream().map(element -> {
                    JsonObject service = new JsonObject();
                    service.put("name", element.getString(0));
                    service.put("host", element.getString(1));
                    service.put("port", element.getInteger(2));
                    service.put("created", new Timestamp(element.getLong(3)).toString());
                    service.put("status", element.getString(4));
                    return service;
                }).collect(Collectors.toList()));
            req.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encodePrettily());
    }

    private void newServiceHandler(RoutingContext req) {
        JsonObject jsonBody = req.getBodyAsJson();
        Future<JsonArray> newServiceFuture = Future.future();
        String url = jsonBody.getString("url");
        String host;
        int port;
        if (url.startsWith("http://")) {
            host = url.substring(7);
            port = 80;
        } else {
            req.response()
                    .putHeader("content-type", "text/plain")
                    .setStatusCode(400)
                    .end(STATUS_ERROR);
            return;
        }
        String serviceName = jsonBody.getString("name");
        if (servicesCache.contains(serviceName)) {
            req.response()
                    .setStatusCode(400)
                    .setStatusMessage(String.format(SERVICE_ALREADY_EXISTS_ERROR_MESSAGE, serviceName))
                    .end(STATUS_ERROR);
            return;
        }

        urlService.addService(jsonBody.getString("name"), host, port, newServiceFuture);
        newServiceFuture.compose(r -> {
            servicesCache.putService(r.getString(0), r);
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end(STATUS_OK);
            return Future.succeededFuture();
        });
    }

    private void deleteServiceHandler(RoutingContext req) {
        JsonObject jsonBody = req.getBodyAsJson();
        String name = jsonBody.getString("name");
        if (StringUtils.nonEmptyString(name) && servicesCache.contains(name)) {
            Future<Void> deleteServiceFuture = Future.future();
            urlService.deleteService(name, deleteServiceFuture);
            deleteServiceFuture.compose(r -> {
                servicesCache.removeService(name);
                req.response()
                        .setStatusCode(200)
                        .end(STATUS_OK);
                return Future.succeededFuture();
            });
        } else {
            req.response()
                    .setStatusCode(400)
                    .setStatusMessage(String.format(NOT_FOUND_ERROR_MESSAGE, name))
                    .end(STATUS_ERROR);
        }
    }
}
