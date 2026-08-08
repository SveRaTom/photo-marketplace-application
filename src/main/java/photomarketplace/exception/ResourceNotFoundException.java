package photomarketplace.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(final String message) {
        super(message);
    }

    public ResourceNotFoundException(final String resourceName, final UUID resourceId) {
        super("%s with id '%s' does not exist.".formatted(resourceName, resourceId));
    }
}
