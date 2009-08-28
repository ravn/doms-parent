package dk.statsbiblioteket.doms.ecm.repository.exceptions;

/**
 * Exception thrown if the ontology in the content model cannot be parsed as
 * such. A subclass of FedoraIllegalContentException, as it is a more specific
 * demand on the contents of a datastream.
 * <br/>
 * This exception will only be thrown if the contents of the datastream was
 * found to be valid xml. If not, it will never be attempted to be parsed as
 * a ontology.
 *
 * @see dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException
 */
public class InvalidOntologyException extends FedoraIllegalContentException {
    public InvalidOntologyException() {
    }

    public InvalidOntologyException(String s) {
        super(s);
    }

    public InvalidOntologyException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidOntologyException(Throwable throwable) {
        super(throwable);
    }
}
