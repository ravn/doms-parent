package dk.statsbiblioteket.doms.ecm.repository.exceptions;

/**
 * This is the mother class of all exceptions for ECM. All exceptions will be
 * regarded as EcmExceptions for purposes of formatting them for user
 * comsumption.
 * <br/>
 * The class is abstract, as no code should ever throw just an EcmException.
 * If something fails, and you really do not know what to throw, throw an
 * UnknownException
 *
 * @see dk.statsbiblioteket.doms.ecm.repository.exceptions.UnknownException
 */
public abstract class EcmException extends Exception {

    public EcmException() {
    }

    public EcmException(String s) {
        super(s);
    }

    public EcmException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public EcmException(Throwable throwable) {
        super(throwable);
    }


}
