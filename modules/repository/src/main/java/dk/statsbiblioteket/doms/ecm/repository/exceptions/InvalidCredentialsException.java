package dk.statsbiblioteket.doms.ecm.repository.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Sep 6, 2010
 * Time: 10:17:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class InvalidCredentialsException extends EcmException{
    public InvalidCredentialsException(String s) {
        super(s);
        statusCode = 401;
    }

    public InvalidCredentialsException(String s, Throwable throwable) {
        super(s, throwable);
        statusCode = 401;
    }
}
