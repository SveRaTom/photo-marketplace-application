package photomarketplace.customoffer.exception;

import java.util.UUID;

public class CustomOfferNotFoundException extends RuntimeException {

    public CustomOfferNotFoundException(final UUID customOfferId) {
        super("Custom offer request with id '%s' does not exist.".formatted(customOfferId));
    }
}
