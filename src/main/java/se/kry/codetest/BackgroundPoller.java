package se.kry.codetest;

import io.vertx.core.Future;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackgroundPoller {

  private Random random = new Random();

  public Future<List<String>> pollServices(Map<String, String> services) {
    services.forEach((url,status)-> services.put(url, random.nextBoolean() ? "OK" : "FAIL"));
    return Future.succeededFuture();
  }
}
