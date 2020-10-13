package se.kry.codetest.poller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import se.kry.codetest.cache.ServicesCache;
import se.kry.codetest.service.ServiceStatus;
import se.kry.codetest.service.UrlService;

import java.util.Collection;

import static se.kry.codetest.datasource.DBConnectorVerticle.DBCONNECTOR_QUEUE;

public class BackgroundPollerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundPollerVerticle.class);

  private ServicesCache servicesCache;
  private UrlService urlService;
  private WebClient webClient;

  public BackgroundPollerVerticle(ServicesCache servicesCache) {
    this.servicesCache = servicesCache;
  }

  @Override
  public void start(Future<Void> startFuture) {
    webClient = WebClient.create(vertx, new WebClientOptions()
            .setTrustAll(true));
    urlService = UrlService.createProxy(vertx, DBCONNECTOR_QUEUE);
    vertx.setPeriodic(1000 * 60, timerId -> this.pollServices());
  }

  private void pollServices() {
    Collection<JsonArray> services = servicesCache.getCachedServices();
    LOGGER.info("Started polling services..");
    for (JsonArray service : services) {
      webClient.get( service.getInteger(2), service.getString(1), "")
        .timeout(3000)
        .send(ar -> {
          if (ar.succeeded()) {
            LOGGER.info(String.format("Service %s with url %s:%d which was added on %s was polled.",
                    service.getString(0),
                    service.getString(1),
                    service.getInteger(2),
                    service.getLong(3)));
            if (ServiceStatus.valueOf(service.getString(4)) == ServiceStatus.DEAD) {
              updateServiceStatus(service, ServiceStatus.ALIVE);
            }
          } else if (ServiceStatus.valueOf(service.getString(4)) == ServiceStatus.ALIVE) {
            LOGGER.info(String.format("Service %s with url %s:%d which was added on %s was polled but no response.",
                    service.getString(0),
                    service.getString(1),
                    service.getInteger(2),
                    service.getLong(3)));
            updateServiceStatus(service, ServiceStatus.DEAD);
          } else {
            LOGGER.error(ar.cause().getMessage());
          }
        });
    }
  }

  private void updateServiceStatus(JsonArray service, ServiceStatus status) {
    Future<Void> updateStatusFutute = Future.future();
    urlService.updateStatus(service.getString(0), status, updateStatusFutute);
    updateStatusFutute.compose(r -> {
      String name = service.getString(0);
      JsonArray updatedService = new JsonArray()
              .add(name)
              .add(service.getString(1))
              .add(service.getInteger(2))
              .add(service.getLong(3))
              .add(status);
      servicesCache.putService(name, updatedService);
      return Future.succeededFuture();
    });
  }
}
