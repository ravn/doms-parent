package net.sourceforge.ecm.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: May 19, 2009
 * Time: 11:10:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class PIDGeneratorException extends EcmException {

    public PIDGeneratorException() {
    }

    public PIDGeneratorException(String s) {
        super(s);
    }

    public PIDGeneratorException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public PIDGeneratorException(Throwable throwable) {
        super(throwable);
    }
}
