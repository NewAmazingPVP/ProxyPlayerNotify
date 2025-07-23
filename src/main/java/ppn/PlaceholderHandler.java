package ppn;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Simple abstraction for placeholder processing.
 */
public interface PlaceholderHandler {
    CompletableFuture<String> format(String message, UUID uuid);

    static PlaceholderHandler noop() {
        return (message, uuid) -> CompletableFuture.completedFuture(message);
    }
}
