package dk.statsbiblioteket.doms.ecm.validator;

import fedora.server.errors.ServerException;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Oct 30, 2008
 * Time: 1:02:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class ValidationFailedException extends ServerException{
    /**
     * Constructs a new ServerException.
     *
     * @param bundleName The bundle in which the message resides.
     * @param code       The identifier for the message in the bundle, aka the key.
     * @param values     Replacements for placeholders in the message, where placeholders
     *                   are of the form {num} where num starts at 0, indicating the 0th
     *                   (1st) item in this array.
     * @param details    Identifiers for messages which provide detail on the error. This
     *                   may empty or null.
     * @param cause      The underlying exception if known, null meaning unknown or none.
     */
    public ValidationFailedException(String bundleName, String code, String[] values, String[] details, Throwable cause) {
        super(bundleName, code, values, details, cause);
    }
}
