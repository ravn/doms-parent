package net.sourceforge.ecm.exceptions;

/**
 * This exception is a wrapper for all xml parsing exceptions. If an operation
 * requires that a datastream is parsed as xml, and the datastream does not
 * contain valid xml, this exception is thrown.
 * <br/>
 * Is also thrown when Trippi fails to parse the return from a call to the
 * Resource index
 *
 */
public class FedoraIllegalContentException extends EcmException {
    public FedoraIllegalContentException() {
    }

    public FedoraIllegalContentException(String s) {
        super(s);
    }

    public FedoraIllegalContentException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public FedoraIllegalContentException(Throwable throwable) {
        super(throwable);
    }
}
