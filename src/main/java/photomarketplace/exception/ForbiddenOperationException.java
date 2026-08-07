package photomarketplace.exception;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(final String message) {
        super(message);
    }
}
