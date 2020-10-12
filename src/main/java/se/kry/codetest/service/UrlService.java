package se.kry.codetest.service;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLClient;

import java.util.List;

@ProxyGen
public interface UrlService {

    @Fluent
    UrlService fetchAllServices(Handler<AsyncResult<List<JsonArray>>> resultHandler);

    @Fluent
    UrlService addService(String name, String url, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    UrlService deleteService(String name, Handler<AsyncResult<Void>> resultHandler);

    @GenIgnore
    static UrlService create(SQLClient dbClient, Handler<AsyncResult<UrlService>> readyHandler) {
        return new UrlServiceImpl(dbClient, readyHandler);
    }

    @GenIgnore
    static UrlService createProxy(Vertx vertx, String address) {
        return new UrlServiceVertxEBProxy(vertx, address);
    }
}
