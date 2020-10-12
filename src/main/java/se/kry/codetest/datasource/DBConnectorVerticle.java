package se.kry.codetest.datasource;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import se.kry.codetest.service.UrlService;
import io.vertx.serviceproxy.ServiceBinder;

public class DBConnectorVerticle extends AbstractVerticle {

  public static final String DBCONNECTOR_QUEUE = "dbconnector.queue";

  private static final String DB_PATH = "jdbc:sqlite:poller.db";
  private static final String DB_DRIVER_CLASS = "org.sqlite.JDBC";
  private static final int DB_MAX_POOL_SIZE = 30;

  @Override
  public void start(Future<Void> startFuture) {
    JsonObject config = new JsonObject()
            .put("url", DB_PATH)
            .put("driver_class", DB_DRIVER_CLASS)
            .put("max_pool_size", DB_MAX_POOL_SIZE);

    SQLClient dbClient = JDBCClient.createShared(vertx, config);
    UrlService.create(dbClient, ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder
            .setAddress(DBCONNECTOR_QUEUE)
            .register(UrlService.class, ready.result());
        startFuture.complete();
      } else {
        startFuture.fail(ready.cause());
      }
    });
  }
}
