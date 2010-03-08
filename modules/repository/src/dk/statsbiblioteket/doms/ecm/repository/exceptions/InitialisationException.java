package dk.statsbiblioteket.doms.ecm.repository.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Mar 8, 2010
 * Time: 5:32:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class InitialisationException extends EcmException{

    public InitialisationException() {
    }

    public InitialisationException(String s) {
        super(s);
    }

    public InitialisationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InitialisationException(Throwable throwable) {
        super(throwable);
    }
}
