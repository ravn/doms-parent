package dk.statsbiblioteket.doms.ecm.exceptions;

/**
 * Exception thrown when an operation is performed on a datastream, that do
 * not exist. If the object itself does not exist, a ObjectNotFoundException
 * is thrown instead.
 *
 * @see dk.statsbiblioteket.doms.ecm.exceptions.ObjectNotFoundException
 */
public class DatastreamNotFoundException extends EcmException{

    public DatastreamNotFoundException() {
    }

    public DatastreamNotFoundException(String s) {
        super(s);
    }

    public DatastreamNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DatastreamNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
