package net.sourceforge.ecm.exceptions;

/**
 * Exception thrown when an operation attempts to use a object as something it
 * is not.
 * <br/>
 * Examples include:
 * <ul>
 * <li>Using a content model as a data object
 * <li>Using a data object as a content model
 * <li>Using a normal data object as a template
 * <li>Using a content model as a template
 * </ul>
 *
 * This exception is, of course, only thrown if the object is found, and it's
 * type can be determined 
 * @see net.sourceforge.ecm.exceptions.ObjectNotFoundException
 */
public class ObjectIsWrongTypeException extends EcmException{
    public ObjectIsWrongTypeException() {
    }

    public ObjectIsWrongTypeException(String s) {
        super(s);
    }

    public ObjectIsWrongTypeException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ObjectIsWrongTypeException(Throwable throwable) {
        super(throwable);
    }
}
