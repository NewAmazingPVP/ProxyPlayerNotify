package ppn;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Placeholder handler backed by PAPIProxyBridge via reflection to keep the dependency optional.
 */
public class PAPIPlaceholderHandler implements PlaceholderHandler {

    private final Object api;
    private final Method formatMethod;

    public PAPIPlaceholderHandler() throws Exception {
        Class<?> apiClass = Class.forName("net.william278.papiproxybridge.api.PlaceholderAPI");
        this.api = apiClass.getMethod("createInstance").invoke(null);
        this.formatMethod = apiClass.getMethod("formatPlaceholders", String.class, UUID.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<String> format(String message, UUID uuid) {
        try {
            return (CompletableFuture<String>) formatMethod.invoke(api, message, uuid);
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(message);
        }
    }
}
