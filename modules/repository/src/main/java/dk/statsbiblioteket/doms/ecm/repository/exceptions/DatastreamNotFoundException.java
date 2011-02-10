package dk.statsbiblioteket.doms.ecm.repository.exceptions;

/**
 * Exception thrown when an operation is performed on a datastream, that do
 * not exist. If the object itself does not exist, a ObjectNotFoundException
 * is thrown instead.
 *
 * @see dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException
 */
public class DatastreamNotFoundException extends EcmException{


    public DatastreamNotFoundException(String s) {
        super(s);
        statusCode = 404;
    }

    public DatastreamNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
        statusCode = 404;
    }

}
