package se.kry.codetest.cache;

import io.vertx.core.json.JsonArray;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class ServicesCache {

    Map<String, JsonArray> cachedServices;

    public ServicesCache(Map<String, JsonArray> services) {
        cachedServices = services;
    }

    public Collection<JsonArray> getCachedServices() {
        return new LinkedList<>(cachedServices.values());
    }

    public void putService(String name, JsonArray service) {
        cachedServices.put(name, service);
    }

    public void removeService(String name) {
        cachedServices.remove(name);
    }

    public boolean contains(String name) {
        return cachedServices.containsKey(name);
    }
}
