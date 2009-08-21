package net.sourceforge.ecm.exceptions;


/**
 * Exception thrown when an operation is performed on a pid, that do not
 * correspond to an object in the repository.
 */
public class ObjectNotFoundException extends EcmException {
    public ObjectNotFoundException() {
    }

    public ObjectNotFoundException(String s) {
        super(s);
    }

    public ObjectNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ObjectNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
