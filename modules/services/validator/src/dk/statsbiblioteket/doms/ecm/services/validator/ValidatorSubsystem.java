package dk.statsbiblioteket.doms.ecm.services.validator;

import dk.statsbiblioteket.doms.ecm.repository.ValidationResult;
import dk.statsbiblioteket.doms.ecm.repository.FedoraConnector;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraConnectionException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.FedoraIllegalContentException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectIsWrongTypeException;
import dk.statsbiblioteket.doms.ecm.repository.exceptions.ObjectNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * General interface to the validator subsystem.
 */
public class ValidatorSubsystem {
    private static final Log LOG = LogFactory.getLog(ValidatorSubsystem.class);


    /**
     * Validate one object against it's content models
     * @param objpid the data object to validate
     * @param fedoraConnector
     * @return a validation result of the validation
     * @throws ObjectNotFoundException if the objpid cannot be found
     */
    public ValidationResult validate(
            String objpid,
            FedoraConnector fedoraConnector) throws ObjectNotFoundException, FedoraIllegalContentException,
             FedoraConnectionException, ObjectIsWrongTypeException {

        LOG.trace("Entering validate(), objpid='" + objpid + "'");



        CompoundContentModel compoundContentModel;

        compoundContentModel = CompoundContentModel.loadFromDataObject(objpid,fedoraConnector);
        LOG.trace("Compound content model created");

        Validator[] validators = {new OntologyValidator(),
                                  new DatastreamValidator()};
        LOG.trace("Validators created");

        ValidationResult globalResult = new ValidationResult();

        for (Validator validator: validators){
            ValidationResult result = null;
            result = validator.validate(
                    objpid,
                    compoundContentModel,
                    fedoraConnector);
            LOG.trace("validator validated");
            globalResult = globalResult.combine(result);
        }
        LOG.trace("All validators have run, returning");

        return globalResult;


    }

    /**
     * Validate the given object against a specific content model
     * @param objpid the given object
     * @param cmpid the specific content model
     * @param fedoraConnector
     * @return a Validation result of the validation
     */
    public ValidationResult validateAgainst(
            String objpid,
            String cmpid,
            FedoraConnector fedoraConnector) throws ObjectNotFoundException, FedoraIllegalContentException,
             FedoraConnectionException, ObjectIsWrongTypeException {

        //Working
        LOG.trace("Entering validateAgainst(), objpid='" + objpid
                  + "', cmpid='"+cmpid+"'");


        LOG.trace("doms user token made");

        CompoundContentModel compoundContentModel;

        compoundContentModel = CompoundContentModel.loadFromContentModel(cmpid,fedoraConnector);

        LOG.trace("Compound content model created");

        Validator[] validators = {new OntologyValidator(), new DatastreamValidator()};
        LOG.trace("Validators created");

        ValidationResult global_result = new ValidationResult();

        for (Validator validator: validators){
            ValidationResult result = validator.validate(objpid, compoundContentModel, fedoraConnector);
            LOG.trace("validator validated");
            global_result = global_result.combine(result);
        }
        LOG.trace("All validators run, returning");


        return global_result;


    }


}
