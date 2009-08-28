package dk.statsbiblioteket.doms.ecm.repository.exceptions;

/**
 * Exception thrown when a fedora operation fails in ways not properly
 * documented in the fedora api.
 *
 * This class have been marked as deprecated, as it is really only a placeholder
 * untill the fedora workings can be properly explored.
 */
@Deprecated
public class UnknownException extends EcmException{
    public UnknownException() {
    }

    public UnknownException(String s) {
        super(s);
    }

    public UnknownException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public UnknownException(Throwable throwable) {
        super(throwable);
    }
}
