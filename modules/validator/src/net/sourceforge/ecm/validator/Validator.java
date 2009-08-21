package net.sourceforge.ecm.validator;

import net.sourceforge.ecm.exceptions.FedoraConnectionException;
import net.sourceforge.ecm.exceptions.FedoraIllegalContentException;
import net.sourceforge.ecm.exceptions.ObjectNotFoundException;
import net.sourceforge.ecm.repository.ValidationResult;


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
