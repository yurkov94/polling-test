package se.kry.codetest.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.Date;
import java.util.List;

public class UrlServiceImpl implements UrlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlServiceImpl.class);
    private final SQLClient dbClient;

    private static final String SQL_INIT_TABLES = "CREATE TABLE IF NOT EXISTS service (id INTEGER NOT NULL PRIMARY KEY, host VARCHAR(128) NOT NULL, port INTEGER NOT NULL, name VARCHAR(128) NOT NULL UNIQUE, created INTEGER NOT NULL, status VARCHAR(5) NOT NULL)";
    private static final String SQL_SELECT_SERVICES = "SELECT name, host, port, created, status FROM service";
    private static final String SQL_INSERT_NEW_SERVICE = "INSERT INTO service (name, host, port, created, status) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_DELETE_SERVICE = "DELETE FROM service WHERE name = ?";
    private static final String SQL_UPDATE_SERVICE_STATUS = "UPDATE service SET status = ? WHERE name = ?";

    public UrlServiceImpl(SQLClient dbClient, Handler<AsyncResult<UrlService>> readyHandler) {
        this.dbClient = dbClient;

        dbClient.getConnection(ar -> {
            if (ar.failed()) {
                LOGGER.error("Could not open a database connection", ar.cause());
                readyHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.execute(SQL_INIT_TABLES, create -> {
                    connection.close();
                    if (create.failed()) {
                        LOGGER.error("Database preparation error", create.cause());
                        readyHandler.handle(Future.failedFuture(create.cause()));
                    } else {
                        readyHandler.handle(Future.succeededFuture(this));
                    }
                });
            }
        });
    }

    @Override
    public UrlService fetchAllServices(Handler<AsyncResult<List<JsonArray>>> resultHandler) {
        dbClient.query(SQL_SELECT_SERVICES, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().getResults()));
            } else {
                LOGGER.error("Database query error", res.cause());
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
        return this;
    }

    @Override
    public UrlService addService(String name, String host, Integer port, Handler<AsyncResult<JsonArray>> resultHandler) {
        JsonArray data = new JsonArray()
                .add(name)
                .add(host)
                .add(port)
                .add(new Date().getTime())
                .add(ServiceStatus.DEAD);
        dbClient.updateWithParams(SQL_INSERT_NEW_SERVICE, data, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(data));
            } else {
                LOGGER.error("Database query error", res.cause());
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
        return this;
    }

    @Override
    public UrlService deleteService(String name, Handler<AsyncResult<Void>> resultHandler) {
        JsonArray data = new JsonArray().add(name);
        execQuery(resultHandler, data, SQL_DELETE_SERVICE);
        return this;
    }

    @Override
    public UrlService updateStatus(String name, ServiceStatus status, Handler<AsyncResult<Void>> resultHandler) {
        JsonArray data = new JsonArray().add(status).add(name);
        execQuery(resultHandler, data, SQL_UPDATE_SERVICE_STATUS);
        return this;
    }

    private void execQuery(Handler<AsyncResult<Void>> resultHandler, JsonArray data, String sqlQuery) {
        dbClient.updateWithParams(sqlQuery, data, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                LOGGER.error("Database query error", res.cause());
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }
}
