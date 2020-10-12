package se.kry.codetest;

import com.mchange.v2.lang.StringUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, String> services = new HashMap<>();
  //TODO use this
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  private static final String NOT_FOUND_ERROR_MESSAGE = "There is no service with url %s";
  private static final String STATUS_OK = "OK";
  private static final String STATUS_ERROR = "Error";


  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    services.put("https://www.kry.se", "UNKNOWN");
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
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
    List<JsonObject> jsonServices = services
            .entrySet()
            .stream()
            .map(service ->
                    new JsonObject()
                            .put("name", service.getKey())
                            .put("status", service.getValue()))
            .collect(Collectors.toList());
    req.response()
            .putHeader("content-type", "application/json")
            .end(new JsonArray(jsonServices).encode());
  }

  private void newServiceHandler(RoutingContext req) {
    JsonObject jsonBody = req.getBodyAsJson();
    // TODO validation
    services.put(jsonBody.getString("url"), "UNKNOWN");
    req.response()
            .putHeader("content-type", "text/plain")
            .end(STATUS_OK);
  }

  private void deleteServiceHandler(RoutingContext req) {
    JsonObject jsonBody = req.getBodyAsJson();
    String key = jsonBody.getString("url");
    if (StringUtils.nonEmptyString(key) && services.containsKey(key)) {
      services.remove(key);
      req.response()
              .setStatusCode(200)
              .end(STATUS_OK);
    } else {
      req.response()
              .setStatusCode(400)
              .setStatusMessage(String.format(NOT_FOUND_ERROR_MESSAGE, key))
              .end(STATUS_ERROR);;
    }
  }
}



