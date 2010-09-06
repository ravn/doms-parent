package dk.statsbiblioteket.doms.ecm.repository.exceptions;


/**
 * Exception thrown when an operation is performed on a pid, that do not
 * correspond to an object in the repository.
 */
public class ObjectNotFoundException extends EcmException {

    public ObjectNotFoundException(String s) {
        super(s);
        statusCode = 404;
    }

    public ObjectNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
        statusCode = 404;
    }

}
