package dk.statsbiblioteket.doms.ecm.repository.exceptions;


/**
 * A development class. Methods not implemented should throw this exception. As
 * such, they will be easy to find via a global search.
 *
 *<br/>
 * This class should not be used in a released version
 */
public class NotImplementedYetException extends RuntimeException {


    public NotImplementedYetException(String s) {
        super(s);
    }

    public NotImplementedYetException(String s, Throwable throwable) {
        super(s, throwable);
    }

}
