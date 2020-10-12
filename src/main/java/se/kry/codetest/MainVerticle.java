package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import se.kry.codetest.cache.ServicesCache;
import se.kry.codetest.datasource.DBConnectorVerticle;
import se.kry.codetest.http.HttpServerVerticle;
import se.kry.codetest.service.UrlService;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static se.kry.codetest.datasource.DBConnectorVerticle.DBCONNECTOR_QUEUE;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {
    Future<String> dbVerticleDeployment = Future.future();
    vertx.deployVerticle(new DBConnectorVerticle(), dbVerticleDeployment);

    dbVerticleDeployment.compose(r -> {
      UrlService urlService = UrlService.createProxy(vertx, DBCONNECTOR_QUEUE);
      Future<List<JsonArray>> services = Future.future();
      urlService.fetchAllServices(services);
      return services.compose(s -> {
        Future<String> httpVerticleDeployment = Future.future();
        ServicesCache servicesCache = new ServicesCache(s.stream().collect(Collectors.toMap(this::getNameFromJsonArray, Function.identity())));
        vertx.deployVerticle(new HttpServerVerticle(servicesCache), httpVerticleDeployment);
        return httpVerticleDeployment;
      });
    });
  }

  private String getNameFromJsonArray(JsonArray array) {
    return array.getString(0);
  }

}



