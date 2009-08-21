package net.sourceforge.ecm.exceptions;

/**
 * Thrown when unable to complete a fedora operation due to some connection
 * problem. This is like an IOException for speaking with the Fedora server
 * over whatever connector is chosen
 */
public class FedoraConnectionException extends EcmException{
    public FedoraConnectionException() {
    }

    public FedoraConnectionException(String s) {
        super(s);
    }

    public FedoraConnectionException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public FedoraConnectionException(Throwable throwable) {
        super(throwable);
    }
}
