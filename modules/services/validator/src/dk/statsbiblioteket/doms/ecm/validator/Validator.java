package dk.statsbiblioteket.doms.ecm.validator;

import dk.statsbiblioteket.doms.ecm.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.exceptions.ObjectNotFoundException;
import dk.statsbiblioteket.doms.ecm.repository.ValidationResult;


/**
 * Interface to the validators
 */
public interface Validator {

    /**
     * Validate the object pid against the compound content model cm
     * @param pid the object to validate
     * @param cm the content model with the specification to validate against
     * @return a validation result
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     */
    ValidationResult validate(String pid, CompoundContentModel cm) throws
                                                                   FedoraConnectionException,
                                                                   FedoraIllegalContentException,
                                                                   ObjectNotFoundException;
}
