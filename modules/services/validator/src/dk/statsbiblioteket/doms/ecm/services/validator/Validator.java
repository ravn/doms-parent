package dk.statsbiblioteket.doms.ecm.services.validator;

import dk.statsbiblioteket.doms.ecm.repository.ValidationResult;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException;


/**
 * Interface to the validators
 */
public interface Validator {

    /**
     * Validate the object pid against the compound content model cm
     * @param pid the object to validate
     * @param cm the content model with the specification to validate against
     * @param fedoraConnector
     * @return a validation result
     * @throws FedoraConnectionException
     * @throws FedoraIllegalContentException
     * @throws ObjectNotFoundException
     */
    ValidationResult validate(String pid, CompoundContentModel cm, FedoraConnector fedoraConnector) throws
                                                                   FedoraConnectionException,
                                                                   FedoraIllegalContentException,
                                                                   ObjectNotFoundException;
}
