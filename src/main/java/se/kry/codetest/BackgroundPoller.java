package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class BackgroundPoller {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundPoller.class);

  public Future<List<String>> pollServices(Collection<JsonArray> services) {
    for (JsonArray service : services) {
      LOGGER.info(String.format("Service %s with url %s which was added on %s was polled.",
              service.getString(0),
              service.getString(1),
              service.getLong(2)));
    }
    return Future.succeededFuture();
  }
}
